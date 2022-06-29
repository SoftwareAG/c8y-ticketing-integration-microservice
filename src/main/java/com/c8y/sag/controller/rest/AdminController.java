package com.c8y.sag.controller.rest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c8y.sag.service.ConfigService;
import com.c8y.sag.service.TicketRecordService;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@RestController
@RequestMapping("admin")
public class AdminController {

	private final static Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
	
	@Autowired
	private ConfigService configService;
	
	@Autowired
	private TicketRecordService trService;
	
	@DeleteMapping(value = "configManagedObject", produces = "application/json")
	public ResponseEntity<String> deleteConfigManagedObject() {
		JSONObject jsonObject = new JSONObject();
		try {
			LOGGER.info("Delete Config Managed Object request received.");
			configService.deleteConfigManagedObject();
			jsonObject.put("message", "Deleted.");
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
		} catch(Exception e) {
			LOGGER.error("Unable to delete Config Managed Object: "+e.getMessage(), e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@DeleteMapping(value = "ticketCreationRecords", produces = "application/json")
	public ResponseEntity<String> deleteTicketCreationRecords() {
		JSONObject jsonObject = new JSONObject();
		try {
			LOGGER.info("Delete all Ticket Creation Records request received.");
			trService.deleteTicketCreationRecords();
			jsonObject.put("message", "Deleted.");
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
		} catch(Exception e) {
			LOGGER.error("Unable to delete Ticket Creation Records: "+e.getMessage(), e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
