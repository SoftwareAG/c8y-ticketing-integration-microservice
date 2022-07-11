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
import com.c8y.sag.constants.Constants;
import com.c8y.sag.model.DeviceAlarmMapping;
import com.c8y.sag.model.TicketingPlatformConfig;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.option.TenantOptionApi;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class ConfigService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);


	@Autowired
	private Platform platform;
	
	@Autowired
	MicroserviceSubscriptionsService subscriptionService;
	
	private ManagedObjectRepresentation getConfigManagedObject() {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			IdentityApi identityApi = platform.getIdentityApi();
			
			ID id = new ID();
			id.setValue(Constants.CONFIG_MANAGED_OBJECT_EXTERNAL_ID);
			id.setType(Constants.CONFIG_MANAGED_OBJECT_EXTERNAL_ID_TYPE);
			
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
				Map<String, Object> tpConfigAsMap = (HashMap<String, Object>) existingConfigMO.getProperty(Constants.TICKET_SERVICE_CONFIG_PROPERTY_NAME);
				if(!hidePassword) {
					try {
						TenantOptionApi tenantOptionApi = platform.getTenantOptionApi();
						OptionPK optionPK = new OptionPK();
						optionPK.setCategory("secrets");
						optionPK.setKey("credentials."+Constants.OPTION_CREDENTIAL_KEY);
						OptionRepresentation or = tenantOptionApi.getOption(optionPK);
						tpConfigAsMap.put("password", or.getValue());
					} catch(SDKException sdke) {
						LOGGER.warn("Password is not stored yet.");
					}
					
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
	public String saveTicketingPlatformConfig(TicketingPlatformConfig tpConfig) {
		try {
			// Save the password securely
			storePassword(tpConfig.getPassword());
			
			// Save managed object
			tpConfig.setPassword(Constants.PLACEHOLDER_PASSWORD);
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
			mor.setName(Constants.CONFIG_MANAGED_OBJECT_NAME);
			mor.setProperty(Constants.TICKET_SERVICE_CONFIG_PROPERTY_NAME, tpConfig);
			mor.setProperty(Constants.DEVICE_ALARM_MAPPING_PROPERTY_NAME, null);
			ManagedObjectRepresentation newMor = inventoryApi.create(mor);
			
			if(newMor != null) {
				IdentityApi identityApi = platform.getIdentityApi();
				
				ExternalIDRepresentation extIDRep = new ExternalIDRepresentation();
				extIDRep.setExternalId(Constants.CONFIG_MANAGED_OBJECT_EXTERNAL_ID);
				extIDRep.setType(Constants.CONFIG_MANAGED_OBJECT_EXTERNAL_ID_TYPE);
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
	public String updateTicketingPlatformConfig(TicketingPlatformConfig tpConfig) throws SDKException {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation existingConfigManagedObject = this.getConfigManagedObject();
			if(existingConfigManagedObject == null) {
				LOGGER.info("Ticketing Platform config doesn't exist. Cannot be updated.");
				return null;
			} else {
				if(!tpConfig.getPassword().equals(Constants.PLACEHOLDER_PASSWORD)) {
					storePassword(tpConfig.getPassword());
				}
				tpConfig.setPassword(Constants.PLACEHOLDER_PASSWORD);
				existingConfigManagedObject.setLastUpdatedDateTime(null);
				existingConfigManagedObject.setProperty(Constants.TICKET_SERVICE_CONFIG_PROPERTY_NAME, tpConfig);
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
				List<DeviceAlarmMapping> existingDamList = (List<DeviceAlarmMapping>) existingConfigMO.getProperty(Constants.DEVICE_ALARM_MAPPING_PROPERTY_NAME);
				existingDamList = damList;
				existingConfigMO.setProperty(Constants.DEVICE_ALARM_MAPPING_PROPERTY_NAME, existingDamList);
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
				List<HashMap<String, String>> existingDAMappings = (ArrayList<HashMap<String, String>>) existingConfigMO.getProperty(Constants.DEVICE_ALARM_MAPPING_PROPERTY_NAME);
				
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
	
	private void storePassword(String password) {
		TenantOptionApi tenantOptionApi =  platform.getTenantOptionApi();
		OptionRepresentation or = new OptionRepresentation();
		or.setCategory("secrets");
		or.setKey("credentials."+Constants.OPTION_CREDENTIAL_KEY);
		or.setValue(password);
		tenantOptionApi.save(or);
	}
	
}
