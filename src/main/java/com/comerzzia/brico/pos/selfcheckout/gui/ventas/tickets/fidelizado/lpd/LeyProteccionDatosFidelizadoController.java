package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.lpd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.api.model.loyalty.FidelizadoBean;
import com.comerzzia.api.model.loyalty.TiposContactoFidelizadoBean;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.fidelizados.FidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.IdentificacionFidelizadoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.firma.FirmaFidelizadoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.firma.FirmaFidelizadoView;
import com.comerzzia.brico.pos.selfcheckout.services.cliente.SelfcheckoutClienteService;
import com.comerzzia.brico.pos.selfcheckout.services.fidelizado.SelfCheckoutFidelizadosService;
import com.comerzzia.brico.pos.selfcheckout.services.fidelizado.TicketFidelizadoCaptacion;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.Controller;
import com.comerzzia.pos.core.gui.view.View;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;

@Component
public class LeyProteccionDatosFidelizadoController extends Controller {

	public static final String RECHAZAR = "RECHAZAR";
	public static final Logger log = Logger.getLogger(LeyProteccionDatosFidelizadoController.class.getName());
	public static final String LPD = "Ley de Protección de Datos";
	public static final String IMPRIMIR_LPD = "Imprimir Documento";
	public static final String CHECK_LPD = "checkLPD";

	@FXML
	protected Label lbTitulo;

	@FXML
	protected CheckBox checkBox;

	@FXML
	protected Button btRetrocederPag, btRechazar, btAceptar;
	
	@FXML
	protected WebView wvTexto;

	@Autowired
	protected Sesion sesion;

	protected byte[] firma;

	@Autowired
	protected SelfcheckoutClienteService scoClienteService;

	@Autowired
	protected VariablesServices variablesServices;

	@Autowired
	protected SelfCheckoutFidelizadosService selfCheckoutFidelizadosService;

	protected TicketManager ticketManager;

	protected DatosFactura factura;

	protected FidelizadoBean fidelizado;
	
	protected WebEngine webEngine;
	
	protected WebHistory webHistory;

