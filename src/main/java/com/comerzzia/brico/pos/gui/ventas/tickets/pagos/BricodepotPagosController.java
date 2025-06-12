package com.comerzzia.brico.pos.gui.ventas.tickets.pagos;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.api.rest.client.exceptions.RestConnectException;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.exceptions.RestTimeoutException;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.api.rest.client.fidelizados.ResponseGetFidelizadoRest;
import com.comerzzia.brico.pos.selfcheckout.services.impresion.SelfCheckoutServicioImpresion;
import com.comerzzia.bricodepot.posservices.client.TarjetasApi;
import com.comerzzia.bricodepot.posservices.client.model.ResponseGetTarjetaregaloRest;
import com.comerzzia.core.model.tiposdocumentos.TipoDocumentoBean;
import com.comerzzia.core.servicios.api.ComerzziaApiManager;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.servicios.variables.Variables;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.dispositivo.comun.tarjeta.CodigoTarjetaController;
import com.comerzzia.pos.dispositivo.comun.tarjeta.CodigoTarjetaView;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.pagos.PagosController;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.giftcard.GiftCardBean;
import com.comerzzia.pos.persistence.mediosPagos.MedioPagoBean;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.payments.methods.PaymentMethodManager;
import com.comerzzia.pos.services.payments.methods.types.GiftCardManager;
import com.comerzzia.pos.services.promociones.Promocion;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.cupones.CuponEmitidoTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Primary
@Component
public class BricodepotPagosController extends PagosController {

	public static final String COD_MP_GIFTCARD = "0020";

	public static final String COD_MP_VALE = "1000";

	@FXML
	protected Label lbTotalArticulosMensaje, lbTotalArticulos;

	@Autowired
	private MediosPagosService mediosPagosService;

	@Autowired
	private VariablesServices variablesServices;

	@Autowired
	protected ComerzziaApiManager comerzziaApiManager;

