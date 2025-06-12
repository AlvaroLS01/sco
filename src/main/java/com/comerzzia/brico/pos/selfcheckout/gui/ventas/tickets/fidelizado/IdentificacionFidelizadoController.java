package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.api.model.loyalty.ColectivosFidelizadoBean;
import com.comerzzia.api.model.loyalty.EnlaceFidelizadoBean;
import com.comerzzia.api.model.loyalty.FidelizadoBean;
import com.comerzzia.api.model.loyalty.TiposContactoFidelizadoBean;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos.VentajasExclusivasController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos.VentajasExclusivasView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.lpd.LeyProteccionDatosFidelizadoController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.lpd.LeyProteccionDatosFidelizadoView;
import com.comerzzia.brico.pos.selfcheckout.services.fidelizado.SelfCheckoutFidelizadosService;
import com.comerzzia.brico.pos.util.format.BricoEmailValidator;
import com.comerzzia.core.util.tipoidentificacion.IValidadorDocumentoIdentificacion;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.core.gui.validation.ValidationUI;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.factura.paises.PaisesController;
import com.comerzzia.pos.gui.ventas.tickets.factura.paises.PaisesView;
import com.comerzzia.pos.persistence.paises.PaisBean;
import com.comerzzia.pos.persistence.tiposIdent.TiposIdentBean;
import com.comerzzia.pos.services.core.paises.PaisNotFoundException;
import com.comerzzia.pos.services.core.paises.PaisService;
import com.comerzzia.pos.services.core.paises.PaisServiceException;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.tiposIdent.TiposIdentNotFoundException;
import com.comerzzia.pos.services.core.tiposIdent.TiposIdentService;
import com.comerzzia.pos.services.core.tiposIdent.TiposIdentServiceException;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@Component
public class IdentificacionFidelizadoController extends WindowController {

	protected static final Logger log = Logger.getLogger(IdentificacionFidelizadoController.class);

	public static final String SCO_ALTA_FIDELIZADO_CODCOLECTIVO = "SCO.ALTA_FIDELIZADO_CODCOLECTIVO";

	public static final String CANCELAR = "CANCELAR";

	public static final String FIDELIZADO = "FIDELIZADO";

	public static final String DOCUMENTO_NO_REPETIDO = "DOCUMENTO_NO_REPETIDO";

	public static final String EMAIL_NO_REPETIDO = "EMAIL_NO_REPETIDO";

	@FXML
	private ComboBox<TiposIdentBean> cbTipoDocIdent;

	@FXML
	private Label lbTitulo, lbError;

	@FXML
	private TextField tfNombre, tfApellidos, tfNumDocIdent, tfCorreo, tfCP, tfCodPais, tfDesPais;

	@FXML
	private Button btBuscarPais;

	@FXML
	private Label lbNombre, lbApellidos, lbTipoDocumento, lbDocumento, lbCorreo, lbCp, lbPais;
	
	@FXML
	private Button btAceptar, btCancelar;
	
	@FXML
	private SelfCheckoutKeyboard keyboard;

	@Autowired
	private Sesion sesion;

	@Autowired
	private TiposIdentService tiposIdentService;

	@Autowired
	private PaisService paisService;

	@Autowired
	protected VariablesServices variablesServices;

	@Autowired
	protected SelfCheckoutFidelizadosService selfCheckoutFidelizadosService;

	protected FormularioDatosIdentificacionFidelizadoBean frDatosIdentificacionFidelizado;

	protected ObservableList<TiposIdentBean> tiposIdent;

	protected FidelizadoBean fidelizado;

	protected TicketManager ticketManager;

	protected String codPais;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		log.debug("initialize()");

		frDatosIdentificacionFidelizado = SpringContext.getBean(FormularioDatosIdentificacionFidelizadoBean.class);
		frDatosIdentificacionFidelizado.setFormField("nombre", tfNombre);
		frDatosIdentificacionFidelizado.setFormField("apellidos", tfApellidos);
		frDatosIdentificacionFidelizado.setFormField("correo", tfCorreo);
		frDatosIdentificacionFidelizado.setFormField("cPostal", tfCP);
		frDatosIdentificacionFidelizado.setFormField("numDocIdent", tfNumDocIdent);
		frDatosIdentificacionFidelizado.setFormField("pais", tfCodPais);