	@Override
	public void initialize(URL url, ResourceBundle rb) {

	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		limpiarFormulario();
		refrescarDatosPantalla();
		configurarIdioma();
		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);
		fidelizado = (FidelizadoBean) getDatos().get(IdentificacionFidelizadoController.FIDELIZADO);
	}

	private void limpiarFormulario() {
		checkBox.setSelected(false);
	}

	private void configurarIdioma() {
		lbTitulo.setText(I18N.getTexto(LPD));
		checkBox.setText(I18N.getTexto(IMPRIMIR_LPD));
		btRetrocederPag.setText(I18N.getTexto("Retroceder Página"));
		btRechazar.setText(I18N.getTexto("Volver"));
		btAceptar.setText(I18N.getTexto("Aceptar Terminos y Condiciones Generales"));
	}

	@Override
	public void initializeFocus() {

	}

	public void refrescarDatosPantalla() {
		log.debug("refrescarDatosPantalla()");
		try {
			webEngine = wvTexto.getEngine();
			webHistory = webEngine.getHistory();
			cargarTexto(true);
		}
		catch (IOException e) {
		}
	}

	@FXML
	public void accionAceptar() {
		getDatos().put(CHECK_LPD, checkBox.isSelected());

		if (sesion.getAplicacion().getTienda().getCliente().getCodpais().toUpperCase().equals("ES")) {
			pantallaFirma();
		}
		
		if (firma != null) {
			getDatos().put(FirmaFidelizadoController.FIRMA, firma);
		}

		try {
			crearFidelizado();
			if (checkBox.isSelected()) {
				selfCheckoutFidelizadosService.accionImprimir(fidelizado);
			}
			getStage().close();
		}
		catch (Exception e) {
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Error durante el alta de un fidelizado : " + e.getMessage()), getStage());
		}

	}

	private void pantallaFirma() {
		getApplication().getMainView().showModalCentered(FirmaFidelizadoView.class, getDatos(), getStage());
		firma = (byte[]) getDatos().get(FirmaFidelizadoController.FIRMA);
		// getDatos().put(FirmaController.FIRMA, firma);
	}

	@FXML
	public void accionRechazar() {
		getDatos().put(RECHAZAR, true);
		getStage().close();
	}

	@FXML
	public void retrocederPag(ActionEvent event) throws FileNotFoundException, IOException {
		ObservableList<Entry> historial = webHistory.getEntries();
		if (!historial.isEmpty()) {
			if(webHistory.getCurrentIndex() == 0) {
				cargarTexto(false);
			}else {
				webHistory.go(-1);
			}
        }
		
	}
	
	private void cargarTexto(Boolean borrarHistorial) throws FileNotFoundException, IOException {
		log.debug("cargarTexto() - Cargando texto web que se muestra por pantalla");
		webEngine.loadContent("");
//		if(webHistory != null && borrarHistorial) {
//			webHistory.getEntries().clear();
//		}
		
		// URL url = Thread.currentThread().getContextClassLoader().getResource("textos_legales/Fidelizados" + AppConfig.pais.toUpperCase() + ".htm");
		URL url = Thread.currentThread().getContextClassLoader().getResource("textos_legales/TCG_BricoClub_CGCU_Privacidad_" + AppConfig.pais.toUpperCase() + ".htm");

		try (FileInputStream inputStream = new FileInputStream(url.getPath())) {
			log.debug("cargarTexto() - Url del fichero de proteccion de datos: " + url.getPath());

			String everything = IOUtils.toString(inputStream, "windows-1252");
			webEngine.loadContent(everything);
		}
//		btRetrocederPag.disableProperty().bind(Bindings.lessThanOrEqual(wvTexto.getEngine().getHistory().currentIndexProperty(), 0));
	}

	public void crearFidelizado() throws RestException, RestHttpException {
		log.debug("crearFidelizado() - Creando el fidelizado con documento " + fidelizado.getDocumento());
		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		FidelizadoRequestRest insertFidelizado = new FidelizadoRequestRest(apiKey, uidActividad, fidelizado, sesion.getAplicacion().getEmpresa().getCodEmpresa(),
		        sesion.getAplicacion().getCodAlmacen());
		insertFidelizado.setTipoNotificacion("NUEVO_USUARIO_FIDELIZADO");

		List<TiposContactoFidelizadoBean> contactos = insertFidelizado.getFidelizado().getContactos();
		for (TiposContactoFidelizadoBean tiposContactoFidelizadoBean : contactos) {
			if(tiposContactoFidelizadoBean.getCodTipoCon().equals("EMAIL")) {
				tiposContactoFidelizadoBean.setRecibeNotificacionesCom(true);
			}
		}

		fidelizado = FidelizadosRest.insertFidelizado(insertFidelizado);
		
		String numTarjeta = selfCheckoutFidelizadosService.getNumTarjetaFidelizado(fidelizado.getIdFidelizado());
		fidelizado.setNumeroTarjeta(numTarjeta);

		VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("Fidelizado creado correctamente"), getStage());

		log.debug("crearFidelizado() - Creando el documento de captación fidelizado para el fidelizado " + fidelizado.getIdFidelizado());
		TicketFidelizadoCaptacion ticketFidelizado = new TicketFidelizadoCaptacion();
		ticketFidelizado.setPdfFidelizado(firma);
		ticketFidelizado.setIdFidelizado(fidelizado.getIdFidelizado());
		selfCheckoutFidelizadosService.registrarTicketFidelizado(ticketFidelizado, fidelizado, firma);
		
		selfCheckoutFidelizadosService.registrarEnlacesColectivoFidelizado(fidelizado);
		asignarFidelizadoAVenta();
	}

	protected void asignarFidelizadoAVenta() {
		for (View view : getApplication().getMainView().getSubViews()) {
			if (view instanceof SelfCheckoutFacturacionArticulosView && view.getController() instanceof SelfCheckoutFacturacionArticulosController) {
				String numTarjeta = fidelizado.getNumeroTarjeta();

				if (StringUtils.isNotBlank(numTarjeta)) {
					SelfCheckoutFacturacionArticulosController facturacionArticulosController = (SelfCheckoutFacturacionArticulosController) view.getController();
					try {
						FidelizacionBean fidelizado = Dispositivos.getInstance().getFidelizacion().consultarTarjetaFidelizado(getStage(), numTarjeta, sesion.getAplicacion().getUidActividad());
						facturacionArticulosController.ticketManager.getTicket().getCabecera().setDatosFidelizado(fidelizado);
					}
					catch (Exception e) {
						log.error("asignarFidelizadoAVenta() - Ha habido un error al consultar el fidelizado recién creado: " + e.getMessage(), e);
						facturacionArticulosController.ticketManager.getTicket().getCabecera().setDatosFidelizado(numTarjeta);
					}
					facturacionArticulosController.refrescarDatosPantalla();
				}
			}
		}
	}
}
