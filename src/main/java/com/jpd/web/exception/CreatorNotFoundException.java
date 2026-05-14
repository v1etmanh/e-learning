package com.jpd.web.exception;


public class CreatorNotFoundException extends BusinessException {
	 public CreatorNotFoundException(String id) {
	        super("CREATOR_NOT_FOUND", "Creator not found with id: " + id);
	    }
}
