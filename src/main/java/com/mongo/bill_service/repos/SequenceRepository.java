package com.mongo.bill_service.repos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongo.bill_service.documents.Sequence;

@Repository
public class SequenceRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	private static final int INCREMENT = 5;

	public int getSequence(String collection) {
		Sequence ret = mongoTemplate.findAndModify(new Query(Criteria.where("_id").is(collection)),
				new Update().inc("seq", INCREMENT), FindAndModifyOptions.options().returnNew(true).upsert(true),
				Sequence.class);
		return ret.getSeq();
	}
}
