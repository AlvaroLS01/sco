package com.comerzzia.brico.pos.gui.ventas;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.api.model.loyalty.TiposContactoFidelizadoBean;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.api.rest.client.fidelizados.ResponseGetFidelizadoRest;
import com.comerzzia.brico.pos.gui.ventas.tickets.SesionTicketManager;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.services.intervenciones.IntervencionesService;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas.BricodepotLineaTicket;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.DispositivoCallback;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.normal.BotonBotoneraNormalComponent;
import com.comerzzia.pos.core.gui.componentes.botonaccion.simple.BotonBotoneraSimpleComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.BotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.ConfiguracionBotonBean;
import com.comerzzia.pos.core.gui.componentes.botonera.PanelBotoneraBean;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.exception.CargarPantallaException;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FormularioLineaArticuloBean;
import com.comerzzia.pos.gui.ventas.tickets.articulos.LineaTicketGui;
import com.comerzzia.pos.persistence.articulos.etiquetas.EtiquetaArticuloBean;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.services.articulos.ArticulosService;
import com.comerzzia.pos.services.cajas.CajaRetiradaEfectivoException;
import com.comerzzia.pos.services.cajas.CajasServiceException;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.sesion.SesionInitException;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.promociones.PromocionesServiceException;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketException;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@Primary
@Component
public class BricodepotFacturacionArticulosController extends FacturacionArticulosController{
	
	public static final String APIKEY = "WEBSERVICES.APIKEY";
	public static final String EMAIL = "EMAIL";
	public static final String SCO_CANTIDAD_MAXIMA_VENTA_AUTONOMA = "SCO.CANTIDAD_MAXIMA_VENTA_AUTONOMA";
	public static final String SCO_IMPORTE_MAXIMA_VENTA_AUTONOMA = "SCO.IMPORTE_MAXIMA_VENTA_AUTONOMA";
	
	@Autowired
	protected SesionTicketManager sesionTicketManager; 
	
	@Autowired
	protected VariablesServices variablesServices;
	
	@Autowired
	protected ArticulosService articulosService;
	
	@Autowired
	private IntervencionesService intervencionesService;
	
	protected String intervencion = "S";
	
	@Override
	public void initializeForm() throws InitializeGuiException {
		super.initializeForm();
		crearIntervencion();
	}
	
	@Override
	public void initializeManager() throws SesionInitException {
		ticketManager = SpringContext.getBean(SelfCheckoutTicketManager.class);
	
		if(sesionTicketManager.getSesionTicketManager()!= null) {
			ticketManager = sesionTicketManager.getSesionTicketManager();
		} else {
			ticketManager.init();
			sesionTicketManager.setSesionTicketManager(ticketManager);
		}
	}
	
