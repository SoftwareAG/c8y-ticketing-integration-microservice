package com.c8y.sag.model;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public enum TicketingPlatformNameEnum {
	AGILEAPPS("AGILEAPPS"), EXTERNAL_REST("EXTERNAL_REST");
	
	private String name;
	
	private TicketingPlatformNameEnum(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
