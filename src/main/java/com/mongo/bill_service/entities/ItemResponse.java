package com.mongo.bill_service.entities;

import java.util.ArrayList;
import java.util.List;

import com.mongo.bill_service.documents.Item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ItemResponse {

	private List<Item> items = new ArrayList<Item>();

}
