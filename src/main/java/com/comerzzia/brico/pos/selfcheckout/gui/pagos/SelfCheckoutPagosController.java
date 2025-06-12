package com.comerzzia.brico.pos.selfcheckout.gui.pagos;

import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.api.rest.client.exceptions.RestConnectException;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.exceptions.RestTimeoutException;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.api.rest.client.fidelizados.ResponseGetFidelizadoRest;
import com.comerzzia.brico.pos.selfcheckout.core.SelfCheckoutMainViewController;
import com.comerzzia.brico.pos.selfcheckout.core.gui.SCOBackgroundTask;
import com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket.insertarcorreo.InsertarCorreoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.busqueda.BotonBotoneraSelfCheckoutComponent;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigopostal.SelfCheckoutIntroducirCodigoPostalController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigopostal.SelfCheckoutIntroducirCodigoPostalView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.services.impresion.SelfCheckoutServicioImpresion;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.SelfCheckoutTicketVentaAbono;
import com.comerzzia.bricodepot.posservices.client.TarjetasApi;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.bricodepot.posservices.client.model.ResponseGetTarjetaregaloRest;
import com.comerzzia.core.servicios.api.ComerzziaApiManager;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.visor.IVisor;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.botonera.BotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.ConfiguracionBotonBean;
import com.comerzzia.pos.core.gui.componentes.botonera.PanelBotoneraBean;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.exception.CargarPantallaException;
import com.comerzzia.pos.core.gui.tablas.celdas.CellFactoryBuilder;
import com.comerzzia.pos.dispositivo.comun.tarjeta.CodigoTarjetaController;
import com.comerzzia.pos.dispositivo.comun.tarjeta.CodigoTarjetaView;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.articulos.LineaTicketGui;
import com.comerzzia.pos.gui.ventas.tickets.pagos.PagosController;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.giftcard.GiftCardBean;
import com.comerzzia.pos.persistence.mediosPagos.MedioPagoBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.payments.PaymentException;
import com.comerzzia.pos.services.payments.events.PaymentErrorEvent;
import com.comerzzia.pos.services.payments.events.PaymentOkEvent;
import com.comerzzia.pos.services.payments.events.PaymentSelectEvent;
import com.comerzzia.pos.services.payments.events.PaymentsErrorEvent;
import com.comerzzia.pos.services.payments.events.PaymentsOkEvent;
import com.comerzzia.pos.services.payments.methods.PaymentMethodManager;
import com.comerzzia.pos.services.payments.methods.types.GiftCardManager;
import com.comerzzia.pos.services.promociones.Promocion;
import com.comerzzia.pos.services.promociones.PromocionesServiceException;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.cupones.CuponAplicadoTicket;
import com.comerzzia.pos.services.ticket.cupones.CuponEmitidoTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.promociones.PromocionTicket;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

@SuppressWarnings("unchecked")
@Component
public class SelfCheckoutPagosController extends PagosController {

	private Logger log = Logger.getLogger(SelfCheckoutPagosController.class);

	final IVisor visor = Dispositivos.getInstance().getVisor();
	
	public static final String COD_MP_GIFTCARD = "0020";

	public static final String COD_MP_VALE = "1000";

	@FXML
	protected Label lbSeleccionMedioPago, lbTextCambio, lbTituloMedioPago, lbImporte, lbTextACuenta, lbTextoCambio, lbTextoPendiente, lbFormaPagoCambio, lbTextDescuentos, lbDescuentos,
	        lbTotalArticulosMensaje, lbTotalArticulos;

	@FXML
	protected TableView<LineaTicketGui> tbLineas;

	@FXML
	protected TableView<LineaTicketGui> tbCoupons;

	@FXML
	protected Tab panelPestanaPagoTarjeta;

	@FXML
	protected TableColumn<LineaTicketGui, String> tcLineasArticulo, tcLineasDescripcion, tcLineasDescripcionCoupons, tcLineasDesglose1, tcLineasDesglose2, tcVendedor;

	@FXML
	protected TableColumn<LineaTicketGui, BigDecimal> tcLineasCantidad, tcLineasPVP, tcLineasImporte, tcLineasImporteCoupons, tcLineasDescuento;

	@FXML
	protected HBox panelBotoneraMediosPago;

	@Autowired
	private VariablesServices variablesService;

	@Autowired
	private MediosPagosService mediosPagosService;

	@Autowired
	protected ComerzziaApiManager comerzziaApiManager;

	private BotoneraComponent botonera;

	ObservableList<LineaTicketGui> lineas = FXCollections.observableList(new ArrayList<LineaTicketGui>());
	ObservableList<LineaTicketGui> lineasCoupons = FXCollections.observableList(new ArrayList<LineaTicketGui>());

	private String valor, email, cp;

	public static final String CP = "CODIGO_POSTAL";

	public static final String PIDE_CP = "SCO.PIDE_CP";

	public static final String ACCION_CANCELAR_TARJETA = "CANCELAR_TARJETA";

