package com.c8y.sag.service.tp;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.model.TicketComment;
import com.c8y.sag.model.TicketRecord;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class ExternalPlatformService implements TicketingPlatformService {
	
	private final Logger LOGGER = LoggerFactory.getLogger(ExternalPlatformService.class);

	@Autowired
	MicroserviceSubscriptionsService subscriptionService;
	
	@Override
	public String createTicket(AlarmRepresentation alarmRep, String comments) throws Exception {
		try {
			final String tenantName = subscriptionService.getTenant();
			if(tenantName == null) {
				throw new NullPointerException("Tenant name from MicroserviceSubscriptionService is null.");
			}
			
			final String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl()+"/tickets";
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.setBasicAuth(ConfigCache.tpConfigMap.get(tenantName).getUsername(), ConfigCache.tpConfigMap.get(tenantName).getPassword());
			
			String desc = "Alarm type: "+ alarmRep.getType()+" Alarm text: "+ alarmRep.getText();
			if(comments != null && !comments.isBlank()) {
				desc = desc + " Additional description: "+comments;
			}
			
			JSONObject requestBody = new JSONObject();
			requestBody.put("name", "Alarm raised for device "+alarmRep.getSource().getId().getValue());
			requestBody.put("description", desc);
			requestBody.put("alarmSeverity", alarmRep.getSeverity());
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody.toString(), httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				String recordId = jsonObject.getJSONObject("record").getString("id");
				LOGGER.info("Ticket "+recordId+" created for Device ID: "+alarmRep.getSource().getId().getValue() + " Alarm Type: "+alarmRep.getType());
				return recordId;
			} else {
				throw new Exception("Unable to create ticket for Device ID: "+alarmRep.getSource().getId().getValue() + " Alarm Type: "+alarmRep.getType()+": "+responseEntity.getStatusCodeValue()+ " "+responseEntity.getStatusCode().getReasonPhrase());
			}
		} catch(Exception e) {
			throw e;
		}
	}

	@Override
	public List<TicketRecord> getTickets(String statusId, String pageSize) throws Exception {
		try {
			final String tenantName = subscriptionService.getTenant();
			if(tenantName == null) {
				throw new NullPointerException("Tenant name from MicroserviceSubscriptionService is null.");
			}
			
			if(pageSize == null || pageSize == "") {
				pageSize = "100";
			}
			
			final String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/tickets?pageSize="+pageSize;
			
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.setBasicAuth(ConfigCache.tpConfigMap.get(tenantName).getUsername(), ConfigCache.tpConfigMap.get(tenantName).getPassword());
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				List<TicketRecord> ticketList = new ArrayList<TicketRecord>();
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				JSONArray jsonArray = jsonObject.getJSONArray("records");
				jsonArray.forEach((record) -> {
					ticketList.add(mapRecord((JSONObject)record));
				});
				return ticketList;
			} else {
				LOGGER.info("Unable fetch tickets from External REST: "+responseEntity.getStatusCodeValue()+" "+responseEntity.getStatusCode().getReasonPhrase());
				return null;
			}
		} catch(Exception e) {
			throw e;
		}
	}
	
	public List<TicketComment> getTicketComments(String ticketId) throws Exception {
		try {
			final String tenantName = subscriptionService.getTenant();
			if(tenantName == null) {
				throw new NullPointerException("Tenant name from MicroserviceSubscriptionService is null.");
			}
			
			String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/tickets/"+ticketId+"/comments";
			
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.setBasicAuth(ConfigCache.tpConfigMap.get(tenantName).getUsername(), ConfigCache.tpConfigMap.get(tenantName).getPassword());
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				List<TicketComment> tcList = new ArrayList<TicketComment>();
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				JSONArray jsonArray = jsonObject.getJSONArray("records");
				jsonArray.forEach((record) -> {
					tcList.add(mapComment((JSONObject)record));
				});
				return tcList;
			} else {
				throw new Exception("Unable to fetch ticket "+ ticketId +" comments from External REST: "+responseEntity.getStatusCodeValue()+" "+responseEntity.getStatusCode().getReasonPhrase());
			}
		} catch(Exception e) {
			throw e;
		}
	}
	
	private TicketRecord mapRecord(JSONObject jsonObject) {
		TicketRecord ticket = new TicketRecord();
		ticket.setId(jsonObject.getString("id"));
		ticket.setName(jsonObject.getString("name"));
		ticket.setDescription(jsonObject.getString("description"));
		ticket.setStatus(jsonObject.getString("status"));
		ticket.setPriority(jsonObject.getString("priority"));
		ticket.setOwner(jsonObject.getString("owner"));
		ticket.setDateCreated(jsonObject.getString("dateCreated"));
		ticket.setDateModified(jsonObject.getString("dateModified"));
		return ticket;
	}
	
	private TicketComment mapComment(JSONObject jsonObject) {
		TicketComment tc = new TicketComment();
		tc.setDescription(jsonObject.getString("description"));
		tc.setCreationDate(jsonObject.getString("dateCreated"));
		return tc;
	}

}
