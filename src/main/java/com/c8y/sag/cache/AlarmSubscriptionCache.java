package com.c8y.sag.cache;

import java.util.concurrent.ConcurrentHashMap;
import com.cumulocity.sdk.client.notification.Subscription;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class AlarmSubscriptionCache {

	public static ConcurrentHashMap<String, Subscription<String>> alarmSubscriptionMap = new ConcurrentHashMap<String, Subscription<String>>();
}
