package com.comerzzia.brico.pos.selfcheckout.core.gui.login;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;

import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionUsuario;
import com.comerzzia.pos.core.gui.login.LoginController;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.util.config.AppConfig;

import javafx.event.ActionEvent;

@SuppressWarnings("rawtypes")
@Primary
@Controller
public class SelfCheckoutLoginController extends LoginController {
	
	private static final Logger log = Logger.getLogger(SelfCheckoutLoginController.class);
	
	private int loginAutomatico;
	
	public static final String USUARIO_SCO = "SELF_CHECKOUT";
	
	protected TicketVenta ticketVenta;

	@Override
	public void initializeForm() {
		super.initializeForm();
		if(loginAutomatico == 0) {
			tfUsuario.setText(SelfCheckOutSesionUsuario.USERNAME_SC);
			tfPassword.setText(SelfCheckOutSesionUsuario.PASSWORD_SC);
			actionBtAceptar(null);
			accionLimpiarFormulario();
			loginAutomatico++;
		}
	}
	
	@Override
	public void actionBtAceptar(ActionEvent event) {
		super.actionBtAceptar(event);
		log.debug("actionBtAceptar() - Cambiando el idioma a " + AppConfig.idioma + " para el usuario ADMINISTRADOR");
		Locale locale = new Locale(AppConfig.idioma, AppConfig.pais);
		Locale.setDefault(locale);
	}

}
