package com.comerzzia.brico.pos.selfcheckout.gui.ventas;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.api.model.loyalty.FidelizadoBean;
import com.comerzzia.api.model.loyalty.TarjetaBean;
import com.comerzzia.api.model.loyalty.TiposContactoFidelizadoBean;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoDocumentoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoEmailRequestRest;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.api.rest.client.fidelizados.ResponseGetFidelizadoRest;
import com.comerzzia.brico.pos.gui.ventas.tickets.SesionTicketManager;
import com.comerzzia.brico.pos.selfcheckout.core.SelfCheckoutMainViewController;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.dialogos.SelfCheckoutVentanaDialogoComponent;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.despedida.SelfCheckoutDespedidaView;
import com.comerzzia.brico.pos.selfcheckout.gui.pagos.SelfCheckoutPagosView;
import com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket.SelfCheckoutTipoObtencionTicketView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.cantidad.SelfCheckoutModificarCantidadController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.cantidad.SelfCheckoutModificarCantidadView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigo.SelfCheckoutIntroducirCodigoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigo.SelfCheckoutIntroducirCodigoView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.insertadatos.InsertarDatosFidelizadoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.insertadatos.InsertarDatosFidelizadoView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.introduccionmanual.IntroduccionManualController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.introduccionmanual.IntroduccionManualView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.SelfCheckoutFacturaView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.IdentificacionFidelizadoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.IdentificacionFidelizadoView;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.SelfCheckoutTicketVentaAbono;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.brico.pos.service.ticket.ServicioEtiquetasArticulo;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.DispositivoCallback;
import com.comerzzia.pos.core.gui.BackgroundTask;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.BotonBotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonaccion.normal.BotonBotoneraNormalComponent;
import com.comerzzia.pos.core.gui.componentes.botonaccion.simple.BotonBotoneraSimpleComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.BotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.ConfiguracionBotonBean;
import com.comerzzia.pos.core.gui.componentes.botonera.PanelBotoneraBean;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.keyboard.Keyboard;
import com.comerzzia.pos.core.gui.exception.CargarPantallaException;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FormularioLineaArticuloBean;
import com.comerzzia.pos.gui.ventas.tickets.articulos.LineaTicketGui;
import com.comerzzia.pos.gui.ventas.tickets.pagos.PagosController;
import com.comerzzia.pos.persistence.fidelizacion.CustomerCouponDTO;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.sesion.SesionInitException;
import com.comerzzia.pos.services.core.sesion.SesionPromociones;
import com.comerzzia.pos.services.core.sesion.coupons.application.CouponsApplicationResultDTO;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.cupones.CuponAplicationException;
import com.comerzzia.pos.services.cupones.CuponUseException;
import com.comerzzia.pos.services.cupones.CuponesServiceException;
import com.comerzzia.pos.services.promociones.DocumentoPromocionable;
import com.comerzzia.pos.services.promociones.PromocionesServiceException;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.copiaSeguridad.CopiaSeguridadTicketService;
import com.comerzzia.pos.services.ticket.cupones.CuponAplicadoTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketException;
import com.comerzzia.pos.services.ticket.lineas.LineasTicketServices;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

@SuppressWarnings("unused")
@Component
public class SelfCheckoutFacturacionArticulosController extends FacturacionArticulosController {

	public static final String TEXTO_IDENTIFICACION = "TEXTO_IDENTIFICACION";
	public static final String TARJETA_CLIENTE_BRICOCLUB= "esTarjetaClienteBricoClub";
	public static final String PARAM_SELF_CHECKOUT_CERRAR = "Self_Checkout_Cerrar";

	public static final String APIKEY = "WEBSERVICES.APIKEY";
	public static final String EMAIL = "EMAIL";

	public static final String SCO_CANTIDAD_MAXIMA_VENTA_AUTONOMA = "SCO.CANTIDAD_MAXIMA_VENTA_AUTONOMA";
	public static final String SCO_IMPORTE_MAXIMA_VENTA_AUTONOMA = "SCO.IMPORTE_MAXIMA_VENTA_AUTONOMA";
	private static final long ACCION_COMPROBANTE = 700010L;
	
	private Logger log = Logger.getLogger(SelfCheckoutFacturacionArticulosController.class);

	@FXML
	private VBox panelIntroduccionArticulos, panelTarjetaFidelizado, panelCupones, panelComprobante, panelDatos;

	@FXML
	private Button btnContinuar, btContinuarFidelizado, btModificarCantidad, btBorrarLinea, btnBuscarArticulos, btSeleccionRapidaArticulos, btFactura, btAtras;

	@FXML
	private Label lbMensajeFidelizado, lbEmailFidelizado, lbDocumentoFidelizado, lbNombreFidelizadoEtiqueta, lbNumTarjetaEtiqueta, lbDocumentoEtiqueta, lbEmailEtiqueta, lbQrAltaFid, lbTotalArticulosMensaje, lbTotalArticulos, lbFactura, lbCompleta;

	@FXML
	private TextField tfCodigo, tfIdenFidelizado;

	@FXML
	private ImageView qrES, qrPT;

	@Autowired
	protected VariablesServices variablesServices;
	
	@Autowired
	protected SesionTicketManager sesionTicketManager;
	
	@Autowired
	protected CopiaSeguridadTicketService copiaSeguridadTicketService;

	@Autowired
	protected LineasTicketServices lineasTicketServices;
	
	@Autowired
	protected ServicioEtiquetasArticulo servicioEtiquetasArticulo;
	
	protected String intervencion;
	
	@FXML
	private SelfCheckoutKeyboard keyboard;
	
	private Boolean facturaA4;
	
	@Autowired
    private SesionPromociones sesionPromociones;
	
	private Boolean volverAPantallaBienvenida = false;
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		super.initialize(url, rb);
		btSeleccionRapidaArticulos.setVisible(false);
		lbNombreFidelizadoEtiqueta.setText(I18N.getTexto("Nombre:"));
		lbNumTarjetaEtiqueta.setText(I18N.getTexto("Tarjeta:"));
		lbDocumentoEtiqueta.setText(I18N.getTexto("Documento:"));
		lbEmailEtiqueta.setText(I18N.getTexto("Email:"));
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		try {
			initializeManager();
			visor = Dispositivos.getInstance().getVisor();
			initTecladoNumerico(tecladoNumerico);

			initKeyboard(keyboard);
			
			log.debug("inicializarComponentes() - Inicialización de componentes...");

			log.debug("inicializarComponentes() - Carga de acciones de botonera inferior");
			try {
				PanelBotoneraBean panelBotoneraBean = getView().loadBotonera();
				botonera = new BotoneraComponent(panelBotoneraBean, panelBotonera.getPrefWidth(), panelBotonera.getPrefHeight(), this, BotonBotoneraNormalComponent.class);
				panelBotonera.getChildren().add(botonera);
			}
			catch (InitializeGuiException e) {
				log.error("initializeComponents() - Error al crear botonera: " + e.getMessage(), e);
			}
			
			

			// Botonera de Tabla
			log.debug("inicializarComponentes() - Carga de acciones de botonera de tabla de ventas");
			List<ConfiguracionBotonBean> listaAccionesAccionesTabla = cargarAccionesTabla();
			botoneraAccionesTabla = new BotoneraComponent(1, listaAccionesAccionesTabla.size(), this, listaAccionesAccionesTabla, panelMenuTabla.getPrefWidth(), panelMenuTabla.getPrefHeight(),
			        BotonBotoneraSimpleComponent.class.getName());
			panelMenuTabla.getChildren().add(botoneraAccionesTabla);

			log.debug("inicializarComponentes() - registrando acciones de la tabla principal");
			crearEventoEnterTabla(tbLineas);
			crearEventoNegarTabla(tbLineas);
			crearEventoEliminarTabla(tbLineas);
			crearEventoNavegacionTabla(tbLineas);

			log.debug("inicializarComponentes() - Configuración de la tabla");
			if (sesion.getAplicacion().isDesglose1Activo()) { // Si hay desglose 1, establecemos el texto
				tcLineasDesglose1.setText(I18N.getTexto(variablesServices.getVariableAsString(VariablesServices.ARTICULO_DESGLOSE1_TITULO)));
			}
			else { // si no hay desgloses, compactamos la línea
				tcLineasDesglose1.setVisible(false);
			}
			if (sesion.getAplicacion().isDesglose2Activo()) { // Si hay desglose 1, establecemos el texto
				tcLineasDesglose2.setText(I18N.getTexto(variablesServices.getVariableAsString(VariablesServices.ARTICULO_DESGLOSE2_TITULO)));
			}
			else { // si no hay desgloses, compactamos la línea
				tcLineasDesglose2.setVisible(false);
			}

			// Inicializamos los formularios
			frValidacionBusqueda = new FormularioLineaArticuloBean();
			frValidacionBusqueda.setFormField("cantidad", tfCantidadIntro);

			frValidacion = SpringContext.getBean(FormularioLineaArticuloBean.class);
			frValidacion.setFormField("codArticulo", tfCodigoIntro);
			frValidacion.setFormField("cantidad", tfCantidadIntro);

			tfPesoIntro.setText(FormatUtil.getInstance().formateaNumero(BigDecimal.ZERO, 3));

			tfCantidadIntro.setText(FormatUtil.getInstance().formateaNumero(BigDecimal.ONE, 3));

			tfCantidadIntro.focusedProperty().addListener(new ChangeListener<Boolean>(){

				@Override
				public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
					if (oldValue) {
						formateaCantidad();
					}
				}
			});

