package com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.insertadatos;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.KeyboardDataDto;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

@Component
public class InsertarDatosFidelizadoController extends WindowController implements Initializable, IContenedorBotonera {
	
	private Logger log = Logger.getLogger(InsertarDatosFidelizadoController.class);
	public static final String DATO_INSERTADO = "DATO_INSERTADO";
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
	private Button btnGmailEs, btnGmailCom, btnOutlookEs, btnOutlookCom, btnAt, btnHotmail, btnHotmailEs, btnYahooEs, btnSapo, btAceptar, btCancelar;

	@Override
	public void realizarAccion(BotonBotoneraComponent botonAccionado) throws CajasServiceException {
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
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
		configurarIdioma();
		accionCorreoDireccion();
		AppConfig.mostrarTecladoAlfanumerico = true;
		KeyboardDataDto keyboardDataDto = new KeyboardDataDto();
		keyboardDataDto.setVisibleAlInicio(true);
		keyboardDataDto.setPintarPiePantalla(true);
		keyboardDataDto.setMostrar(true);
		tfInsertarEmail.setUserData(keyboardDataDto);
		
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
				btnGmailEs.setVisible(false);
				btnGmailEs.setManaged(false);
				btnOutlookEs.setVisible(false);
				btnOutlookEs.setManaged(false);
				btnOutlookCom.setVisible(false);
				btnOutlookCom.setManaged(false);
				btnGmailCom.setVisible(true);
				btnHotmail.setVisible(true);
				btnHotmail.setManaged(true);
				btnHotmailEs.setVisible(true);
				btnHotmailEs.setManaged(true);
				btnYahooEs.setVisible(true);
				btnYahooEs.setManaged(true);
				btnSapo.setVisible(false);
				btnSapo.setManaged(false);
				btnAt.setVisible(true);
				break;
			default:
				break;
		}
	}

        @FXML
        public void accionAceptar() {
                String input = tfInsertarEmail.getText().trim();

		if (StringUtils.isBlank(input)) {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No puede dejar el campo vacío."), getStage());
			return;
		}

                if (input.contains("@")) {
                        String errorKey = BricoEmailValidator.getValidationErrorKey(input);
                        if (errorKey != null) {
                                VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto(errorKey), getStage());
                                return;
                        }
                } else {
                        if (!(esNIF(input) || esNIFPortugues(input))) {
                                VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("El documento seleccionado no es válido."), getStage());
                                return;
                        }
                }

		getDatos().put("valor", valor);
		getDatos().put(DATO_INSERTADO, input);
		getStage().close();
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

	    btnHotmailEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	            resetearCorreo();
	            tfInsertarEmail.setText(tfInsertarEmail.getText() + "@hotmail.es");
	            tfInsertarEmail.requestFocus();
	            tfInsertarEmail.selectEnd();
	        }
	    });

	    btnYahooEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	            resetearCorreo();
	            tfInsertarEmail.setText(tfInsertarEmail.getText() + "@yahoo.es");
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

       private boolean esNIF(String nif) {
               String patronNIF = "^[0-9]{8}[A-HJ-NP-TV-Z]$|^[ABCDEFGHJKLMNPQRSUVW][0-9]{7}[0-9A-J]$";
               Pattern pattern = Pattern.compile(patronNIF, Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(nif);
               return matcher.matches();
       }

       private boolean esNIFPortugues(String nif) {
               String patronNIFPortugues = "^[0-9]{9}$";
               Pattern pattern = Pattern.compile(patronNIFPortugues, Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(nif);
               return matcher.matches();
       }

	@FXML
	public void accionCancelar() {
		log.debug("accionCancelar() - Comprobando si continuamos");
		getDatos().put(DATO_INSERTADO, "cancelar");
//		AppConfig.mostrarTecladoAlfanumerico = false;
		getStage().close();
	}
	
	protected void configurarIdioma() {

		if (sesion.getAplicacion().getTienda().getCliente().getCodpais().toUpperCase().equals("PT")) {
			lbTitulo.setText(I18N.getTexto("Introduce tu DNI/NIF o email"));
		}
		else {
			lbTitulo.setText(I18N.getTexto("Introduce tu DNI/NIF o email"));
			btAceptar.setText(I18N.getTexto("Aceptar"));
			btCancelar.setText(I18N.getTexto("Cancelar"));
		}

	}
}
