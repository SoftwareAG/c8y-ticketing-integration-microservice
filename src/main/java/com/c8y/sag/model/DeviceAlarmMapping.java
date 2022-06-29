package com.c8y.sag.model;

import java.util.Map;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class DeviceAlarmMapping {

	private String deviceId;
	private String alarmType;
	
	public DeviceAlarmMapping() {
		
	}
	
	public DeviceAlarmMapping(Map<String, String> map) {
		this.deviceId = map.get("deviceId");
		this.alarmType = map.get("alarmType");
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getAlarmType() {
		return alarmType;
	}
	public void setAlarmType(String alarmType) {
		this.alarmType = alarmType;
	}
	
	
}