			if (AppConfig.rutaImagenes != null) {
				if (!AppConfig.interfazInfo.isPantallaCompleta()) {
					if (AppConfig.interfazInfo.getAlto() == 768) {
						lbTotal.setMaxHeight(140);
						lbTotalMensaje.setLayoutY(49);
					}
					else if (AppConfig.interfazInfo.getAlto() < 768) {
						lbTotal.setMaxHeight(95);
						lbTotalMensaje.setLayoutY(95);
						lbTotal.getStyleClass().add("total-image");
					}
					applyTotalLabelStyle(lbTotal);
				}

				tbLineas.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<LineaTicketGui>(){

					@Override
					public void changed(ObservableValue<? extends LineaTicketGui> arg0, LineaTicketGui itemAntiguo, LineaTicketGui itemNuevo) {
						if (itemNuevo != null) {
							imagenArticulo.mostrarImagen(itemNuevo.getArticulo());
						}
					}
				});
			}
			else {
				panelImagen.setVisible(false);
				panelImagen.setManaged(false);
				panelImagen.getChildren().clear();
			}

			registraEventoTeclado(new EventHandler<KeyEvent>(){

				@Override
				public void handle(KeyEvent event) {
					if (event.getCode() == KeyCode.MULTIPLY) {
						if (tfCantidadIntro.isFocused()) {
							tfCodigoIntro.requestFocus();
							tfCodigoIntro.selectAll();
						}
						else {
							cambiarCantidad();
						}
					}
				}
			}, KeyEvent.KEY_RELEASED);

			addSeleccionarTodoCampos();
			
			if(ticketManager.isTicketVacio()){
				consultarCopiaSeguridad();				
			}

		}
		catch (CargarPantallaException | SesionInitException | TicketsServiceException | DocumentoException ex) {
			log.error("inicializarComponentes() - Error inicializando pantalla de venta de artículos");
			VentanaDialogoComponent.crearVentanaError("Error cargando pantalla. Para mas información consulte el log.", getStage());
		}

		try {
			List<ConfiguracionBotonBean> listaAccionesAccionesTabla = cargarAccionesTabla();
			listaAccionesAccionesTabla = listaAccionesAccionesTabla.subList(0, 4);
			botoneraAccionesTabla = new BotoneraComponent(listaAccionesAccionesTabla.size(), 1, this, listaAccionesAccionesTabla, panelMenuTabla.getPrefWidth(), panelMenuTabla.getPrefHeight(),
			        BotonBotoneraSimpleComponent.class.getName());
			panelMenuTabla.getChildren().clear();
			panelMenuTabla.getChildren().add(botoneraAccionesTabla);
		}
		catch (Exception e) {
			log.error("initializeComponents() - No se ha podido cargar la botonera de la tabla: " + e.getMessage(), e);
		}
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		String cod = (String)getDatos().get("lecturaRecogida");
		
		comprobarIdioma();
		
		tfCantidadIntro.setText(FormatUtil.getInstance().formateaNumero(BigDecimal.ONE, 3));

		if (getDatos().get(PARAM_SELF_CHECKOUT_CERRAR) != null && (Boolean) getDatos().get(PARAM_SELF_CHECKOUT_CERRAR) == true) {
			getDatos().put(PARAM_SELF_CHECKOUT_CERRAR, null);
			getApplication().getMainView().showModal(SelfCheckoutDespedidaView.class);
			((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
		}

		// Si se viene desde ADMIN sin introducir líneas, recogemos la intervención
		if (ticketManager.getTicket() != null) {
			intervencion = ((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).getIntervencion();
		}

		super.initializeForm();

		mostrarIntroduccionArticulos();

		tbLineas.setFocusTraversable(false);

		if (StringUtils.isBlank(lbEmailFidelizado.getText())) {
			lbEmailFidelizado.setText("");
		}
		if(StringUtils.isBlank(lbDocumentoFidelizado.getText())) {
			lbDocumentoFidelizado.setText("");
		}

		btBorrarLinea.setDisable(true); // BRICO-470
		lbTotalArticulosMensaje.setText(I18N.getTexto("Nº UNIDADES"));

//		inicializarQRAltaFidelizado();
		
		if (StringUtils.isNotBlank(cod)) {
			tfCodigoIntro.setText(cod);
			volverAPantallaBienvenida = true;
			Platform.runLater(() -> {
				nuevoCodigoArticulo();
	        });
		}
		
		btAtras.setVisible(false);
		tfIdenFidelizado.setOnMouseClicked(event -> consultarFidelizado());
		
		if(getDatos().get("volverAtras") != null) {
			mostrarIntroduccionTarjetaFidelizado();
		}
		facturaA4=false;
	}

	public void initKeyboard(Keyboard keyboard) {
		try {
			keyboard = new SelfCheckoutKeyboard();
		}
		catch (IOException | URISyntaxException e) {
			log.error("Error cargando teclado");
		}
		keyboard.onController(this);
		keyboard.setPopupVisible(true, tfIdenFidelizado, this.getStage(), Boolean.TRUE);
	}
	
	public void cancelar() {
		log.debug("cancelar()");
		try {			
			if (ticketManager.getTicket().getLineas().size() > 0) {
				boolean confirmacion = VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("¿Estás seguro de querer eliminar todas las líneas del ticket?"), getStage());
				if (!confirmacion) {
					return;
				}
				
				TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.ANULACION_TICKET, sesion);
				abrirVentanaAutorizacion(auditEvent, getDatos());
				if (datos.get(RequestAuthorizationController.PERMITIR_ACCION).equals(true)) {
					((SelfCheckoutTicketManager) ticketManager).eliminarTicketCompleto(); // BRICOD-231
					resetearCantidad();
					refrescarDatosPantalla();
					initializeFocus();
					tbLineas.getSelectionModel().clearSelection();

					visor.escribirLineaArriba(I18N.getTexto("---NUEVO CLIENTE---"));
					visor.modoEspera();

					((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
				}
			}
			else {
				((SelfCheckoutTicketManager) ticketManager).eliminarTicketCompleto(); // BRICOD-231
				resetearCantidad();
				refrescarDatosPantalla();
				initializeFocus();
				tbLineas.getSelectionModel().clearSelection();

				visor.escribirLineaArriba(I18N.getTexto("---NUEVO CLIENTE---"));
				visor.modoEspera();

				((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
			}
		}
		catch (TicketsServiceException ex) {
			log.error("accionAnularTicket() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
		catch (DocumentoException ex) {
			log.error("accionAnularTicket() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
		catch (PromocionesServiceException ex) {
			log.error("accionAnularTicket() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
	}

	@FXML
	public void modificarCantidad() throws LineaTicketException {
		int idLinea = getLineaSeleccionada().getIdLinea();
		LineaTicket linea = (LineaTicket) ticketManager.getTicket().getLinea(idLinea);
		BigDecimal cantidadOriginal = linea.getCantidad();
		getDatos().put(SelfCheckoutModificarCantidadController.PARAMETRO_LINEA, linea);

		getApplication().getMainView().showModalCentered(SelfCheckoutModificarCantidadView.class, getDatos(), getStage());

		BigDecimal cantidad = (BigDecimal) getDatos().get(SelfCheckoutModificarCantidadController.PARAMETRO_CANTIDAD);

		if (cantidad != null) {
			linea.setCantidad(cantidad);
			if (!((SelfCheckoutTicketManager) ticketManager).getVentaIsAutorizada()) {
				if (superaMaxImporteModificarCantidadArticulo(linea) || superaMaxVentasModificarCantidadArticulo(cantidad, cantidadOriginal)) {
					linea.setCantidad(cantidadOriginal);
				}
			}
			ticketManager.recalcularConPromociones();
			ticketManager.guardarCopiaSeguridadTicket();
			refrescarDatosPantalla();
		}
	}

	public void introducirCodigo() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(TICKET_KEY, ticketManager);
		getApplication().getMainView().showModalCentered(SelfCheckoutIntroducirCodigoView.class, params, getStage());

		String codArt = (String) params.get(SelfCheckoutIntroducirCodigoController.PARAM_CODART);
		if (StringUtils.isNotBlank(codArt)) {
			tfCodigoIntro.setText(codArt);
			nuevoCodigoArticulo();
		}

		activarBotones();
	}

	protected void activarBotones() {
		if (ticketManager.getTicket().getLineas() != null && !ticketManager.getTicket().getLineas().isEmpty()) {
			btModificarCantidad.setDisable(false);
			btBorrarLinea.setDisable(false);
		}
		else {
			btModificarCantidad.setDisable(true);
			btBorrarLinea.setDisable(true);
		}
		btBorrarLinea.setDisable(true); // BRICO-470
	}

	public void mostrarIntroduccionArticulos() {
		activarBotones();

		panelIntroduccionArticulos.setVisible(true);
		panelIntroduccionArticulos.setManaged(true);

		panelTarjetaFidelizado.setVisible(false);
		panelTarjetaFidelizado.setManaged(false);

		panelCupones.setVisible(false);
		panelCupones.setManaged(false);
		
		panelComprobante.setVisible(false);
		panelComprobante.setManaged(false);
		
		panelDatos.setVisible(false);
		panelDatos.setManaged(false);
		
		btAtras.setVisible(false);
		
		btnContinuar.setVisible(true);
	}

	public void mostrarIntroduccionTarjetaFidelizado() {
		if (panelTarjetaFidelizado.isVisible()) {
			abrirVentanaTipoComprobante();
		}
		else if (panelComprobante.isVisible()) {
			abrirPagos();
		}
		else{
			panelIntroduccionArticulos.setVisible(false);
			panelIntroduccionArticulos.setManaged(false);

			panelTarjetaFidelizado.setVisible(true);
			panelTarjetaFidelizado.setManaged(true);

//			pintarDatosFidelizado();

			panelCupones.setVisible(false);
			panelCupones.setManaged(false);
			
			panelComprobante.setVisible(false);
			panelComprobante.setManaged(false);
			
			panelDatos.setVisible(true);
			panelDatos.setManaged(true);
			
			btAtras.setVisible(true);
		}
	}

	public void mostrarIntroduccionCupones() {
		panelIntroduccionArticulos.setVisible(false);
		panelIntroduccionArticulos.setManaged(false);

		panelTarjetaFidelizado.setVisible(false);
		panelTarjetaFidelizado.setManaged(false);

		panelCupones.setVisible(true);
		panelCupones.setManaged(true);
		
	}

	private void pintarDatosFidelizado() {
		if (ticketManager.getTicket().getCabecera().getDatosFidelizado() != null) {
			lbMensajeFidelizado.setText(I18N.getTexto("¿Son correctos los datos?"));
		}
		else {
			lbMensajeFidelizado.setText(I18N.getTexto("<html>" + "Escanea tu TARJETA DE FIDELIZACIÓN o introduzca EMAIL o DOCUMENTO DE <font color='red'>IDENTIDAD</font>" + "</html>\""));
			btFactura.setText(I18N.getTexto("Factura"));
			lbNombreFidelizadoEtiqueta.setText(I18N.getTexto("Nombre:"));
			lbNumTarjetaEtiqueta.setText(I18N.getTexto("Tarjeta:"));
			lbDocumentoEtiqueta.setText(I18N.getTexto("Documento:"));
			lbEmailEtiqueta.setText(I18N.getTexto("Email:"));
		}
	}

	@Override
	protected void abrirVentanaPagos() {
		getDatos().put("checkFT", facturaA4);
		getApplication().getMainView().showModal(SelfCheckoutPagosView.class, getDatos());
	}

	protected void resetearCantidad() {
		tfCantidadIntro.setText(FormatUtil.getInstance().formateaNumero(BigDecimal.ONE, 3));
	}

	public void introducirCodigoManual() {
		getApplication().getMainView().showModalCentered(IntroduccionManualView.class, getDatos(), getStage());

		String codigo = (String) getDatos().get(IntroduccionManualController.PARAM_CODIGO);

                if (StringUtils.isNotBlank(codigo) && codigo.contains("@")) {
                        String errorKey = BricoEmailValidator.getValidationErrorKey(codigo);
                        if (errorKey != null) {
                                VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto(errorKey), getStage());
                                return;
                        }
                        buscarFidelizado(codigo);
                }
                else if (StringUtils.isNotBlank(codigo) && (esNIF(codigo) || esNIFPortugues(codigo))) {
                        buscarFidelizado(codigo);
                }
		else if (StringUtils.isNotBlank(codigo)) {

			log.debug("introducirCodigoManual() - Introduciendo manualmente el código: " + codigo);

			if (panelTarjetaFidelizado.isVisible()) {
				log.debug("introducirCodigoManual() - Se intentará introducir como tarjeta de fidelizado.");

				if (!Dispositivos.getInstance().getFidelizacion().isPrefijoTarjeta(codigo)) {
					VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("El dato introducido no corresponde a ningún fidelizado."), getStage());
					return;
					
				}
			}
			else if (panelCupones.isVisible()) {
				log.debug("introducirCodigoManual() - Se intentará introducir como cupón.");

				if (!((SesionPromociones) sesion.getSesionPromociones()).isCoupon(codigo)) {
					VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("El código introducido no es un cupón correcto."), getStage());
					return;
				}
			}

			tfCodigoIntro.setText(codigo);
			nuevoCodigoArticulo();
			refrescarDatosPantalla();
		}

		resetFocus();
	}

	private boolean esDNI(String dni) {
        // Patrón para verificar el DNI
        String patronDNI = "^[0-9]{8}[A-HJ-NP-TV-Z]$";

        // Compilar el patrón
        Pattern pattern = Pattern.compile(patronDNI, Pattern.CASE_INSENSITIVE);

        // Crear un objeto Matcher
        Matcher matcher = pattern.matcher(dni);

        // Verificar si coincide con el patrón
        return matcher.matches();
    }
	
	private boolean esCIF(String cif) {
        // Patrón para verificar el CIF
        String patronCIF = "^[ABCDEFGHJKLMNPQRSUVW][0-9]{7}[0-9A-J]$";
        
        // Compilar el patrón
        Pattern pattern = Pattern.compile(patronCIF, Pattern.CASE_INSENSITIVE);

        // Crear un objeto Matcher
        Matcher matcher = pattern.matcher(cif);

        // Verificar si coincide con el patrón
        return matcher.matches();
    }
	
	private boolean esNIF(String nif) {
        // Patrón para verificar tanto DNI como CIF
        String patronNIF = "^[0-9]{8}[A-HJ-NP-TV-Z]$|^[ABCDEFGHJKLMNPQRSUVW][0-9]{7}[0-9A-J]$";

        // Compilar el patrón
        Pattern pattern = Pattern.compile(patronNIF, Pattern.CASE_INSENSITIVE);

        // Crear un objeto Matcher
        Matcher matcher = pattern.matcher(nif);

        // Verificar si coincide con alguno de los patrones
        return matcher.matches();
    }
	
	private boolean esNIFPortugues(String nif) {
        // Patrón para verificar el NIF portugués
        String patronNIFPortugues = "^[0-9]{9}$";

        // Compilar el patrón
        Pattern pattern = Pattern.compile(patronNIFPortugues, Pattern.CASE_INSENSITIVE);

        // Crear un objeto Matcher
        Matcher matcher = pattern.matcher(nif);

        // Verificar si coincide con el patrón
        return matcher.matches();
    }

	public void buscarFidelizado(String codigo) {

		ticketManager.recalcularConPromociones();

		if (StringUtils.isNotBlank(codigo)) {
			BackgroundTask<ResponseGetFidelizadoRest> task = new BackgroundTask<ResponseGetFidelizadoRest>(){

				@Override
				protected ResponseGetFidelizadoRest call() throws Exception {

					FidelizadoBean fidelizado = null;
					String apiKey = variablesServices.getVariableAsString(APIKEY);
					String uidActividad = sesion.getAplicacion().getUidActividad();
					String texto = codigo;

					if (texto.contains("@")) {
						ConsultarFidelizadoEmailRequestRest request = new ConsultarFidelizadoEmailRequestRest(apiKey, uidActividad, texto);
						fidelizado = FidelizadosRest.getFidelizadoPorEmail(request);
					}
					else {
						ConsultarFidelizadoDocumentoRequestRest request = new ConsultarFidelizadoDocumentoRequestRest(apiKey, uidActividad, texto);
						fidelizado = FidelizadosRest.getFidelizadoPorDocumento(request);

						// NO SE ENCONTRÓ POR DOCCUMENTO FIDELIZADO, EN CASO DE SER TARJETA CONSULTAMOS POR ELLA.
						if (fidelizado == null && codigo.length() == 13) {
								ConsultarFidelizadoRequestRest consultaRest = new ConsultarFidelizadoRequestRest(apiKey, uidActividad, codigo);
								ResponseGetFidelizadoRest fidelizadoTarjeta = FidelizadosRest.getFidelizado(consultaRest);
								fidelizado = new FidelizadoBean();
								fidelizado.setIdFidelizado(fidelizadoTarjeta.getIdFidelizado());
						}
					}

					ResponseGetFidelizadoRest fidelizadoRest = recuperarFidelizado(apiKey, uidActividad, fidelizado.getIdFidelizado());

					return fidelizadoRest;
				}

				@Override
				protected void succeeded() {
					super.succeeded();
					ResponseGetFidelizadoRest response = getValue();
					if (response != null) {
						panelDatos.setVisible(true);
						panelDatos.setManaged(true);
						lbNombreFidelizadoEtiqueta.setVisible(true);
						lbNumTarjetaEtiqueta.setVisible(true);
						lbDocumentoEtiqueta.setVisible(true);
						lbEmailEtiqueta.setVisible(true);
						tfCodigoIntro.setText(response.getNumeroTarjeta());
					}
					else {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central."), getStage());
					}
					
					nuevoCodigoArticulo();
					refrescarDatosPantalla();
				}

				@Override
				protected void failed() {
					super.failed();
					lbNombreTarjetaFidelizado.setText(I18N.getTexto("FIDELIZADO NO ENCONTRADO"));
					lbNombreFidelizadoEtiqueta.setVisible(false);
					lbNumTarjetaEtiqueta.setVisible(false);
					lbDocumentoEtiqueta.setVisible(false);
					lbEmailEtiqueta.setVisible(false);
					lbNombreTarjetaFidelizado.setText("");
					lbNumTarjetaFidelizado.setText("");
					lbEmailFidelizado.setText("");
					lbDocumentoFidelizado.setText("");
					Throwable e = getException();
					log.error("buscarFidelizado:failed() - Error recuperando la respuesta del ws: " + e.getMessage(), e);
					if (codigo.length() == 13) {
						getDatos().put(TARJETA_CLIENTE_BRICOCLUB, true);
					}
					if (VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("Aún no eres miembro del BricoClub, ¿deseas hacerte miembro?"), getStage())) {
						getDatos().put(TEXTO_IDENTIFICACION, tfIdenFidelizado.getText());
						getApplication().getMainView().showModalCentered(IdentificacionFidelizadoView.class, getDatos(), getStage());
						
						if(getDatos().get(IdentificacionFidelizadoController.CANCELAR) == null) {
							buscarFidelizado(ticketManager.getTicket().getCabecera().getDatosFidelizado().getDocumento());
						}
					}
				}

			};
			task.start();

		}
		else {
			VentanaDialogoComponent.crearVentanaAviso("Debes rellenar los datos para realizar la busqueda", getStage());
			return;
		}
	}

	@Override
	public void abrirPagos() {
		log.trace("abrirPagos()");
		if (insertandoLinea) {
			return;
		}
		if (!ticketManager.isTicketVacio()) {
			if (validarNumerosSerie()) {
				log.debug("abrirPagos() - El ticket tiene líneas");

				Dispositivos.getInstance().getFidelizacion().ignorarTarjetaFidelizado();

				useCustomerCoupons();

				getDatos().put(EMAIL, lbEmailFidelizado.getText());
				
				// [BRICO-617] Si durante la venta se ha entrado en ADMIN seteamos la intervención a S
				((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setIntervencion(intervencion);
				intervencion = null; // Se elimina para el próximo ticket
				
				abrirVentanaSeleccionTicket();
				
				getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);
				
				abrirVentanaPagos();
				
				initializeFocus();
				if (!getDatos().containsKey(PagosController.ACCION_CANCELAR) && !getDatos().containsKey(PagosController.IMP_MAX_SUPERADO)) {
					try {
						crearNuevoTicket();
						refrescarDatosPantalla();
						cerrarPantallaPagos();
						if (getDatos().get(PARAM_SELF_CHECKOUT_CERRAR) != null && (Boolean) getDatos().get(PARAM_SELF_CHECKOUT_CERRAR) == true) {
							getDatos().put(PARAM_SELF_CHECKOUT_CERRAR, null);
							
							if(getDatos().containsKey("papelSelect")) {
								getDatos().put("mensajeFactura", true);
							}
							getDatos().remove("papelSelect");
							
							if(getDatos().containsKey("correoSelect")) {
								getDatos().put("mensajeCorreo", true);
							}
							getDatos().remove("correoSelect");
							
							if(getDatos().containsKey("ambosSelect")) {
								getDatos().put("mensajeAmbos", true);
							}
							getDatos().remove("ambosSelect");
							
							getApplication().getMainView().showModal(SelfCheckoutDespedidaView.class, getDatos());
							
							((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
						}

					}
					catch (Exception e) {
						log.error("abrirPagos() - Ha habido un error al crear el ticket: " + e.getMessage());

					}
					cerrarPantallaPagos();
				}
				refrescarDatosPantalla();
			}
			else {
				log.warn("abrirPagos() - Ticket vacio");
				VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("El ticket no contiene líneas de artículo."), this.getStage());
			}
		}
	}

	@Override
	public void accionVerFidelizado() {
	}

	@Override
	public void useCustomerCoupons() {
		FidelizacionBean customerData = ticketManager.getTicket().getCabecera().getDatosFidelizado();

		if (customerData != null && customerData.getAvailableCoupons() != null && !customerData.getAvailableCoupons().isEmpty()) {
//			Se comenta la pantalla de cupones para no mostrarla
//			seeCustomerCoupons();

			DocumentoPromocionable ticket = (DocumentoPromocionable) ticketManager.getTicket();
			CouponsApplicationResultDTO result = sesion.getSesionPromociones().applyCoupons(customerData.getActiveCoupons(), ticket, false);

			ticketManager.getTicket().getCabecera().getTotales().recalcular();

			if (result != null) {
				String message = "";
				if (result.getAppliedCoupons() != null && !result.getAppliedCoupons().isEmpty()) {
					int appliedCoupons = result.getAppliedCoupons().size();
					if (appliedCoupons == 1) {
						message = I18N.getTexto("Se ha podido aplicar un cupón.", appliedCoupons);
					}
					else {
						message = I18N.getTexto("Se han podido aplicar {0} cupones.", appliedCoupons);
					}
				}

				if (result.getErrorCoupons() != null && !result.getErrorCoupons().isEmpty()) {
					if (StringUtils.isNotBlank(message)) {
						message = message + System.lineSeparator() + System.lineSeparator();
					}

					int errorCoupons = result.getErrorCoupons().size();
					if (errorCoupons == 1) {
						message = message + I18N.getTexto("No se ha podido aplicar un cupón.", errorCoupons);
					}
					else {
						message = message + I18N.getTexto("No se han podido aplicar {0} cupones.", errorCoupons);
					}
					VentanaDialogoComponent.crearVentanaInfo(null, message, getStage());
				}
				refrescarDatosPantalla();

			}
		}
	}

	private ResponseGetFidelizadoRest recuperarFidelizado(String apiKey, String uidActividad, Long idFidelizado) {

		log.debug("recuperarFidelizado() - recuperando datos del fidelizado");
		ConsultarFidelizadoRequestRest consultaRest = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
		consultaRest.setIdFidelizado(idFidelizado.toString());

		ResponseGetFidelizadoRest fidelizado = null;
		try {
			log.debug("recuperarFidelizado() - recuperando tarjetas del fidelizado");
			List<TarjetaBean> tarjetas = FidelizadosRest.getTarjetasFidelizado(consultaRest);
			consultaRest.setNumeroTarjeta(tarjetas.get(0).getNumeroTarjeta());

			fidelizado = FidelizadosRest.getFidelizado(consultaRest);
			consultaRest.setIdFidelizado(fidelizado.getIdFidelizado().toString());

			List<TiposContactoFidelizadoBean> contactos = FidelizadosRest.getContactos(consultaRest);

			log.debug("recuperarFidelizado() - recuperando tipos de contacto del fidelizado");
			for (TiposContactoFidelizadoBean tiposContacto : contactos) {
				if (tiposContacto.getCodTipoCon().equals("EMAIL")) {
					fidelizado.setEmail(tiposContacto.getValor());
				}
				else if (tiposContacto.getCodTipoCon().equals("MOVIL") || tiposContacto.getCodTipoCon().equals("TELEFONO1")) {
					fidelizado.setTelefono1(tiposContacto.getValor());
				}
				else if (tiposContacto.getCodTipoCon().equals("TELEFONO2")) {
					fidelizado.setTelefono2(tiposContacto.getValor());
				}
			}

		}
		catch (RestException | RestHttpException e1) {
			log.error("No se ha podido recuperar el fidelizado " + e1.getMessage(), e1);
		}
		return fidelizado;
	}

	@Override
	public void refrescarDatosPantalla() {
		log.debug("refrescarDatosPantalla() - Refrescando datos de pantalla...");

		BotonBotoneraComponent boton = botoneraAccionesTabla.getBotonBotonera("SEE_CUSTOMER_COUPONS");
		if (boton != null && boton instanceof BotonBotoneraSimpleComponent) {
			((BotonBotoneraSimpleComponent) boton).setDisable(true);
		}

		FidelizacionBean datosFidelizado = ticketManager.getTicket().getCabecera().getDatosFidelizado();
		if (datosFidelizado == null) {
			lbNombreTarjetaFidelizado.setText("");
			lbNombreTarjetaFidelizado.getStyleClass().remove("infoFidelizado");
			lbNombreFidelizado.setVisible(false);
			lbNombreFidelizado.setText("");
			lbNumTarjetaFidelizado.setText("");
			lbNumFidelizado.setVisible(false);
			lbSaldoTarjetaFidelizado.setText("");
			lbSaldoFidelizado.setVisible(false);

			// BRICO-371
			lbNombreFidelizadoEtiqueta.setVisible(false);
			lbNumTarjetaEtiqueta.setVisible(false);
			lbDocumentoEtiqueta.setVisible(false);
			lbEmailEtiqueta.setVisible(false);
		}
		else {
			if (datosFidelizado.getNumTarjetaFidelizado() != null) {
				lbNumFidelizado.setVisible(true);
				lbNumTarjetaFidelizado.setText(datosFidelizado.getNumTarjetaFidelizado());
			}
			else {
				lbNumFidelizado.setVisible(false);
				lbNumTarjetaFidelizado.setText("");
			}
			if (datosFidelizado.getNombre() != null) {
				lbNombreFidelizado.setVisible(true);
				lbNombreTarjetaFidelizado.setText(datosFidelizado.getNombre() + " " + datosFidelizado.getApellido());
				lbNombreTarjetaFidelizado.getStyleClass().add("infoFidelizado");
			}
			else {
				lbNombreTarjetaFidelizado.setText("");
				lbNombreTarjetaFidelizado.getStyleClass().remove("infoFidelizado");
				lbNombreFidelizado.setVisible(false);
			}
			if (datosFidelizado.getSaldoTotal() != null && !BigDecimalUtil.isIgualACero(datosFidelizado.getSaldoTotal())) {
				lbSaldoFidelizado.setVisible(true);
				lbSaldoTarjetaFidelizado.setText(datosFidelizado.getSaldoTotal().toString());
			}
			else {
				lbSaldoTarjetaFidelizado.setText("");
				lbSaldoFidelizado.setVisible(false);
			}

			// BRICO-467
			if (StringUtils.isNotBlank(datosFidelizado.getDocumento())) {
				lbDocumentoFidelizado.setText(datosFidelizado.getDocumento());
			}

			if (datosFidelizado.getAdicionales() != null && datosFidelizado.getAdicionales().get(EMAIL) != null) {
				String email = (String) datosFidelizado.getAdicionales().get(EMAIL);
				if (StringUtils.isNotBlank(email)) {
					lbEmailFidelizado.setText(email);
				}
			}
			// BRICO-467
			if (datosFidelizado.getAvailableCoupons() != null && !datosFidelizado.getAvailableCoupons().isEmpty()) {
				if (boton != null && boton instanceof BotonBotoneraSimpleComponent) {
					((BotonBotoneraSimpleComponent) boton).setDisable(false);
				}
			}
		}

		String totalFormateado = FormatUtil.getInstance().formateaImporte(ticketManager.getTicket().getTotales().getTotal());
		resetAutosizeLabelTotalFont();
		lbTotal.setText(totalFormateado);
		obtenerCantidadTotal();
		applyTotalLabelStyle(lbTotal);

		lbCodCliente.setText(((TicketVentaAbono) ticketManager.getTicket()).getCliente().getCodCliente());
		lbDesCliente.setText(((TicketVentaAbono) ticketManager.getTicket()).getCliente().getDesCliente());

		LineaTicketGui selectedItem = getLineaSeleccionada();
		lineas.clear();
		for (LineaTicket lineaTicket : ((TicketVentaAbono) ticketManager.getTicket()).getLineas()) {
			lineas.add(createLineaGui(lineaTicket));
		}
		for (CuponAplicadoTicket cupon : ((TicketVentaAbono) ticketManager.getTicket()).getCuponesAplicados()) {
			lineas.add(createLineaGui(cupon));
		}

		Collections.reverse(lineas);
		if (selectedItem != null) {
			tbLineas.getSelectionModel().select(lineas.indexOf(searchIdLinea(selectedItem)));
		}
		if (getLineaSeleccionada() == null) {
			tfCodigoIntro.requestFocus();
		}
		tbLineas.scrollTo(0);

		if (imagenArticulo != null) {
			imagenArticulo.setImage(null);
		}

		obtenerCantidadTotal();

//		pintarDatosFidelizado();

		activarBotones();
	}

	@Override
	public void nuevoCodigoArticulo() {
		// no dejar introducir líneas en un ticket nuevo si ha superado el importe de bloqueo de retirada
		if (tbLineas.getItems().size() == 0 && checkBloqueoRetirada()) {
			tfCodigoIntro.clear();
			return;
		}

		// Validamos los datos
		if (!tfCodigoIntro.getText().trim().isEmpty()) {
			
			// Comprobamos si el articulo a insertar tiene ciertas etiquetas y en caso afirmativo nos traemos el nombre
			String consultarEtiquetaArticulo = servicioEtiquetasArticulo.consultarEtiquetaArticulo(tfCodigoIntro.getText());
			// Mostramos PopUp / Ventana autorización segun la etiqueta.
			boolean tipologiaPopUp = mostrarPopUpTipologias(consultarEtiquetaArticulo);
			
			if (!tipologiaPopUp) {
				if (volverAPantallaBienvenida) {
					volverAPantallaBienvenida = false;
					cancelar();
				}
				return;
			}
			
			log.debug("nuevoCodigoArticulo() - Creando línea de artículo");
			String codCodeCoupon = tfCodigoIntro.getText();
			frValidacion.setCantidad(tfCantidadIntro.getText().trim());
			frValidacion.setCodArticulo(tfCodigoIntro.getText().trim().toUpperCase());
			BigDecimal cantidad = frValidacion.getCantidadAsBigDecimal();
			tfCodigoIntro.clear();

			if (accionValidarFormulario() && cantidad != null && !BigDecimalUtil.isIgualACero(cantidad)) {
				log.debug("nuevoCodigoArticulo()- Formulario validado");
				
				if (ticketManager.getTicket().getCabecera().getDatosFidelizado() != null) {
					if (codCodeCoupon.startsWith("2") || codCodeCoupon.startsWith("9")) {
						for (CustomerCouponDTO coupon : ticketManager.getTicket().getCabecera().getDatosFidelizado().getAvailableCoupons() ) {
							if (coupon.getCouponCode().equals(codCodeCoupon)) {
								try {
									if (sesionPromociones.aplicarCupon(coupon, (SelfCheckoutTicketVentaAbono) ticketManager.getTicket())){
										log.debug("nuevoCodigoArticulo()- Aplicando cupón: (" + coupon.getCouponCode() + ") " + coupon.getCouponName());
										ticketManager.recalcularConPromociones();
										refrescarDatosPantalla();
										
										return;
									}
								} catch (CuponUseException | CuponesServiceException | CuponAplicationException e) {
									log.error("Ha ocurrido un error aplicando el cupón con código: " + coupon.getCouponCode(), e);
								}
							}
						}
						
					}
				}
				// Si es prefijo de tarjeta fidelizacion, marcamos la venta como fidelizado y llamamos al REST
				if (Dispositivos.getInstance().getFidelizacion().isPrefijoTarjeta(frValidacion.getCodArticulo())) {

					ticketManager.recalcularConPromociones();
					refrescarDatosPantalla();

					Dispositivos.getInstance().getFidelizacion().cargarTarjetaFidelizado(frValidacion.getCodArticulo(), getStage(), new DispositivoCallback<FidelizacionBean>(){

						@Override
						public void onSuccess(FidelizacionBean tarjeta) {
							if (tarjeta.isBaja()) {
								VentanaDialogoComponent.crearVentanaError(I18N.getTexto("La tarjeta de fidelización {0} no está activa", tarjeta.getNumTarjetaFidelizado()), getStage());
								if (VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("Aún no eres miembro del BricoClub, ¿deseas hacerte miembro?"), getStage())) {
									getDatos().put(TEXTO_IDENTIFICACION, tfIdenFidelizado.getText());
									getApplication().getMainView().showModalCentered(IdentificacionFidelizadoView.class, getDatos(), getStage());
									
									if(getDatos().get(IdentificacionFidelizadoController.CANCELAR) == null) {
										buscarFidelizado(ticketManager.getTicket().getCabecera().getDatosFidelizado().getDocumento());
									}
								}
								tarjeta = null;
								ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
							}
							else {
								// Tarjeta válida - lo seteamos en el ticket
								ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
								ResponseGetFidelizadoRest fidelizado = recuperarFidelizadoByNumTarjeta(tarjeta.getNumTarjetaFidelizado());
								lbNombreFidelizadoEtiqueta.setVisible(true);
								lbNombreFidelizado.setText(fidelizado.getDocumento() + " " + fidelizado.getApellidos());
								lbNumTarjetaEtiqueta.setVisible(true);
								lbNumTarjetaFidelizado.setText(fidelizado.getNumeroTarjeta());
								lbDocumentoEtiqueta.setVisible(true);
								lbDocumentoFidelizado.setText(StringUtils.isNotBlank(fidelizado.getDocumento()) ? fidelizado.getDocumento() : "");
								lbEmailEtiqueta.setVisible(true);

								// BRICO-467
								String email = StringUtils.isNotBlank(fidelizado.getEmail()) ? fidelizado.getEmail() : "";
								lbEmailFidelizado.setText(email);
								ticketManager.getTicket().getCabecera().getDatosFidelizado().putAdicional(EMAIL, email);
								// BRICO-467
							}

							ticketManager.recalcularConPromociones();
							refrescarDatosPantalla();
						}

						@Override
						public void onFailure(Throwable e) {
							// Los errores se muestran desde el código del dispositivo
							// Quitamos los datos de fidelizado
							FidelizacionBean tarjeta = null;
							ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
							lbEmailFidelizado.setText("");
							lbDocumentoFidelizado.setText("");
							ticketManager.recalcularConPromociones();
							refrescarDatosPantalla();
						}
					});
					return;
				}

				// BRICOD-231
				try {
					log.debug("nuevoCodigoArticulo() - Consultamos si sobrepasa el límite de la cesta");
					if (!((SelfCheckoutTicketManager) ticketManager).getVentaIsAutorizada()) {
						if ((superaMaxImporte(frValidacion) || superaMaxCantidadArticulos(BigDecimal.ONE))) {
							return;
						}
					}
				}
				catch (LineaTicketException e) {
					log.debug("nuevoCodigoArticulo() - El código introducido" + frValidacion.getCodArticulo() + "no es de un artículo. No se comprobará si se supera el límte de cantidad y/o importe máximo");
				}
				
				NuevoCodigoArticuloTask taskArticulo = SpringContext.getBean(NuevoCodigoArticuloTask.class, this, frValidacion.getCodArticulo(), cantidad); // anidada
				taskArticulo.start();
			}
		}
	}

	private ResponseGetFidelizadoRest recuperarFidelizadoByNumTarjeta(String numTarjeta) {
		log.debug("recuperarFidelizadoByNumTarjeta() - recuperando datos");
		String apiKey = variablesServices.getVariableAsString(APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		ConsultarFidelizadoRequestRest consultaRest = new ConsultarFidelizadoRequestRest(apiKey, uidActividad, numTarjeta);
		ResponseGetFidelizadoRest fidelizado = null;

		try {
			log.debug("recuperarFidelizadoByNumTarjeta() - recuperando datos del fidelizado por el numero de tarjeta");
			fidelizado = FidelizadosRest.getFidelizado(consultaRest);
			consultaRest.setIdFidelizado(fidelizado.getIdFidelizado().toString());

			List<TiposContactoFidelizadoBean> contactos = FidelizadosRest.getContactos(consultaRest);

			for (TiposContactoFidelizadoBean tiposContacto : contactos) {
				if (tiposContacto.getCodTipoCon().equals("EMAIL")) {
					fidelizado.setEmail(tiposContacto.getValor());
				}
				else if (tiposContacto.getCodTipoCon().equals("MOVIL") || tiposContacto.getCodTipoCon().equals("TELEFONO1")) {
					fidelizado.setTelefono1(tiposContacto.getValor());
				}
				else if (tiposContacto.getCodTipoCon().equals("TELEFONO2")) {
					fidelizado.setTelefono2(tiposContacto.getValor());
				}
			}

		}
		catch (RestException | RestHttpException e1) {
			log.error("No se ha podido recuperar el fidelizado " + e1.getMessage(), e1);
		}
		return fidelizado;
	}

	private void resetFocus() { // BRICO-433
		tfCodigoIntro.clear();
		tfCodigoIntro.requestFocus();
	}
	
	@Override
	protected void obtenerCantidadTotal() {
		TicketVentaAbono ticket = (TicketVentaAbono) ticketManager.getTicket();
		BigDecimal cantidad = ticket.getCantidadTotal();
		lbTotalArticulos.setText(FormatUtil.getInstance().formateaNumero(cantidad.abs()));
	}
	
	@Override
	public void initializeManager() throws SesionInitException {
		ticketManager = SpringContext.getBean(SelfCheckoutTicketManager.class);
		
		if(sesionTicketManager.getSesionTicketManager()!= null) {
			ticketManager = sesionTicketManager.getSesionTicketManager();
		}else {
			ticketManager.init();
			sesionTicketManager.setSesionTicketManager(ticketManager);
		}
	}

	public SesionTicketManager getSesionTicketManager() {
		return sesionTicketManager;
	}
	
	public void setSesionTicketManager(SesionTicketManager sesionTicketManager) {
		this.sesionTicketManager = sesionTicketManager;
	}

	// BRICOD-231
	public Boolean superaMaxCantidadArticulos(BigDecimal cantidadArticulos) {
		// Validamos que no sobrepasen el máximo de ventas
		log.debug("superaMaxCantidadArticulos() - Comprobando si se supera la cantidad máxima de artículos");
			// BRICOD-326: Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un Number
			String cantidadMaxString = variablesServices.getVariableAsString(SCO_CANTIDAD_MAXIMA_VENTA_AUTONOMA);
			BigDecimal cantidadMax = StringUtils.isBlank(cantidadMaxString) ? BigDecimal.ZERO : new BigDecimal(cantidadMaxString);
			if (BigDecimalUtil.isMayorACero(cantidadMax)) { // En caso de que la variable no sea NULL
				BigDecimal totalArticulos = ticketManager.getTicket().getCabecera().getCantidadArticulos();
				BigDecimal sumaTotal = totalArticulos.add(cantidadArticulos);
				if(BigDecimalUtil.isMayorOrIgual(sumaTotal, cantidadMax)) {
					return mostrarPantallaSuperaMax(); 
				}
			}
		return false;
	}
	
	// BRICOD-329
	public Boolean superaMaxVentasModificarCantidadArticulo(BigDecimal cantidadArticulos, BigDecimal cantidadOriginalArticulos) {
		// Validamos que no sobrepasen el máximo de ventas
		log.debug("superaMaxVentasModificarCantidadArticulo() - Comprobando si se supera la cantidad máxima de artículos");
		// Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un Number
		String cantidadMaxString = variablesServices.getVariableAsString(SCO_CANTIDAD_MAXIMA_VENTA_AUTONOMA);
		BigDecimal cantidadMax = StringUtils.isBlank(cantidadMaxString) ? BigDecimal.ZERO : new BigDecimal(cantidadMaxString);
		if (BigDecimalUtil.isMayorACero(cantidadMax)) { // En caso de que la variable no sea NULL
			BigDecimal totalArticulosRestantes = ticketManager.getTicket().getCabecera().getCantidadArticulos().subtract(cantidadOriginalArticulos);
			BigDecimal sumaTotal = totalArticulosRestantes.add(cantidadArticulos);
			if(BigDecimalUtil.isMayorOrIgual(cantidadArticulos, cantidadMax)) {
				return mostrarPantallaSuperaMax(); 
			}
		}
		return false;
	}
	
	
	// BRICOD-231
	public Boolean superaMaxImporte(FormularioLineaArticuloBean frValidacion) throws LineaTicketException {
		// Validamos que no sobrepasen el importe máximo
		log.debug("superaMaxImporte() - Comprobando si se supera el importe máximo de venta");

			LineaTicket linea = ticketManager.nuevaLineaArticulo(frValidacion.getCodArticulo(), null, null, frValidacion.getCantidadAsBigDecimal(), null);
			ticketManager.eliminarLineaArticulo(linea.getIdLinea());

			// BRICOD-326: Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un Number
			String importeMaxString = variablesServices.getVariableAsString(SCO_IMPORTE_MAXIMA_VENTA_AUTONOMA);
			BigDecimal importeMax = StringUtils.isBlank(importeMaxString) ? BigDecimal.ZERO : new BigDecimal(importeMaxString);
			if (BigDecimalUtil.isMayorACero(importeMax)) { // En caso de que el valor de la variable no sea NULL

				BigDecimal importeTotal = ticketManager.getTicket().getCabecera().getTotales().getTotal();
				BigDecimal sumaTotal = importeTotal.add(linea.getImporteTotalConDto());
				if(BigDecimalUtil.isMayorOrIgual(sumaTotal, importeMax)) {
					return mostrarPantallaSuperaMax(); 
				}
			}
		return false;
	}
	
	// BRICOD-231
	public Boolean superaMaxImporteModificarCantidadArticulo(LineaTicket lineaSeleccionada) throws LineaTicketException {
		// Validamos que no sobrepasen el importe máximo
		log.debug("superaMaxImporte() - Comprobando si se supera el importe máximo de venta");
			// BRICOD-326: Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un Number
			String importeMaxString = variablesServices.getVariableAsString(SCO_IMPORTE_MAXIMA_VENTA_AUTONOMA);
			BigDecimal importeMax = StringUtils.isBlank(importeMaxString) ? BigDecimal.ZERO : new BigDecimal(importeMaxString);
			if (BigDecimalUtil.isMayorACero(importeMax)) { // En caso de que el valor de la variable no sea NULL

				ticketManager.recalcularConPromociones();
				BigDecimal importeTotal = ticketManager.getTicket().getCabecera().getTotales().getTotal();
				if(BigDecimalUtil.isMayorOrIgual(importeTotal, importeMax)) {
						return mostrarPantallaSuperaMax(); 
				}
			}
		return false;
	}
	
	// BRICOD-231
	protected Boolean mostrarPantallaSuperaMax() {
			log.debug("mostrarPantallaSuperaMax() - Se está superando el máximo de artículos/importe");
			if (SelfCheckoutVentanaDialogoComponent.crearVentanaConfirmacionUnBotonAceptar(
			        I18N.getTexto("Su compra necesita la asistencia de un empleado. Disculpe las molestias."), getStage())) {

				TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.VALIDAR_COMPRA, sesion);
				HashMap<String, Object> datosSupervisor = new HashMap<>();
				datosSupervisor.put("notCancel", true);
				return abrirVentanaAutorizacion(auditEvent, datosSupervisor);
			}
			cancelar();
			return true;
	}

	// BRICOD-231
	protected Boolean abrirVentanaAutorizacion(TicketAuditEvent auditEvent, HashMap<String, Object> datos) {
		log.debug("abrirVentanaAutorizacion() - Inicio del proceso de auditoria");
		List<TicketAuditEvent> events = new ArrayList<>();
		events.add(auditEvent);
		datos.put(RequestAuthorizationController.AUDIT_EVENT, events);
		
		getApplication().getMainView().showModalCentered(RequestAuthorizationView.class, datos, this.getStage());
		if((Boolean)datos.get(RequestAuthorizationController.PERMITIR_ACCION) && !((SelfCheckoutTicketManager)ticketManager).getVentaIsAutorizada()) {
			if(ticketManager != null) {
				((SelfCheckoutTicketManager)ticketManager).setVentaIsAutorizada(Boolean.TRUE);
			}
		}
		return !((Boolean)datos.get(RequestAuthorizationController.PERMITIR_ACCION));
	}

	protected void abrirVentanaTipoComprobante() {
		btnContinuar.setVisible(false);
		panelIntroduccionArticulos.setVisible(false);
		panelIntroduccionArticulos.setManaged(false);

		panelTarjetaFidelizado.setVisible(false);
		panelTarjetaFidelizado.setManaged(false);

		panelCupones.setVisible(false);
		panelCupones.setManaged(false);
		
		panelComprobante.setVisible(true);
		panelComprobante.setManaged(true);
		
		panelDatos.setVisible(false);
		panelDatos.setManaged(false);
		
		btAtras.setVisible(true);
	}
	
	/**
	 * Mostramos PopUp o Ventana autorización con mensaje según etiqueta.
	 * 
	 * @param etiqueta
	 * @return
	 */
	public boolean mostrarPopUpTipologias(String etiqueta) {
		log.debug("mostrarPopUpTipologias() - Comprobando PopUp tipológias");
		try {
			String texto ="";
			String textoVentanaAutorizacion = "";
			boolean autorizado = false;
			Locale locale = new Locale(((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getCodIdiomaCliente());
			TicketAuditEvent auditEvent = null;
			if (StringUtils.isNotBlank(etiqueta)) {
				switch (etiqueta.toLowerCase()) {
					case "aire acondicionado":
						textoVentanaAutorizacion = I18N.getTexto("Se deben entregar al cliente formularios A y B de aires acondicionados.", locale) + "\r\n"
							        + I18N.getTexto("El formulario A deberá rellenarse en el momento de la venta, firmarlo el cliente y conservarlo por parte de la tienda.", locale) + "\r\n"
							        + I18N.getTexto(
							                "Se entregarán dos copias del formulario B al cliente indicándole que nos debe remitir una copia, una vez el equipo haya sido instalado por un profesional homologado.",
							                locale);

							auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.VALIDAR_COMPRA, sesion);
							getDatos().put(RequestAuthorizationController.AUTORIZAR_TIPOLOGIA, textoVentanaAutorizacion);
							abrirVentanaAutorizacion(auditEvent, datos);
							autorizado = (boolean) getDatos().get(RequestAuthorizationController.PERMITIR_ACCION);
							return autorizado;
	 
						case "producto precursor explosivo regulado":
							auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.VALIDAR_COMPRA, sesion);
							textoVentanaAutorizacion = I18N.getTexto("Solicite ayuda a un empleado", locale);
							getDatos().put(RequestAuthorizationController.AUTORIZAR_TIPOLOGIA, textoVentanaAutorizacion);
							abrirVentanaAutorizacion(auditEvent, datos);
							autorizado = (boolean) getDatos().get(RequestAuthorizationController.PERMITIR_ACCION);
							return autorizado;

						case "producto biocida uso profesional":
							auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.VALIDAR_COMPRA, sesion);
							textoVentanaAutorizacion = I18N.getTexto(
							        "Necesita acreditación de usuario profesional para la compra de este producto. Comunicación al cliente que la ficha de seguridad se encuentra en página web https://www.bricodepot.es/ en apartado especificaciones del producto.",
							        locale);
							getDatos().put(RequestAuthorizationController.AUTORIZAR_TIPOLOGIA, textoVentanaAutorizacion);
							abrirVentanaAutorizacion(auditEvent, datos);
							autorizado = (boolean) getDatos().get(RequestAuthorizationController.PERMITIR_ACCION);
							return autorizado;
					}

				}
				if (StringUtils.isNotBlank(texto)) {
					log.debug("mostrarPopUpTipologias() - " + texto);
					VentanaDialogoComponent.crearVentanaInfo(texto, getStage());
					return true;
				}
			}
			catch (Exception e) {
				log.error("mostrarPopUpTipologias() - No se ha encontrado propiedades para el articulo: " + tfCodigoIntro.getText());
			}
			return true;
		}
	
	@FXML
	protected void consultarFidelizado() {
		log.debug("consultarFidelizado() - Consultando datos fidelizado con documento : " + tfIdenFidelizado.getText());
		getApplication().getMainView().showModalCentered(InsertarDatosFidelizadoView.class, getDatos(), getStage());
		String datoInsertado = (String) getDatos().get(InsertarDatosFidelizadoController.DATO_INSERTADO);
		if (datoInsertado.equals("cancelar")) {
			tfCodigoIntro.requestFocus();
			return;
		}

		if (StringUtils.isBlank(datoInsertado)) {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("El campo no puede estar vacío"), getStage());
			tfCodigoIntro.requestFocus();
			return;
		}
		tfIdenFidelizado.setText(datoInsertado);
		buscarFidelizado(datoInsertado);
		tfCodigoIntro.requestFocus();
	}
	
	@FXML
	public void crearFacturaCompleta() {
		facturaA4 = false;
		accionFacturaCompleta();
	}

	@FXML
	private void crearFacturaA4() {
		accionFacturaCompleta();
	}
	
	protected void accionFacturaCompleta() {
		log.debug("accionFacturaCompleta() - Creando factura completa");
		getDatos().put("fidelizado", ticketManager.getTicket().getCabecera().getDatosFidelizado());
		getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);
		getApplication().getMainView().showModalCentered(SelfCheckoutFacturaView.class, getDatos(), this.getStage());
		
		if(((TicketVentaAbono) ticketManager.getTicket()).getDatosFacturacion() != null) {
			facturaA4 = true;
		}
		else {
			facturaA4 = false;
		}
		
		mostrarIntroduccionTarjetaFidelizado();
	}
	
	public void comprobarIdioma() {
		switch(((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getCodIdiomaCliente()) {
			case "de":
				lbFactura.setText(I18N.getTexto("Completa"));
				lbCompleta.setText(I18N.getTexto("Factura"));
				break;
			default:
				lbFactura.setText(I18N.getTexto("Factura Completa"));
				lbCompleta.setVisible(false);
				break;
		}
	}
	
	@FXML
	private void cambiarFacturaSimplificada() {
		log.debug("cambiarFacturaSimplificada() - Cambiando tipo de documento activo.");
		try {
			
			//En caso de haber seleccionado previamente una opción con factura, la eliminamos.
			if(ticketManager.getTicket().getCabecera().getCliente().getDatosFactura()!=null)
				ticketManager.getTicket().getCabecera().getCliente().setDatosFactura(null);		
			
			facturaA4 = false;			
			ticketManager.setDocumentoActivo(sesion.getAplicacion().getDocumentos().getDocumento("FS"));
			mostrarIntroduccionTarjetaFidelizado();

		}
		catch (DocumentoException e) {
			log.error("cambiarFacturaSimplificada() - Ha habido un error al cambiar a FS", e);
		}
	}
	
	private void abrirVentanaSeleccionTicket() {
		log.debug("abrirVentanaSeleccionTicket() - Inicializando ventana de seleccion de envio de ticket...");
		getDatos().put(EMAIL, lbEmailFidelizado.getText());
		getApplication().getMainView().showModalCentered(SelfCheckoutTipoObtencionTicketView.class, getDatos(), getStage());

	}
	
}
