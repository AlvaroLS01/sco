package com.comerzzia.brico.pos.selfcheckout.gui.pagos.tipoobtencionticket.insertarcorreo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.KeyboardDataDto;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.brico.pos.util.format.BricoEmailValidator;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.BotonBotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.IContenedorBotonera;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumerico;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.cajas.CajasServiceException;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

@Component
public class InsertarCorreoController extends WindowController implements Initializable, IContenedorBotonera {
	
	private Logger log = Logger.getLogger(InsertarCorreoController.class);
	public static final String EMAIL_INSERTADO = "EMAIL_INSERTADO";
	@Autowired
	protected Sesion sesion;
	
	@FXML
	protected TextField tfInsertarEmail;
	@FXML
	protected Label lbTitulo;
	
	protected String valor;
	
	protected static final Boolean muestraAbajo = false;
	
	@FXML
	private TecladoNumerico tecladoNumerico;
	
	@FXML 
	private SelfCheckoutKeyboard keyboard;
	
	@FXML
	private Button btnGmailEs, btnGmailCom, btnOutlookEs, btnOutlookCom, btnAt, btnHotmail, btnSapo, btAceptar, btCancelar;

	@Override
	public void realizarAccion(BotonBotoneraComponent botonAccionado) throws CajasServiceException {
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		lbTitulo.setText(I18N.getTexto("Inserte su correo electrónico"));
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		try {
			keyboard =  new SelfCheckoutKeyboard();
		}
		catch (IOException | URISyntaxException e) {
			log.error("Error cargando teclado");
		}
		keyboard.onController(this);
		keyboard.setPopupVisible(Boolean.TRUE, tfInsertarEmail, this.getStage(), Boolean.FALSE);

//		tecladoNumerico.setVisible(false);
	}
	@Override
    public void initializeFocus() {
		tfInsertarEmail.requestFocus();
    }

	@Override
	public void initializeForm() throws InitializeGuiException {
		log.debug("initializeForm() - recogiendo los datos de la pantalla de seleccion de tipo de ticket");
		
		AppConfig.mostrarTecladoAlfanumerico = true;
		KeyboardDataDto keyboardDataDto = new KeyboardDataDto();
		keyboardDataDto.setVisibleAlInicio(true);
		keyboardDataDto.setPintarPiePantalla(true);
		keyboardDataDto.setMostrar(true);
		accionCorreoDireccion();
		tfInsertarEmail.setUserData(keyboardDataDto);
		configurarIdioma();
		valor = (String) getDatos().get("valor");
		String emailRecogido = (String) getDatos().get(SelfCheckoutFacturacionArticulosController.EMAIL);
		tfInsertarEmail.setText(StringUtils.isNotBlank(emailRecogido) ? emailRecogido.trim() : "".trim());
		switch (sesion.getAplicacion().getTienda().getCliente().getCodpais().toLowerCase()) {
			case "pt":
				btnGmailEs.setVisible(false);
				btnGmailEs.setManaged(false);
				btnGmailCom.setVisible(true);
				btnHotmail.setVisible(true);
				btnOutlookCom.setVisible(true);
				btnOutlookEs.setVisible(false);
				btnOutlookEs.setManaged(false);
				btnSapo.setVisible(true);
				btnAt.setVisible(true);
				break;
			case "es":
				btnGmailEs.setVisible(true);
				btnGmailCom.setVisible(true);
				btnHotmail.setVisible(false);
				btnHotmail.setManaged(false);
				btnOutlookCom.setVisible(true);
				btnOutlookEs.setVisible(true);
				btnSapo.setVisible(false);
				btnSapo.setManaged(false);
				btnAt.setVisible(true);
				break;
			default:
				break;
		}
	}
	
	public void accionCorreoDireccion() {
		btnGmailCom.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@gmail.com");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });

	    btnGmailEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@gmail.es");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });

	    btnOutlookEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@outlook.es");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });

	    btnOutlookCom.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@outlook.com");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });
	    
	    btnHotmail.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@hotmail.com");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });
	    
	    btnSapo.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@sapo.pt");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });
	    
	    btnAt.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfInsertarEmail.setText(tfInsertarEmail.getText() + "@");
	        	tfInsertarEmail.requestFocus();
	        	tfInsertarEmail.selectEnd();
	        }
	    });

	}
	
	public void resetearCorreo() {
		if(tfInsertarEmail.getText().contains("@")) {
			String[] mailDividido = tfInsertarEmail.getText().split("@");
			if (mailDividido.length > 1) {
				tfInsertarEmail.setText(mailDividido[0]);
			}
		}
	}

	@FXML
	public void accionAceptar() {
		log.debug("accionAceptar() - validando campo");
		String email = tfInsertarEmail.getText().trim();

		if (StringUtils.isBlank(email)) {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No puede dejar el campo vacío."), getStage());
			return;
		}

		// Usamos BricoEmailValidator:
		String errorKey = BricoEmailValidator.getValidationErrorKey(email);
		if (errorKey != null) {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto(errorKey), getStage());
			tfInsertarEmail.requestFocus();
			return;
		}

		getDatos().put("valor", valor);
		getDatos().put(EMAIL_INSERTADO, email);
		getStage().close();
	}

	@FXML
	public void accionCancelar() {
		log.debug("accionCancelar() - Comprobando si continuamos");
		boolean confirmacion = VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("Si cancela, su ticket será entregado solo en papel."), getStage());
		
		if(!confirmacion) {
			return;
		} 
		getDatos().put(EMAIL_INSERTADO, "");
//		AppConfig.mostrarTecladoAlfanumerico = false;
		getStage().close();
	}
	
	 private boolean validarEmail(String email) {
        String patron = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";
        Pattern pattern = Pattern.compile(patron);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
	 }
	 
	protected void configurarIdioma() {
		if (sesion.getAplicacion().getTienda().getCliente().getCodpais().toUpperCase().equals("PT")) {
			lbTitulo.setText(I18N.getTexto("Inserte su correo electrónico"));
		}
		else {
			lbTitulo.setText(I18N.getTexto("Inserte su correo electrónico"));
			btAceptar.setText(I18N.getTexto("Aceptar"));
			btCancelar.setText(I18N.getTexto("Cancelar"));
		}
	}
}
