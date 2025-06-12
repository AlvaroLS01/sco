package com.comerzzia.brico.pos.gui.ventas.tickets;

import org.springframework.stereotype.Component;

import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
 
@Component
public class SesionTicketManager {
	
	protected TicketManager sesionTicketManager;
	
	public TicketManager getSesionTicketManager() {
		return sesionTicketManager;
	}

	public void setSesionTicketManager(TicketManager sesionTicketManager) {
		this.sesionTicketManager = sesionTicketManager;
	}
}
