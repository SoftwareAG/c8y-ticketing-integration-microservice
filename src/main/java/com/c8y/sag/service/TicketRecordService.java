package com.c8y.sag.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.c8y.sag.constants.Constants;
import com.c8y.sag.model.TicketCreationRecord;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class TicketRecordService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TicketRecordService.class);
	
	
	@Autowired
	private Platform platform;
	
	public void addRecord(AlarmRepresentation alarmRep, String ticketId) {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
			mor.setName(Constants.TICKET_CREATION_RECORD_NAME);
			mor.setType(Constants.TICKET_CREATION_RECORD_TYPE);
			mor.setProperty(Constants.TICKET_CREATION_RECORD_FRAGMENT_PREFIX + alarmRep.getSource().getId().getValue(), "{}");
			mor.setProperty("deviceId", alarmRep.getSource().getId().getValue());
			mor.setProperty("alarmId", alarmRep.getId().getValue());
			mor.setProperty("ticketId", ticketId);
			ManagedObjectRepresentation newMOR = inventoryApi.create(mor);
			LOGGER.info("Ticket Creation Record created: "+newMOR.getId().getValue());
		} catch(SDKException sdke) {
			throw sdke;
		}
	}
	
	public List<TicketCreationRecord> getRecords(String deviceId) {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			InventoryFilter inventoryFilter = null;
			if(deviceId == null || deviceId.isBlank()) {
				inventoryFilter = new InventoryFilter().byType(Constants.TICKET_CREATION_RECORD_TYPE);
			} else {
				inventoryFilter = new InventoryFilter().byFragmentType(Constants.TICKET_CREATION_RECORD_FRAGMENT_PREFIX + deviceId);
			}
			ManagedObjectCollection moc = inventoryApi.getManagedObjectsByFilter(inventoryFilter);
			List<TicketCreationRecord> tcrList = new ArrayList<TicketCreationRecord>();
			if(moc != null) {
				for(ManagedObjectRepresentation mor: moc.get().allPages()) {
					tcrList.add(map(mor));
				}
			}
			return tcrList;
		} catch(SDKException sdke) {
			throw sdke;
		}
	}
	
	public void deleteTicketCreationRecords() {
		try {
			InventoryApi inventoryApi = platform.getInventoryApi();
			InventoryFilter inventoryFilter = new InventoryFilter().byType(Constants.TICKET_CREATION_RECORD_TYPE);
			ManagedObjectCollection moc = inventoryApi.getManagedObjectsByFilter(inventoryFilter);
			if(moc != null) {
				for(ManagedObjectRepresentation mor: moc.get().allPages()) {
					inventoryApi.delete(mor.getId());
					LOGGER.info("Deleted Ticket Creation Record: "+mor.getId().getValue());
				}
			}
		} catch(SDKException sdke) {
			throw sdke;
		}
	}
	
	private TicketCreationRecord map(ManagedObjectRepresentation mor) {
		TicketCreationRecord tcr = new TicketCreationRecord();
		tcr.setId(mor.getId().getValue());
		tcr.setAlarmId((String) mor.getProperty("alarmId"));
		tcr.setTicketId((String) mor.getProperty("ticketId"));
		tcr.setDeviceId((String) mor.getProperty("deviceId"));
		return tcr;
	}
	
}
