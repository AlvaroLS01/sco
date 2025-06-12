package com.comerzzia.brico.pos.selfcheckout.gui.ventas;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.permisos.exception.SinPermisosException;
import com.comerzzia.pos.core.gui.view.ActionView;
import com.comerzzia.pos.persistence.core.acciones.AccionBean;
import com.comerzzia.pos.services.core.permisos.PermisoException;

@Component
public class SelfCheckoutFacturacionArticulosView extends ActionView {
	
	@Override
	public void initialize(AccionBean accion, HashMap<String, Object> datosVentana) throws InitializeGuiException, PermisoException, SinPermisosException {
		fxmlLoader = null;
	    super.initialize(accion, datosVentana);
	}

}
