package com.c8y.sag.subscriber;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class AlarmNotification {

	private Object data;
	private String realtimeAction;
	
	public AlarmNotification() {
		
	}
	
	public AlarmNotification(Object data, String realtimeAction) {
		this.data = data;
		this.realtimeAction = realtimeAction;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getRealtimeAction() {
		return realtimeAction;
	}

	public void setRealtimeAction(String realtimeAction) {
		this.realtimeAction = realtimeAction;
	}
	
}
