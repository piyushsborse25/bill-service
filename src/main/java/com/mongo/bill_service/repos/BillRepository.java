package com.mongo.bill_service.repos;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mongo.bill_service.documents.BillDetails;

@Repository
public interface BillRepository extends MongoRepository<BillDetails, Integer> {

}
