package com.c8y.sag.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.c8y.sag.model.TicketRecord;
import com.c8y.sag.model.TicketingPlatformNameEnum;
import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.model.Ticket;
import com.c8y.sag.model.TicketComment;
import com.c8y.sag.model.TicketCreationRecord;
import com.c8y.sag.service.CumulocityService;
import com.c8y.sag.service.TicketRecordService;
import com.c8y.sag.service.tp.AgileAppsService;
import com.c8y.sag.service.tp.ExternalPlatformService;
import com.c8y.sag.service.tp.TicketingPlatformService;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;


/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@RestController
@RequestMapping("/tickets")
public class TicketController {
	
	private final Logger LOGGER = LoggerFactory.getLogger(TicketController.class);
	
	@Autowired
	private BeanFactory beanFactory;
	
	@Autowired
	private TicketRecordService trService;
	
	@Autowired
	private CumulocityService c8yService;
	
	@Autowired
	private MicroserviceSubscriptionsService microserviceSubscriptionService;
	
	private TicketingPlatformService tpService;
	
	
	/**
	 * Get all tickets.
	 * 
	 * @param deviceId
	 * @param statusId
	 * @param pageSize
	 * @return
	 */
	@GetMapping(produces = "application/json")
	public ResponseEntity<String> getTickets(@RequestParam(value = "deviceId", required = false) String deviceId, @RequestParam(value = "statusId", required = false) String statusId, @RequestParam(value = "pageSize", required = false) String pageSize) {
		JSONObject jsonObject = new JSONObject();
		try {
			if(deviceId != null && deviceId != "") {
				try {
					Integer.parseInt(deviceId);
				} catch(NumberFormatException nfe) {
					jsonObject.put("message", "Invalid device id.");
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
				}
				
			}
			if(statusId != null && statusId != "") {
				try {
					Integer.parseInt(statusId);
				} catch(NumberFormatException nfe) {
					jsonObject.put("message", "Invalid status id.");
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
				}
				
			}
			if(pageSize != null && pageSize != "") {
				try {
					Integer.parseInt(pageSize);
				} catch(NumberFormatException nfe) {
					jsonObject.put("message", "Invalid page size.");
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
				}
			}
			
			// Get Ticketing Platform Service object based on Platform in TPConfiguration
			if(ConfigCache.tpConfigMap.get(microserviceSubscriptionService.getTenant()).getName().getName().equals(TicketingPlatformNameEnum.AGILEAPPS.getName())) {
				tpService = beanFactory.getBean(AgileAppsService.class);
			} else {
				tpService = beanFactory.getBean(ExternalPlatformService.class);
			}
			
			List<TicketRecord> trList = tpService.getTickets(statusId, pageSize);
			
			List<TicketCreationRecord> tcrList = trService.getRecords(deviceId);
			Map<String, TicketCreationRecord> tcrMap = new HashMap<String, TicketCreationRecord>();
			for(TicketCreationRecord tcr: tcrList) {
				tcrMap.put(tcr.getTicketId(), tcr);
			}
			
			List<Ticket> tList = new ArrayList<Ticket>();
			
			for(TicketRecord tr: trList) {
				TicketCreationRecord tcr = tcrMap.get(tr.getId());
				if(tcr != null) {
					Ticket t = new Ticket();
					t.setId(tr.getId());
					t.setSubject(tr.getName());
					t.setDescription(tr.getDescription());
					t.setStatus(tr.getStatus());
					t.setCreationDate(tr.getDateCreated());
					t.setLastUpdateDate(tr.getDateModified());
					t.setAlarmId(tcr.getAlarmId());
					t.setDeviceId(tcr.getDeviceId());
					t.setPriority(tr.getPriority());
					t.setOwner(tr.getOwner());
					tList.add(t);
				}
			}
			
			jsonObject.put("records", tList);
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
		} catch(Exception e) {
			LOGGER.error("Unable to fetch records. "+e.getMessage(), e);
			jsonObject.put("message", e.getMessage());
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	/**
	 * Create new ticket.
	 * 
	 * @param map
	 * @return
	 */
	@PostMapping(consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> createTicket(@RequestBody Map<String, String> map) {
		JSONObject jsonObject = new JSONObject();
		try {
			if(map == null || !map.containsKey("alarmId")) {
				jsonObject.put("message", "Alarm id is missing.");
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
			} else {
				try {
					Integer.parseInt(map.get("alarmId"));
				} catch(NumberFormatException nfe) {
					jsonObject.put("message", "Invalid alarm id.");
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
				}
			}
			AlarmRepresentation alarmRep = c8yService.getAlarmById(map.get("alarmId"));
			if(alarmRep == null) {
				jsonObject.put("message", "Invalid alarm id.");
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
			} else {
				final String tenant = microserviceSubscriptionService.getTenant();
				
				// Get Ticketing Platform Service object based on Platform in TPConfiguration
				if(ConfigCache.tpConfigMap.get(tenant).getName().getName().equals(TicketingPlatformNameEnum.AGILEAPPS.getName())) {
					tpService = beanFactory.getBean(AgileAppsService.class);
				} else {
					tpService = beanFactory.getBean(ExternalPlatformService.class);
				}
				
				String ticketId = null;
				
				if(alarmRep.getStatus().equals("ACTIVE")) {
					try {
						// Create ticket on Ticketing Platform
						ticketId = tpService.createTicket(alarmRep, map.get("comments"));
						
						if(ticketId != null) {
							// Add ticket creation record
							trService.addRecord(alarmRep, ticketId);
							
							if(ConfigCache.tpConfigMap.get(tenant).isAutoAcknowledgeAlarm()) {
								c8yService.acknowledgeAlarm(alarmRep);
							}
						}
						
					} catch(Exception e) {
						LOGGER.error("Error creating ticket: "+e.getMessage(), e);
					}
					
					JSONObject idJSONObject = new JSONObject();
					idJSONObject.put("id", ticketId);
					jsonObject.put("record", idJSONObject);
					
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.CREATED);
				} else {
					jsonObject.put("message", "Alarm status is not ACTIVE.");
					return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
				}
			}
		} catch(Exception e) {
			LOGGER.error("Unable to create ticket. "+e.getMessage(), e);
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Get ticket comments by ticket id.
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping(value = "{id}/comments")
	public ResponseEntity<String> getTicketComments(@PathVariable("id") String id) {
		JSONObject jsonObject = new JSONObject();
		try {
			try {
				Integer.parseInt(id);
			} catch(NumberFormatException nfe) {
				jsonObject.put("message", "Invalid id.");
				return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.BAD_REQUEST);
			}
			
			// Get Ticketing Platform Service object based on Platform in TPConfiguration
			if(ConfigCache.tpConfigMap.get(microserviceSubscriptionService.getTenant()).getName().getName().equals(TicketingPlatformNameEnum.AGILEAPPS.getName())) {
				tpService = beanFactory.getBean(AgileAppsService.class);
			} else {
				tpService = beanFactory.getBean(ExternalPlatformService.class);
			}
			
			List<TicketComment> tcList = tpService.getTicketComments(id);
			
			jsonObject.put("records", tcList);
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
		} catch(Exception e) {
			LOGGER.error("Unable to get ticket. "+e.getMessage(), e);
			return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
}
