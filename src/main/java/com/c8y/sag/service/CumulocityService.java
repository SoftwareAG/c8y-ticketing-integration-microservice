package com.c8y.sag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.alarm.AlarmApi;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

@Service
public class CumulocityService {

	@Autowired
	private Platform platform;
	
	public AlarmRepresentation getAlarmById(String id) {
		try {
			AlarmApi alarmApi = platform.getAlarmApi();
			GId gid = new GId(id);
			return alarmApi.getAlarm(gid);
		} catch(SDKException sdke) {
			throw sdke;
		}
	}
	
	public void acknowledgeAlarm(AlarmRepresentation alarmRep) {
		try {
			AlarmApi alarmApi = platform.getAlarmApi();
			alarmRep.setStatus("ACKNOWLEDGED");
			alarmApi.update(alarmRep);
		} catch(SDKException sdke) {
			throw sdke;
		}
	}
}
