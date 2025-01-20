package com.mongo.bill_service.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Document(collection = "filesRepo")
@Data
@NoArgsConstructor		
@AllArgsConstructor
@ToString
public class MyFile {

	@Transient
	public static final String SEQUENCE = "file-seq";

	@Id
	private int fileId;
	private String fileName;
    private String contentType;
    private int contentLength;
    private byte[] data;
}
