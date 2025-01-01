package com.mongo.bill_service.documents;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Item {

	@Transient
	public static final String SEQUENCE = "itm-seq";
	
	@Id
	private int itemId = -1;
	
	private String name;
	private int quantity;
	private double rate;
	private double value;
	private List<String> participants;

}
