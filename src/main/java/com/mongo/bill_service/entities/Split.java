package com.mongo.bill_service.entities;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongo.bill_service.serializers.DoubleRoundOffSerializer;

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
	
	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	private double split;
	
	private int itemcount;
	
}
