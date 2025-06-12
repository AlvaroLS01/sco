package com.comerzzia.brico.pos.selfcheckout.gui.despedida;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@Component
public class SelfCheckoutDespedidaController extends WindowController {

	@FXML
	Label lbDespedida;
	
	@Autowired
	protected Sesion sesion;
	
	protected Boolean mensajeFactura, mensajeCorreo, mensajeAmbos,isFacturaA4;
	
	protected String emailEnvio; 
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		
	}

	@Override
	public void initializeFocus() {
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
        // Reiniciar los valores
        mensajeFactura = null;
        mensajeCorreo = null;
        mensajeAmbos = null;
        emailEnvio = null;
        isFacturaA4 = null;
		
		if(getDatos().containsKey("mensajeFactura")) {
			mensajeFactura = (Boolean) getDatos().get("mensajeFactura");			
		}
		if(getDatos().containsKey("mensajeCorreo")) {
			mensajeCorreo = (Boolean) getDatos().get("mensajeCorreo");			
		}
		if(getDatos().containsKey("mensajeAmbos")) {
			mensajeAmbos = (Boolean) getDatos().get("mensajeAmbos");			
		}
		if(getDatos().containsKey("emailEnvio")) {
			emailEnvio = (String) getDatos().get("emailEnvio");			
		}
		if(getDatos().containsKey("checkFT")) {
			isFacturaA4 =(Boolean) getDatos().get("checkFT");		
		}
		
		
		configurarIdioma();

		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				}
				catch (Exception e) {
				}
				AppConfig.idioma = ((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getStoreLanguageCode().toLowerCase();
				AppConfig.pais = ((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getTienda().getCliente().getCodpais();
				getStage().close();

			}
		});
	}

	protected void configurarIdioma() {
		lbDespedida.setText("");
		
		String gracias = I18N.getTexto("¡Gracias por su visita!");
		String infoCorreo = I18N.getTexto("Su factura ha sido enviada a")+ " " + emailEnvio;
		
		// por gramatica alemana se sustituye la estructura del mensaje por la siguiente
		if(infoCorreo.contains("Ihre Rechnung wird an")) {
			infoCorreo = infoCorreo.replaceAll(infoCorreo,"Ihre Rechnung wird an " + emailEnvio +" geschickt");
		}
		
		lbDespedida.setText(I18N.getTexto("\t\t\t"+gracias));
		
		if(mensajeAmbos != null && mensajeAmbos || mensajeCorreo!= null && mensajeCorreo) {
			lbDespedida.setText(lbDespedida.getText() + I18N.getTexto("\n"+ infoCorreo+"."));
		}
		

	}

}
