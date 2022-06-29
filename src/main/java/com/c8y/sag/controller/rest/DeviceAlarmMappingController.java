package com.c8y.sag.controller.rest;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.service.ConfigService;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@RestController
@RequestMapping("/damappings")
public class DeviceAlarmMappingController {
	
	private final Logger LOGGER = LoggerFactory.getLogger(DeviceAlarmMappingController.class);
	
	@Autowired
	private ConfigService configSvc;

	/**
	 * Add Device Alarm Mappings.
	 * 
	 * @param newDamList
	 * @return
	 */
	@PostMapping(consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> addMappingConfig(@RequestBody List<DeviceAlarmMapping> newDamList) {
		JSONObject jsonObject = new JSONObject();
		try {
			if(newDamList != null) {
				for(DeviceAlarmMapping daMapping: newDamList) {
					if(daMapping.getAlarmType() == null || daMapping.getAlarmType().isBlank()) {
						jsonObject.put("message", "Alarm type is missing.");
						return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
					}
					if(daMapping.getDeviceId() == null || daMapping.getDeviceId().isBlank()) {
						jsonObject.put("message", "Device id is missing.");
						return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
					}
				}
			}
			
			boolean isSaved = configSvc.saveDeviceAlarmMapping(newDamList);
			if(isSaved) {
				jsonObject.put("message", "Saved.");
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
			} else {
				jsonObject.put("message", "Unable to save.");
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
			}
		} catch(Exception e) {
			LOGGER.error("Unable to add Device Alarm Mapping "+e.getMessage(), e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Get Device Alarm Mappings.
	 * 
	 * @return
	 */
	@GetMapping(produces = "application/json")
	public ResponseEntity<String> getAllMappingConfigs() {
		JSONObject jsonObject = new JSONObject();
		try {
			List<DeviceAlarmMapping> daMappings = configSvc.getDeviceAlarmMapping();
			if(daMappings == null) {
				List<String> emptyList = new ArrayList<>();
				jsonObject.put("records", emptyList);
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
			} else {
				jsonObject.put("records", daMappings);
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
			}
		} catch(Exception e) {
			LOGGER.error("Unable to get Device Alarm Mappings: "+e.getMessage(), e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
}
