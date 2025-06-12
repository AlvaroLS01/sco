package com.comerzzia.brico.pos.selfcheckout.services.cliente;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.context.annotation.Scope;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "ticketClienteCaptacion")
@Scope("prototype")
public class TicketClienteCaptacion {

	@XmlElement(name = "pdf_cliente")
	protected byte[] pdfCliente;

	@XmlElement(name = "cif_cliente")
	protected String cif;

	@XmlElement(name = "fecha_alta")
	protected String fechaAlta;

	@XmlElement(name = "operacion") // ALTA o MOD(ificación)
	protected String operacion;

	public byte[] getPdfCliente() {
		return pdfCliente;
	}

	public void setPdfCliente(byte[] pdfCliente) {
		this.pdfCliente = pdfCliente;
	}

	public String getCif() {
		return cif;
	}

	public void setCif(String cif) {
		this.cif = cif;
	}

	public String getFechaAlta() {
		return fechaAlta;
	}

	public void setFechaAlta(String fechaAlta) {
		this.fechaAlta = fechaAlta;
	}

	public String getOperacion() {
		return operacion;
	}

	public void setOperacion(String operacion) {
		this.operacion = operacion;
	}

}

