package com.mongo.bill_service.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DoubleRoundOffSerializer extends JsonSerializer<Double> {

	@Override
	public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value != null) {
			gen.writeNumber(roundDouble(value));
		}
	}

	public static double roundDouble(Double value) {
		BigDecimal roundedValue = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
		return roundedValue.doubleValue();
	}

	public static double roundDouble(Integer value) {
		BigDecimal roundedValue = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
		return roundedValue.doubleValue();
	}
}
