package com.mongo.bill_service.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mongo.bill_service.consts.Consts;
import com.mongo.bill_service.documents.BillDetails;
import com.mongo.bill_service.documents.Item;
import com.mongo.bill_service.entities.ItemResponse;
import com.mongo.bill_service.entities.Split;
import com.mongo.bill_service.exception.BillException;
import com.mongo.bill_service.repos.BillRepository;
import com.mongo.bill_service.repos.SequenceRepository;

@RestController
public class BillController {

	@Autowired
	BillRepository billRepository;

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

		LocalDateTime dateTime = LocalDateTime.parse(dateStr,
				DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		billDetails.setBillDate(formatted);
		return billDetails;
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
				Aggregation.project("items.name", "items.quantity", "items.rate", "items.value", "items.participants"));

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
			int totalItems = searchRequest.getItems().size();
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

	@GetMapping(path = "/bill/{billId}/download")
	public ResponseEntity<ByteArrayResource> downloadFormattedExcel(@PathVariable Integer billId) {

		BillDetails billDetails = getBillById(billId);

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			// Create a sheet
			Sheet sheet = workbook.createSheet("Bill Details");

			// Create styles
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

			// Add bill summary section
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

			// Add participants
			sheet.createRow(rowNum++).createCell(0)
					.setCellValue("Participants: " + String.join(", ", billDetails.getParticipants()));

			// Add a gap
			rowNum++;

			// Add item details section
			row = sheet.createRow(rowNum++);
			String[] headers = { "Name", "Quantity", "Rate", "Value", "Participants" };
			for (int i = 0; i < headers.length; i++) {
				Cell cell = row.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			for (Item item : billDetails.getItems()) {
				int i = 0;
				row = sheet.createRow(rowNum++);

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

			// Adjust column width
			for (int j = 0; j < headers.length; j++) {
				sheet.autoSizeColumn(j);
			}

			// Add Items

			for (String person : billDetails.getParticipants()) {
				addSheet(workbook, person, items(billId, person));
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

	public static Sheet addSheet(Workbook workbook, String sheetName, List<Item> items) {
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

			// Add header row
			String[] headers = { "Item ID", "Name", "Quantity", "Rate", "Value", "Participants" };
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// Add item data
			int rowNum = 1;
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

				Cell cell6 = row.createCell(colNum++);
				cell6.setCellValue(String.join(", ", item.getParticipants()));
				cell6.setCellStyle(borderStyle);
			}

			// Adjust column widths
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

			addSheet(workbook, "ITEMS", items);
			// Write workbook to output stream
			workbook.write(out);

			// Return the response with the Excel file
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