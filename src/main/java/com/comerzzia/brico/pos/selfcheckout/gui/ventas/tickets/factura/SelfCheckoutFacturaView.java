package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura;

import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.pagos.datoscliente.SelfCheckoutCambiarDatosClienteView;
import com.comerzzia.pos.core.gui.view.ModalView;

@Component
public class SelfCheckoutFacturaView extends ModalView {

	@Override
	public Object loadCustomController(){
    	return new SelfCheckoutFacturaController();

    }


	@Override
	protected String getFXMLName() {
		return getFXMLName(SelfCheckoutCambiarDatosClienteView.class);

	}

}