	@Override
	public void initializeComponents() throws InitializeGuiException {
		try {
			initializeManager();
			visor = Dispositivos.getInstance().getVisor();
			initTecladoNumerico(tecladoNumerico);

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
	}
	public void fidelizacion() {
		log.debug("fidelizacion()");
		Dispositivos.getInstance().getFidelizacion().pedirTarjetaFidelizado(getStage(), new DispositivoCallback<FidelizacionBean>(){

			@Override
			public void onSuccess(FidelizacionBean tarjeta) {
				if (tarjeta.isBaja()) {
					VentanaDialogoComponent.crearVentanaError(I18N.getTexto("La tarjeta de fidelización {0} no está activa", tarjeta.getNumTarjetaFidelizado()), getStage());
					tarjeta = null;
					ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
				}
				else {
					// Tarjeta válida - lo seteamos en el ticket
					ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
				}
				
				ResponseGetFidelizadoRest fidelizado = recuperarFidelizadoByNumTarjeta(tarjeta.getNumTarjetaFidelizado());
				
				HashMap<String, Object> adicionales = new HashMap<String, Object>() ;
				adicionales.put(EMAIL, fidelizado.getEmail());
				ticketManager.getTicket().getCabecera().getDatosFidelizado().setAdicionales(adicionales);
				
				ticketManager.recalcularConPromociones();
				guardarCopiaSeguridad();
				refrescarDatosPantalla();
			}

			@Override
			public void onFailure(Throwable e) {
				// Los errores se muestran desde el código del dispositivo
				// Quitamos los datos de fidelizado
				FidelizacionBean tarjeta = null;
				ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);	
				guardarCopiaSeguridad();
				ticketManager.recalcularConPromociones();
				refrescarDatosPantalla();
			}

		});
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
	
	@Override
	public void nuevoCodigoArticulo() {
		// no dejar introducir líneas en un ticket nuevo si ha superado el importe de bloqueo de retirada
		if (tbLineas.getItems().size() == 0 && checkBloqueoRetirada()) {			
			tfCodigoIntro.clear();
			return;
		}
		
		// Validamos los datos
		if (!tfCodigoIntro.getText().trim().isEmpty()) {
			log.debug("nuevoCodigoArticulo() - Creando línea de artículo");

			frValidacion.setCantidad(tfCantidadIntro.getText().trim());
			frValidacion.setCodArticulo(tfCodigoIntro.getText().trim().toUpperCase());
			BigDecimal cantidad = frValidacion.getCantidadAsBigDecimal();
			tfCodigoIntro.clear();

			if (accionValidarFormulario() && cantidad != null && !BigDecimalUtil.isIgualACero(cantidad)) {
				log.debug("nuevoCodigoArticulo()- Formulario validado");

				// Si es prefijo de tarjeta fidelizacion, marcamos la venta como fidelizado y llamamos al REST
				if (Dispositivos.getInstance().getFidelizacion().isPrefijoTarjeta(frValidacion.getCodArticulo())) {

					ticketManager.recalcularConPromociones();
					refrescarDatosPantalla();

					Dispositivos.getInstance().getFidelizacion().cargarTarjetaFidelizado(frValidacion.getCodArticulo(), getStage(), new DispositivoCallback<FidelizacionBean>(){

						@Override
						public void onSuccess(FidelizacionBean tarjeta) {
							if (tarjeta.isBaja()) {
								VentanaDialogoComponent.crearVentanaError(I18N.getTexto("La tarjeta de fidelización {0} no está activa", tarjeta.getNumTarjetaFidelizado()), getStage());
								tarjeta = null;
								ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
							}
							else {
								// Tarjeta válida - lo seteamos en el ticket
								ticketManager.getTicket().getCabecera().setDatosFidelizado(tarjeta);
								
								ResponseGetFidelizadoRest fidelizado = recuperarFidelizadoByNumTarjeta(tarjeta.getNumTarjetaFidelizado());
								HashMap<String, Object> adicionales = new HashMap<String, Object>() ;
								adicionales.put(EMAIL, fidelizado.getEmail());
								ticketManager.getTicket().getCabecera().getDatosFidelizado().setAdicionales(adicionales);
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
							ticketManager.recalcularConPromociones();
							refrescarDatosPantalla();
						}
					});
					return;
				}

				NuevoCodigoArticuloTask taskArticulo = SpringContext.getBean(NuevoCodigoArticuloTask.class, this, frValidacion.getCodArticulo(), cantidad); // anidada
				taskArticulo.start();
			}
		}
	}
	@Override
	public synchronized LineaTicket insertarLineaVenta(String sCodart, String sDesglose1, String sDesglose2, BigDecimal nCantidad)
	        throws LineaTicketException, PromocionesServiceException, DocumentoException, CajasServiceException, CajaRetiradaEfectivoException {

		BricodepotLineaTicket linea = (BricodepotLineaTicket) ticketManager.nuevaLineaArticulo(sCodart, sDesglose1, sDesglose2, nCantidad, getStage(), null, false);
		mostrarPopUpTipologias(linea);
		return linea;
	}
	
	/**
	 * Metodo para leer ciertas propiedades y mostrar un popUp en base a estas
	 * @param linea
	 */
	public void mostrarPopUpTipologias(BricodepotLineaTicket linea) {
		log.debug("mostrarPopUpTipologias() - Iniciando PopUp tipológias");
		try {
			String texto = null;
			List<EtiquetaArticuloBean> etiquetas = linea.getArticulo().getEtiquetas();

			for (EtiquetaArticuloBean etiquetaArticuloBean : etiquetas) {
				switch (etiquetaArticuloBean.getEtiqueta().toLowerCase()) {
					case "aire acondicionado":
						texto = I18N.getTexto("Se deben entregar al cliente formularios A y B de aires acondicionados.")+"\r\n"
								+ "\r\n"
								+ I18N.getTexto("El formulario A deberá rellenarse en el momento de la venta, firmarlo el cliente y conservarlo por parte de la tienda.")+"\r\n"
								+ "\r\n"
								+ I18N.getTexto("Se entregarán dos copias del formulario B al cliente indicándole que nos debe remitir una copia, una vez el equipo haya sido instalado por un profesional homologado.");
						break;

					case "producto precursor explosivo regulado":
						texto = I18N.getTexto("Productos a notificar en caso de más de 6 unidades. Ver procedimiento precursores explosivos.");
						break;

					case "producto biocida uso profesional":
						texto = I18N.getTexto("Necesita acreditación de usuario profesional para la compra de este producto. Comunicación al cliente que la ficha de seguridad se encuentra en página web https://www.bricodepot.es/ en apartado especificaciones del producto.");
						break;

					default:
						break;
				}
			}

			if (StringUtils.isNotBlank(texto)) {
				log.debug("mostrarPopUpTipologias() - " + texto);
				VentanaDialogoComponent.crearVentanaInfo(texto, getStage());
			}
		}
		catch (Exception e) {
			log.error("mostrarPopUpTipologias() - No se ha encontrado propiedades para el articulo: " + linea);
		}
	}

	// BRICOD-231
	public Boolean superaMaxCantidadArticulos(BigDecimal cantidadArticulos) {
		// Validamos que no sobrepasen el máximo de ventas
		log.debug("superaMaxCantidadArticulos() - Comprobando si se supera la cantidad máxima de artículos");
		String codigoArticulo = frValidacion.getCodArticulo() == null ? "":frValidacion.getCodArticulo();
		if (!Dispositivos.getInstance().getFidelizacion().isPrefijoTarjeta(codigoArticulo)) {
			// BRICOD-326: Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un Number
			String cantidadMaxString = variablesServices.getVariableAsString(SCO_CANTIDAD_MAXIMA_VENTA_AUTONOMA);
			BigDecimal cantidadMax = StringUtils.isBlank(cantidadMaxString) ? BigDecimal.ZERO : new BigDecimal(cantidadMaxString);
			if (cantidadMax.compareTo(BigDecimal.ZERO) != 0) { // En caso de que la variable no sea NULL

				BigDecimal totalArticulos = ticketManager.getTicket().getCabecera().getCantidadArticulos();
				BigDecimal sumaTotal = totalArticulos.add(cantidadArticulos);
				return mostrarPantallaSuperaMax(cantidadMax, totalArticulos, sumaTotal);
			}
		}
		return false;
	}
	
	// BRICOD-231
	public Boolean comprobarSuperaMaxImporte(FormularioLineaArticuloBean frValidacion) {
		// Validamos que no sobrepasen el importe máximo
		log.debug("comprobarSuperaMaxImporte() - Comprobando si se supera el importe máximo de venta");
		if (!Dispositivos.getInstance().getFidelizacion().isPrefijoTarjeta(frValidacion.getCodArticulo())) {
			LineaTicket linea = new LineaTicket();
			try {
				linea = ticketManager.nuevaLineaArticulo(frValidacion.getCodArticulo(), null, null, frValidacion.getCantidadAsBigDecimal(), null);
				ticketManager.eliminarLineaArticulo(linea.getIdLinea());
			}
			catch (LineaTicketException e) {
				log.error("comprobarSuperaMaxImporte() - No se ha encontrado el artículo)", e);
			}

			log.debug("comprobarSuperaMaxImporte() - Comprobando si se supera el importe máximo de venta");
			// BRICOD-326: Primero obtenemos la variable en String por si su valor es una cadena vacía y no NULL o un
			// Number
			String importeMaxString = variablesServices.getVariableAsString(SCO_IMPORTE_MAXIMA_VENTA_AUTONOMA);
			BigDecimal importeMax = StringUtils.isBlank(importeMaxString) ? BigDecimal.ZERO : new BigDecimal(importeMaxString);
			if (importeMax.compareTo(BigDecimal.ZERO) != 0) { // En caso de que la variable no sea NULL

				BigDecimal importeTotal = ticketManager.getTicket().getCabecera().getTotales().getTotal();
				BigDecimal sumaTotal = importeTotal.add(linea.getImporteTotalConDto());
				return mostrarPantallaSuperaMax(importeMax, importeTotal, sumaTotal);
			}
		}
		return false;
	}

	// BRICOD-231
	protected Boolean mostrarPantallaSuperaMax(BigDecimal importeMax, BigDecimal importeTotal, BigDecimal sumaTotal) {
		if (importeTotal.compareTo(importeMax) >= 0 || sumaTotal.compareTo(importeMax) > 0) {
			log.debug("mostrarPantallaSuperaMax() - Se está superando el máximo de artículos/importe");
			if (VentanaDialogoComponent.crearVentanaConfirmacion(
			        I18N.getTexto("Su compra necesita la asistencia de un empleado. Disculpe las molestias."), getStage())) {

				TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.VALIDAR_COMPRA, sesion);
				HashMap<String, Object> datosSupervisor = new HashMap<>();
				return abrirVentanaAutorizacion(auditEvent, datosSupervisor);
			}
			cancelarVenta();
			return true;
		}
		return false;
	}

	// BRICOD-231
	protected Boolean abrirVentanaAutorizacion(TicketAuditEvent auditEvent, HashMap<String, Object> datos) {
		log.debug("abrirVentanaAutorizacion() - Inicio del proceso de auditoria");
		List<TicketAuditEvent> events = new ArrayList<>();
		events.add(auditEvent);
		datos.put(RequestAuthorizationController.AUDIT_EVENT, events);
		
		getApplication().getMainView().showModalCentered(RequestAuthorizationView.class, datos, this.getStage());
		if((Boolean)datos.get(RequestAuthorizationController.PERMITIR_ACCION)) {
			if(ticketManager != null) {
				((SelfCheckoutTicketManager)ticketManager).setVentaIsAutorizada(true);
			}
		}
		return !((Boolean)datos.get(RequestAuthorizationController.PERMITIR_ACCION));
	}
	
	// BRICOD-231
	@Override
	public void abrirBusquedaArticulos() {
		if (!((SelfCheckoutTicketManager) ticketManager).getVentaIsAutorizada() && superaMaxCantidadArticulos(BigDecimal.ONE)) {
            return;
        }
        super.abrirBusquedaArticulos();
	}

	@Override
	public void abrirPagos() {
		// En caso de que el ticket fuera null la primera vez que se crea intervención, lo volvemos a intentar antes de pasar a pagos
		if(StringUtils.isBlank(((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).getIntervencion())) {
			crearIntervencion();
		}
		super.abrirPagos();
	}
	
	public void crearIntervencion() {
		if ("S".equals(sesion.getSesionUsuario().getUsuario().getPuedeCambiarMenu())) {
			try {
				log.debug("actionBtAceptar() - Iniciando intervención por el usuario " + sesion.getSesionUsuario().getUsuario().getDesusuario());
				intervencionesService.crearIntervencion();
				((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setIntervencion(intervencion);
			}
			catch (Exception e) {
				log.error("actionBtAceptar() - Error creando intervención en ticket: " + e.getMessage(), e);
			}
		}
	}
	
	@Override
	protected LineaTicket nuevoArticuloTaskCall(String codArticulo, BigDecimal cantidad)
	        throws LineaTicketException, PromocionesServiceException, DocumentoException, CajasServiceException, InitializeGuiException, CajaRetiradaEfectivoException {
		LineaTicket lineaTicket = super.nuevoArticuloTaskCall(codArticulo, cantidad);
		((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).setIntervencion(intervencion);
		return lineaTicket;
	}
}

