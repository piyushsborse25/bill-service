package com.mongo.bill_service.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.mongo.bill_service.documents.BillDetails;
import com.mongo.bill_service.documents.Item;
import com.mongo.bill_service.repos.SequenceRepository;

@Component
public class BillListener extends AbstractMongoEventListener<BillDetails> {

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	SequenceRepository sequenceRepository;

	@Override
	public void onBeforeConvert(BeforeConvertEvent<BillDetails> event) {
		if (event.getSource().getBillId() < 1) {
			int billId = sequenceRepository.getSequence(BillDetails.SEQUENCE);
			event.getSource().setBillId(billId);

			for (Item item : event.getSource().getItems()) {
				item.setItemId(sequenceRepository.getSequence(Item.SEQUENCE));
			}
		} else {
			for (Item item : event.getSource().getItems()) {
				if (item.getItemId() < 1) {
					item.setItemId(sequenceRepository.getSequence(Item.SEQUENCE));
				}
			}
		}
		super.onBeforeConvert(event);
	}
}
