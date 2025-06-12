package com.comerzzia.brico.pos.selfcheckout.gui.conversion;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.validation.ConstraintViolation;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.gui.ventas.devoluciones.SelfCheckoutFormularioConsultaTicketBean;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.SelfCheckoutFacturaController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.SelfCheckoutFacturaView;
import com.comerzzia.brico.pos.selfcheckout.services.autorizacion.SCOServicioAutorizacionFacturas;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.SelfCheckoutTicketVentaAbono;
import com.comerzzia.brico.pos.service.conversion.ConversionService;
import com.comerzzia.bricodepot.posservices.client.ConversionApi;
import com.comerzzia.bricodepot.posservices.client.model.Conversion;
import com.comerzzia.core.servicios.api.ComerzziaApiManager;
import com.comerzzia.core.servicios.contadores.ServicioContadoresImpl;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.util.base64.Base64Coder;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.visor.IVisor;
import com.comerzzia.pos.core.gui.BackgroundTask;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.core.gui.validation.ValidationUI;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.cajas.CajaEstadoException;
import com.comerzzia.pos.services.cajas.CajasServiceException;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.documentos.Documentos;
import com.comerzzia.pos.services.core.menu.MenuService;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.devices.DeviceException;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.promociones.Promocion;
import com.comerzzia.pos.services.ticket.ITicket;
import com.comerzzia.pos.services.ticket.Ticket;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.TicketsService;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.cabecera.DatosDocumentoOrigenTicket;
import com.comerzzia.pos.services.ticket.cupones.CuponEmitidoTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.pagos.IPagoTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.pagos.tarjeta.DatosPeticionPagoTarjeta;
import com.comerzzia.pos.services.ticket.pagos.tarjeta.DatosRespuestaPagoTarjeta;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@SuppressWarnings({ "unchecked", "rawtypes", "deprecation"})
@Component
public class ConversionController extends WindowController {

	protected Logger log = Logger.getLogger(ConversionController.class);

	public static final String CONVERSION = "CONVERSION";
	private static final String COD_PAIS_PORTUGAL = "PT";
	private static final String COD_PAIS_ESPAÑA = "ES";
	public static final String PLANTILLA_CUPON = "cupon_promocion";
	public static final String PLANTILLA_VALE = "vale";
	public static final String REQUIERE_AUTORIZACION = "requiereAutorizacion";

	@FXML
	private TextField tfLocalizador;
	private String localizadorConversion;
	@FXML
	protected Label lbMensajeError;
	final IVisor visor = Dispositivos.getInstance().getVisor();
	protected TicketManager ticketManager;
	@Autowired
	protected Documentos documentos;
	protected SelfCheckoutFormularioConsultaTicketBean frConsultaTicket;
	@Autowired
	protected TicketsService ticketsService;

	@Autowired
	private MediosPagosService mediosPagosService;

	@Autowired
	protected MenuService menuService;

	@Autowired
	protected ConversionService conversionService;

	@Autowired
	private Sesion sesion;

	@Autowired
	private VariablesServices variablesServices;
	
	@Autowired
	private SCOServicioAutorizacionFacturas servicioAutorizacion;

	@FXML
	protected Button btAceptar;

	@Autowired
	protected ComerzziaApiManager comerzziaApiManager;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		frConsultaTicket = SpringContext.getBean(SelfCheckoutFormularioConsultaTicketBean.class);
		frConsultaTicket.setFormField("codOperacion", tfLocalizador);
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		tfLocalizador.setText("");
		tfLocalizador.requestFocus();
		ticketManager = SpringContext.getBean(TicketManager.class);

		// Realizamos las comprobaciones de apertura automática de caja y de cierre de caja obligatorio
		try {
			comprobarAperturaPantalla();
		}
		catch (CajasServiceException | CajaEstadoException e) {
			log.error("initializeForm() - Error inicializando pantalla:" + e.getMessageI18N(), e);
			throw new InitializeGuiException(e.getMessageI18N(), e);
		}

