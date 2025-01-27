package com.mongo.bill_service.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongo.bill_service.consts.Consts;
import com.mongo.bill_service.documents.BillDetails;
import com.mongo.bill_service.documents.Item;
import com.mongo.bill_service.documents.MyFile;
import com.mongo.bill_service.entities.ItemResponse;
import com.mongo.bill_service.entities.Split;
import com.mongo.bill_service.exception.BillException;
import com.mongo.bill_service.repos.BillRepository;
import com.mongo.bill_service.repos.FileRepository;
import com.mongo.bill_service.repos.SequenceRepository;
import com.mongo.bill_service.serializers.DoubleRoundOffSerializer;

@RestController
public class BillController {

	@Autowired
	BillRepository billRepository;

	@Autowired
	FileRepository fileRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	SequenceRepository sequenceRepository;

	@GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String root() {
		return Consts.welcomeHTML;
	}

	@GetMapping(path = "/bills")
	public List<BillDetails> find() {

		return billRepository.findAll();
	}

	@GetMapping(path = "/bill/{billId}")
	public BillDetails getBillById(@PathVariable Integer billId) {

		BillDetails billDetails = billRepository.findById(billId).get();
		String dateStr = billDetails.getBillDate();

		LocalDate dateTime = null;

		try {
			dateTime = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		} catch (Exception e) {
			dateTime = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		}
		String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		billDetails.setBillDate(formatted);
		return billDetails;
	}

	@DeleteMapping(path = "/bill/{billId}")
	public boolean deleteBillById(@PathVariable Integer billId) {
		try {
			billRepository.deleteById(billId);
			return true;
		} catch (Exception e) {
			System.out.println("Invalid Bill Id");
		}

		return false;
	}

	@GetMapping(path = "/bill/{billId}/items")
	public List<ItemResponse> items(@PathVariable Integer billId) {

		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.project("items").andExclude("_id"));

		AggregationResults<ItemResponse> result = mongoTemplate.aggregate(aggregation, "billRepo", ItemResponse.class);

