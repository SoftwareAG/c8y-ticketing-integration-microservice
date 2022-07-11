package com.c8y.sag.controller.rest;


import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.constants.Constants;
import com.c8y.sag.model.TicketingPlatformConfig;
import com.c8y.sag.model.TicketingPlatformNameEnum;
import com.c8y.sag.service.AlarmSubscriptionService;
import com.c8y.sag.service.ConfigService;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@RestController
@RequestMapping("/tpconfig")
public class TicketingPlatformConfigController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TicketingPlatformConfigController.class);
	
	@Autowired
	private ConfigService configService;
	
	@Autowired
	private MicroserviceSubscriptionsService subscriptionService;
	
	@Autowired
	private AlarmSubscriptionService alarmSubscriptionService;
	
	@GetMapping(produces = "application/json")
	public ResponseEntity<Object> getTicketingPlatformConfig() {
		JSONObject jsonObject = new JSONObject();
		try {
			TicketingPlatformConfig tpConfig = configService.getTicketingPlatformConfig(true);
			if(tpConfig == null) {
				jsonObject.put("message", "Config doesn't exist.");
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.BAD_REQUEST);
			} else {
				jsonObject.put("record", tpConfig);
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.OK);
			}
		} catch(Exception e) {
			LOGGER.error("Unable to get TPConfig: "+e, e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping(consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> addTicketingPlatformConfig(@RequestBody TicketingPlatformConfig newTPConfig) {
		JSONObject jsonObject = new JSONObject();
		try {
			String validationMessage = validateTPConfig(newTPConfig);
			if(validationMessage != "") {
				jsonObject.put("message", validationMessage);
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.BAD_REQUEST);
			}
			TicketingPlatformConfig existingTPConfig = this.configService.getTicketingPlatformConfig(true);
			if(existingTPConfig == null) {
				String recordId = configService.saveTicketingPlatformConfig(newTPConfig);
				JSONObject idJSONObject = new JSONObject();
				idJSONObject.put("id", recordId);
				jsonObject.put("record", idJSONObject);
		
				final String tenant = subscriptionService.getTenant();
				if(ConfigCache.tpConfigMap.get(tenant).isAlarmSubscription()) {
					// subscribe alarm notifications
					alarmSubscriptionService.subscribeAlarmNotifications(tenant);
				} else {
					// unsubscribe alarm notifications
					alarmSubscriptionService.unsubscribeAlarmNotifications(tenant);
				}
				
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.CREATED);
			} else {
				jsonObject.put("message", "Config already exists.");
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.BAD_REQUEST);
			}
		} catch(Exception e) {
			LOGGER.error("Unable to add TPConfig: "+e, e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PutMapping(consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> updateTicketingPlatformConfig(@RequestBody TicketingPlatformConfig updatedTPConfig) {
		JSONObject jsonObject = new JSONObject();
		try {
			String validationMessage = validateTPConfig(updatedTPConfig);
			if(validationMessage != "") {
				jsonObject.put("message", validationMessage);
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.BAD_REQUEST);
			}
			
			String recordId = configService.updateTicketingPlatformConfig(updatedTPConfig);
			if(recordId == null) {
				jsonObject.put("message", "Config doesn't exist");
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.BAD_REQUEST);
			} else {
				JSONObject idJSONObject = new JSONObject();
				idJSONObject.put("id", recordId);
				jsonObject.put("record", idJSONObject);
				
				final String tenant = subscriptionService.getTenant();
				if(ConfigCache.tpConfigMap.get(tenant).isAlarmSubscription()) {
					// subscribe alarm notifications
					alarmSubscriptionService.subscribeAlarmNotifications(tenant);
				} else {
					// unsubscribe alarm notifications
					alarmSubscriptionService.unsubscribeAlarmNotifications(tenant);
				}
				
				return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.OK);
			}
		} catch(Exception e) {
			LOGGER.error("Unable to update TPConfig: "+e, e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<Object>(jsonObject.toMap(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private String validateTPConfig(TicketingPlatformConfig tpConfig) {
		if(tpConfig.getUsername() == null || tpConfig.getUsername().isBlank()) {
			return "Username is missing.";
		}
		if(tpConfig.getPassword() == null || tpConfig.getPassword().isBlank()) {
			return "Password is missing.";
		}
		if(tpConfig.getTenantUrl() == null || tpConfig.getTenantUrl().isBlank()) {
			return "Tenant url is missing.";
		}
		if(tpConfig.getTicketRecordTemplateUrl() == null || tpConfig.getTicketRecordTemplateUrl().isBlank()) {
			return "Ticket record template url is missing.";
		}
		if(tpConfig.getName() == null) {
			return "Platform name is missing.";
		} else {
			if(tpConfig.getName().getName().equals(TicketingPlatformNameEnum.AGILEAPPS.getName())) {
				if(tpConfig.getAccountId() == null || tpConfig.getAccountId().isBlank()) {
					return "Account id is missing.";
				}
			}
		}
		return "";
	}
	
}