		visor.escribirLineaArriba(I18N.getTexto("--NUEVA DEVOLUCION--"));

		tfLocalizador.setText("");

		List<String> tiposDocumentoAbonables = documentos.getTiposDocumentoAbonables();
		if (tiposDocumentoAbonables.isEmpty()) {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No está configurado el tipo de documento nota de crédito en el entorno."), this.getStage());
			btAceptar.setDisable(true);
		}
		else {
			btAceptar.setDisable(false);
		}

		for (String tipoDoc : tiposDocumentoAbonables) {
			try {
				if (documentos.getDocumento(tipoDoc) != null) {
					break;
				}
			}
			catch (DocumentoException ex) {
				log.error("No se ha encontrado el documento asociado", ex);
			}
		}

		lbMensajeError.setText("");
		
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
	}

	@Override
	public void initializeFocus() {
	}

	@FXML
	public void accionAceptar() {
		log.debug("accionAceptar() - Se aceptó la conversión del ticket :" + tfLocalizador.getText() + " a factura");
		lbMensajeError.setText("");
		if (validarFormularioConsultaCliente()) {

			ticketManager = SpringContext.getBean(TicketManager.class);
			String codigo = frConsultaTicket.getCodOperacion();

			try {
				if (codigo != null) {
					new RecuperarTicketConversion(codigo).start();
				}
				else {
					ticketManager.crearVentanaErrorContador(getStage());
				}
			}
			catch (Exception e) {
				VentanaDialogoComponent.crearVentanaError(getStage(), String.format(I18N.getTexto("El documento %s no se ha encontrado")), e);
			}
		}
		getStage().close();
	}

	protected boolean validarFormularioConsultaCliente() {
		boolean valido;

		// Limpiamos los errores que pudiese tener el formulario
		frConsultaTicket.clearErrorStyle();
		// Limpiamos el posible error anterior
		lbMensajeError.setText("");

		frConsultaTicket.setCodOperacion(tfLocalizador.getText());
		localizadorConversion = tfLocalizador.getText();

		// Validamos el formulario de login
		Set<ConstraintViolation<SelfCheckoutFormularioConsultaTicketBean>> constraintViolations = ValidationUI.getInstance().getValidator().validate(frConsultaTicket);
		if (constraintViolations.size() >= 1) {
			ConstraintViolation<SelfCheckoutFormularioConsultaTicketBean> next = constraintViolations.iterator().next();
			frConsultaTicket.setErrorStyle(next.getPropertyPath(), true);
			frConsultaTicket.setFocus(next.getPropertyPath());
			lbMensajeError.setText(next.getMessage());
			valido = false;
		}
		else {
			valido = true;
		}

		return valido;
	}

	/**
	 * Realiza las comprobaciones de apertura automática de caja y de cierre de caja obligatorio
	 * 
	 * @throws CajasServiceException
	 * @throws CajaEstadoException
	 * @throws InitializeGuiException
	 */
	protected void comprobarAperturaPantalla() throws CajasServiceException, CajaEstadoException, InitializeGuiException {
		if (!sesion.getSesionCaja().isCajaAbierta()) {
			Boolean aperturaAutomatica = variablesServices.getVariableAsBoolean(VariablesServices.CAJA_APERTURA_AUTOMATICA, true);
			if (aperturaAutomatica) {
				VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("No hay caja abierta. Se abrirá automáticamente."), getStage());
				sesion.getSesionCaja().abrirCajaAutomatica();
			}
			else {
				VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No hay caja abierta. Deberá ir a la gestión de caja para abrirla."), getStage());
				throw new InitializeGuiException(false);
			}
		}

		if (!ticketManager.comprobarCierreCajaDiarioObligatorio()) {
			String fechaCaja = FormatUtil.getInstance().formateaFecha(sesion.getSesionCaja().getCajaAbierta().getFechaApertura());
			String fechaActual = FormatUtil.getInstance().formateaFecha(new Date());
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se puede realizar la venta. El día de apertura de la caja {0} no coincide con el del sistema {1}", fechaCaja, fechaActual),
			        getStage());
			throw new InitializeGuiException(false);
		}
	}

	@FXML
	public void accionCancelar() {
		log.debug("accionCancelar() - Se canceló la conversión del ticket :" + tfLocalizador.getText() + " a factura");
		getStage().close();
	}

	@FXML
	public void keyReleased(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			accionAceptar();
		}
	}

	public class RecuperarTicketConversion extends BackgroundTask<Boolean> {

		private String codigo;

		public RecuperarTicketConversion(String codigo) {
			this.codigo = codigo;
		}

		@Override
		protected Boolean call() throws Exception {
			return ((SelfCheckoutTicketManager) ticketManager).recuperarTicketConversion(codigo, true);
		}

		@Override
		protected void failed() {
			super.failed();
			if (getException() instanceof com.comerzzia.pos.util.exception.Exception) {
				VentanaDialogoComponent.crearVentanaError(getStage(), getCMZException().getMessage(), getCMZException());
			}
			else {
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Lo sentimos, ha ocurrido un error."), getException());
			}
		}

		@Override
		protected void succeeded() {
			super.succeeded();
			boolean res = getValue();
			recuperarTicketConversionSucceeded(res);
		}
	}

	protected void recuperarTicketConversionSucceeded(boolean encontrado) {
		try {
			if (encontrado) {
				boolean esMismoTratamientoFiscal = ticketManager.comprobarTratamientoFiscalDev();
				if (!esMismoTratamientoFiscal) {
					try {
						ticketManager.eliminarTicketCompleto();
					}
					catch (Exception e) {
						log.error("recuperarTicketDevolucionSucceeded() - Ha habido un error al eliminar los tickets: " + e.getMessage(), e);
					}

					lbMensajeError.setText(I18N.getTexto("El ticket fue realizando en una tienda con un tratamiento fiscal diferente al de esta tienda. No se puede realizar esta devolución."));
					return;
				}
				
				boolean recoveredOnline = ticketManager.getTicket().getCabecera().getDatosDocOrigen().isRecoveredOnline();
				if (!recoveredOnline) {
					VentanaDialogoComponent.crearVentanaAviso(
							I18N.getTexto("No se podrá realizar la conversión a FT. Acuda a un empleado."), getStage());
					return;
				}

				DatosSesionBean datosSesion = new DatosSesionBean();
				datosSesion.setUidActividad(sesion.getAplicacion().getUidActividad());
				datosSesion.setUidInstancia(sesion.getAplicacion().getUidInstancia());
				datosSesion.setLocale(new Locale(AppConfig.idioma, AppConfig.pais));
				comerzziaApiManager.registerAPI("ConversionApi", ConversionApi.class, "posservices");
				ConversionApi api = comerzziaApiManager.getClient(datosSesion, "ConversionApi");
				TicketVenta ticketOrigen = ticketManager.getTicketOrigen();
				Conversion conversion = api.getConversion(ticketOrigen.getUidTicket());
				if (conversion == null) {
					convertirFSaFT();
				}
				else {
					// Si tiene conversion previa y no tiene uidTicket, significa que el ticket tiene promociones y devoluciones simultaneamente
					if (StringUtils.isBlank(conversion.getUidTicket())) {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se permite la conversión de una factura con devoluciones y promociones al mismo tiempo"), getStage());
						return;
					}
					VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("La FS ya ha sido convertida en FT."), getStage());
					return;
				}
			}
			else {
				VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No se reconoce la factura, se debe contactar con un empleado"), getStage());				
			}
		}
		catch (Exception e) {
			log.error("error:", e);
		}
	}

	private void convertirFSaFT() {
		log.debug("convertirFSaFT() - convirtiendo de FS a FT");
		if (!comprobarPagosTicketParcial()) {
			abrirVentanaDatosFactura(false);
		}
	}

	private boolean comprobarPagosTicketParcial() {
		boolean lineasParciales = false;
		List<LineaTicket> linesOrigin = ticketManager.getTicketOrigen().getLineas();
		int lineasTotalesDevueltas = 0;
		for(LineaTicket line : linesOrigin) {
			if(BigDecimalUtil.isMayorACero(line.getCantidadDevuelta())) {
				lineasTotalesDevueltas++;
			}
		}
		
		if(lineasTotalesDevueltas > 0 && lineasTotalesDevueltas < linesOrigin.size()) {
			lineasParciales = true;
		}
		
		if (lineasParciales || ((SelfCheckoutTicketManager) ticketManager).getTicketOrigen().getPagos().size() > 1) {
			String textoPopUp = I18N.getTexto("No se podrá realizar la conversión a FT. Acuda a un empleado.");
			VentanaDialogoComponent.crearVentanaAviso(textoPopUp, getStage());
			return true;
		}else if (lineasTotalesDevueltas == linesOrigin.size()){
			String textoPopUp = I18N.getTexto("No se puede convertir la FS a FT porque se ha realizado una devolución completa");
			VentanaDialogoComponent.crearVentanaAviso(textoPopUp, getStage());
			return true;
		}

		return false;
	}

	private void terminarConversionFT() throws TicketsServiceException {
		log.debug("terminarConversionFT() - registrando en bbdd");
		ticketsService.setContadorIdTicket((Ticket) ticketManager.getTicket());

		ticketManager.guardarCopiaSeguridadTicket();
		ticketsService.registrarTicket((Ticket) ticketManager.getTicket(), ticketManager.getDocumentoActivo(), false);

		// IMPRIMIR FACTURA
		imprimirConversion();
	}

	private void mostrarImpresionTicket() throws DocumentoException {
		log.debug("mostrarImpresionTicket() - Imprimiendo documento de la conversión.");

		boolean hayPagosTarjeta = false;
		for (Object pago : ticketManager.getTicket().getPagos()) {
			if (pago instanceof PagoTicket && ((PagoTicket) pago).getDatosRespuestaPagoTarjeta() != null) {
				hayPagosTarjeta = true;
				break;
			}
		}

		String formatoImpresion = ticketManager.getTicket().getCabecera().getFormatoImpresion();
		getDatos().put(SelfCheckoutFacturaController.DATOS_CLIENTE_CONVERSION, localizadorConversion);

		if (formatoImpresion.equals(TipoDocumentoBean.PROPIEDAD_FORMATO_IMPRESION_NO_CONFIGURADO)) {
			log.info("imprimir() - Formato de impresion no configurado, no se imprimira.");
			return;
		}

		Map<String, Object> mapaParametros = new HashMap<String, Object>();
		mapaParametros.put("ticket", ticketManager.getTicket());
		mapaParametros.put("BRICO_CABECERA", (BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera());

		if (mapaParametros.get("ticket") instanceof SelfCheckoutTicketVentaAbono) {
			SelfCheckoutTicketVentaAbono t = (SelfCheckoutTicketVentaAbono) ticketManager.getTicket();
			if (StringUtils.isNotBlank(t.getNumPedido())) {
				mapaParametros.put("numPedido", t.getNumPedido());
			}
			if (StringUtils.isNotBlank(t.getNumTarjetaRegalo())) {
				mapaParametros.put("numTarjetaRegalo", t.getNumTarjetaRegalo());
			}
		}

		if (hayPagosTarjeta) {
			mapaParametros.put("listaPagosTarjeta", getPagosTarjetas());
			mapaParametros.put("listaPagosTarjetaDatosPeticion", getPagosTarjetasDatosPeticion());
		}
		mapaParametros.put("urlQR", variablesServices.getVariableAsString("TPV.URL_VISOR_DOCUMENTOS"));
		FidelizacionBean datosFidelizado = ticketManager.getTicket().getCabecera().getDatosFidelizado();
		mapaParametros.put("paperLess", datosFidelizado != null && datosFidelizado.getPaperLess() != null && datosFidelizado.getPaperLess());
		if (ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals(sesion.getAplicacion().getDocumentos().getDocumento(Documentos.FACTURA_COMPLETA).getCodtipodocumento())) {
			mapaParametros.put("empresa", sesion.getAplicacion().getEmpresa());
		}

		if (ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals("FR")
				|| ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals("FT")
				|| ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals("NC")
				&& sesion.getAplicacion().getTienda().getCliente().getCodpais().equals(COD_PAIS_ESPAÑA)) {
				mapaParametros.put("DEVOLUCION", true);
		}
		if (sesion.getAplicacion().getTienda().getCliente().getCodpais().equals(COD_PAIS_PORTUGAL)) {
			mapaParametros.put("esCopia", true);
		}
		
		try {
			addQR(ticketManager.getTicket(), mapaParametros);
			aniadirLogoParametrosImprimir(mapaParametros);
			ServicioImpresion.imprimir(formatoImpresion, mapaParametros);
		}
		catch (Exception e) {
			log.error("mostrarImpresionTicket() - Ha ocurrido un error al mostrar el ticket de devolución: " + e.getMessage());
		}
		

	}
	private void imprimirConversion() {
		log.debug("imprimirConversion() - imprimiendo documento Factura Completa");
		try {
			while (true) {
				boolean hayPagosTarjeta = false;
				for (Object pago : ticketManager.getTicket().getPagos()) {
					if (pago instanceof PagoTicket && ((PagoTicket) pago).getDatosRespuestaPagoTarjeta() != null) {
						hayPagosTarjeta = true;
						break;
					}
				}
				mostrarImpresionTicket();

				if (hayPagosTarjeta) {
					if (VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("¿Es correcta la impresión del recibo del pago con tarjeta?"), getStage())) {
						break;
					}
				}
				else {
					break;
				}
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
				if (!ticketManager.isEsDevolucion()) {
					// Imprimimos vale para cambio
					if (mediosPagosService.isCodMedioPagoVale(ticketManager.getTicket().getTotales().getCambio().getCodMedioPago(), ticketManager.getTicket().getCabecera().getTipoDocumento())
					        && !BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getCambio().getImporte())) {
						printVale(ticketManager.getTicket().getTotales().getCambio());
					}
				}
				else {
					if (documentos.isDocumentoAbono(ticketManager.getTicket().getCabecera().getCodTipoDocumento())) {
						// Es una devoluciÃ³n donde el signo del tipo de documento es positivo, imprimimos vales de
						// pagos
						List<PagoTicket> pagos = ((TicketVenta) ticketManager.getTicket()).getPagos();
						for (PagoTicket pago : pagos) {
							if (mediosPagosService.isCodMedioPagoVale(pago.getCodMedioPago(), ticketManager.getTicket().getCabecera().getTipoDocumento())
							        && BigDecimalUtil.isMenorACero(pago.getImporte())) {
								printVale(pago);
							}
						}
					}
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

	private void abrirVentanaDatosFactura(boolean isOperacionFlexpoint) {
		log.debug("abrirVentanaDatosFactura() - abriendo ventana de Datos Factura");
		getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);
		getDatos().put(CONVERSION, true);
		getDatos().put(REQUIERE_AUTORIZACION, servicioAutorizacion.requiereAutorizacionPais());
		getDatos().put(SelfCheckoutFacturaController.DATOS_CLIENTE_CONVERSION, tfLocalizador.getText());
		if (ticketManager.getTicketOrigen().getCabecera().getDatosFidelizado() != null) {
			getDatos().put(SelfCheckoutFacturaController.CLIENTE_CONVERSION, ticketManager.getTicketOrigen().getCabecera().getDatosFidelizado().getNumTarjetaFidelizado());
		}
		getApplication().getMainView().showModalCentered(SelfCheckoutFacturaView.class, getDatos(), this.getStage());

		boolean usuarioCancela = getDatos().containsKey("cancela");
		boolean usuarioNoTienePermisos = getDatos().get(RequestAuthorizationController.PERMITIR_ACCION) != null && getDatos().get(RequestAuthorizationController.PERMITIR_ACCION).equals(false);
		if (usuarioCancela || usuarioNoTienePermisos) {
			log.debug("convertirFSaFT() - Cancela convirtiendo de FS a FT o no tiene permisos el usuario");
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Se ha cancelado la conversion a FT"), getStage());
			tfLocalizador.setText("");
			return;
		}
		if (!isOperacionFlexpoint) {
			generarDevolucion();
		}
	}

	

	protected void printVale(IPagoTicket iPagoTicket) throws DeviceException {
		Map<String, Object> mapaParametrosTicket = new HashMap<String, Object>();
		mapaParametrosTicket.put("ticket", ticketManager.getTicket());
		mapaParametrosTicket.put("urlQR", variablesServices.getVariableAsString("TPV.URL_VISOR_DOCUMENTOS"));
		mapaParametrosTicket.put("importeVale", FormatUtil.getInstance().formateaImporte(iPagoTicket.getImporte().abs()));
		FidelizacionBean datosFidelizado = ticketManager.getTicket().getCabecera().getDatosFidelizado();
		mapaParametrosTicket.put("paperLess", datosFidelizado != null && datosFidelizado.getPaperLess() != null && datosFidelizado.getPaperLess());
		mapaParametrosTicket.put("esCopia", Boolean.FALSE);
		ServicioImpresion.imprimir(PLANTILLA_VALE, mapaParametrosTicket);
		mapaParametrosTicket.put("esCopia", Boolean.TRUE);
		ServicioImpresion.imprimir(PLANTILLA_VALE, mapaParametrosTicket);
	}

	protected List<DatosRespuestaPagoTarjeta> getPagosTarjetas() {
		log.debug("getPagosTarjetas");
		List<DatosRespuestaPagoTarjeta> listaPagosTarjeta = new ArrayList<DatosRespuestaPagoTarjeta>();
		List<PagoTicket> listaPagos = ticketManager.getTicket().getPagos();
		for (PagoTicket pago : listaPagos) {
			if (pago.getDatosRespuestaPagoTarjeta() != null) {
				DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta = pago.getDatosRespuestaPagoTarjeta();
				listaPagosTarjeta.add(datosRespuestaPagoTarjeta);
			}
		}
		return listaPagosTarjeta;
	}

	protected List<DatosPeticionPagoTarjeta> getPagosTarjetasDatosPeticion() {
		log.debug("getPagosTarjetasDatosPeticion()");
		List<DatosPeticionPagoTarjeta> listaPagosTarjeta = new ArrayList<DatosPeticionPagoTarjeta>();
		List<PagoTicket> listaPagos = ticketManager.getTicket().getPagos();
		for (PagoTicket pago : listaPagos) {
			if (pago.getDatosRespuestaPagoTarjeta() != null) {
				DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta = pago.getDatosRespuestaPagoTarjeta();
				DatosPeticionPagoTarjeta datosPeticion = datosRespuestaPagoTarjeta.getDatosPeticion();
				listaPagosTarjeta.add(datosPeticion);
			}
		}
		return listaPagosTarjeta;
	}

	private void generarDevolucion() {
		log.debug("generarDevolucion() - Generando documento Nota de Crédito...");
		TicketVenta ticketOrigen = ((SelfCheckoutTicketManager) ticketManager).getTicketOrigen();
		DatosFactura datosFacturacion = new DatosFactura();
		try {

			try {
				log.debug("generarDevolucion() - seteando documento a NC...");
				ticketManager.setDocumentoActivo(sesion.getAplicacion().getDocumentos().getDocumento("NC"));
				BricodepotCabeceraTicket cabeceraOrigen = (BricodepotCabeceraTicket) ticketOrigen.getCabecera();
				((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setDatosEnvio(cabeceraOrigen.getDatosEnvio());
				datosFacturacion = ((TicketVenta) ticketManager.getTicket()).getDatosFacturacion();
				((TicketVenta) ticketManager.getTicket()).setDatosFacturacion(null);
				((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setFsToFt(true);
			}
			catch (DocumentoException e) {
				log.error("Error inicializando ticket");
			}
			((SelfCheckoutTicketManager) ticketManager).addLineas();
			((SelfCheckoutTicketManager) ticketManager).addPagos();
			ticketManager.getTicket().getTotales().recalcular();

			if (ticketManager.getTicket().getIdTicket() == null) {
				ticketsService.setContadorIdTicket((Ticket) ticketManager.getTicket());
			}
			ticketManager.guardarCopiaSeguridadTicket();
			// BRICOD-504 - Seteamos la fecha de devolución tras inicializar el nuevo ticket
			if (ticketOrigen != null && ticketOrigen.getCabecera() != null) {
				((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setFechaTicketOrigen(ticketOrigen.getCabecera().getFechaAsLocale());
			}
			log.debug("generarDevolucion() - registro en bbdd");
			ticketsService.registrarTicket((Ticket) ticketManager.getTicket(), ticketManager.getDocumentoActivo(), true);
			ticketOrigen.setDatosFacturacion(datosFacturacion);
			
			DatosDocumentoOrigenTicket datosDocOrigen = ticketManager.getTicket().getCabecera().getDatosDocOrigen();
			ticketManager.getTicket().getCabecera().getDatosDocOrigen();
			if (sesion.getAplicacion().getTienda().getCliente().getCodpais().equals(COD_PAIS_PORTUGAL)) {
				log.debug("generarDevolucion() - Imprimiendo documento devolución para la FS de la conversión.");
				mostrarImpresionTicket();
			}
			ticketManager.finalizarTicket();
			ticketManager.inicializarTicket();
			((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setFsToFt(true);
			Long contadorFT = ServicioContadoresImpl.get().obtenerValorContador(conversionService.generarDatosSesion(), ticketManager.getDocumentoActivo().getIdContador());
			ticketManager.setDocumentoActivo(sesion.getAplicacion().getDocumentos().getDocumento("FT"));
			
			conversionService.generarFT(((TicketVenta) ticketManager.getTicket()),  ticketOrigen, contadorFT, datosDocOrigen, datosFacturacion);
			
			terminarConversionFT();
		}
		catch (Exception e) {
			log.error("generarDevolucion() - Ha habido un problema al guardar la nota de crédito: " + e.getMessage(), e);
			VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Ha habido un error al guardar el documento de devolución. Contacte con un administrador."), e);
		}

	}

	private void addQR(ITicket ticketOrigen, Map<String, Object> parameters) throws Exception, IOException {
		if (ticketOrigen.getCabecera() instanceof BricodepotCabeceraTicket) {
			log.debug("addQr() - La información fiscal ya viene en el ticket.");

			if (ticketOrigen.getCabecera().getFiscalData() != null) {
				if (!ticketOrigen.getCabecera().getFiscalData().getProperties().isEmpty() && ticketOrigen.getCabecera().getFiscalData().getProperty(SelfCheckoutTicketManager.PROPERTY_QR) != null) {

					String data = ticketOrigen.getCabecera().getFiscalData().getProperty(SelfCheckoutTicketManager.PROPERTY_QR).getValue();

					log.debug("refrescarDatosPantalla() - Generando imagen del QR de Portugal");

					Base64Coder coder = new Base64Coder(Base64Coder.UTF8);
					String qr = coder.decodeBase64(data);
					BufferedImage qrImage = generateQRCodeImage(qr);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ImageIO.write(qrImage, "jpeg", os);
					InputStream is = new ByteArrayInputStream(os.toByteArray());
					parameters.put("QR_PORTUGAL", is);
				}
			}
			else {
				log.debug("addQr() - La información fiscal no viene en el ticket.");
			}
		}
		else {
			log.debug("addQr() - La información fiscal no viene en el ticket.");
		}
	}

	private BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);

		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}

	private void aniadirLogoParametrosImprimir(Map<String, Object> mapaParametros) throws IOException {
		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			InputStream is = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			mapaParametros.put("LOGO", is);
			is.close();
		}
	}
}