		return result.getMappedResults();
	}

	@GetMapping(path = "/bill/{billId}/person/{person}/items")
	public List<Item> items(@PathVariable Integer billId, @PathVariable String person) {

		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.unwind("items"),
				Aggregation.match(Criteria.where("items.participants")
						.regex(Pattern.compile("^" + person + "$", Pattern.CASE_INSENSITIVE))),
				Aggregation.project("items._id", "items.name", "items.quantity", "items.rate", "items.value",
						"items.participants"));

		AggregationResults<Item> result = mongoTemplate.aggregate(aggregation, "billRepo", Item.class);

		return result.getMappedResults();
	}

	@GetMapping(path = "/bill/{billId}/item/{itemId}")
	public List<Item> items(@PathVariable Integer billId, @PathVariable int itemId) {

		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.unwind("items"), Aggregation.match(Criteria.where("items._id").is(itemId)),
				Aggregation.project("items._id", "items.name", "items.quantity", "items.rate", "items.value",
						"items.participants"));

		AggregationResults<Item> result = mongoTemplate.aggregate(aggregation, "billRepo", Item.class);

		return result.getMappedResults();
	}

	@GetMapping(path = "/bill/{billId}/split")
	public List<Split> split(@PathVariable("billId") Integer id) {

		BillDetails bill = billRepository.findById(id).get();

		Map<String, Split> split = new HashMap<String, Split>();
		for (Item item : bill.getItems()) {
			int totalP = item.getParticipants().size();
			for (String participant : item.getParticipants()) {
				double newValue = (item.getValue() * 1.0) / totalP;
				newValue = DoubleRoundOffSerializer.roundDouble(newValue);

				Split sp = new Split(participant, newValue, 1);
				split.merge(participant, sp, BillController::mergeSplit);
			}
		}

		return new ArrayList<Split>(split.values());
	}

	@PostMapping(path = "/bill/save")
	public BillDetails save(@RequestBody BillDetails searchRequest) {

		BillDetails result = null;
		try {
			double sum = searchRequest.getItems().stream().map(t -> String.valueOf(t.getValue()))
					.mapToDouble(Double::valueOf).sum();
			int quant = searchRequest.getItems().stream().map(t -> String.valueOf(t.getQuantity()))
					.mapToInt(Integer::valueOf).sum();
			Set<String> participants = searchRequest.getItems().stream().map(t -> t.getParticipants())
					.flatMap(t -> t.stream()).distinct().collect(Collectors.toSet());
			int totalItems = searchRequest.getItems().size();
			searchRequest.setParticipants(participants);
			searchRequest.setTotalValue(sum);
			searchRequest.setTotalQuantity(quant);
			searchRequest.setTotalItems(totalItems);
			result = billRepository.save(searchRequest);
		} catch (Exception e) {
			throw new BillException("ERRO1",
					"Invalid bill format: Missing or incorrect attributes. Please review and resubmit.");
		}

		return result;
	}

	@PostMapping("/file/upload")
	public void uploadFile(@RequestParam(value = "file") MultipartFile fileToUpload) {

		try {
			MyFile file = new MyFile();
			file.setFileName(fileToUpload.getOriginalFilename());
			file.setContentType(fileToUpload.getContentType());
			file.setContentLength(fileToUpload.getBytes().length);
			file.setData(fileToUpload.getBytes());

			fileRepository.save(file);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@GetMapping("/file/{fileId}/download")
	public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Integer fileId) {

		MyFile downFile = null;

		Optional<MyFile> file = fileRepository.findById(fileId);
		downFile = file.isPresent() ? file.get() : null;

		if (downFile != null) {
			ByteArrayResource resource = new ByteArrayResource(downFile.getData());

			return ResponseEntity.status(HttpStatus.OK)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + downFile.getFileName())
					.contentType(MediaType.valueOf(downFile.getContentType()))
					.contentLength(downFile.getContentLength()).body(resource);
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ByteArrayResource(null));
	}

	@GetMapping(path = "/bill/{billId}/download")
	public ResponseEntity<ByteArrayResource> downloadFormattedExcel(@PathVariable Integer billId) {

		BillDetails billDetails = getBillById(billId);

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Bill Details");

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
			headerStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle borderStyle = workbook.createCellStyle();
			borderStyle.setBorderTop(BorderStyle.THIN);
			borderStyle.setBorderBottom(BorderStyle.THIN);
			borderStyle.setBorderLeft(BorderStyle.THIN);
			borderStyle.setBorderRight(BorderStyle.THIN);
			borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle boldStyle = workbook.createCellStyle();
			Font boldFont = workbook.createFont();
			boldFont.setBold(true);
			boldStyle.setFont(boldFont);

			int rowNum = 0;
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue("Bill Summary");
			row.getCell(0).setCellStyle(boldStyle);

			sheet.createRow(rowNum++).createCell(0).setCellValue("Bill Id: " + billDetails.getBillId());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Store: " + billDetails.getStore());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Address: " + billDetails.getAddress());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Phone: " + billDetails.getPhone());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Bill Number: " + billDetails.getBillNumber());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Bill Date: " + billDetails.getBillDate());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Time: " + billDetails.getTime());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Cashier: " + billDetails.getCashier());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Paid By: " + billDetails.getPaidBy());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Total Items: " + billDetails.getTotalItems());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Total Quantity: " + billDetails.getTotalQuantity());
			sheet.createRow(rowNum++).createCell(0).setCellValue("Total Value: " + billDetails.getTotalValue());

			sheet.createRow(rowNum++).createCell(0)
					.setCellValue("Participants: " + String.join(", ", billDetails.getParticipants()));

			rowNum++;

			row = sheet.createRow(rowNum++);
			String[] headers = { "Item ID", "Name", "Quantity", "Rate", "Value", "Participants" };
			for (int i = 0; i < headers.length; i++) {
				Cell cell = row.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			for (Item item : billDetails.getItems()) {
				int i = 0;
				row = sheet.createRow(rowNum++);

				Cell cell0 = row.createCell(i++);
				cell0.setCellValue(item.getItemId());
				cell0.setCellStyle(borderStyle);

				Cell cell1 = row.createCell(i++);
				cell1.setCellValue(item.getName());
				cell1.setCellStyle(borderStyle);

				Cell cell2 = row.createCell(i++);
				cell2.setCellValue(item.getQuantity());
				cell2.setCellStyle(borderStyle);

				Cell cell3 = row.createCell(i++);
				cell3.setCellValue(item.getRate());
				cell3.setCellStyle(borderStyle);

				Cell cell4 = row.createCell(i++);
				cell4.setCellValue(item.getValue());
				cell4.setCellStyle(borderStyle);

				Cell cell5 = row.createCell(i++);
				cell5.setCellValue(String.join(", ", item.getParticipants()));
				cell5.setCellStyle(borderStyle);
			}

			for (int j = 0; j < headers.length; j++) {
				sheet.autoSizeColumn(j);
			}

			addSheetForSplits(workbook, "Splits", split(billId));
			for (String person : billDetails.getParticipants()) {
				addSheet(workbook, person, items(billId, person), false);
			}

			workbook.write(out);

			byte[] data = out.toByteArray();
			ByteArrayResource resource = new ByteArrayResource(data);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
			String timestamp = LocalDateTime.now().format(formatter);
			String fileName = "BILL_" + timestamp + ".xlsx";
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
					.contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(data.length).body(resource);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	public static Sheet addSheet(Workbook workbook, String sheetName, List<Item> items, boolean forItems) {
		try {

			// Create a sheet
			Sheet sheet = workbook.createSheet(sheetName);

			// Create header style
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			// Create border style
			CellStyle borderStyle = workbook.createCellStyle();
			borderStyle.setBorderTop(BorderStyle.THIN);
			borderStyle.setBorderBottom(BorderStyle.THIN);
			borderStyle.setBorderLeft(BorderStyle.THIN);
			borderStyle.setBorderRight(BorderStyle.THIN);
			borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			// Add header row for main table
			String[] itemHeaders = { "Item ID", "Name", "Quantity", "Rate", "Value", "Participants" };
			String[] splitHeaders = { "Item ID", "Name", "Quantity", "Rate", "Value", "Your Half", "Participants" };

			String[] headers = forItems ? itemHeaders : splitHeaders;

			Row headerRow = sheet.createRow(3);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// Add item data starting from row 3
			int rowNum = 4;
			double totalHalf = 0;
			double totalValue = 0;
			for (Item item : items) {
				Row row = sheet.createRow(rowNum++);

				int colNum = 0;

				Cell cell1 = row.createCell(colNum++);
				cell1.setCellValue(item.getItemId());
				cell1.setCellStyle(borderStyle);

				Cell cell2 = row.createCell(colNum++);
				cell2.setCellValue(item.getName());
				cell2.setCellStyle(borderStyle);

				Cell cell3 = row.createCell(colNum++);
				cell3.setCellValue(item.getQuantity());
				cell3.setCellStyle(borderStyle);

				Cell cell4 = row.createCell(colNum++);
				cell4.setCellValue(item.getRate());
				cell4.setCellStyle(borderStyle);

				Cell cell5 = row.createCell(colNum++);
				cell5.setCellValue(item.getValue());
				cell5.setCellStyle(borderStyle);

				double currHalf = DoubleRoundOffSerializer
						.roundDouble((item.getValue() / item.getParticipants().size()));
				totalValue += item.getValue();
				totalHalf += currHalf;

				if (!forItems) {
					Cell cell6 = row.createCell(colNum++);
					cell6.setCellValue(currHalf);
					cell6.setCellStyle(borderStyle);
				}

				Cell cell7 = row.createCell(colNum++);
				cell7.setCellValue(String.join(", ", item.getParticipants()));
				cell7.setCellStyle(borderStyle);
			}

			// Calculate the total value of all items
			double finalTotal = DoubleRoundOffSerializer.roundDouble(forItems ? totalValue : totalHalf);

			// Add row for Sheet Name
			Row totalRow1 = sheet.createRow(0);
			Cell totalLabelCell1 = totalRow1.createCell(0);
			totalLabelCell1.setCellValue("Name: ");
			totalLabelCell1.setCellStyle(headerStyle);
			Cell totalValueCell1 = totalRow1.createCell(1);
			totalValueCell1.setCellValue(sheetName);

			// Add row for Total Value
			Row totalRow = sheet.createRow(1);
			Cell totalLabelCell = totalRow.createCell(0);
			totalLabelCell.setCellValue("Total Value: ");
			totalLabelCell.setCellStyle(headerStyle);
			Cell totalValueCell = totalRow.createCell(1);
			totalValueCell.setCellValue("₹ " + finalTotal);

			// Auto-size columns
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			return sheet;

		} catch (Exception e) {
			return null;
		}
	}

	public static Sheet addSheetForSplits(Workbook workbook, String sheetName, List<Split> splits) {
		try {

			Sheet sheet = workbook.createSheet(sheetName);

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle borderStyle = workbook.createCellStyle();
			borderStyle.setBorderTop(BorderStyle.THIN);
			borderStyle.setBorderBottom(BorderStyle.THIN);
			borderStyle.setBorderLeft(BorderStyle.THIN);
			borderStyle.setBorderRight(BorderStyle.THIN);
			borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			double totalAmount = splits.stream().map(t -> t.getSplit()).mapToDouble(Double::valueOf).sum();
			totalAmount = DoubleRoundOffSerializer.roundDouble(totalAmount);

			// Add row for Sheet Name
			Row totalRow1 = sheet.createRow(0);
			Cell totalLabelCell1 = totalRow1.createCell(0);
			totalLabelCell1.setCellValue("Total: ");
			totalLabelCell1.setCellStyle(headerStyle);
			Cell totalValueCell1 = totalRow1.createCell(1);
			totalValueCell1.setCellValue("₹ " + totalAmount);

			String[] headers = { "Name", "Split Amount", "Item Count" };
			Row headerRow = sheet.createRow(2);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			int rowNum = 3;
			for (Split split : splits) {
				Row row = sheet.createRow(rowNum++);

				int colNum = 0;

				Cell cell1 = row.createCell(colNum++);
				cell1.setCellValue(split.getName());
				cell1.setCellStyle(borderStyle);

				Cell cell2 = row.createCell(colNum++);
				cell2.setCellValue(split.getSplit());
				cell2.setCellStyle(borderStyle);

				Cell cell3 = row.createCell(colNum++);
				cell3.setCellValue(split.getItemcount());
				cell3.setCellStyle(borderStyle);
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			return sheet;

		} catch (Exception e) {
			return null;
		}
	}

	@PostMapping(path = "/items/download", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ByteArrayResource> downloadItemExcel(@RequestBody List<Item> items) {

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			addSheet(workbook, "ITEMS", items, true);

			workbook.write(out);

			byte[] data = out.toByteArray();
			ByteArrayResource resource = new ByteArrayResource(data);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
			String timestamp = LocalDateTime.now().format(formatter);
			String fileName = "ITEMS_" + timestamp + ".xlsx";
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
					.contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(data.length).body(resource);

		} catch (IOException e) {
			return ResponseEntity.status(500).build();
		}
	}

	public static Split mergeSplit(Split old, Split latest) {
		old.setSplit(old.getSplit() + latest.getSplit());
		old.setItemcount(old.getItemcount() + latest.getItemcount());
		return old;
	}
}