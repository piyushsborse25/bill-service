package com.mongo.bill_service.entities;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Split {

	private String name;
	private double split;
	private int itemcount;
	private List<String> items = new ArrayList<String>();
	
}
