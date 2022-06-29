package com.c8y.sag.model;

import java.util.Map;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class TicketingPlatformConfig {
	
	private TicketingPlatformNameEnum name;
	private String username;
	private String password;
	private String tenantUrl;
	private String accountId;
	private String ticketRecordTemplateUrl;
	private boolean alarmSubscription;
	private boolean autoAcknowledgeAlarm;
	
	public TicketingPlatformConfig() {
		
	}
	
	public TicketingPlatformConfig(Map<String, Object> map) {
		this.name = TicketingPlatformNameEnum.valueOf((String) map.get("name"));
		this.username = (String) map.get("username");
		this.password = (String) map.get("password");
		this.tenantUrl = (String) map.get("tenantUrl");
		this.accountId = (String) map.get("accountId");
		this.ticketRecordTemplateUrl = map.get("ticketRecordTemplateUrl") == null ? "" : (String) map.get("ticketRecordTemplateUrl");
		this.alarmSubscription = map.get("alarmSubscription") == null ? false : (boolean) map.get("alarmSubscription");
		this.autoAcknowledgeAlarm = map.get("autoAcknowledgeAlarm") == null ? false : (boolean) map.get("autoAcknowledgeAlarm");
	}
	
	
	public TicketingPlatformNameEnum getName() {
		return name;
	}
	public void setName(TicketingPlatformNameEnum name) {
		this.name = name;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getTenantUrl() {
		return tenantUrl;
	}
	public void setTenantUrl(String tenantUrl) {
		this.tenantUrl = tenantUrl;
	}
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getTicketRecordTemplateUrl() {
		return ticketRecordTemplateUrl;
	}

	public void setTicketRecordTemplateUrl(String ticketRecordTemplateUrl) {
		this.ticketRecordTemplateUrl = ticketRecordTemplateUrl;
	}

	public boolean isAlarmSubscription() {
		return alarmSubscription;
	}
	public void setAlarmSubscription(boolean alarmSubscription) {
		this.alarmSubscription = alarmSubscription;
	}
	public boolean isAutoAcknowledgeAlarm() {
		return autoAcknowledgeAlarm;
	}
	public void setAutoAcknowledgeAlarm(boolean autoAcknowledgeAlarm) {
		this.autoAcknowledgeAlarm = autoAcknowledgeAlarm;
	}

}
