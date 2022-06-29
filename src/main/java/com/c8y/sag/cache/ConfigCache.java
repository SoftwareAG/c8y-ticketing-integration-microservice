package com.c8y.sag.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.model.TicketingPlatformConfig;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class ConfigCache {
	
	public static ConcurrentHashMap<String , TicketingPlatformConfig> tpConfigMap = new ConcurrentHashMap<String, TicketingPlatformConfig>();
	public static ConcurrentHashMap<String , List<DeviceAlarmMapping>> daMappingsMap = new ConcurrentHashMap<String, List<DeviceAlarmMapping>>();
	
}
