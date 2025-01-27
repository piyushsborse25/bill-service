package com.mongo.bill_service.entities;

import java.util.Collection;

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
public class SplitResponse {

	private Collection<Split> details;
	
	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	private double total;

}
