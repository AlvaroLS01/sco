package com.comerzzia.brico.pos.selfcheckout.services.fidelizado;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.context.annotation.Scope;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "ticketFidelizadoCaptacion")
@Scope("prototype")
public class TicketFidelizadoCaptacion {

	@XmlElement(name = "pdf_fidelizado")
	protected byte[] pdfFidelizado;

	@XmlElement(name = "id_fidelizado")
	protected long idFidelizado;

	@XmlElement(name = "fecha_alta")
	protected String fechaAlta;

	public byte[] getPdfFidelizado() {
		return pdfFidelizado;
	}

	public void setPdfFidelizado(byte[] pdfFidelizado) {
		this.pdfFidelizado = pdfFidelizado;
	}

	public long getIdFidelizado() {
		return idFidelizado;
	}

	public void setIdFidelizado(long idFidelizado) {
		this.idFidelizado = idFidelizado;
	}

	public String getFechaAlta() {
		return fechaAlta;
	}

	public void setFechaAlta(String fechaAlta) {
		this.fechaAlta = fechaAlta;
	}

}
