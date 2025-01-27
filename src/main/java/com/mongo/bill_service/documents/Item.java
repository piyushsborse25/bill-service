package com.mongo.bill_service.documents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

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
public class Item {

	@Transient
	public static final String SEQUENCE = "itm-seq";

	@Id
	private int itemId = -1;

	private String name;

	private int quantity;

	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	@Getter(value = AccessLevel.NONE)
	private double rate;

	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	@Getter(value = AccessLevel.NONE)
	private double value;

	private List<String> participants = new ArrayList<String>();

	public double getRate() {
		return DoubleRoundOffSerializer.roundDouble(rate);
	}

	public double getValue() {
		return DoubleRoundOffSerializer.roundDouble(value);
	}

}
