package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.pagos.datoscliente;

import java.awt.Button;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.gui.ventas.tickets.pagos.datosCliente.CambiarDatosClienteController;

import javafx.fxml.FXML;

@Primary
@Component
public class SelfCheckoutCambiarDatosClienteController extends CambiarDatosClienteController {

	@FXML
	protected Button btBuscarPais;

	@Override
	public void initializeForm() {
		try {

			super.initializeForm();

		}
		catch (InitializeGuiException e) {
			log.debug("SelfCheckoutCambiarDatosClienteController() - initializeForm() error al inicializar");
		}

		tfCodCliente.setDisable(true);
		tfDesCliente.setDisable(true);
		tfRazonSocial.setDisable(true);
		tfDomicilio.setDisable(true);
		tfPoblacion.setDisable(true);
		tfProvincia.setDisable(true);
		tfLocalidad.setDisable(true);
		tfCP.setDisable(true);
		tfTelefono.setDisable(true);
		tfCodPais.setDisable(true);
		tfDesPais.setDisable(true);
		btBuscar.setVisible(false);
		btBuscarPais.setVisible(false);

	}

}

	
	    