	@Override
	public void initializeComponents() {
		MediosPagosService.mediosPagoContado = MediosPagosService.mediosPagoContado.stream().filter(p -> !p.getCodMedioPago().equals("0020")).collect(Collectors.toList());
		MediosPagosService.mediosPagoTarjetas = MediosPagosService.mediosPagoTarjetas.stream().filter(p -> !p.getCodMedioPago().equals("1000")).collect(Collectors.toList());
		super.initializeComponents();
		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);
		obtenerCantidadTotal();
		lbTotalArticulosMensaje.setText(I18N.getTexto("Nº UNIDADES"));
	}

	@Override
	public void refrescarDatosPantalla() {
		super.refrescarDatosPantalla();
		obtenerCantidadTotal();

	}

	protected void obtenerCantidadTotal() {
		TicketVentaAbono ticket = (TicketVentaAbono) ticketManager.getTicket();
		BigDecimal cantidad = ticket.getCantidadTotal();
		lbTotalArticulos.setText(FormatUtil.getInstance().formateaNumero(cantidad.abs()));
	}

	@Override
	protected void imprimir() {
		try {
			while (true) {
				boolean hayPagosTarjeta = false;
				for (Object pago : ticketManager.getTicket().getPagos()) {
					if (pago instanceof PagoTicket && ((PagoTicket) pago).getDatosRespuestaPagoTarjeta() != null) {
						hayPagosTarjeta = true;
						break;
					}
				}

				String formatoImpresion = ticketManager.getTicket().getCabecera().getFormatoImpresion();

				if (formatoImpresion.equals(TipoDocumentoBean.PROPIEDAD_FORMATO_IMPRESION_NO_CONFIGURADO)) {
					log.info("imprimir() - Formato de impresion no configurado, no se imprimira.");
					return;
				}

				Map<String, Object> mapaParametros = SelfCheckoutServicioImpresion.setearParametrosImpresion(hayPagosTarjeta, ticketManager.getTicket(), sesion);

				if (ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals("FT")) {
					mapaParametros.put("esImpresionA4", true);
				}
				else {
					mapaParametros.put("esImpresionA4", false);
				}

				SelfCheckoutServicioImpresion.imprimir(formatoImpresion, mapaParametros);

				// Rompemos el bucle para realizar una sola impresion del ticket
				break;

			}

			// Cupones
			if (BigDecimalUtil.isMayorACero(ticketManager.getTicket().getTotales().getTotal())) {
				List<CuponEmitidoTicket> cupones = ((TicketVentaAbono) ticketManager.getTicket()).getCuponesEmitidos();
				if (cupones.size() > 0) {
					Map<String, Object> mapaParametrosCupon = new HashMap<String, Object>();
					mapaParametrosCupon.put("ticket", ticketManager.getTicket());
					FidelizacionBean datosFidelizado = ticketManager.getTicket().getCabecera().getDatosFidelizado();
					mapaParametrosCupon.put("paperLess", datosFidelizado != null && datosFidelizado.getPaperLess() != null && datosFidelizado.getPaperLess());
					for (CuponEmitidoTicket cupon : cupones) {
						mapaParametrosCupon.put("cupon", cupon);
						SimpleDateFormat df = new SimpleDateFormat();
						mapaParametrosCupon.put("fechaEmision", df.format(ticketManager.getTicket().getCabecera().getFecha()));

						Promocion promocionAplicacion = sesion.getSesionPromociones().getPromocionActiva(cupon.getIdPromocionAplicacion());
						if (promocionAplicacion != null) {
							Date fechaInicio = promocionAplicacion.getFechaInicio();
							if (fechaInicio == null || fechaInicio.before(ticketManager.getTicket().getCabecera().getFecha())) {
								mapaParametrosCupon.put("fechaInicio", FormatUtil.getInstance().formateaFecha(ticketManager.getTicket().getCabecera().getFecha()));
							}
							else {
								mapaParametrosCupon.put("fechaInicio", FormatUtil.getInstance().formateaFecha(fechaInicio));
							}
							Date fechaFin = promocionAplicacion.getFechaFin();
							mapaParametrosCupon.put("fechaFin", FormatUtil.getInstance().formateaFecha(fechaFin));

						}
						else {
							mapaParametrosCupon.put("fechaInicio", "");
							mapaParametrosCupon.put("fechaFin", "");
						}
						if (cupon.getMaximoUsos() != null) {
							mapaParametrosCupon.put("maximoUsos", cupon.getMaximoUsos().toString());
						}
						else {
							mapaParametrosCupon.put("maximoUsos", "");
						}

						ServicioImpresion.imprimir(PLANTILLA_CUPON, mapaParametrosCupon);
					}
				}
				// Imprimimos vale para cambio
				if (mediosPagosService.isCodMedioPagoVale(ticketManager.getTicket().getTotales().getCambio().getCodMedioPago(), ticketManager.getTicket().getCabecera().getTipoDocumento())
				        && !BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getCambio().getImporte())) {
					printVale(ticketManager.getTicket().getTotales().getCambio());
				}
			}
			else {
				// Imprimimos vales para pagos si estamos en devoluciÃ³n pero no si es de cambio (pago positivo en una
				// devolucion donde los pagos son negativos)
				List<PagoTicket> pagos = ((TicketVenta) ticketManager.getTicket()).getPagos();
				for (PagoTicket pago : pagos) {
					if (mediosPagosService.isCodMedioPagoVale(pago.getCodMedioPago(), ticketManager.getTicket().getCabecera().getTipoDocumento()) && !BigDecimalUtil.isMayorACero(pago.getImporte())) {
						printVale(pago);
					}
				}
			}
		}
		catch (Exception e) {
			log.error("imprimir() - " + e.getClass().getName() + " - " + e.getLocalizedMessage(), e);
			VentanaDialogoComponent.crearVentanaError(getStage(),
			        I18N.getTexto("Lo sentimos, ha ocurrido un error al imprimir.") + System.lineSeparator() + System.lineSeparator() + I18N.getTexto("El error es: ") + e.getMessage(), e);
		}
	}

	@Override
	protected void askGiftCardNumber(PaymentMethodManager source) {
		try {
			HashMap<String, Object> parametros = new HashMap<>();
			parametros.put(CodigoTarjetaController.PARAMETRO_IN_TEXTOCABECERA, I18N.getTexto("Lea o escriba el código de barras de la tarjeta de regalo"));
			parametros.put(CodigoTarjetaController.PARAMETRO_TIPO_TARJETA, "GIFTCARD");

			POSApplication posApplication = POSApplication.getInstance();
			posApplication.getMainView().showModalCentered(CodigoTarjetaView.class, parametros, getStage());
			String apiKey = variablesServices.getVariableAsString(Variables.WEBSERVICES_APIKEY);

			String numTarjeta = (String) parametros.get(CodigoTarjetaController.PARAMETRO_NUM_TARJETA);
			ResponseGetTarjetaregaloRest tarjetaRest = new ResponseGetTarjetaregaloRest();

			if (StringUtils.isNotBlank(numTarjeta)) {
				String uidActividad = sesion.getAplicacion().getUidActividad();
				ConsultarFidelizadoRequestRest paramConsulta = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
				paramConsulta.setNumeroTarjeta(numTarjeta);

				ResponseGetFidelizadoRest result = FidelizadosRest.getTarjetaRegalo(paramConsulta);

				DatosSesionBean datosSesion = new DatosSesionBean();
				datosSesion.setUidActividad(sesion.getAplicacion().getUidActividad());
				datosSesion.setUidInstancia(sesion.getAplicacion().getUidInstancia());
				datosSesion.setLocale(new Locale(AppConfig.idioma, AppConfig.pais));
				TarjetasApi api = comerzziaApiManager.getClient(datosSesion, "TarjetasApi");
				tarjetaRest = api.validarTarjeta(numTarjeta);

				if (tarjetaRest.getTarjetaValida()) {
					GiftCardBean tarjetaRegalo = SpringContext.getBean(GiftCardBean.class);
					tarjetaRegalo.setNumTarjetaRegalo(result.getNumeroTarjeta());
					tarjetaRegalo.setBaja(result.getBaja().equals("S"));
					tarjetaRegalo.setActiva(result.getActiva().equals("S"));
					tarjetaRegalo.setSaldoProvisional(BigDecimal.ZERO);
					tarjetaRegalo.setSaldo(BigDecimal.valueOf(result.getSaldo()));
					tarjetaRegalo.setSaldoProvisional(BigDecimal.valueOf(result.getSaldoProvisional()));
					tarjetaRegalo.setCodTipoTarjeta(result.getTipoTarjeta() != null ? result.getTipoTarjeta().getCodtipotarj() : null);

					if (tarjetaRegalo != null) {
						if (tarjetaRegalo.isBaja()) {
							VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("La tarjeta introducida está dada de baja."), getStage());
						}
						else {
							MedioPagoBean medioPago = mediosPagosService.getMedioPago(source.getPaymentCode());
							// TODO
							// boolean esMedioPagoCorrectoTipoTarj =
							// Dispositivos.getInstance().getGiftCard().esMedioPago(tarjetaRegalo.getCodTipoTarjeta(),
							// medioPago);
							// if(!esMedioPagoCorrectoTipoTarj) {
							// VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("La tarjeta introducida no es del
							// tipo permitido para este medio de pago."), getStage());
							// return;
							// }

							lbSaldo.setText(I18N.getTexto("Saldo") + ": (" + FormatUtil.getInstance().formateaImporte(tarjetaRegalo.getSaldoTotal()) + ")");
							lbMedioPago.setText(medioPago.getDesMedioPago());
							String tipoTarjeta = result.getTipoTarjeta().getCodtipotarj();

							if (tipoTarjeta.equals("R") || tipoTarjeta.equals("P") || tipoTarjeta.equals("GC")) {
								MedioPagoBean medioPagoTarjetaRegalo = mediosPagosService.getMedioPago(COD_MP_GIFTCARD);
								if (medioPagoTarjetaRegalo == null) {
									throw new Exception("Esta forma de pago “Giftcard“ no está activa.");
								}
								if (ticketManager.getTicket().getCabecera().esVenta() && !medioPagoTarjetaRegalo.getVisibleVenta()) {
									throw new Exception("Esta forma de pago “Giftcard“ no está activa para la venta.");
								}
								else if (ticketManager.isEsDevolucion() && !medioPagoTarjetaRegalo.getVisibleDevolucion()) {
									throw new Exception("Esta forma de pago “Giftcard“ no está activa para la devolución.");
								}
								medioPagoSeleccionado = medioPagoTarjetaRegalo;
								PaymentMethodManager managerGiftcard = paymentsManager.getPaymentsMehtodManagerAvailables().get(COD_MP_GIFTCARD);
								managerGiftcard.addParameter(GiftCardManager.PARAM_TARJETA, tarjetaRegalo);
							}
							else if (tipoTarjeta.equals("AB") || tipoTarjeta.equals("ABC")) {
								MedioPagoBean medioPagoVale = mediosPagosService.getMedioPago(COD_MP_VALE);
								if (medioPagoVale == null) {
									throw new Exception("Esta forma de pago “Vale“ no está activa.");
								}
								if (ticketManager.getTicket().getCabecera().esVenta() && !medioPagoVale.getVisibleVenta()) {
									throw new Exception("Esta forma de pago “Vale“ no está activa para la venta.");
								}
								else if (ticketManager.isEsDevolucion() && !medioPagoVale.getVisibleDevolucion()) {
									throw new Exception("Esta forma de pago “Vale“ no está activa para la devolución.");
								}
								medioPagoSeleccionado = medioPagoVale;
								PaymentMethodManager managerVale = paymentsManager.getPaymentsMehtodManagerAvailables().get(COD_MP_VALE);
								managerVale.addParameter(GiftCardManager.PARAM_TARJETA, tarjetaRegalo);
							}

							GiftCardBean tarjetaRegaloPago = obtenerPagoTarjetaRegalo(tarjetaRegalo);

							if (tarjetaRegaloPago != null) {
								asociarPagoTarjetaRegalo(ticketManager.getTicket().getCabecera().esVenta(), tarjetaRegaloPago);
							}
							else {
								lbSaldo.setText(I18N.getTexto("Saldo") + ": (" + FormatUtil.getInstance().formateaImporte(tarjetaRegalo.getSaldoTotal()) + ")");
								if (ticketManager.getTicket().getCabecera().esVenta()) {
									if (BigDecimalUtil.isMayor(ticketManager.getTicket().getTotales().getPendiente(), tarjetaRegalo.getSaldoTotal())) {
										tfImporte.setText(FormatUtil.getInstance().formateaImporte(tarjetaRegalo.getSaldoTotal()));
									}
								}
							}
						}
					}

					source.addParameter(GiftCardManager.PARAM_TARJETA, tarjetaRegalo);
				}
				else {
					if (tarjetaRest.getErrorValidacion().equals("PREFIJO")) {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Esta tarjeta no es válida, prefijo incorrecto."), getStage());
					}
					else if (tarjetaRest.getErrorValidacion().equals("LONGITUD")) {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Esta tarjeta no es válida, longitud incorrecta."), getStage());
					}
					else if (tarjetaRest.getErrorValidacion().equals("FORMATO")) {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Esta tarjeta no es válida."), getStage());
					
					}
					selectDefaultPaymentMethod();
				}
			}
			else {
				selectDefaultPaymentMethod();
			}

		}

		catch (Exception e) {
			log.error("askGiftCardNumber() - Ha habido un error al pedir el número de tarjeta: " + e.getMessage(), e);

			if (e instanceof RestHttpException) {
				String message = I18N.getTexto("Lo sentimos, ha ocurrido un error en la petición.") + System.lineSeparator() + System.lineSeparator() + e.getMessage();
				VentanaDialogoComponent.crearVentanaError(getStage(), message, e);
			}
			else if (e instanceof RestConnectException) {
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("No se ha podido conectar con el servidor"), e);
			}
			else if (e instanceof RestTimeoutException) {
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("El servidor ha tardado demasiado tiempo en responder"), e);
			}
			else if (e instanceof RestException) {
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Lo sentimos, ha ocurrido un error en la petición"), e);
			}
			else {
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto(e.getMessage()), e);
			}

			selectDefaultPaymentMethod();
		}
	}
}
