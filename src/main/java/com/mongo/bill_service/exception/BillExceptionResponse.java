package com.mongo.bill_service.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class BillExceptionResponse  {

	private String errorCode;
	private String errorMessage;
	
	public BillExceptionResponse(BillException billException) {
		this.errorCode = billException.getErrorCode();
		this.errorMessage = billException.getErrorMessage();
	}

}
