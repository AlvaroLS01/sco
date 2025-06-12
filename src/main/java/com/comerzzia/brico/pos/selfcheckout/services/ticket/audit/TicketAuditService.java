package com.comerzzia.brico.pos.selfcheckout.services.ticket.audit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.comerzzia.core.util.xml.XMLDocumentException;
import com.comerzzia.core.util.xml.XMLDocumentUtils;
import com.comerzzia.pos.persistence.tickets.TicketBean;
import com.comerzzia.pos.services.core.contadores.ServicioContadores;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.ticket.TicketsService;
import com.comerzzia.pos.util.xml.MarshallUtil;

/**
 * 
 * @author jbn
 * @since 27/08/2021
 * @version 1.0
 */
@Service
public class TicketAuditService {

	public static final String AUDITABLE_EVENT_ID_COUNTER = "ID_AUDITABLE_EVENT";

	protected static final Logger log = Logger.getLogger(TicketAuditService.class);

	@Autowired
	private Sesion sesion;
	@Autowired
	private ServicioContadores contadoresService;
	@Autowired
	private TicketsService ticketsService;

	/**
	 * Convierte un evento de auditoria en un ticket y lo guarda en DB
	 * 
	 * @see com.comerzzia.bricodepot.pos.gui.ventas.tickets.BricodepotTicketManager
	 * @see com.comerzzia.bricodepot.pos.services.ticket.BricodepotTicketService
	 * 
	 * @param auditEvent Evento
	 */
	public synchronized void saveAuditEvent(TicketAuditEvent auditEvent) {
		byte[] xmlTicket = null;
		TicketBean ticket;
		log.debug("registerAuditEvent() - Construyendo objeto persistente");
		try {
			// Construimos objeto persistente
			ticket = new TicketBean();

			// uid documento
			String uidTicket = UUID.randomUUID().toString();
			ticket.setUidTicket(uidTicket);
			// id documento
			Long idTicket = contadoresService.obtenerValorContador(AUDITABLE_EVENT_ID_COUNTER,
					sesion.getAplicacion().getUidActividad());
			ticket.setIdTicket(idTicket);
			// serie documento
			String serieTicket = auditEvent.getCodAlmacen() + "/"
					+ sesion.getSesionCaja().getCajaAbierta().getCodCaja();
			ticket.setSerieTicket(serieTicket);
			// cod documento
			String codigoTicket = auditEvent.getCodAlmacen() + "/"
					+ sesion.getSesionCaja().getCajaAbierta().getCodCaja() + "/" + String.format("%08d", idTicket);
			ticket.setCodTicket(codigoTicket);
			// firma documento (no
			ticket.setFirma("*");
			// tipo documento
			ticket.setIdTipoDocumento(661166L);

			ticket.setCodAlmacen(auditEvent.getCodAlmacen());
			ticket.setCodcaja(sesion.getSesionCaja().getCajaAbierta().getCodCaja());
			ticket.setFecha(auditEvent.getMoment());

			// localizador
			// formato: yyMMdd[codalmacen][idticket con padding][3 ultimos caracteres del
			// uid ticket]
			SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
			String locator = format.format(auditEvent.getMoment()) + auditEvent.getCodAlmacen()
					+ String.format("%06d", idTicket) + StringUtils.right(ticket.getUidTicket(), 3);
			ticket.setLocatorId(locator);

			// xml
			xmlTicket = MarshallUtil.crearXML(auditEvent);
			ticket.setTicket(xmlTicket);

			log.debug("registerAuditEvent() - Saving ticket");
			ticketsService.insertarTicket(null, ticket, false);
		} catch (Exception e) {
			log.error("registerAuditEvent() - Error saving document: " + e.getMessage());
		} finally {

		}

	}

	/**
	 * Devuelve una lista de TicketAuditEvent a partir de el XML de un ticket
	 * 
	 * @param byteArray Byte array de un ticket
	 * @return Se devuelve una lista vacia si no encuentra eventos
	 * @throws Exception
	 */
	public static List<TicketAuditEvent> EventsFromByteArray(byte[] byteArray) throws Exception {	
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(byteArray));
			return getEventsFromByteArray(doc);
		} catch (Exception e) {
			log.error("getEventsFromByteArray() - Error parsing document: " + e.getMessage());
			//throw new AuditServiceException(e);
			return new ArrayList<>();
		}
	}

	/**
	 * Devuelve eventos de auditoria de la cabecera de un ticket
	 * 
	 * @param document
	 * @return Una lista the eventos, vacia si no hay nada en el ticket
	 * @throws AuditServiceException 
	 * @throws XMLDocumentException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 */
	public static List<TicketAuditEvent> getEventsFromByteArray(Document document) throws AuditServiceException {
		List<TicketAuditEvent> res = new ArrayList<>();
		try {
			Element cabecera = XMLDocumentUtils.getElement(document.getDocumentElement(), "cabecera",
					true);
			Element eventosWrapper = XMLDocumentUtils.getElement(cabecera, "eventos_auditoria",
					true);
			if (eventosWrapper.hasChildNodes()) {
				List<Element> eventos = XMLDocumentUtils.getChildElements(eventosWrapper);
				for (Element e : eventos) {
					TicketAuditEvent auditEvent = (TicketAuditEvent) MarshallUtil.leerXML(ByteArrayFromElement(e),
							TicketAuditEvent.class);
					res.add(auditEvent);
				}
			}
			return res;
		} catch (Exception e) {
			log.error("getEventsFromByteArray() - Error parsing document: " + e.getMessage());
			throw new AuditServiceException(e);
		}
	}

	/**
	 * Convert element to byte array
	 * 
	 * @param element
	 * @return
	 * @throws AuditServiceException 
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 */
	public static byte[] ByteArrayFromElement(Element element) throws AuditServiceException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ElementToStream(element, outputStream);
			return outputStream.toByteArray();
		} catch (AuditServiceException e) {
			log.error("ByteArrayFromElement() - Error parsing document: " + e.getMessage());
			throw new AuditServiceException(e);
		}	
	}

	/**
	 * Convert dom element to outputstream Code from:
	 * http://www.java2s.com/Code/Java/XML/ConvertElementToStream.htm
	 * 
	 * @param element
	 * @param out
	 * @throws AuditServiceException 
	 */
	public static void ElementToStream(Element element, OutputStream out) throws AuditServiceException {
		try {
			DOMSource source = new DOMSource(element);
			StreamResult result = new StreamResult(out);
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.transform(source, result);
		} catch (Exception e) {
			log.error("ElementToStream() - Error converting element to stream: " + e.getMessage());
			throw new AuditServiceException(e);
		}
	}

}
