package com.strategyengine.xrpl.fsedistributionservice.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException{

	private static final long serialVersionUID = 6278452094348973408L;

	public BadRequestException(String message) {
       super(message);
       
    }
}