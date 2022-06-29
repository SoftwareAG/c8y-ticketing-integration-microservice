package com.c8y.sag;

import org.springframework.boot.SpringApplication;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@MicroserviceApplication
public class TicketingApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketingApplication.class, args);
	}

}
