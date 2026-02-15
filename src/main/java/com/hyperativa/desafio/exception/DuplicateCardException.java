package com.hyperativa.desafio.exception;

public class DuplicateCardException extends RuntimeException {
    
	private static final long serialVersionUID = 2975208974341996045L;

	public DuplicateCardException(String message) {
        super(message);
    }
}
