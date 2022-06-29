package com.c8y.sag.service.tp;

import java.util.List;

import com.c8y.sag.model.TicketComment;
import com.c8y.sag.model.TicketRecord;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public interface TicketingPlatformService {

	public String createTicket(AlarmRepresentation alarmRep, String comments) throws Exception;
	
	public List<TicketRecord> getTickets(String statusId, String pageSize) throws Exception;
	
	public List<TicketComment> getTicketComments(String ticketId) throws Exception;
}
