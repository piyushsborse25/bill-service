package com.mongo.bill_service.documents;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongo.bill_service.serializers.DoubleRoundOffSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PriceBreakdown {

	public enum Types {
		DISCOUNT, TAX, EXTRA_CHARGES, CASHBACK
	}

	private String name;

	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	@Getter(value = AccessLevel.NONE)
	private double value;

	private Types type;
	
	public double getValue() {
		return DoubleRoundOffSerializer.roundDouble(value);
	}

}
