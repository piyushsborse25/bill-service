package com.mongo.bill_service.entities;

import java.util.Collection;

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
	private double total;

}
