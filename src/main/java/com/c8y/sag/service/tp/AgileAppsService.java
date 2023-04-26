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
public class AgileAppsService implements TicketingPlatformService {
	
	private final Logger LOGGER = LoggerFactory.getLogger(AgileAppsService.class);
	
	@Autowired
	MicroserviceSubscriptionsService subscriptionService;
	
	private String getLoginRequestBody(final String tenantName) {
		JSONObject loginKey = new JSONObject();
		loginKey.put("userName", ConfigCache.tpConfigMap.get(tenantName).getUsername());
		loginKey.put("password", ConfigCache.tpConfigMap.get(tenantName).getPassword());
		JSONObject platformKey = new JSONObject();
		platformKey.put("login", loginKey);
		JSONObject loginRequestBody = new JSONObject();
		loginRequestBody.put("platform", platformKey);
		return loginRequestBody.toString();
	}
	
	private HttpEntity<String> getLoginRequest(final String tenantName) {
		List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
		acceptContentTypes.add(MediaType.APPLICATION_JSON);
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.setAccept(acceptContentTypes);
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(getLoginRequestBody(tenantName), httpHeaders);
		return httpEntity;
	}
	
	private String getSessionId(final String tenantName) throws Exception {
		final String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/networking/rest/login";
		RestTemplate restTemplate = new RestTemplate();
		
		ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, getLoginRequest(tenantName), String.class);
		if(responseEntity.getStatusCode().is2xxSuccessful()) {
			HttpHeaders responseHeaders = responseEntity.getHeaders();
			List<String> cookieHeaders = responseHeaders.get("Set-Cookie");
			StringBuilder sb = new StringBuilder();
			
			for(String cookie: cookieHeaders) {
				sb.append(cookie.substring(0, cookie.indexOf(";")));
				sb.append("; ");
			}
			return sb.toString();
		} else {
			throw new Exception("Login to AgileApps failed. "+responseEntity.getStatusCodeValue()+ " "+responseEntity.getStatusCode().getReasonPhrase());
		}
	}

	@Override
	public String createTicket(AlarmRepresentation alarmRep, String comments) throws Exception {
		try {
			final String tenantName = subscriptionService.getTenant();
			if(tenantName == null) {
				throw new NullPointerException("Tenant name from MicroserviceSubscriptionService is null.");
			}
			
			final String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/networking/rest/record/cases";
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.add("Cookie", getSessionId(tenantName));
			
			String desc = "Alarm type: "+ alarmRep.getType()+" Alarm text: "+ alarmRep.getText();
			if(comments != null && !comments.isBlank()) {
				desc = desc + " Additional description: "+comments;
			}
			
			JSONObject recordKey = new JSONObject();
			recordKey.put("subject", "Alarm raised for device "+alarmRep.getSource().getId().getValue());
			recordKey.put("description", desc);
			recordKey.put("priority", mapAlarmSeverityToPriority(alarmRep.getSeverity()));
			recordKey.put("account", ConfigCache.tpConfigMap.get(tenantName).getAccountId());
			JSONObject platformKey = new JSONObject();
			platformKey.put("record", recordKey);
			JSONObject requestBody = new JSONObject();
			requestBody.put("platform", platformKey);
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody.toString(), httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				String recordId = jsonObject.getJSONObject("platform").getJSONObject("message").getString("id");
				LOGGER.info("Ticket "+recordId+" created for Device ID: "+alarmRep.getSource().getId().getValue() + " Alarm Type: "+alarmRep.getType());
				return recordId;
			} else {
				throw new Exception("Unable to create ticket for Device ID: "+alarmRep.getSource().getId().getValue() + " Alarm Type: "+alarmRep.getType()+": "+responseEntity.getStatusCodeValue()+ " "+responseEntity.getStatusCode().getReasonPhrase());
			}
		} catch(Exception e) {
			LOGGER.error("Error creating ticket in AgileApps: "+e.getMessage());
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
			
			String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/networking/rest/record/cases?fieldList=description,id,subject,status,date_created,date_modified,priority,owner_id&filter=account="+ConfigCache.tpConfigMap.get(tenantName).getAccountId()+"&pageSize="+pageSize+"&sortBy=date_modified&sortOrder=desc";
			if(statusId != null && !statusId.isBlank()) {
				url = url + " AND status="+statusId;
			}
			
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.add("Cookie", getSessionId(tenantName));
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				List<TicketRecord> ticketList = new ArrayList<TicketRecord>();
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				if(Integer.valueOf(jsonObject.getJSONObject("platform").getString("recordCount")) == 1) {
					ticketList.add(mapRecord(jsonObject.getJSONObject("platform").getJSONObject("record")));
					return ticketList;
				} else if(Integer.valueOf(jsonObject.getJSONObject("platform").getString("recordCount")) > 1) {
					JSONArray jsonArray = jsonObject.getJSONObject("platform").getJSONArray("record");
					jsonArray.forEach((record) -> {
						ticketList.add(mapRecord((JSONObject)record));
					});
					return ticketList;
				} else {
					LOGGER.info("No tickets found in AgileApps.");
					return ticketList;
				}
			} else {
				LOGGER.info("Unable fetch tickets from AgileApps: "+responseEntity.getStatusCodeValue()+" "+responseEntity.getStatusCode().getReasonPhrase());
				return null;
			}
			
		} catch(Exception e) {
			throw e;
		}
	}
	
	@Override
	public List<TicketComment> getTicketComments(String ticketId) throws Exception {
		try {
			final String tenantName = subscriptionService.getTenant();
			if(tenantName == null) {
				throw new NullPointerException("Tenant name from MicroserviceSubscriptionService is null.");
			}
			
			String url = ConfigCache.tpConfigMap.get(tenantName).getTenantUrl() + "/networking/rest/record/history?fieldList=description,id,category,date_created&filter=related_to='cases:"+ticketId+"' AND category='29|27'";
			
			RestTemplate restTemplate = new RestTemplate();
			
			List<MediaType> acceptContentTypes = new ArrayList<MediaType>();
			acceptContentTypes.add(MediaType.APPLICATION_JSON);
			
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.setAccept(acceptContentTypes);
			httpHeaders.add("Cookie", getSessionId(tenantName));
			
			HttpEntity<String> httpEntity = new HttpEntity<String>(httpHeaders);
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
			
			if(responseEntity.getStatusCode().is2xxSuccessful()) {
				List<TicketComment> tcList = new ArrayList<TicketComment>();
				JSONObject jsonObject = new JSONObject(responseEntity.getBody());
				if(Integer.valueOf(jsonObject.getJSONObject("platform").getString("recordCount")) == 1) {
					tcList.add(mapComment(jsonObject.getJSONObject("platform").getJSONObject("record")));
					return tcList;
				} else if(Integer.valueOf(jsonObject.getJSONObject("platform").getString("recordCount")) > 1) {
					JSONArray jsonArray = jsonObject.getJSONObject("platform").getJSONArray("record");
					jsonArray.forEach((record) -> {
						tcList.add(mapComment((JSONObject)record));
					});
					return tcList;
				} else {
					LOGGER.info("No comments found for ticket: "+ticketId+" in AgileApps.");
					return tcList;
				}
			} else {
				throw new Exception("Unable to fetch ticket "+ ticketId +" comments from AgileApps: "+responseEntity.getStatusCodeValue()+" "+responseEntity.getStatusCode().getReasonPhrase());
			}
		} catch(Exception e) {
			throw e;
		}
	}
	
	private TicketRecord mapRecord(JSONObject jsonObject) {
		TicketRecord tr = new TicketRecord();
		tr.setId(jsonObject.getString("id"));
		tr.setName(jsonObject.getString("subject"));
		tr.setDescription(jsonObject.getString("description"));
		tr.setStatus(jsonObject.getJSONObject("status").getString("displayValue"));
		tr.setDateCreated(jsonObject.getString("date_created"));
		tr.setDateModified(jsonObject.getString("date_modified"));
		tr.setPriority(jsonObject.getJSONObject("priority").getString("displayValue"));
		tr.setOwner(jsonObject.getJSONObject("owner_id").getString("displayValue"));
		return tr;
	}
	
	private TicketComment mapComment(JSONObject jsonObject) {
		TicketComment tc = new TicketComment();
		tc.setId(jsonObject.getString("id"));
		tc.setDescription(jsonObject.getString("description"));
		tc.setCreationDate(jsonObject.getString("date_created"));
		return tc;
	}
	
	private String mapAlarmSeverityToPriority(String alarmSeverity) {
		if(alarmSeverity.equals("CRITICAL")) {
			return "1";
		} else if(alarmSeverity.equals("MAJOR")) {
			return "2";
		} else if(alarmSeverity.equals("MINOR")) {
			return "3";
		} else {
			return "4";
		}
	}

}