	protected boolean cbFT;

	protected String emailInsertado;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		super.initialize(url, rb);

		tcLineasArticulo.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcLineasArticulo", null, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasDescripcion.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcLineasDescripcion", null, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasDesglose1.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcLineasDesglose1", null, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasDesglose2.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcLineasDesglose2", null, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasPVP.setCellFactory(CellFactoryBuilder.createCellRendererCeldaImporte("tbLineas", "tcLineasPVP", CellFactoryBuilder.ESTILO_ALINEACION_DCHA));
		tcLineasImporte.setCellFactory(CellFactoryBuilder.createCellRendererCeldaImporte("tbLineas", "tcLineasImporte", CellFactoryBuilder.ESTILO_ALINEACION_DCHA));
		tcLineasDescuento.setCellFactory(CellFactoryBuilder.createCellRendererCeldaPorcentaje("tbLineas", "tcLineasDescuento", 2, CellFactoryBuilder.ESTILO_ALINEACION_DCHA));
		tcVendedor.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcVendedor", 2, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasCantidad.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbLineas", "tcLineasCantidad", 3, CellFactoryBuilder.ESTILO_ALINEACION_DCHA));

		tcLineasDescripcionCoupons.setCellFactory(CellFactoryBuilder.createCellRendererCelda("tbCoupons", "tcLineasDescripcionCoupons", null, CellFactoryBuilder.ESTILO_ALINEACION_IZQ));
		tcLineasImporteCoupons.setCellFactory(CellFactoryBuilder.createCellRendererCeldaImporte("tbCoupons", "tcLineasImporteCoupons", CellFactoryBuilder.ESTILO_ALINEACION_DCHA));

		Boolean usaDescuentoEnLinea = variablesService.getVariableAsBoolean(VariablesServices.TICKETS_USA_DESCUENTO_EN_LINEA);
		if (!usaDescuentoEnLinea) {
			tcLineasDescuento.setVisible(false);
		}
		Boolean vendedorVisible = variablesService.getVariableAsBoolean(VariablesServices.TPV_COLUMNA_VENDEDOR_VISIBLE, false);
		if (!vendedorVisible) {
			tcVendedor.setVisible(false);
		}

		// Definimos un factory para cada celda para aumentar el rendimiento
		tcLineasArticulo.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getArtProperty();
			}
		});
		tcLineasDescripcion.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getDescripcionProperty();
			}
		});
		tcLineasCantidad.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, BigDecimal>, ObservableValue<BigDecimal>>(){

			@Override
			public ObservableValue<BigDecimal> call(CellDataFeatures<LineaTicketGui, BigDecimal> cdf) {
				return cdf.getValue().getCantidadProperty();
			}
		});
		tcLineasDesglose1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getDesglose1Property();
			}
		});
		tcLineasDesglose2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getDesglose2Property();
			}
		});
		tcLineasPVP.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, BigDecimal>, ObservableValue<BigDecimal>>(){

			@Override
			public ObservableValue<BigDecimal> call(CellDataFeatures<LineaTicketGui, BigDecimal> cdf) {
				return cdf.getValue().getPvpProperty();
			}
		});
		tcLineasImporte.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, BigDecimal>, ObservableValue<BigDecimal>>(){

			@Override
			public ObservableValue<BigDecimal> call(CellDataFeatures<LineaTicketGui, BigDecimal> cdf) {
				return cdf.getValue().getImporteTotalFinalProperty();
			}
		});
		tcLineasDescuento.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, BigDecimal>, ObservableValue<BigDecimal>>(){

			@Override
			public ObservableValue<BigDecimal> call(CellDataFeatures<LineaTicketGui, BigDecimal> cdf) {
				return cdf.getValue().getDescuentoProperty();
			}
		});
		tcVendedor.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getVendedorProperty();
			}
		});

		Callback<TableView<LineaTicketGui>, TableRow<LineaTicketGui>> callback = new Callback<TableView<LineaTicketGui>, TableRow<LineaTicketGui>>(){

			@Override
			public TableRow<LineaTicketGui> call(TableView<LineaTicketGui> p) {
				return new TableRow<LineaTicketGui>(){

					@Override
					protected void updateItem(LineaTicketGui linea, boolean empty) {
						super.updateItem(linea, empty);
						if (linea != null) {
							if (linea.isCupon()) {
								if (!getStyleClass().contains("cell-renderer-cupon")) {
									getStyleClass().add("cell-renderer-cupon");
								}
							}
							else if (linea.isLineaDocAjeno()) {
								if (!getStyleClass().contains("cell-renderer-lineaDocAjeno")) {
									getStyleClass().add("cell-renderer-lineaDocAjeno");
								}
							}
							else {
								getStyleClass().removeAll(Collections.singleton("cell-renderer-lineaDocAjeno"));
								getStyleClass().removeAll(Collections.singleton("cell-renderer-cupon"));
							}
						}
						else {
							getStyleClass().removeAll(Collections.singleton("cell-renderer-lineaDocAjeno"));
							getStyleClass().removeAll(Collections.singleton("cell-renderer-cupon"));

						}
					}
				};
			}
		};
		tbLineas.setRowFactory(callback);

		tcLineasDescripcionCoupons.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, String>, ObservableValue<String>>(){

			@Override
			public ObservableValue<String> call(CellDataFeatures<LineaTicketGui, String> cdf) {
				return cdf.getValue().getDescripcionProperty();
			}
		});

		tcLineasImporteCoupons.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LineaTicketGui, BigDecimal>, ObservableValue<BigDecimal>>(){

			@Override
			public ObservableValue<BigDecimal> call(CellDataFeatures<LineaTicketGui, BigDecimal> cdf) {
				return cdf.getValue().getImporteTotalFinalProperty();
			}
		});

		Callback<TableView<LineaTicketGui>, TableRow<LineaTicketGui>> callback2 = new Callback<TableView<LineaTicketGui>, TableRow<LineaTicketGui>>(){

			@Override
			public TableRow<LineaTicketGui> call(TableView<LineaTicketGui> p) {
				return new TableRow<LineaTicketGui>(){

					@Override
					protected void updateItem(LineaTicketGui linea, boolean empty) {
						super.updateItem(linea, empty);
						if (linea != null) {
							if (linea.isCupon()) {
								if (!getStyleClass().contains("cell-renderer-cupon")) {
									// getStyleClass().add("cell-renderer-cupon");
								}
							}
							else if (linea.isLineaDocAjeno()) {
								if (!getStyleClass().contains("cell-renderer-lineaDocAjeno")) {
									getStyleClass().add("cell-renderer-lineaDocAjeno");
								}
							}
							else {
								getStyleClass().removeAll(Collections.singleton("cell-renderer-lineaDocAjeno"));
								getStyleClass().removeAll(Collections.singleton("cell-renderer-cupon"));
							}
						}
						else {
							getStyleClass().removeAll(Collections.singleton("cell-renderer-lineaDocAjeno"));
							getStyleClass().removeAll(Collections.singleton("cell-renderer-cupon"));

						}
					}
				};
			}
		};
		tbCoupons.setRowFactory(callback2);

	}

	@Override
	public void initializeComponents() {
		try {
			PanelBotoneraBean botoneraMediosPagos = null;
			try {
				botoneraMediosPagos = getView().loadBotonera("_sc_panel.xml");
			}
			catch (InitializeGuiException ex) {
				log.info("inicializarComponentes() - No cargando botonera personalizada de mediospago \"xxx_sc_panel.xml\": " + ex.getMessage());
			}

			panelBotoneraMediosPago.getChildren().clear();
			double anchoBotonera = botoneraMediosPagos.getLineasBotones().get(0).getLineaBotones().size() * 120.0;
			botonera = new BotoneraComponent(botoneraMediosPagos, anchoBotonera, 110.0, this, BotonBotoneraSelfCheckoutComponent.class);
			panelBotoneraMediosPago.getChildren().add(botonera);

		}
		catch (CargarPantallaException ex) {
			log.error("inicializarComponentes() - Error creando botonera para medio de pago. error : " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Error cargando pantalla pagos."), getStage());
		}

		// try {
		// List<ConfiguracionBotonBean> listaAccionesAccionesTabla = cargarAccionesTabla();
		// listaAccionesAccionesTabla = listaAccionesAccionesTabla.subList(0, 4);
		// botoneraAccionesTabla = new BotoneraComponent(listaAccionesAccionesTabla.size(), 1, this,
		// listaAccionesAccionesTabla, panelMenuTabla.getPrefWidth(), panelMenuTabla.getPrefHeight(),
		// BotonBotoneraSimpleComponent.class.getName());
		// panelMenuTabla.getChildren().clear();
		// panelMenuTabla.getChildren().add(botoneraAccionesTabla);
		// }
		// catch (Exception e) {
		// log.error("initializeComponents() - No se ha podido cargar la botonera de la tabla: " + e.getMessage(), e);
		// }
		addCallbackPintadoLineas();

		inicializarFocos();

		addSeleccionarTodoCampos();

		registrarAccionCerrarVentanaEscape();

		tbCoupons.setPlaceholder(new Label(""));

		setShowKeyboard(false);
		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);

	}

	protected List<ConfiguracionBotonBean> cargarAccionesTabla() {
		List<ConfiguracionBotonBean> listaAcciones = new ArrayList<>();
		listaAcciones.add(new ConfiguracionBotonBean("iconos/navigate_up2.png", null, null, "ACCION_TABLA_PRIMER_REGISTRO", "REALIZAR_ACCION")); // "Home"
		listaAcciones.add(new ConfiguracionBotonBean("iconos/navigate_up.png", null, null, "ACCION_TABLA_ANTERIOR_REGISTRO", "REALIZAR_ACCION")); // "Page
		                                                                                                                                          // Up"
		listaAcciones.add(new ConfiguracionBotonBean("iconos/navigate_down.png", null, null, "ACCION_TABLA_SIGUIENTE_REGISTRO", "REALIZAR_ACCION")); // "Page
		                                                                                                                                             // Down"
		listaAcciones.add(new ConfiguracionBotonBean("iconos/navigate_down2.png", null, null, "ACCION_TABLA_ULTIMO_REGISTRO", "REALIZAR_ACCION")); // "End"
		return listaAcciones;
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		enProceso = false;

		super.initializeForm();

		configurarIdioma();

		// Asignamos las lineas a la tabla
		lineas.clear();
		lineasCoupons.clear();

		for (LineaTicket linea : (List<LineaTicket>) ticketManager.getTicket().getLineas()) {
			LineaTicketGui gui = new LineaTicketGui(linea);
			lineas.add(gui);
		}
		for (PromocionTicket promocion : (List<PromocionTicket>) ticketManager.getTicket().getPromociones()) {
			if (promocion.isDescuentoMenosIngreso()) {
				CuponAplicadoTicket cuponAplicado = new CuponAplicadoTicket();
				cuponAplicado.setTextoPromocion(promocion.getTextoPromocion());
				cuponAplicado.setImporteTotalAhorrado(promocion.getImporteTotalAhorro());

				LineaTicketGui gui = new LineaTicketGui(cuponAplicado);

				lineasCoupons.add(gui);
			}
		}

		tbLineas.setItems(lineas);
		tbCoupons.setItems(lineasCoupons);

		LineaTicketGui selectedItem = getLineaSeleccionada();
		if (selectedItem != null) {
			tbLineas.getSelectionModel().select(lineas.indexOf(searchIdLinea(selectedItem)));
		}
		tbLineas.scrollTo(0);

		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				if (BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getPendiente())) {
					try {
						String texto = I18N.getTexto("¡GRACIAS A LOS DESCUENTOS, SU COMPRA ES GRATUITA!") + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
						        + I18N.getTexto("Descuentos aplicados:") + System.lineSeparator();

						for (PromocionTicket promocion : (List<PromocionTicket>) ticketManager.getTicket().getPromociones()) {
							texto = texto + System.lineSeparator() + " - " + StringUtils.leftPad(FormatUtil.getInstance().formateaImporteMoneda(promocion.getImporteTotalAhorro()), 15) + "     "
							        + promocion.getTextoPromocion();
						}
						texto = texto + System.lineSeparator() + "----------------" + System.lineSeparator() + " - "
						        + StringUtils.leftPad(FormatUtil.getInstance().formateaImporteMoneda(ticketManager.getTicket().getTotales().getTotalAPagar()), 15);

						VentanaDialogoComponent.crearVentanaInfo(texto, getStage());
						setearDatosParaEnvioTicket();
						aceptar();
						getDatos().put(SelfCheckoutFacturacionArticulosController.PARAM_SELF_CHECKOUT_CERRAR, true);
					}
					catch (Exception e) {
						log.error("refrescarDatosPantalla() - Ha habido un error al intentar finalizar la venta: " + e.getMessage(), e);
						VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("No se ha podido finalizar la venta. Contacte con un responsable de tienda"), e);
					}
				}
			}
		});
		if (getDatos().containsKey("valor")) {
			valor = (String) getDatos().get("valor");
		}
		if (getDatos().containsKey(SelfCheckoutFacturacionArticulosController.EMAIL)) {
			email = (String) getDatos().get(SelfCheckoutFacturacionArticulosController.EMAIL);
		}else {
			email = null;
		}
		
		if (!StringUtils.isNotBlank(email)) {
			valor = "Papel";
		}
		if (getDatos().containsKey("checkFT")) {
			cbFT = (boolean) getDatos().get("checkFT");
		}
		if (getDatos().containsKey(InsertarCorreoController.EMAIL_INSERTADO)) {
			emailInsertado = (String) getDatos().get(InsertarCorreoController.EMAIL_INSERTADO);
		}else {
			emailInsertado = null;		}
		
	}

	protected LineaTicketGui createLineaGui(CuponAplicadoTicket cupon) {
		return new LineaTicketGui(cupon);
	}

	@Override
	public void accionCancelar() {
		log.debug("accionCancelar()");
		try {
			boolean confirmacion = VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("¿Estás seguro de querer eliminar todas las líneas del ticket?"), getStage());
			if (!confirmacion) {
				return;
			}
			
			if (ticketManager.getTicket().getLineas().size() > 0) {
				TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.ANULACION_TICKET, sesion);
				abrirVentanaAutorizacion(auditEvent, getDatos());
				if (datos.get(RequestAuthorizationController.PERMITIR_ACCION).equals(true)) {
					((SelfCheckoutTicketManager) ticketManager).eliminarTicketCompleto(); // BRICOD-231
					refrescarDatosPantalla();
					initializeFocus();
					tbLineas.getSelectionModel().clearSelection();

					visor.escribirLineaArriba(I18N.getTexto("---NUEVO CLIENTE---"));
					visor.modoEspera();

					((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
					getStage().close();
				}
			}
			else {
				((SelfCheckoutTicketManager) ticketManager).eliminarTicketCompleto(); // BRICOD-231
				refrescarDatosPantalla();
				initializeFocus();
				tbLineas.getSelectionModel().clearSelection();

				visor.escribirLineaArriba(I18N.getTexto("---NUEVO CLIENTE---"));
				visor.modoEspera();

				((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaBienvenidaSelfCheckout();
				getStage().close();
			}
		}
		catch (TicketsServiceException ex) {
			log.error("accionCancelar() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
		catch (DocumentoException ex) {
			log.error("accionCancelar() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
		catch (PromocionesServiceException ex) {
			log.error("accionCancelar() - Error inicializando nuevo ticket: " + ex.getMessage(), ex);
			VentanaDialogoComponent.crearVentanaError(getStage(), ex.getMessageI18N(), ex);
		}
	}

	protected void configurarIdioma() {
		tcLineasArticulo.setText(I18N.getTexto("ARTÍCULO"));
		tcPagosFormaPago.setText(I18N.getTexto("FORMA DE PAGO"));
		tcLineasDescripcion.setText(I18N.getTexto("DESCRIPCIÓN"));
		lbSeleccionMedioPago.setText(I18N.getTexto("Seleccione su medio de pago"));
		tcLineasDesglose1.setText(I18N.getTexto("Talla"));
		tcLineasDesglose2.setText(I18N.getTexto("Color"));
		tcLineasCantidad.setText(I18N.getTexto("UND."));
		tcLineasPVP.setText(I18N.getTexto("PVP"));
		tcLineasDescuento.setText(I18N.getTexto("DTO."));
		tcLineasImporte.setText(I18N.getTexto("IMPORTE"));
		tcVendedor.setText(I18N.getTexto("Vendedor"));
		tcPagosImporte.setText(I18N.getTexto("IMPORTE"));
		lbTextEntregado.setText(I18N.getTexto("ENTREGADO"));
		lbTextCambio.setText(I18N.getTexto("CAMBIO"));
		lbTextAPagar.setText(I18N.getTexto("A PAGAR"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
		lbTitulo.setText(I18N.getTexto("Pagos"));
		lbMedioPago.setText(I18N.getTexto("EFECTIVO"));
		btAnotarPago.setText(I18N.getTexto("Anotar pago"));
		panelPestanaPagoEfectivo.setText(I18N.getTexto("Efectivo"));
		panelPestanaPagoTarjeta.setText(I18N.getTexto("TARJETA"));
		panelPestanaPagoContado.setText(I18N.getTexto("Otros"));
		lbTextACuenta.setText(I18N.getTexto("A CUENTA"));
		lbTextoCambio.setText(I18N.getTexto("CAMBIO"));
		lbTextoPendiente.setText(I18N.getTexto("PENDIENTE"));
		lbFormaPagoCambio.setText(I18N.getTexto("FORMA PAGO CAMBIO:"));
		lbMedioPagoVuelta.setText(I18N.getTexto("N/D"));
		btAceptar.setText(I18N.getTexto("Aceptar"));
		lbTituloMedioPago.setText(I18N.getTexto("Medio de Pago"));
		lbImporte.setText(I18N.getTexto("Importe"));
		tcLineasDescripcionCoupons.setText(I18N.getTexto("DESCRIPCIÓN DE OTROS DESCUENTOS"));
		tcLineasImporteCoupons.setText(I18N.getTexto("IMPORTE"));
		lbTextDescuentos.setText(I18N.getTexto("OTROS DESCUENTOS"));
		lbTotalArticulosMensaje.setText(I18N.getTexto("Nº UNIDADES"));
	}

	protected LineaTicketGui getLineaSeleccionada() {
		LineaTicketGui linea = tbLineas.getSelectionModel().getSelectedItem();
		if (linea == null) {
			tbLineas.getSelectionModel().selectFirst();
			linea = tbLineas.getSelectionModel().getSelectedItem();
		}
		return linea;
	}

	protected LineaTicketGui searchIdLinea(LineaTicketGui selectedItem) {
		for (LineaTicketGui linea : tbLineas.getItems()) {
			if (linea.getIdLinea() != null) {
				if (linea.getIdLinea().equals(selectedItem.getIdLinea())) {
					return linea;
				}
			}
			else {
				// Es cupón
				if (linea.getArticulo().equals(selectedItem.getArticulo())) {
					return linea;
				}
			}
		}
		return null;
	}

	public void seleccionarMedioPagoSelfCheckout(HashMap<String, String> parametros) {
		log.debug("seleccionarMedioPagoSelfCheckout() - comprobando medio de pago");

		if (parametros.containsKey("codMedioPago")) {
			String codMedioPago = parametros.get("codMedioPago");
			log.debug("Se procede a buscar el codMedioPago: " + codMedioPago);
			MedioPagoBean medioPago = mediosPagosService.getMedioPago(codMedioPago);
			if (medioPago != null) {
				log.debug("Medio de pago seleccionado: " + medioPago.getDesMedioPago());
					setearCodigoPostal();

				setearDatosParaEnvioTicket();

				medioPagoSeleccionado = medioPago;
				paymentsManager.select(medioPago.getCodMedioPago());

			}
			else {
				log.debug("No se ha encontrado el medio de pago con codigo: " + codMedioPago);
			}
		}
	}

	@Override
	protected void pay(PaymentMethodManager paymentMethodManager, String codMedioPago, BigDecimal importe) {
		final BigDecimal importeFinal = importe;
		final String codMedioPagoFinal = codMedioPago;
		btAnotarPago.setDisable(true);
		if (codMedioPagoFinal.equals("0010")) {

			new SCOBackgroundTask<Void>(){

				@Override
				protected Void call() throws Exception {
					paymentsManager.pay(codMedioPagoFinal, importeFinal);
					return null;
				}

				@SuppressWarnings("rawtypes")
				@Override
				protected void succeeded() {
					visor.modoPago(visorConverter.convert((TicketVenta) ticketManager.getTicket()));
					escribirVisor();
					btAnotarPago.setDisable(false);
					super.succeeded();
				}

				@Override
				protected void failed() {
					super.failed();
					Throwable e = getException();
					stage.close();

					if (e instanceof PaymentException) {
						PaymentErrorEvent errorEvent = new PaymentErrorEvent(this, ((PaymentException) e).getPaymentId(), e, ((PaymentException) e).getErrorCode(),
						        ((PaymentException) e).getMessage());
						PaymentsErrorEvent event = new PaymentsErrorEvent(this, errorEvent);
						paymentsManager.getEventsHandler().paymentsError(event);

					}
					else {
						PaymentErrorEvent errorEvent = new PaymentErrorEvent(this, -1, e, null, null);
						PaymentsErrorEvent event = new PaymentsErrorEvent(this, errorEvent);
						paymentsManager.getEventsHandler().paymentsError(event);
					}
					btAnotarPago.setDisable(false);

				}
			}.start(getStage());

		}
		else {
			super.pay(paymentMethodManager, codMedioPagoFinal, importeFinal);
		}

	}

	@SuppressWarnings("rawtypes")
	private void setearCodigoPostal() {
		log.debug("setearCodigoPostal() - Abriendo ventana de CP");
		BricodepotCabeceraTicket cabecera = (BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera();

		TicketVenta ticketVenta = (TicketVenta) ticketManager.getTicket();
		DatosFactura datosFactura = ticketVenta.getDatosFacturacion();

		String pideCP = variablesService.getVariableAsString(PIDE_CP);

		// caso 0 : No pide CP desde bbdd
		if (pideCP.equals("N")) {
			cp = "";

			// caso 1 : comprobar si trae fidelizado asociado, si trae cp blanco pido, que no sigue
		}
		else if (cabecera.getDatosFidelizado() != null && StringUtils.isNotBlank(cabecera.getDatosFidelizado().getCp())) {
			log.debug("setearDatosParaEnvioTicket() - Extraemos el cp del fidelizado");
			cp = cabecera.getDatosFidelizado().getCp();
		}
		else if (cabecera.getDatosFidelizado() != null && StringUtils.isBlank(cabecera.getDatosFidelizado().getCp())
		        && StringUtils.isNotBlank(cabecera.getDatosFidelizado().getNumTarjetaFidelizado())) {
			abrirVentanaCP(cabecera);

			log.debug("setearDatosParaEnvioTicket() - No tiene cp en el fidelizado");
			cp = (String) getDatos().get(SelfCheckoutIntroducirCodigoPostalController.PARAM_CP);

			// caso 2 : Solo para FT , si el cliente no tiene cp lo pido
		}
		else if (datosFactura != null && StringUtils.isNotBlank(datosFactura.getCp()) && cabecera.getCodTipoDocumento().equals("FT")) {
			log.debug("setearDatosParaEnvioTicket() - No tiene fidelizado, consultamos rl cp del cliente de la factura");
			cp = datosFactura.getCp();

		}
		else { // caso 3 : no se identifica y no pide FT
			log.debug("setearDatosParaEnvioTicket() - Venta anonima pedimos cp");
			abrirVentanaCP(cabecera);

			log.debug("setearDatosParaEnvioTicket() - Recogiendo valores");
			cp = (String) getDatos().get(SelfCheckoutIntroducirCodigoPostalController.PARAM_CP);

		}

		log.debug("setearDatosParaEnvioTicket() - datos recogidos de ventana de codifo postal: " + cp);
		if (StringUtils.isNotBlank(cp)) {
			log.debug("setearDatosParaEnvioTicket() - Seteando datos en cabecera");
			cabecera.setCpVenta(cp);
		}
	}

	private void abrirVentanaCP(BricodepotCabeceraTicket cabecera) {
		log.debug("abrirVentanaCP() - Inicializando ventana de CP...");
		getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);
		// BRICO-426 Evita NPE al hacer venta sin fidelizado
		if (cabecera.getDatosFidelizado() != null && cabecera.getDatosFidelizado().getCp() != null) {
			getDatos().put(CP, cabecera.getDatosFidelizado().getCp());
		}
		// BRICO-426
		getApplication().getMainView().showModalCentered(SelfCheckoutIntroducirCodigoPostalView.class, getDatos(), getStage());
	}

	private void setearDatosParaEnvioTicket() {
		log.debug("setearDatosParaEnvioTicket() - Abriendo ventana de selección");
		// abrirVentanaSeleccionTicket();

		log.debug("setearDatosParaEnvioTicket() - Recogiendo valores");

		if (StringUtils.isNotBlank(valor)) {
			BricodepotCabeceraTicket cabecera = (BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera();
			cabecera.setTipoImpresion(valor.toUpperCase());
		}

		if (StringUtils.isNotBlank(emailInsertado)) {
			email = emailInsertado;
		}
		
		if (StringUtils.isNotBlank(email) && ((valor.equals("Correo") || (valor.equals("Ambos"))))) {
			getDatos().put("correoSelect", true);
			getDatos().put("emailEnvio", email);
		}

		if (cbFT) {
			getDatos().put("checkFT", true);
		}
		
		log.debug("setearDatosParaEnvioTicket() - datos recogidos de ventana de seleccion de envio de ticket: " + "valor - " + valor + " / email - " + email);
		BricodepotCabeceraTicket cabecera = (BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera();
		if (valor != null && !valor.equals("papel") && StringUtils.isNotBlank(email)) {
			log.debug("setearDatosParaEnvioTicket() - Seteando datos en cabecera");
			cabecera.setEmailEnvioTicket(email);
		}
	}

	@Override
	protected void processPaymentOk(PaymentsOkEvent event) {
		PaymentOkEvent eventOk = event.getOkEvent();

		if (!eventOk.isCanceled()) {
			addPayment(eventOk);
		}
		else {
			deletePayment(eventOk);
		}

		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				refrescarDatosPantalla();
				selectDefaultPaymentMethod();
				comprobarPagoCubierto();
			}
		});
	}

	public void comprobarPagoCubierto() {
		if (BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getPendiente())) {
			try {
				aceptar();
				getDatos().put(SelfCheckoutFacturacionArticulosController.PARAM_SELF_CHECKOUT_CERRAR, true);
			}
			catch (DocumentoException e) {
				log.error("refrescarDatosPantalla() - Ha habido un error al intentar finalizar la venta: " + e.getMessage(), e);
				VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("No se ha podido finalizar la venta. Contacte con un responsable de tienda"), e);
			}
		}
	}

	@Override
	public void refrescarDatosPantalla() {
		super.refrescarDatosPantalla();

		lbTotal.setText(ticketManager.getTicket().getTotales().getPendiente() == null ? "0" : FormatUtil.getInstance().formateaImporte(ticketManager.getTicket().getTotales().getPendiente()));
		BigDecimal totalCupones = BigDecimal.ZERO;
		if ((List<PromocionTicket>) ticketManager.getTicket().getPromociones() != null) {
			for (PromocionTicket promocion : (List<PromocionTicket>) ticketManager.getTicket().getPromociones()) {
				if (promocion.isDescuentoMenosIngreso()) {
					totalCupones = totalCupones.add(promocion.getImporteTotalAhorro() == null ? BigDecimal.ZERO : promocion.getImporteTotalAhorro());
				}
			}
		}
		if (totalCupones != null) {
			lbDescuentos.setText(FormatUtil.getInstance().formateaImporte(totalCupones));
		}
		obtenerCantidadTotal();
	}

	@Override
	protected void accionSalvarTicketSucceeded(boolean repiteOperacion) {
		log.debug("accionSalvarTicketSucceeded() - Comprobando datos");

		if (StringUtils.isNotBlank(valor)) {
			if (valor.equalsIgnoreCase("correo") && StringUtils.isNotBlank(emailInsertado)) {
				log.debug("accionSalvarTicketSucceeded() - valor seleccionado correo");
				// Mostramos la ventana con la información de importe pagado, cambio...
				if (!BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getCambio().getImporte())) {
					mostrarVentanaCambio();
				}

				if (repiteOperacion) {
					enProceso = false;
					initTicketManager(false);
					aceptarPagos(false);
				}
				else {
					ticketManager.notificarContadores();
					ticketManager.finalizarTicket();

					getStage().close();
				}
			}
			else { // PAPEL - AMBOS
				log.debug("accionSalvarTicketSucceeded() - valor seleccionado papel/ambos");
				imprimir();
				// Mostramos la ventana con la información de importe pagado, cambio...
				if (!BigDecimalUtil.isIgualACero(ticketManager.getTicket().getTotales().getCambio().getImporte())) {
					mostrarVentanaCambio();
				}

				if (repiteOperacion) {
					enProceso = false;
					initTicketManager(false);
					aceptarPagos(false);
				}
				else {
					ticketManager.notificarContadores();
					ticketManager.finalizarTicket();

					getStage().close();
				}
			}
		}
		else {
			super.accionSalvarTicketSucceeded(repiteOperacion);
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

			String numTarjeta = (String) parametros.get(CodigoTarjetaController.PARAMETRO_NUM_TARJETA);
			ResponseGetTarjetaregaloRest tarjetaRest = new ResponseGetTarjetaregaloRest();

			if (StringUtils.isNotBlank(numTarjeta)) {
				String apiKey = variablesService.getVariableAsString(com.comerzzia.core.servicios.variables.Variables.WEBSERVICES_APIKEY);
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
						else if (tarjetaRegalo.getSaldoTotal().compareTo(BigDecimal.ZERO) == 0) {
							VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("La tarjeta introducida tiene saldo 0."), getStage());
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
									BigDecimal pendiente = ticketManager.getTicket().getTotales().getPendiente();
									BigDecimal importeTarjetaRegalo = tarjetaRegalo.getSaldoTotal();

									BigDecimal importePago = importeTarjetaRegalo;
									if (BigDecimalUtil.isMayor(importeTarjetaRegalo, pendiente)) {
										importePago = pendiente;
									}

									anotarPago(importePago);
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
				String mensaje = e.getMessage();
				int posicionInicioNumeroTarjeta = mensaje.lastIndexOf(":");
				String mensajeError = mensaje.substring(0, posicionInicioNumeroTarjeta + 1).trim();
				String numeroTarjeta = mensaje.substring(posicionInicioNumeroTarjeta + 1, mensaje.length()).trim();
				String tituloTraducido = I18N.getTexto("Lo sentimos, ha ocurrido un error en la petición.") + System.lineSeparator() + System.lineSeparator();
				String mensajeTraducido = I18N.getTexto(mensajeError + " {0}", numeroTarjeta);
				VentanaDialogoComponent.crearVentanaError(getStage(), tituloTraducido + mensajeTraducido, new RestHttpException(400, mensajeTraducido, e));
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

	@SuppressWarnings("rawtypes")
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

				if (ticketManager.getTicket().getCabecera().getCodTipoDocumento().equals("FT") && cbFT) {

					if (mapaParametros.get("ticket") instanceof SelfCheckoutTicketVentaAbono) {
						SelfCheckoutTicketVentaAbono t = (SelfCheckoutTicketVentaAbono) ticketManager.getTicket();
						if (StringUtils.isNotBlank(t.getNumTarjetaRegalo())) {
							mapaParametros.put("numTarjetaRegalo", t.getNumTarjetaRegalo());
						}
					}

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

			if (StringUtils.isNotBlank(valor) && (valor.equals("Papel"))) {
				getDatos().put("papelSelect", true);
			}

			if (StringUtils.isNotBlank(valor) && (valor.equals("Ambos"))) {
				getDatos().put("ambosSelect", true);
			}
			
			if(StringUtils.isNotBlank(email) && valor.equals("Correo")) {
				getDatos().put("emailEnvio", email);				
			}

		}
		catch (Exception e) {
			log.error("imprimir() - " + e.getClass().getName() + " - " + e.getLocalizedMessage(), e);
			VentanaDialogoComponent.crearVentanaError(getStage(),
			        I18N.getTexto("Lo sentimos, ha ocurrido un error al imprimir.") + System.lineSeparator() + System.lineSeparator() + I18N.getTexto("El error es: ") + e.getMessage(), e);
		}
	}

	/* Método que se ejecuta al seleccionar el checkbox de factura completa */
	// @FXML
	// protected void accionCbFacturaCompleta() {
	// if (!ticketManager.getDocumentoActivo().getCodtipodocumento().equals(Documentos.FACTURA_COMPLETA)) {
	// String msg = I18N.getTexto("Para seleccionar esta opción debe elegir Factura Completa");
	// VentanaDialogoComponent.crearVentanaInfo(msg, getStage());
	// cbFacturaCompleta.setSelected(false);
	// }
	// }

	protected void obtenerCantidadTotal() {
		TicketVentaAbono ticket = (TicketVentaAbono) ticketManager.getTicket();
		BigDecimal cantidad = ticket.getCantidadTotal();
		lbTotalArticulos.setText(FormatUtil.getInstance().formateaNumero(cantidad.abs()));
	}

	@Override
	protected void selectCustomPaymentMethod(PaymentSelectEvent paymentSelectEvent) {
		anotarPago(ticketManager.getTicket().getTotales().getPendiente());
	}

	public void volverAtras() {
		ticketManager.getTicket().getCuponesAplicados().clear();
		ticketManager.recalcularConPromociones();
		super.accionCancelar();
	}

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
}
