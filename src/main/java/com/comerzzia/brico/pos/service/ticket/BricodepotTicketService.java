package com.comerzzia.brico.pos.service.ticket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditService;
import com.comerzzia.core.util.config.ComerzziaApp;
import com.comerzzia.core.util.mybatis.session.SqlSession;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.mediosPagos.MedioPagoBean;
import com.comerzzia.pos.persistence.mybatis.SessionFactory;
import com.comerzzia.pos.persistence.mybatis.SpringTransactionSqlSession;
import com.comerzzia.pos.persistence.tickets.TicketBean;
import com.comerzzia.pos.services.core.listeners.ListenersExecutor;
import com.comerzzia.pos.services.core.listeners.tipos.ticket.SalvadoTicketListener;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.ticket.Ticket;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.TicketsService;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.cabecera.FirmaTicket;
import com.comerzzia.pos.services.ticket.pagos.IPagoTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.xml.MarshallUtil;

@Primary
@Component
@SuppressWarnings("rawtypes")
public class BricodepotTicketService extends TicketsService {

	@Autowired
	private TicketAuditService ticketAuditService;
	@Autowired
	private ListenersExecutor listenersExecutor;

	@Override
	public synchronized void registrarTicket(Ticket ticket, TipoDocumentoBean tipoDocumento, boolean procesarTicket) throws TicketsServiceException {
		List<TicketAuditEvent> auditEvents = new ArrayList<>();
		if (((BricodepotCabeceraTicket) ticket.getCabecera()).getAuditEvents() != null) {
			auditEvents = ((BricodepotCabeceraTicket) ticket.getCabecera()).getAuditEvents();
		}
		log.debug("registrarTicket() - Procesando ticket ... ");
		SqlSession sqlSession = SpringContext.getBean(SpringTransactionSqlSession.class);
		byte[] xmlTicket = null;
		TicketBean ticketBean;
		try {
			// Establecemos fecha del ticket
			Date fecha = new Date();
			// LUST-121850 - Se añade log para comprobar que fecha recibimos antes de formatearla
			log.debug("registrarTicket() - Fecha de registro del ticket: " + fecha);
			ticket.setFecha(fecha);

			ticket.setFechaContable(sesion.getSesionCaja().getCajaAbierta().getFechaContable());

			ComerzziaApp comerzziaApp = ComerzziaApp.get();
			ticket.setSoftwareVersion(comerzziaApp.getVersionRevision());

			if (comerzziaApp.getLocalRepositoryVersion().equals(comerzziaApp.getRemoteRepositoryVersion())) {
				ticket.setLocalCopyVersion(comerzziaApp.getLocalRepositoryVersion());
			}
			else {
				ticket.setLocalCopyVersion(comerzziaApp.getLocalRepositoryVersion() + "|" + comerzziaApp.getRemoteRepositoryVersion());
			}

			sqlSession.openSession(SessionFactory.openSession());

			reiniciarContadoresLineas(ticket);

			IPagoTicket cambio = ticket.getTotales().getCambio();

			List<PagoTicket> pagos = ((TicketVenta) ticket).getPagos();

			// Borramos pagos a cero
			ListIterator<PagoTicket> listIterator = pagos.listIterator();
			while (listIterator.hasNext()) {
				PagoTicket pago = listIterator.next();
				if (BigDecimalUtil.isIgualACero(pago.getImporte())) {
					listIterator.remove();
				}
			}

			// Añadimos un pago a cero si el importe total es cero y no hay pagos
			if (BigDecimalUtil.isIgualACero(ticket.getCabecera().getTotales().getTotal()) && pagos.size() == 0) {
				PagoTicket pagoVacio = createPago();
				pagoVacio.setMedioPago(MediosPagosService.medioPagoDefecto);
				pagoVacio.setImporte(BigDecimal.ZERO);

				Integer paymentId = generateNewPaymentId(ticket);
				if (paymentId != null && paymentId > 0) {
					pagoVacio.setPaymentId(paymentId);
				}

				((TicketVenta) ticket).addPago(pagoVacio);
			}

			// Generamos movimientos de caja
			registrarMovimientosCaja((TicketVentaAbono) ticket, cambio, pagos, sqlSession);

			// Añadimos el cambio como un pago
			if (!BigDecimalUtil.isIgualACero(ticket.getCabecera().getTotales().getCambio().getImporte())) {
				IPagoTicket pagoCodMedPagoCambio = ((TicketVenta) ticket).getPago(cambio.getMedioPago().getCodMedioPago());
				MedioPagoBean medioPagoCambio = ticket.getCabecera().getTotales().getCambio().getMedioPago();

				if (pagoCodMedPagoCambio == null) {
					pagoCodMedPagoCambio = createPago();
					pagoCodMedPagoCambio.setEliminable(false);
					pagoCodMedPagoCambio.setMedioPago(medioPagoCambio);

					Integer paymentId = generateNewPaymentId(ticket);
					if (paymentId != null && paymentId > 0) {
						pagoCodMedPagoCambio.setPaymentId(paymentId);
					}

					((TicketVenta) ticket).addPago(pagoCodMedPagoCambio);
				}

				pagoCodMedPagoCambio.setImporte(pagoCodMedPagoCambio.getImporte().subtract(((TicketVenta) ticket).getTotales().getCambio().getImporte()));
			}

			String firma = generarFirma(sqlSession, ticket);

			log.debug("registrarTicket() - Ejecutando listeners posteriores al guardado del ticket");
			listenersExecutor.executeListeners(SalvadoTicketListener.class, "executeBeforeSave", sqlSession, ticket, tipoDocumento);

			// Construimos objeto persistente
			log.debug("registrarTicket() - Construyendo objeto persistente...");
			ticketBean = new TicketBean();
			ticketBean.setCodAlmacen(ticket.getCabecera().getTienda().getAlmacenBean().getCodAlmacen());
			ticketBean.setCodcaja(ticket.getCodCaja());
			ticketBean.setFecha(ticket.getFecha());
			ticketBean.setIdTicket(ticket.getIdTicket());
			ticketBean.setUidTicket(ticket.getUidTicket());
			ticketBean.setIdTipoDocumento(ticket.getCabecera().getTipoDocumento());
			ticketBean.setCodTicket(ticket.getCabecera().getCodTicket());
			ticketBean.setSerieTicket(ticket.getCabecera().getSerieTicket());
			ticketBean.setFirma(firma);
			ticketBean.setLocatorId(ticket.getCabecera().getLocalizador());

			String hashControl = ticket.getCabecera().getFirma().getHashControl();
			FirmaTicket firmaTicket = new FirmaTicket();
			firmaTicket.setHashControl(hashControl);
			firmaTicket.setFirma(ticketBean.getFirma());
			ticket.getCabecera().setFirma(firmaTicket);

			setFiscalData(ticket);

			log.debug("registrarTicket() - Generando XML del ticket...");
			xmlTicket = MarshallUtil.crearXML(ticket);
			ticketBean.setTicket(xmlTicket);

			log.debug("registrarTicket() - TICKET INSERT: " + ticket.getUidTicket());
			log.trace(new String(xmlTicket, "UTF-8") + "\n");

			insertarTicket(sqlSession, ticketBean, false);

			log.debug("registrarTicket() - Ejecutando listeners posteriores al guardado del ticket");
			listenersExecutor.executeListeners(SalvadoTicketListener.class, "executeAfterSave", sqlSession, ticket, tipoDocumento, ticketBean);

			log.debug("registrarTicket() - Eliminando copia de seguridad...");
			copiaSeguridadTicketService.eliminarBackup(sqlSession, ticketBean.getUidTicket());
			log.debug("registrarTicket() - Confirmando transacción...");
			sqlSession.commit();
			log.debug("registrarTicket() - Ticket salvado correctamente.");
		}
		catch (Throwable e) {
			try {
				sqlSession.rollback();
			}
			catch (Exception e2) {
				log.error("registrarTicket() - " + e2.getClass().getName() + " - " + e2.getLocalizedMessage(), e2);
			}
			String msg = "Se ha producido un error procesando ticket con uid " + ticket.getUidTicket() + " : " + e.getMessage();
			log.error("registrarTicket() - " + msg, e);
			throw new TicketsServiceException(e);
		}
		finally {
			sqlSession.close();
		}

		if (ticketBean != null && xmlTicket != null && procesarTicket) {
			try {
				log.debug("registrarTicket() - Procesando ticket...");
				procesarTicket(ticketBean, xmlTicket);
			}
			catch (Exception e) {
				log.warn("registrarTicket() - Ha ocurrido un error procesando ticket: " + e.getMessage(), e);
			}
		}

		try {
			log.debug("registrarTicket() - Ejecutando listeners posteriores al commit del ticket");
			listenersExecutor.executeListeners(SalvadoTicketListener.class, "executeAfterCommit", sqlSession, ticket, tipoDocumento);
		}
		catch (Exception e) {
			throw new TicketsServiceException(e);
		}

		for (TicketAuditEvent auditEvent : auditEvents) {
			auditEvent.setUidTicketVenta(ticket.getUidTicket());
			ticketAuditService.saveAuditEvent(auditEvent);
		}
	}

}