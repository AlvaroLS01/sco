package com.comerzzia.brico.pos.service.ticket.copiaSeguridad;

import java.io.UnsupportedEncodingException;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.copiaSeguridad.CopiaSeguridadTicketService;
import com.comerzzia.pos.util.xml.MarshallUtil;
import com.comerzzia.pos.util.xml.MarshallUtilException;

@Service
@Primary
public class BricodepotCopiaSeguridadTicketService extends CopiaSeguridadTicketService {

	@Override
	@SuppressWarnings("rawtypes")
	public synchronized void guardarBackupTicketActivo(TicketVenta ticket) throws TicketsServiceException {
		try {
			super.guardarBackupTicketActivo(ticket);
		}
		catch (NumberFormatException e) {
			String msg = "Se ha producido un error eliminando la copia de seguridad del ticket debido a un formato incorrecto: " + e.getMessage();
			log.error("guardarBackupTicketActivo() - " + msg, e);
			try {
				byte[] xmlTicket = MarshallUtil.crearXML(ticket);
				log.error("TICKET: " + ticket.getUidTicket() + "\n" + new String(xmlTicket, "UTF-8") + "\n");
			}
			catch (MarshallUtilException | UnsupportedEncodingException e1) {
				log.error("Error al intentar parsear el xml del ticket de venta", e1);
			}
			throw new TicketsServiceException(e);
		}
	}
}
