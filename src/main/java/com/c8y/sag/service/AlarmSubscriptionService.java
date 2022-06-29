package com.c8y.sag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.svenson.JSON;
import org.svenson.JSONParser;

import com.c8y.sag.cache.AlarmSubscriptionCache;
import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.model.TicketingPlatformNameEnum;
import com.c8y.sag.service.tp.AgileAppsService;
import com.c8y.sag.service.tp.ExternalPlatformService;
import com.c8y.sag.service.tp.TicketingPlatformService;
import com.c8y.sag.subscriber.AlarmNotification;
import com.c8y.sag.subscriber.AlarmSubscriber;
import com.c8y.sag.utils.Utils;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.notification.Subscription;
import com.cumulocity.sdk.client.notification.SubscriptionListener;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class AlarmSubscriptionService {
	
	private final Logger LOGGER = LoggerFactory.getLogger(AlarmSubscriptionService.class);
	
	@Autowired
	private PlatformImpl platform;
	
	@Autowired
	private BeanFactory beanFactory;
	
	@Autowired
	private MicroserviceSubscriptionsService microserviceSubscriptionsService;
	
	private TicketingPlatformService tpService;
	
	@Autowired
	private TicketRecordService trService;
	
	@Autowired
	private CumulocityService c8yService;
	
	private final JSONParser jsonParser = JSONBase.getJSONParser();
	private final JSON json = JSON.defaultJSON();
	
	public void subscribeAlarmNotifications(final String tenant) {
		
		final AlarmSubscriber subscriber = new AlarmSubscriber(platform);
		
		if(AlarmSubscriptionCache.alarmSubscriptionMap.get(tenant) == null) {
			
			LOGGER.info("Going to subscribe alarm notifications for tenant: "+tenant);
			
			// Get Ticketing Platform Service object based on Platform in TPConfiguration
			if(ConfigCache.tpConfigMap.get(tenant).getName().getName().equals(TicketingPlatformNameEnum.AGILEAPPS.getName())) {
				tpService = beanFactory.getBean(AgileAppsService.class);
			} else {
				tpService = beanFactory.getBean(ExternalPlatformService.class);
			}
			
			Subscription<String> alarmSubscription = subscriber.subscribe("*", new SubscriptionListener<String, AlarmNotification>() {
				
				@Override
				public void onNotification(Subscription<String> arg0, AlarmNotification notification) {
					try {
						final AlarmRepresentation alarmRep = jsonParser.parse(AlarmRepresentation.class, json.forValue(notification.getData()));
						
						for(DeviceAlarmMapping daMapping: ConfigCache.daMappingsMap.get(tenant)) {
							try {
								// determine alarm
								if(Utils.isAlarmEligibleForTicket(alarmRep, daMapping)) {
										
									LOGGER.info("Device ID: " + daMapping.getDeviceId() + " Alarm Type: " + alarmRep.getType() + " matched.");
									
									
									microserviceSubscriptionsService.runForTenant(tenant, () -> {
										
										if(tpService == null) {
											System.out.println("TP service is null 2.");
										}
										
										String ticketId = null;
										try {
											// Create ticket on Ticketing Platform
											ticketId = tpService.createTicket(alarmRep, null);
											
											if(ConfigCache.tpConfigMap.get(tenant).isAutoAcknowledgeAlarm()) {
												c8yService.acknowledgeAlarm(alarmRep);
											}
											
										} catch(Exception e) {
											LOGGER.error("Error creating ticket: "+e.getMessage(), e);
										}
										
										if(ticketId != null) {
											// Add ticket creation record
											trService.addRecord(alarmRep, ticketId);
										}
										
									});

									// Don't create duplicate tickets even if duplicate mappings are available.
									break;
								}
							} catch(Exception e) {
								LOGGER.error("Error processing alarm notification: "+ e.getMessage(), e);
							}
						}
					} catch(Exception e) {
						LOGGER.error("Exception after alarm notification received: "+ e.getMessage(), e);
					}
					
				}
				
				@Override
				public void onError(Subscription<String> arg0, Throwable arg1) {
					LOGGER.error("Exception onError() during alarms subscription: "+ arg1.getMessage(), arg1);
					
				}
			});
			
			// Add subscription to cache
			AlarmSubscriptionCache.alarmSubscriptionMap.put(tenant, alarmSubscription);
			
		} else {
			LOGGER.info("Alarm notifications are already subscribed for tenant: "+tenant);
		}
		
	}
	
	public void unsubscribeAlarmNotifications(final String tenant) {
		try {
			if(AlarmSubscriptionCache.alarmSubscriptionMap.get(tenant) != null) {
				LOGGER.info("Going to unsubscribe alarm notifications for tenant: "+tenant);
				AlarmSubscriptionCache.alarmSubscriptionMap.get(tenant).unsubscribe();

				// Remove subscription from cache
				AlarmSubscriptionCache.alarmSubscriptionMap.remove(tenant);
			} 
		} catch(Exception e) {
			LOGGER.error("Alarm notification unsubscribe failed: "+e.getMessage(), e);
		}
	}
}
