package com.c8y.sag.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.c8y.sag.cache.ConfigCache;
import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.model.TicketingPlatformConfig;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class ConfigService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
	
	private static final String CONFIG_MANAGED_OBJECT_EXTERNAL_ID = "ticketingintegrationms";
	private static final String CONFIG_MANAGED_OBJECT_EXTERNAL_ID_TYPE = "ticketingintegrationms";
	
	private static final String CONFIG_MANAGED_OBJECT_NAME = "ticketingintegrationconfig";
	private static final String TICKET_SERVICE_CONFIG_PROPERTY_NAME = "customTicketingPlatformConfig";
	private static final String DEVICE_ALARM_MAPPING_PROPERTY_NAME = "customDeviceAlarmMappingConfig";
	
	private static final String PLACEHOLDER_PASSWORD = "dummypassword";

	@Autowired
	private Platform platform;
	
	@Autowired
	MicroserviceSubscriptionsService subscriptionService;
	
	private ManagedObjectRepresentation getConfigManagedObject() {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			IdentityApi identityApi = platform.getIdentityApi();
			
			ID id = new ID();
			id.setValue(CONFIG_MANAGED_OBJECT_EXTERNAL_ID);
			id.setType(CONFIG_MANAGED_OBJECT_EXTERNAL_ID_TYPE);
			
			ExternalIDRepresentation extIDRep = identityApi.getExternalId(id);
			
			ManagedObjectRepresentation shrinkedConfigMO = null;
			if(extIDRep == null) {
				LOGGER.info("External ID is null.");
				return null;
			} else {
				shrinkedConfigMO = extIDRep.getManagedObject();
				if(shrinkedConfigMO == null) {
					LOGGER.info("External ID is not assigned to ManagedObject. Going to delete External ID.");
					identityApi.deleteExternalId(extIDRep);
					return null;
				} else {
					GId gId = new GId(shrinkedConfigMO.getId().getValue());
					ManagedObjectRepresentation configMO = inventoryApi.get(gId);
					if(configMO == null) {
						LOGGER.info("External ID is assigned to a Config ManagedObject that doesn't exist. Going to delete External ID.");
						identityApi.deleteExternalId(extIDRep);
						return null;
					} else {
						return configMO;
					}
				}
			}
		} catch(SDKException sdke) {
			LOGGER.info("External ID doesn't exist.");
			return null;
		}

	}
	
	/**
	 * To retrieve ticketing platform configuration
	 * @return
	 */
	public TicketingPlatformConfig getTicketingPlatformConfig(boolean hidePassword) throws SDKException {
		try {
			ManagedObjectRepresentation existingConfigMO = this.getConfigManagedObject();
			if(existingConfigMO != null) {
				Map<String, Object> tpConfigAsMap = (HashMap<String, Object>) existingConfigMO.getProperty(TICKET_SERVICE_CONFIG_PROPERTY_NAME);
				if(hidePassword) {
					tpConfigAsMap.put("password", PLACEHOLDER_PASSWORD);
				}
				return new TicketingPlatformConfig(tpConfigAsMap);
			} else {
				return null;
			}
		} catch(SDKException sdke) {
			LOGGER.error("Exception getting Ticketing Platform config. "+sdke);
			throw sdke;
		}
	}
	
	/**
	 * To add ticketing platform configuration
	 * @param tspConfig
	 */
	public String saveTicketingPlatformConfig(TicketingPlatformConfig tspConfig) {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
			mor.setName(CONFIG_MANAGED_OBJECT_NAME);
			mor.setProperty(TICKET_SERVICE_CONFIG_PROPERTY_NAME, tspConfig);
			mor.setProperty(DEVICE_ALARM_MAPPING_PROPERTY_NAME, null);
			ManagedObjectRepresentation newMor = inventoryApi.create(mor);
			
			if(newMor != null) {
				
				IdentityApi identityApi = platform.getIdentityApi();
				
				ExternalIDRepresentation extIDRep = new ExternalIDRepresentation();
				extIDRep.setExternalId(CONFIG_MANAGED_OBJECT_EXTERNAL_ID);
				extIDRep.setType(CONFIG_MANAGED_OBJECT_EXTERNAL_ID_TYPE);
				extIDRep.setManagedObject(newMor);
				identityApi.create(extIDRep);
				
				//Update ConfigCache
				final String tenantName = subscriptionService.getTenant();
				ConfigCache.tpConfigMap.put(tenantName, getTicketingPlatformConfig(false));
				LOGGER.info("tpConfig updated in ConfigCache.");
				return newMor.getId().getValue();
			}
			
			return null;
		} catch(SDKException sdke) {
			LOGGER.error("Exception saving Ticketing Platform config. "+sdke);
			throw sdke;
		}
	}
	
	/**
	 * To update ticketing platform configuration
	 * @param tspConfig
	 */
	public String updateTicketingPlatformConfig(TicketingPlatformConfig tspConfig) throws SDKException {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation existingConfigManagedObject = this.getConfigManagedObject();
			if(existingConfigManagedObject == null) {
				LOGGER.info("Ticketing Platform config doesn't exist. Cannot be updated.");
				return null;
			} else {
				if(tspConfig.getPassword().equals(PLACEHOLDER_PASSWORD)) {
					String existingPassword = (String)((HashMap<String, Object>) existingConfigManagedObject.getProperty(TICKET_SERVICE_CONFIG_PROPERTY_NAME)).get("password");
					tspConfig.setPassword(existingPassword);
				}
				existingConfigManagedObject.setLastUpdatedDateTime(null);
				existingConfigManagedObject.setProperty(TICKET_SERVICE_CONFIG_PROPERTY_NAME, tspConfig);
				ManagedObjectRepresentation updatedMor = inventoryApi.update(existingConfigManagedObject);
				
				if(updatedMor != null) {
					// Update ConfigCache
					final String tenantName = subscriptionService.getTenant();
					ConfigCache.tpConfigMap.put(tenantName, getTicketingPlatformConfig(false));
					LOGGER.info("tpConfig updated in ConfigCache.");
					return updatedMor.getId().getValue();
				}
				return null;
			}
		} catch(SDKException sdke) {
			LOGGER.error("Exception updating Ticketing Platform config. "+sdke);
			throw sdke;
		}
	}
	
	/**
	 * Deletes managed object containing configuration from inventory.
	 */
	public void deleteConfigManagedObject() {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation existingConfigMO = getConfigManagedObject();
			if(existingConfigMO == null) {
				LOGGER.info("Confguration managed object doesn't exist. Nothing to delete.");
			} else {
				inventoryApi.delete(existingConfigMO.getId());
				LOGGER.info("Configuration managed object deleted successfully.");
			}
		} catch(Exception e) {
			LOGGER.error("Exception deleting configuration managed object: "+e);
		}
	}
	
	/**
	 * To save Device Alarm Mappings
	 * @param damList
	 */
	public boolean saveDeviceAlarmMapping(List<DeviceAlarmMapping> damList) {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation existingConfigMO = this.getConfigManagedObject();
			if(existingConfigMO == null) {
				LOGGER.info("Configuration managed object doesn't exist. Device Alarm mapping cannot be added.");
				return false;
			} else {
				List<DeviceAlarmMapping> existingDamList = (List<DeviceAlarmMapping>) existingConfigMO.getProperty(DEVICE_ALARM_MAPPING_PROPERTY_NAME);
				existingDamList = damList;
				existingConfigMO.setProperty(DEVICE_ALARM_MAPPING_PROPERTY_NAME, existingDamList);
				existingConfigMO.setLastUpdatedDateTime(null);
				inventoryApi.update(existingConfigMO);
				
				// Update ConfigCache
				final String tenantName = subscriptionService.getTenant();
				ConfigCache.daMappingsMap.put(tenantName, damList);
				LOGGER.info("daMappings updated in ConfigCache.");
				return true;
			}
		} catch(SDKException sdke) {
			LOGGER.error("Exception saving Device Alarm Mapping. "+sdke);
			throw sdke;
		}
	}
	
	/**
	 * To get Device Alarm Mappings
	 * @return
	 */
	public List<DeviceAlarmMapping> getDeviceAlarmMapping() {
		try {
			ManagedObjectRepresentation existingConfigMO = this.getConfigManagedObject();
			if(existingConfigMO == null) {
				LOGGER.info("Configuration managed object doesn't exist. Nothing to get.");
				return null;
			} else {
				List<HashMap<String, String>> existingDAMappings = (ArrayList<HashMap<String, String>>) existingConfigMO.getProperty(DEVICE_ALARM_MAPPING_PROPERTY_NAME);
				
				if(existingDAMappings == null) {
					return null;
				} else {
					List<DeviceAlarmMapping> existingDAMappingsList = new ArrayList<DeviceAlarmMapping>();
					
					for(Map<String, String> m: existingDAMappings) {
						existingDAMappingsList.add(new DeviceAlarmMapping(m));
					}
					
					return existingDAMappingsList;
				}
				
				
			}
		} catch(SDKException sdke) {
			LOGGER.error("Exception getting Device Alarm Mappings. "+sdke);
			throw sdke;
		}
	}
	
}
