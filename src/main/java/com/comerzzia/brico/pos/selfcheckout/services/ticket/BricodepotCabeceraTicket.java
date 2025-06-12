package com.comerzzia.brico.pos.selfcheckout.services.ticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.brico.pos.util.format.BricodepotFormatUtil;
import com.comerzzia.pos.services.ticket.cabecera.CabeceraTicket;

@XmlAccessorType(XmlAccessType.FIELD)
@Component
@Primary
@Scope("prototype")
public class BricodepotCabeceraTicket extends CabeceraTicket {

	protected static final Logger log = Logger.getLogger(BricodepotCabeceraTicket.class);
	
	@XmlElement(name = "email_envio_ticket")
	private String emailEnvioTicket;

	@XmlElement(name = "codigo_postal_venta")
	private String cpVenta;

	@XmlElement(name = "intervencion")
	private String intervencion;

	@XmlElementWrapper(name = "eventos_auditoria")
	@XmlElement(name = "evento")
	protected List<TicketAuditEvent> auditEvents;

	@XmlElement(name = "tipo_impresion")
	protected String tipoImpresion;

	@XmlElement(name = "fechaTicketOrigen")
	protected String fechaTicketOrigen;

	@XmlElement(name = "fechaDevolucion")
	protected String fechaDevolucion;

	@XmlElement(name = "conversion_fs_ft")
	private Boolean fsToFt;

	public String getEmailEnvioTicket() {
		return emailEnvioTicket;
	}

	public void setEmailEnvioTicket(String emailEnvioTicket) {
		this.emailEnvioTicket = emailEnvioTicket;
	}

	public String getCpVenta() {
		return cpVenta;
	}

	public String getIntervencion() {
		return intervencion;
	}

	public void setIntervencion(String intervencion) {
		this.intervencion = intervencion;
	}

	public void setIntervencion(Boolean intervencion) {
		this.intervencion = intervencion ? "S" : "N";
	}

	public void setCpVenta(String cpVenta) {
		this.cpVenta = cpVenta;
	}

	public void addAuditEvent(TicketAuditEvent auditEvent) {
		if (this.auditEvents == null) {
			this.auditEvents = new ArrayList<>();
		}
		this.auditEvents.add(auditEvent);
	}

	public List<TicketAuditEvent> getAuditEvents() {
		return auditEvents;
	}

	public void setAuditEvents(List<TicketAuditEvent> auditEvents) {
		this.auditEvents = auditEvents;
	}

	public String getTipoImpresion() {
		return tipoImpresion;
	}

	public void setTipoImpresion(String tipoImpresion) {
		this.tipoImpresion = tipoImpresion;
	}

	public String getFechaTicketOrigen() {
		return fechaTicketOrigen;
	}

	public void setFechaTicketOrigen(String fechaTicketOrigen) {
		this.fechaTicketOrigen = fechaTicketOrigen;
	}

	public String getFechaDevolucion() {
		return fechaDevolucion;
	}

	public void setFechaDevolucion(String fechaDevolucion) {
		this.fechaDevolucion = fechaDevolucion;
	}

	public Boolean getFsToFt() {
		return fsToFt;
	}

	public void setFsToFt(Boolean fsToFt) {
		this.fsToFt = fsToFt;
	}
	
	@Override
	public Date getFecha() {
		try {
			return super.getFecha();
		} catch (NumberFormatException e) {
			log.error("getFecha() - Error obteniendo fecha: " + e.getMessage());
			return BricodepotFormatUtil.getInstance().desformateaFechaHoraTicket(fecha);

		}
	}
	
}
