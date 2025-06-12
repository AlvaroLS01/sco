package com.comerzzia.brico.pos.selfcheckout.gui.ventas.introduccionmanual;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumerico;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

@Component
public class IntroduccionManualController extends WindowController {
	
	private Logger log = Logger.getLogger(IntroduccionManualController.class);

	public static final String PARAM_CODIGO = "IntroduccionManual_Codigo";

	protected static final Boolean muestraAbajo = Boolean.FALSE;

	@FXML
	private TextField tfCodigo;
	
	@FXML
	private Button btAceptar, btCancelar, btnGmailEs, btnGmailCom, btnOutlookEs, btnOutlookCom, btnAt, btnHotmail, btnSapo;
	
	@FXML
	private Label lbIntroCodigo;

	@FXML
	private TecladoNumerico tecladoNumerico;
	
	@FXML 
	private SelfCheckoutKeyboard keyboard; 
	
	@Autowired 
	protected Sesion sesion;
	
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
		keyboard.setPopupVisible(true, tfCodigo, this.getStage(), muestraAbajo);

//		tecladoNumerico.setVisible(false);
	}

	@Override
	public void initializeFocus() {
		tfCodigo.requestFocus();
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		tfCodigo.clear();
		configurarIdioma();
		keyboard.setPopupVisible(true, tfCodigo, this.getStage(), muestraAbajo);
		accionCorreoDireccion();
		
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
	            tfCodigo.setText(tfCodigo.getText() + "@gmail.com");
	            tfCodigo.requestFocus();
	            tfCodigo.selectEnd();
	            
	        }
	    });

	    btnGmailEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	            tfCodigo.setText(tfCodigo.getText() + "@gmail.es");
	            tfCodigo.requestFocus();
	            tfCodigo.selectEnd();
	        }
	    });

	    btnOutlookEs.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	            tfCodigo.setText(tfCodigo.getText() + "@outlook.es");
	            tfCodigo.requestFocus();
	            tfCodigo.selectEnd();
	        }
	    });

	    btnOutlookCom.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	            tfCodigo.setText(tfCodigo.getText() + "@outlook.com");
	            tfCodigo.requestFocus();
	            tfCodigo.selectEnd();
	        }
	    });

	    btnHotmail.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfCodigo.setText(tfCodigo.getText() + "@hotmail.com");
	        	tfCodigo.requestFocus();
	        	tfCodigo.selectEnd();
	        }
	    });
	    
	    btnSapo.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	        	tfCodigo.setText(tfCodigo.getText() + "@sapo.pt");
	        	tfCodigo.requestFocus();
	        }
	    });
	    
	    btnAt.setOnAction(new EventHandler<ActionEvent>() {
	        @Override
	        public void handle(ActionEvent event) {
	        	resetearCorreo();
	            tfCodigo.setText(tfCodigo.getText() + "@");
	            tfCodigo.requestFocus();
	            tfCodigo.selectEnd();
	        }
	    });

	}
	public void resetearCorreo() {
		if(tfCodigo.getText().contains("@")) {
			String[] mailDividido = tfCodigo.getText().split("@");
			if (mailDividido.length > 1) {
				tfCodigo.setText(mailDividido[0]);
			}
		}
	}
	public void accionAceptar() {
		String codigo = tfCodigo.getText();
		log.debug("accionAceptar() - Introduciendo código: " + codigo);
		
		if(StringUtils.isNotBlank(codigo)) {
			getDatos().put(PARAM_CODIGO, codigo);
			getStage().close();
		}
		else {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No se ha introducido ningún código"), getStage());
		}
	}
	
	protected void configurarIdioma() {

		if(sesion.getAplicacion().getTienda().getCliente().getCodpais().toUpperCase().equals("PT")) {
			lbIntroCodigo.setText(I18N.getTexto("Por favor, introduce tu NIF o email"));
		}else {
			lbIntroCodigo.setText(I18N.getTexto("Por favor, introduce tu DNI/CIF o email"));
			btAceptar.setText(I18N.getTexto("Aceptar"));
			btCancelar.setText(I18N.getTexto("Cancelar"));		
		}

	}

}
