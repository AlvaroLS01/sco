package com.comerzzia.brico.pos.service.conversion;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.SelfCheckoutTicketVentaAbono;
import com.comerzzia.core.servicios.empresas.EmpresaException;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.cabecera.DatosDocumentoOrigenTicket;
import com.comerzzia.pos.services.ticket.cabecera.SubtotalIvaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.pagos.IPagoTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.promociones.PromocionLineaTicket;
import com.comerzzia.pos.util.config.SpringContext;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Service
public class ConversionService {

	private static final Logger log = Logger.getLogger(ConversionService.class.getName());

	@Autowired
	protected Sesion sesion;

	public void generarFT(TicketVenta ticketPrincipal, TicketVenta ticketOrigen, Long contadorFT, DatosDocumentoOrigenTicket datosDocOrigen, DatosFactura datosFacturacion) {
		log.debug("generaFT() - Generando documento Factura Completa...");
		try {
			if (!datosDocOrigen.getCodTipoDoc().equals("FLEX")) {

				rellenarCabecera(ticketPrincipal, ticketOrigen, contadorFT);
				rellenerSubtotales(ticketPrincipal, ticketOrigen);
				rellenarLineas(ticketPrincipal, ticketOrigen);
				rellenarPagos(ticketPrincipal, ticketOrigen);

				// Total cantidad artículos
				((TicketVentaAbono) ticketPrincipal).getCantidadTotal();

				ticketPrincipal.setDatosFacturacion(datosFacturacion);
				ticketPrincipal.getCabecera().setDatosDocOrigen(datosDocOrigen);
				// BRICOD-504 - Seteamos la fecha de devolución tras inicializar el nuevo ticket
				if (ticketOrigen.getCabecera() != null) {
					((BricodepotCabeceraTicket) ticketPrincipal.getCabecera()).setFechaTicketOrigen(ticketOrigen.getCabecera().getFechaAsLocale());
				}
			}

			// BRICOD-703-Pasamos-informacion-fidelizado
			if (((SelfCheckoutTicketVentaAbono) ticketOrigen).getCabecera().getDatosFidelizado() != null) {
				((SelfCheckoutTicketVentaAbono) ticketPrincipal).getCabecera().setDatosFidelizado(((SelfCheckoutTicketVentaAbono) ticketOrigen).getCabecera().getDatosFidelizado());
			}
			
			ticketPrincipal.getTotales().recalcular();

		}
		catch (Exception e) {
			log.error("generaFT() - Ha habido un problema al guardar la nota de crédito: " + e.getMessage(), e);
		}
	}

	private void rellenarPagos(TicketVenta ticketPrincipal, TicketVenta ticketOrigen) {
		List<PagoTicket> pagos = ticketOrigen.getPagos();
		for (PagoTicket pago : pagos) {
			pago.setImporte(pago.getImporte().abs());
		}

		// BRICOD-871 - Se le añade el cambio al pago para cuadrar los movimientos
		for (PagoTicket pago : pagos) {
			if (pago.getCodMedioPago().equals(MediosPagosService.medioPagoDefecto.getCodMedioPago())) {
				BigDecimal cambio = ticketPrincipal.getCabecera().getTotales().getCambio().getImporte();
				pago.setImporte(pago.getImporte().add(cambio));
				break;
			}
		}
		ticketPrincipal.setPagos(pagos);
	}

	private void rellenarLineas(TicketVenta ticketPrincipal, TicketVenta ticketOrigen) {
		List<LineaTicket> lineaTicketOrigen = ticketOrigen.getLineas();
		for (LineaTicket linea : lineaTicketOrigen) {

			LineaTicket lineaActual = linea.clone();
			
			if(lineaActual.getPromociones() != null && !lineaActual.getPromociones().isEmpty()) {
				for(PromocionLineaTicket promoLinea : linea.getPromociones()) {
					if(promoLinea.isDescuentoMenosMargen()) {
						promoLinea.setImporteTotalDto(promoLinea.getImporteTotalDtoMenosMargen());
					}
				}
			}
			
			lineaActual.recalcularImporteFinal();
			
			((TicketVentaAbono) ticketPrincipal).addLinea(lineaActual);
		}
	}

	private void rellenerSubtotales(TicketVenta ticketPrincipal, TicketVenta ticketOrigen) {
		for (SubtotalIvaTicket subtotalIva : (List<SubtotalIvaTicket>) ticketOrigen.getCabecera().getSubtotalesIva()) {
			((TicketVentaAbono) ticketPrincipal).getCabecera().getSubtotalesIva().add(subtotalIva);
		}
	}

	private void rellenarCabecera(TicketVenta ticketPrincipal, TicketVenta ticketOrigen, Long contadorFT) {
		BricodepotCabeceraTicket cabeceraTicket = (BricodepotCabeceraTicket) ticketPrincipal.getCabecera();
		BricodepotCabeceraTicket origenCabecera = (BricodepotCabeceraTicket) ticketOrigen.getCabecera();

		cabeceraTicket.setIdTicket(contadorFT);
		cabeceraTicket.setCodTicket(cabeceraTicket.getCodTipoDocumento() + " " + cabeceraTicket.getFechaAsLocale().substring(6, 10) + cabeceraTicket.getCliente().getCodCliente()
		        + cabeceraTicket.getCodCaja() + "/" + StringUtils.leftPad(cabeceraTicket.getIdTicket().toString(), 8, "0"));
		cabeceraTicket.setFechaTicketOrigen(ticketOrigen.getCabecera().getFechaAsLocale());
		((TicketVentaAbono) ticketPrincipal).setPromociones(ticketOrigen.getPromociones());
		((TicketVentaAbono) ticketPrincipal).setCuponesEmitidos(ticketOrigen.getCuponesEmitidos());
		((TicketVentaAbono) ticketPrincipal).setCuponesAplicados(ticketOrigen.getCuponesAplicados());

		((TicketVentaAbono) ticketPrincipal).getCabecera().setCantidadArticulos(ticketOrigen.getCabecera().getCantidadArticulos());
		
		cabeceraTicket.setDatosEnvio(origenCabecera.getDatosEnvio());

		if (ticketPrincipal.getTotales().getCambio() == null) {
			IPagoTicket cambio = SpringContext.getBean(PagoTicket.class);
			cambio.setMedioPago(MediosPagosService.medioPagoDefecto);
			cambio.setImporte(BigDecimal.ZERO);
			ticketPrincipal.getTotales().setCambio(cambio);
		}
	}

	public DatosSesionBean generarDatosSesion() throws EmpresaException {
		DatosSesionBean datosSesion = new DatosSesionBean();
		datosSesion.setUidActividad(sesion.getAplicacion().getUidActividad());
		datosSesion.setUidInstancia(sesion.getAplicacion().getUidInstancia());
		return datosSesion;
	}
}