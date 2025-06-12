package com.comerzzia.brico.pos.selfcheckout.services.core.sesion;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.services.core.sesion.SesionInitException;
import com.comerzzia.pos.services.core.sesion.SesionUsuario;
import com.comerzzia.pos.services.core.usuarios.UsuarioInvalidLoginException;

@Primary
@Component
public class SelfCheckOutSesionUsuario extends SesionUsuario {

	public static String USERNAME_SC = "SELF_CHECKOUT";
	public static String PASSWORD_SC = "selfcheckout";

	public boolean isUsuarioSelfCheckout() {
		return usuario.getUsuario().equals(USERNAME_SC);
	}
	
	public void initSelfCheckout() throws SesionInitException, UsuarioInvalidLoginException {
	    super.init(USERNAME_SC, PASSWORD_SC);
	}
	
}
