package com.comerzzia.brico.pos.selfcheckout.services.ticket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas.BricodepotLineaTicket;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.cabecera.ISubtotalIvaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.format.FormatUtil;

@Component
@Primary
@XmlRootElement(name = "ticket")
@Scope("prototype")
public class SelfCheckoutTicketVentaAbono extends TicketVentaAbono {

	private Logger log = Logger.getLogger(SelfCheckoutTicketVentaAbono.class);
	public static final String CODIGO_PAGO_ECOMMERCE = "0402";
	public List<LineaTicket> getLineasAgrupadas() {
		List<LineaTicket> lineasOriginales = lineas;
		try {

			List<Integer> lineasYaAgrupadas = new ArrayList<Integer>();
			List<LineaTicket> nuevasLineas = new ArrayList<LineaTicket>();

			List<LineaTicket> copiaLineasAux = new ArrayList<LineaTicket>(lineas);

			for (LineaTicket linea : lineas) {
				if (!lineasYaAgrupadas.contains(linea.getIdLinea())) {
					lineasYaAgrupadas.add(linea.getIdLinea());

					if (copiaLineasAux.contains(linea)) {
						copiaLineasAux.remove(linea);
					}

					BigDecimal cantidadTotal = linea.getCantidad();
					BigDecimal importeTotalPromociones = linea.getImporteTotalPromociones();
					BigDecimal importeTotalConDto = linea.getImporteTotalConDto();
					BigDecimal importeConDto = linea.getImporteConDto();
					Iterator<LineaTicket> itAux = copiaLineasAux.iterator();

					while (itAux.hasNext()) {
						LineaTicket lineaAux = itAux.next();

						if (tienenMismasCondicionesVenta(linea, lineaAux)) {
							itAux.remove();
							lineasYaAgrupadas.add(lineaAux.getIdLinea());
							copiaLineasAux.remove(lineaAux);

							cantidadTotal = cantidadTotal.add(lineaAux.getCantidad());
							importeTotalPromociones = importeTotalPromociones.add(lineaAux.getImporteTotalPromociones());
							importeTotalConDto = importeTotalConDto.add(lineaAux.getImporteTotalConDto());
							importeConDto  = importeConDto.add(lineaAux.getImporteConDto());
						}
					}

					if (!BigDecimalUtil.isIgualACero(cantidadTotal)) {
						linea.setImporteTotalConDto(importeTotalConDto);
						linea.setCantidad(cantidadTotal);
						linea.setImporteTotalPromociones(importeTotalPromociones);
						linea.setImporteConDto(importeConDto);
						
						//Operación para extraer el porcentaje de iva de cada artículo.
						List<ISubtotalIvaTicket> porcentajes = this.getCabecera().getSubtotalesIva();
						for (ISubtotalIvaTicket imp : porcentajes) {
							if(imp.getCodImpuesto().equals(linea.getCodImpuesto())) {
								linea.setCodImpuesto(imp.getPorcentaje().toString().split("\\.")[0]);
								 BigDecimal lineaIva = BigDecimalUtil.redondear(BigDecimalUtil.porcentaje(linea.getPrecioConDto().multiply(cantidadTotal), imp.getPorcentaje()), 4);
								if(linea instanceof BricodepotLineaTicket) {
									((BricodepotLineaTicket)linea).setIvaLinea(FormatUtil.getInstance().formateaNumero(lineaIva, 4));									
								}
							}
						}
						nuevasLineas.add(linea);
					}
				}
			}
			lineas = nuevasLineas;
			return nuevasLineas;
		}
		catch (Exception e) {
			log.error("agruparLineas() - Ha habido un error al agrupar líneas: " + e.getMessage(), e);
			return lineasOriginales;
		}
	}

	private boolean tienenMismasCondicionesVenta(LineaTicket linea, LineaTicket lineaAux) {
		if (!linea.getCodArticulo().equals(lineaAux.getCodArticulo())) {
			return false;
		}
		if (!BigDecimalUtil.isIgual(linea.getPrecioTotalConDto(), lineaAux.getPrecioTotalConDto())) {
			return false;
		}
		if(linea.getCantidad().signum() != lineaAux.getCantidad().signum()) {
			return false;
		}
		return true;
	}
	
	public String getNumTarjetaRegalo() {
		String numTarjetaRegalo = "";
		for (PagoTicket pago : pagos) {
				if(pago.getGiftcards()!=null && !pago.getGiftcards().isEmpty()) {
				numTarjetaRegalo = numTarjetaRegalo + " " + pago.getGiftcards().get(0).getNumTarjetaRegalo();
			}
		}
		numTarjetaRegalo = numTarjetaRegalo.trim();
		numTarjetaRegalo.replace(" ", "/");
        if (numTarjetaRegalo.endsWith("/")) {
        	numTarjetaRegalo = numTarjetaRegalo.substring(0, numTarjetaRegalo.length() - 1);
        }	
		return StringUtils.isNotBlank(numTarjetaRegalo) ? numTarjetaRegalo : "";
	}
	
	public String getNumPedido() {

		for (LineaTicket linea : lineas) {
			String numPresupuesto = ((BricodepotLineaTicket) linea).getNumPresupuesto();
			if (StringUtils.isNotBlank(numPresupuesto)) {
				return numPresupuesto;
			}
		}
		// ((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).getNumPresupuesto();

		for (PagoTicket pago : pagos) {
			if (pago.getCodMedioPago().equals(CODIGO_PAGO_ECOMMERCE) && pago.getExtendedData().containsKey("documento")) {
				return (String) pago.getExtendedData().get("documento");
			}
		}

		return null;
	}
}
