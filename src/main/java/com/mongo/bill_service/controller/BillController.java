package com.mongo.bill_service.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.MediaType;
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
import com.mongo.bill_service.entities.SplitResponse;
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
	public BillDetails find(@PathVariable Integer billId) {

		return billRepository.findById(billId).get();
	}
	
	@GetMapping(path = "/bill/{billId}/items")
	public List<ItemResponse> items(@PathVariable Integer billId) {

		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.project("items").andExclude("_id"));

		AggregationResults<ItemResponse> result = mongoTemplate.aggregate(aggregation, "billRepo", ItemResponse.class);

		return result.getMappedResults();
	}

	@GetMapping(path = "/bill/{billId}/person/{person}/items")
	public List<Item> items(@PathVariable Integer billId, @PathVariable String person) {

		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.unwind("items"),
				Aggregation.match(Criteria.where("items.participants")
						.regex(Pattern.compile("^" + person + "$", Pattern.CASE_INSENSITIVE))),
				Aggregation.project("items.name", "items.quantity", "items.rate", "items.value", "items.participants")
		);

		AggregationResults<Item> result = mongoTemplate.aggregate(aggregation, "billRepo", Item.class);

		return result.getMappedResults();
	}
	
	@GetMapping(path = "/bill/{billId}/item/{itemId}")
	public List<Item> items(@PathVariable Integer billId, @PathVariable int itemId) {

		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("_id").is(billId)),
				Aggregation.unwind("items"),
				Aggregation.match(Criteria.where("items._id").is(itemId)),
				Aggregation.project("items._id", "items.name", "items.quantity", "items.rate", "items.value", "items.participants")
		);

		AggregationResults<Item> result = mongoTemplate.aggregate(aggregation, "billRepo", Item.class);

		return result.getMappedResults();
	}

	@GetMapping(path = "/bill/{billId}/split", produces = MediaType.TEXT_HTML_VALUE)
	public String split(@PathVariable("billId") Integer id) {

		BillDetails bill = billRepository.findById(id).get();

		Map<String, Split> split = new HashMap<String, Split>();
		for (Item item : bill.getItems()) {
			int totalP = item.getParticipants().size();
			for (String participant : item.getParticipants()) {
				double newValue = (item.getValue() * 1.0) / totalP;

				Split sp = new Split(participant, newValue, 1, new ArrayList<String>(Arrays.asList(item.getName())));
				split.merge(participant, sp, BillController::mergeSplit);
			}
		}

		double totalBill = split.values().stream().map(t -> t.getSplit()).mapToDouble(Double::valueOf).sum();

		SplitResponse res = new SplitResponse(split.values(), totalBill);

		return Consts.getSplitHTML(res);
	}

	@PostMapping(path = "/bill/save")
	public BillDetails save(@RequestBody BillDetails searchRequest) {

		BillDetails result = null;
		try {
			result = billRepository.save(searchRequest);
		} catch (Exception e) {
			throw new BillException("ERRO1", "Invalid bill format: Missing or incorrect attributes. Please review and resubmit.");
		}

		return result;
	}
	
	public static Split mergeSplit(Split old, Split latest) {
		old.getItems().addAll(latest.getItems());
		old.setSplit(old.getSplit() + latest.getSplit());
		old.setItemcount(old.getItemcount() + latest.getItemcount());
		return old;
	}
}