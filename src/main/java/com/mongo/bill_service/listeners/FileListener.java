package com.mongo.bill_service.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.mongo.bill_service.documents.MyFile;
import com.mongo.bill_service.repos.SequenceRepository;

@Component
public class FileListener extends AbstractMongoEventListener<MyFile> {

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	SequenceRepository sequenceRepository;

	@Override
	public void onBeforeConvert(BeforeConvertEvent<MyFile> event) {
		if (event.getSource().getFileId() < 1) {
			int fileId = sequenceRepository.getSequence(MyFile.SEQUENCE);
			event.getSource().setFileId(fileId);
		}
		super.onBeforeConvert(event);
	}
}
