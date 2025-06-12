package com.comerzzia.brico.pos.selfcheckout.services.core.sesion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.bricodepot.posservices.client.TarjetasApi;
import com.comerzzia.core.servicios.api.ComerzziaApiManager;
import com.comerzzia.pos.services.core.sesion.SesionAplicacion;
import com.comerzzia.pos.services.core.sesion.SesionInitException;

@Component
@Primary
public class SelfCheckOutSesionAplicacion extends SesionAplicacion {

	protected String codIdiomaCliente;

	@Autowired
	private ComerzziaApiManager comerzziaApiManager;

	public String getCodIdiomaCliente() {
		return codIdiomaCliente;
	}

	public void setCodIdiomaCliente(String codIdiomaCliente) {
		this.codIdiomaCliente = codIdiomaCliente;
	}

	@Override
	public void init() throws SesionInitException {
		// TODO Auto-generated method stub
		super.init();
		comerzziaApiManager.registerAPI("TarjetasApi", TarjetasApi.class, "posservices");
	}

}
