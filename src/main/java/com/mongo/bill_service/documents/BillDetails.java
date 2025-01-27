package com.mongo.bill_service.documents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongo.bill_service.serializers.DoubleRoundOffSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Document(collection = "billRepo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BillDetails {

	@Transient
	public static final String SEQUENCE = "bill-seq";

	@Id
	private int billId = -1;

	private String store;
	private String address;
	private String phone;
	private String billNumber;
	private String billDate;
	private String time;
	private String cashier;
	private List<Item> items = new ArrayList<Item>();
	private Set<String> participants = new HashSet<String>();
	private String paidBy;
	private int totalItems;
	private int totalQuantity;

	@JsonSerialize(using = DoubleRoundOffSerializer.class)
	@Getter(value = AccessLevel.NONE)
	private double totalValue;

	public double getTotalValue() {
		return DoubleRoundOffSerializer.roundDouble(totalValue);
	}

}
