package com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.comerzzia.brico.pos.selfcheckout.persistence.motivos.Motivo;
import com.comerzzia.pos.services.ticket.cabecera.ISubtotalIvaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Primary
@Scope("prototype")
@SuppressWarnings({"unchecked"})
@XmlAccessorType(XmlAccessType.FIELD)
public class BricodepotLineaTicket extends LineaTicket {

	private static final long serialVersionUID = 1788007908872222861L;

	@XmlElement(name = "motivo")
	private Motivo motivo;

	@XmlElement(name = "isAnticipo")
	private Boolean isAnticipo = false;
	
	private String numPresupuesto;
	
	private boolean conversionAFT;

	private BigDecimal porcentajeIvaConversion;

	private String porcentajeIva;
	
	public Motivo getMotivo() {
		return motivo;
	}

	public void setMotivo(Motivo motivo) {
		this.motivo = motivo;
	}

	private String ivaLinea;

	public String obtenerPorcentajeImpuestoArticulo(LineaTicket linea) {
		List<ISubtotalIvaTicket> porcentajes = cabecera.getSubtotalesIva();
		for (ISubtotalIvaTicket imp : porcentajes) {
			if (imp.getCodImpuesto().equals(linea.getCodImpuesto())) {
				String porcentajeArticulo = imp.getPorcentajeAsString().split(",")[0];
				return porcentajeArticulo;
			}
		}
		return null;
	}

	public String getIvaLinea() {

		return ivaLinea;
	}

	public void setIvaLinea(String ivaLinea) {
		this.ivaLinea = ivaLinea;
	}

	public String getNumPresupuesto() {
		return numPresupuesto;
	}

	public void setNumPresupuesto(String numPresupuesto) {
		this.numPresupuesto = numPresupuesto;
	}
	
	public Boolean getIsAnticipo() {
		return isAnticipo;
	}

	public void setIsAnticipo(Boolean isAnticipo) {
		this.isAnticipo = isAnticipo;
	}
	
	public boolean isConversionAFT() {
		return conversionAFT;
	}

	public void setConversionAFT(boolean conversionAFT) {
		this.conversionAFT = conversionAFT;
	}

	public BigDecimal getPorcentajeIvaConversion() {
		return porcentajeIvaConversion;
	}

	public void setPorcentajeIvaConversion(BigDecimal porcentajeIvaConversion) {
		this.porcentajeIvaConversion = porcentajeIvaConversion;
	}

	public String getPorcentajeIva() {
		porcentajeIva = obtenerPorcentajeImpuestoArticulo(this);
		return porcentajeIva;
	}
	
	public String setPorcentajeIva(String porcentajeIva) {
		this.porcentajeIva = porcentajeIva;
		return this.porcentajeIva;
	}
}
