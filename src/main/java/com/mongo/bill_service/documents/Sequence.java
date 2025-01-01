package com.mongo.bill_service.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Document(collection = "sequence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Sequence {

	@Id
	private String name;
	private int seq;
}
