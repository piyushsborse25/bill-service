package com.mongo.bill_service.documents;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
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
	private List<Item> items;
	private int totalItems;
	private int totalQuantity;
	private double totalValue;
}
