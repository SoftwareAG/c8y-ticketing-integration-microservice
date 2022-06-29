package com.c8y.sag.utils;

import com.c8y.sag.model.DeviceAlarmMapping;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class Utils {

	public static boolean isAlarmEligibleForTicket(AlarmRepresentation alarmRep, DeviceAlarmMapping daMapping) {
		if(alarmRep != null) {
			if(alarmRep.getStatus().equals("ACTIVE")) {
				if(daMapping.getDeviceId().equals(alarmRep.getSource().getId().getValue()) && daMapping.getAlarmType().equals(alarmRep.getType())) {
					return true;
				}
			}
		}
		return false;
	}
}
