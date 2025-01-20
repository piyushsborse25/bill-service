package com.mongo.bill_service.repos;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mongo.bill_service.documents.MyFile;

@Repository
public interface FileRepository extends MongoRepository<MyFile, Integer> {

}
