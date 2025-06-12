package com.comerzzia.brico.pos.selfcheckout.services.intervenciones;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "intervenciones")
public class IntervencionesDto {

	@XmlElement(name = "uidActividad")
	private String uidActividad;
	
	@XmlElement(name = "uidTicket")
	private String uidTicket;
	
	@XmlElement(name = "codalm")
	private String codalm;
	
	@XmlElement(name = "codcaja")
	private String codcaja;
	
	@XmlElement(name = "fecha")
	private Date fecha;
	
	@XmlElement(name = "idUsuario")
	private Long idUsuario;

	public String getUidActividad() {
		return uidActividad;
	}

	public void setUidActividad(String uidActividad) {
		this.uidActividad = uidActividad;
	}

	public String getUidTicket() {
		return uidTicket;
	}

	public void setUidTicket(String uidTicket) {
		this.uidTicket = uidTicket;
	}

	public String getCodalm() {
		return codalm;
	}

	public void setCodalm(String codalm) {
		this.codalm = codalm;
	}

	public String getCodcaja() {
		return codcaja;
	}

	public void setCodcaja(String codcaja) {
		this.codcaja = codcaja;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

}
