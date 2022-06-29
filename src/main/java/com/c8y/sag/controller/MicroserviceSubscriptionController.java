package com.c8y.sag.controller;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.model.TicketingPlatformConfig;
import com.c8y.sag.service.AlarmSubscriptionService;
import com.c8y.sag.service.ConfigService;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionRemovedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Controller
public class MicroserviceSubscriptionController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceSubscriptionController.class);
	
	@Autowired
	private MicroserviceSubscriptionsService subscriptionsService;
	
	@Autowired
	private ConfigService configSvc;
	
	@Autowired
	private AlarmSubscriptionService alarmSubscriptionSvc;

	@EventListener
	private void onMicroserviceSubscribed(final MicroserviceSubscriptionAddedEvent msae) {
		try {
			final String tenant = msae.getCredentials().getTenant();
			
			subscriptionsService.runForTenant(tenant, () -> {
				LOGGER.info("Microservice Subscription Added Event received.");
				loadConfig(tenant);
			});
			
		} catch(Exception e) {
			LOGGER.error("Microservice Subscribe exception: "+e.getMessage(), e);
		}
		
	}
	
	@EventListener
	private void onMicroserviceUnsubscribed(final MicroserviceSubscriptionRemovedEvent msre) {
		try {
			final String tenant = msre.getTenant();
					
			subscriptionsService.runForTenant(tenant, () -> {
				LOGGER.info("Microservice Subscription Removed Event received.");
				alarmSubscriptionSvc.unsubscribeAlarmNotifications(tenant);
			});
		} catch(Exception e) {
			LOGGER.error("Microservice Unsubscription exception: "+e.getMessage(), e);
		}
	}
	
	private void loadConfig(final String tenant) {
		LOGGER.info("Microservice is going to load configuration from ManagedObject.");

		TicketingPlatformConfig tpConfig = configSvc.getTicketingPlatformConfig(false);
		if(tpConfig == null) {
			LOGGER.info("ManagedObject does not exist.");
		} else {
			// Initialise tpConfig in ConfigCache
			ConfigCache.tpConfigMap.put(tenant, tpConfig);
			
			LOGGER.info("Ticketing Platform configuration loaded sucessfully.");
			
			if(ConfigCache.tpConfigMap.get(tenant) != null && ConfigCache.tpConfigMap.get(tenant).isAlarmSubscription()) {
				// Subscribe to alarm notifications
				alarmSubscriptionSvc.subscribeAlarmNotifications(tenant);
			}
			
			List<DeviceAlarmMapping> daMappings = configSvc.getDeviceAlarmMapping();
			if(daMappings == null || daMappings.size() == 0) {
				LOGGER.info("ManagedObject doesn't have device and alarm mappings.");
			} else {
				// Initialise daMappings in ConfigCache
				ConfigCache.daMappingsMap.put(tenant, daMappings);
				
				LOGGER.info("Device Alarm Mappings loaded successfully.");
			}
		}
	}
	
}
