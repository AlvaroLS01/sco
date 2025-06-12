package com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket.insertarcorreo.InsertarCorreoController;
import com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket.insertarcorreo.InsertarCorreoView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.BotonBotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.IContenedorBotonera;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.cajas.CajasServiceException;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

@Component
public class SelfCheckoutTipoObtencionTicketController extends WindowController implements Initializable, IContenedorBotonera {

	private static final Logger log = Logger.getLogger(SelfCheckoutTipoObtencionTicketController.class.getName());

	public static final String PAPEL = "Papel";
	public static final String AMBOS = "Ambos";
	public static final String CORREO = "Correo";

	@FXML
	protected Button btnPapel, btnCorreo, btnAmbos;
	
	@FXML
	protected Label lbPregunta, lbPapel, lbEmail, lbAmbos;
	protected String valor, email;
	
	@FXML
	public void seleccionarTipoFacturaPapel() {
	    seleccionarTipoFactura(PAPEL);
	}

	@FXML
	public void seleccionarTipoFacturaCorreo() {
	    seleccionarTipoFactura(CORREO);
	}

	@FXML
	public void seleccionarTipoFacturaAmbos() {
	    seleccionarTipoFactura(AMBOS);
	}

	@Override
	public void realizarAccion(BotonBotoneraComponent botonAccionado) throws CajasServiceException {	
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		log.debug("initialize() - Inicializando ventana...");
	}

	private void seleccionarTipoFactura(String tipoFactura) {
		valor = tipoFactura;
		log.debug("seleccionarTipoFactura() - valor seleccionado: " + valor);
		accionAceptar(); // Llama al método de aceptación
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		log.debug("inicializarComponentes()");
		registrarAccionCerrarVentanaEscape();
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		log.debug("initializeForm() -  seteando datos recogidos en variables");
		configurarIdioma();
		email = (String) getDatos().get(SelfCheckoutFacturacionArticulosController.EMAIL);
	}

	@Override
	public void initializeFocus() {
		btnPapel.requestFocus();
	}

	@FXML
	public void accionAceptar() {
		// Lógica del botón aceptar según el valor seleccionado
		if (StringUtils.isNotBlank(valor) && valor.equals(PAPEL)) {
			log.debug("accionAceptar() - valor seleccionado: " + PAPEL);
			getDatos().put("valor", valor);
			getStage().close();
			return;
		}

		String correoInsertado = "";
		log.debug("accionAceptar() - comprobando email seleccionado: " + email);

		if (StringUtils.isNotBlank(email)) {
			log.debug("accionAceptar() - comprobando valor seleccionado: " + valor);
			if (StringUtils.isNotBlank(valor)) {
				if (valor.equals(PAPEL)) {
					log.debug("accionAceptar() - valor seleccionado: " + valor);
					getDatos().put("valor", valor);
				} else {
					getDatos().put(SelfCheckoutFacturacionArticulosController.EMAIL, email);
					getDatos().put("valor", valor);
					log.debug("accionAceptar() - abriendo ventana de insercción de correo");
					abrirVentanaInsertarCorreo();
					correoInsertado = (String) getDatos().get(InsertarCorreoController.EMAIL_INSERTADO);
					email = email.equals(correoInsertado) ? email : correoInsertado;
					getDatos().put(SelfCheckoutFacturacionArticulosController.EMAIL, email);
				}
				getStage().close();
			} else {
				VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Debe elegir una opción o pulsar cancelar"), getStage());
			}
		} else {
			abrirVentanaInsertarCorreo();
			getDatos().put("valor", valor);
			correoInsertado = (String) getDatos().get(InsertarCorreoController.EMAIL_INSERTADO);
			getDatos().put(SelfCheckoutFacturacionArticulosController.EMAIL, correoInsertado);
		}

		getStage().close();
	}

	private void abrirVentanaInsertarCorreo() {
		log.debug("abrirVentanaInsertarCorreo() - abriendo ventana de inserción de correo");
		getApplication().getMainView().showModalCentered(InsertarCorreoView.class, getDatos(), getStage());
	}

	@FXML
	public void accionCancelar() {
		log.debug("accionCancelar() - comprobando si cancela la acción de selección de tipo de envío de ticket");
		getDatos().put("valor", "papel");
		getStage().close();
	}

	protected void configurarIdioma() {
		lbPregunta.setText(I18N.getTexto("¿Cómo quiere su factura?"));
		lbPapel.setText(I18N.getTexto("Papel"));
		lbEmail.setText(I18N.getTexto("Correo"));
		lbAmbos.setText(I18N.getTexto("Ambos"));
	}
}