		tfCodPais.setDisable(true);
		tfDesPais.setDisable(true);

	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		try {
			keyboard = new SelfCheckoutKeyboard();
		}
		catch (IOException | URISyntaxException e) {
			log.error("Error cargando teclado");
		}
		keyboard.onController(this);
		keyboard.setPopupVisible(true, tfNombre, this.getStage(), Boolean.TRUE);

		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		refrescarDatosPantalla();
		configurarIdioma();
		tiposIdent = FXCollections.observableArrayList();
		loadTiposIdentificacion();
		cbTipoDocIdent.setItems(tiposIdent);
		definirPaisPorDefecto();

		String identificacion = (String) getDatos().get(SelfCheckoutFacturacionArticulosController.TEXTO_IDENTIFICACION);
		if (StringUtils.isNotBlank(identificacion)) {
			if (identificacion.contains("@")) {
				tfCorreo.setText(identificacion);
				tfCorreo.setDisable(true);
			}
			else {
				if (!cbTipoDocIdent.getItems().isEmpty()) {
					cbTipoDocIdent.setValue(cbTipoDocIdent.getItems().get(0));
				}
				
				boolean validacion = getDatos().containsKey(SelfCheckoutFacturacionArticulosController.TARJETA_CLIENTE_BRICOCLUB);
				if (validacion) {
					tfNumDocIdent.setText("");
					tfNumDocIdent.setDisable(false);
				}
				else {
					tfNumDocIdent.setText(identificacion);
					tfNumDocIdent.setDisable(true);
				}
			}
		}
	}

	private void definirPaisPorDefecto() {
		PaisBean pais = obtenerPais(AppConfig.pais);
		tfCodPais.setText(pais.getCodPais().toUpperCase());
		tfDesPais.setText(pais.getDesPais().toUpperCase());
	}

	protected PaisBean obtenerPais(String codPais) {
		PaisBean pais = null;

		try {
			pais = paisService.consultarCodPais(codPais.toUpperCase());
		}
		catch (PaisNotFoundException ex) {
			log.error("No se encontró el código del fidelizado.");
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Error en la búsqueda, país no encontrado"), getStage());
			tfCodPais.setText("");
			tfDesPais.setText("");
		}
		catch (PaisServiceException ex) {
			log.error("Error buscando el código del fidelizado.", ex);
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Error en la búsqueda del país del fidelizado."), getStage());
			tfCodPais.setText("");
			tfDesPais.setText("");
		}

		return pais;
	}

	@Override
	public void initializeFocus() {
		tfNombre.requestFocus();
	}

	@FXML
	public void accionAceptarIntro(KeyEvent event) {
		log.debug("accionAceptarIntro()");
		if (event.getCode() == KeyCode.ENTER) {
			accionAceptar(null);
		}
	}

	@FXML
	public void accionBuscarPais(ActionEvent event) {
		getApplication().getMainView().showModalCentered(PaisesView.class, getDatos(), this.getStage());

		if (getDatos() != null && getDatos().containsKey(PaisesController.PARAMETRO_SALIDA_PAIS)) {
			PaisBean pais = (PaisBean) getDatos().get(PaisesController.PARAMETRO_SALIDA_PAIS);
			tfDesPais.setText(pais.getDesPais().toUpperCase());
			tfCodPais.setText(pais.getCodPais().toUpperCase());
			codPais = pais.getCodPais().toUpperCase();
			loadTiposIdentificacion();
		}
	}

	@FXML
	public void accionBuscar(ActionEvent event) {
	}

	@FXML
	public void accionAceptar(ActionEvent event) {
		if (validarFormulario()) {
			ventanaColectivo();
			if (getDatos().get(VentajasExclusivasController.CANCELAR) == null) {
				ventanaLPD();
				if (getDatos().get(LeyProteccionDatosFidelizadoController.RECHAZAR) == null) {
					getStage().close();
				}
			}
			
		}
	}

	protected FidelizadoBean getDatosFidelizado() {
		FidelizadoBean fidelizado = new FidelizadoBean();

		String nombre = tfNombre.getText();
		String apellidos = tfApellidos.getText();
		String codTipoIden = "";
		if (cbTipoDocIdent.getValue() != null) {
			codTipoIden = cbTipoDocIdent.getValue().getCodTipoIden();
		}
		String numDocIdent = tfNumDocIdent.getText();
		String correo = tfCorreo.getText();
		String codPostal = tfCP.getText();
		String codPais = tfCodPais.getText();
		String desPais = tfDesPais.getText();

		fidelizado.setNombre(nombre);
		fidelizado.setApellidos(apellidos);
		fidelizado.setDocumento(numDocIdent);
		fidelizado.setCodTipoIden(codTipoIden);
		fidelizado.setCp(codPostal);
		fidelizado.setCodPais(codPais);
		fidelizado.setDesPais(desPais);

		String codAlmFav = sesion.getAplicacion().getCodAlmacen();
		if (codAlmFav != null) {
			EnlaceFidelizadoBean enlace = new EnlaceFidelizadoBean();
			enlace.setIdClase("D_TIENDAS_TBL.CODALM");
			enlace.setIdObjeto(codAlmFav);

			fidelizado.setEnlace(enlace);
		}
		ColectivosFidelizadoBean colectivo = new ColectivosFidelizadoBean();
		ColectivosFidelizadoBean sector = new ColectivosFidelizadoBean();
		List<ColectivosFidelizadoBean> colectivos = new ArrayList<>();
		if (getDatos().containsKey(VentajasExclusivasController.CODCOLECTIVO)) {
			colectivo.setCodColectivo(getDatos().get(VentajasExclusivasController.CODCOLECTIVO).toString());
			colectivos.add(colectivo);
		}
		if (getDatos().containsKey(VentajasExclusivasController.CODSECTOR)) {
			sector.setCodColectivo(getDatos().get(VentajasExclusivasController.CODSECTOR).toString());
			colectivos.add(sector);
		}
		if (!colectivos.isEmpty()) {
			fidelizado.setColectivos(colectivos);
		}

		List<TiposContactoFidelizadoBean> tiposContacto = new ArrayList<>();
		if (correo != null) {
			TiposContactoFidelizadoBean contactoEmail = new TiposContactoFidelizadoBean();
			contactoEmail.setCodTipoCon("EMAIL");
			contactoEmail.setRecibeNotificaciones(true);
			contactoEmail.setValor(correo);
			tiposContacto.add(contactoEmail);
		}
		fidelizado.setContactos(tiposContacto);

		return fidelizado;
	}

	protected boolean validarFormulario() {
		log.debug("validarFormulario()");

		String sResultado = validarDocumento();
		if (sResultado != null) {
			boolean guardar = VentanaDialogoComponent.crearVentanaConfirmacion(sResultado, getStage());
			if (!guardar) {
				tfNumDocIdent.requestFocus();
				return false;
			}
		}

		String nombreFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfNombre);
		String apellidoFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfApellidos);
		String numDocIdentFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfNumDocIdent).replaceAll("\\s", "");
		String correoFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfCorreo).replaceAll("\\s", "");
		String cpFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfCP);
		String paisFidelizado = frDatosIdentificacionFidelizado.trimTextField(tfCodPais);

		if (selfCheckoutFidelizadosService.compruebaDocumentoNoRepetido(numDocIdentFidelizado)) {
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Este documento ya está asignado a otro fidelizado"), getStage());
			return false;
		}

		String errorKey = BricoEmailValidator.getValidationErrorKey(correoFidelizado);
		if (errorKey != null) {
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto(errorKey), getStage());
			tfCorreo.requestFocus();
			return false;
		}

		if (selfCheckoutFidelizadosService.compruebaEmailNoRepetido(correoFidelizado)) {
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Este Email ya está asignado a otro fidelizado"), getStage());
			return false;
		}

		frDatosIdentificacionFidelizado.setNombre(nombreFidelizado);
		frDatosIdentificacionFidelizado.setApellidos(apellidoFidelizado);
		frDatosIdentificacionFidelizado.setNumDocIdent(numDocIdentFidelizado);
		frDatosIdentificacionFidelizado.setCorreo(correoFidelizado);
		frDatosIdentificacionFidelizado.setcPostal(cpFidelizado);
		frDatosIdentificacionFidelizado.setPais(paisFidelizado);

		frDatosIdentificacionFidelizado.clearErrorStyle();
		lbError.setText("");

		boolean valido = true;
		Set<ConstraintViolation<FormularioDatosIdentificacionFidelizadoBean>> violations = ValidationUI.getInstance().getValidator().validate(frDatosIdentificacionFidelizado);
		for (ConstraintViolation<FormularioDatosIdentificacionFidelizadoBean> cv : violations) {
			Path path = cv.getPropertyPath();
			frDatosIdentificacionFidelizado.setErrorStyle(path, true);
			if (valido) {
				lbError.setText(cv.getMessage());
				Platform.runLater(() -> frDatosIdentificacionFidelizado.setFocus(path));
			}
			valido = false;
		}

		return valido;
	}

	public String validarDocumento() {
		TiposIdentBean tipoIden = cbTipoDocIdent.getSelectionModel().getSelectedItem();
		String sResultado = null;
		if (tipoIden != null && StringUtils.isNotBlank(tipoIden.getCodTipoIden()) && StringUtils.isNotBlank(tipoIden.getClaseValidacion()) && StringUtils.isNotBlank(tfNumDocIdent.getText())) {
			String claseValidacion = tipoIden.getClaseValidacion();
			String cif = tfNumDocIdent.getText();

			try {
				IValidadorDocumentoIdentificacion validadorDocumentoIdentificacion = (IValidadorDocumentoIdentificacion) Class.forName(claseValidacion).newInstance();
				if (!validadorDocumentoIdentificacion.validarDocumentoIdentificacion(cif)) {
					sResultado = I18N.getTexto("El documento indicado no es válido, ¿desea guardar el fidelizado de todas formas?");
				}
			}
			catch (Exception e) {
				sResultado = I18N.getTexto("Ha habido un error al intentar validar el documento, ¿desea guardar el fidelizado de todas formas?");
				log.error("validarDocumento() - Ha habido un error al intentar validar el documento: " + e.getMessage());
			}
		}
		return sResultado;
	}

	@FXML
	void accionCancelar(ActionEvent event) {
		getDatos().put(CANCELAR, true);
		super.accionCancelar();
	}

	protected void refrescarDatosPantalla() {
		limpiarCampos();
	}

	protected void limpiarCampos() {
		tfNombre.setText("");
		tfApellidos.setText("");
		tfNumDocIdent.setText("");
		tfNumDocIdent.setDisable(false);
		cbTipoDocIdent.getSelectionModel().clearSelection();
		tfCorreo.setText("");
		tfCorreo.setDisable(false);
		tfCP.setText("");
		tfCodPais.setText("");
		tfDesPais.setText("");

		lbError.setText("");
	}

	protected void loadTiposIdentificacion() {
		try {
			tiposIdent.clear();

			String codPais = StringUtils.isNotEmpty(tfCodPais.getText()) ? tfCodPais.getText() : sesion.getAplicacion().getTienda().getCliente().getCodpais();
			List<TiposIdentBean> tiposIdentificacion = tiposIdentService.consultarTiposIdent(null, true, codPais);
			if (tiposIdentificacion.isEmpty()) {
				// Añadimos elemento vacío
				tiposIdent.add(new TiposIdentBean());
			}
			else {
				tiposIdent.addAll(tiposIdentificacion);
			}
		}
		catch (TiposIdentNotFoundException ex) {
		}
		catch (TiposIdentServiceException ex) {
			log.error("Error consultando los tipos de identificación.", ex);
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Error consultando los documentos de identificación de la tienda."), this.getStage());
		}
		catch (Exception ex) {
			log.error("Se produjo un error en el tratamiento de los tipos de identificacion", ex);
		}
		cbTipoDocIdent.getSelectionModel().clearSelection();
	}

	protected void ventanaLPD() {
		getDatos().put(FIDELIZADO, getDatosFidelizado());
		getApplication().getMainView().showModalCentered(LeyProteccionDatosFidelizadoView.class, getDatos(), getStage());
	}

	protected void configurarIdioma() {
		lbTitulo.setText(I18N.getTexto("Datos del cliente para alta de fidelizado"));
		lbNombre.setText(I18N.getTexto("Nombre"));
		lbApellidos.setText(I18N.getTexto("Apellidos"));
		lbTipoDocumento.setText(I18N.getTexto("Tipo Documento"));
		lbDocumento.setText(I18N.getTexto("Documento"));
		lbCorreo.setText(I18N.getTexto("Correo"));
		lbCp.setText(I18N.getTexto("C.P."));
		lbPais.setText(I18N.getTexto("Pais"));
		btAceptar.setText(I18N.getTexto("Aceptar"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
	}
	
	private void ventanaColectivo() {
		getApplication().getMainView().showModalCentered(VentajasExclusivasView.class, getDatos(), getStage());
	}
}
