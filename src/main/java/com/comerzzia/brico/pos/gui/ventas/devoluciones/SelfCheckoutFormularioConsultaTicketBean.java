package com.comerzzia.brico.pos.gui.ventas.devoluciones;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.validation.FormularioGui;

@Component
@Scope("prototype")
public class SelfCheckoutFormularioConsultaTicketBean extends FormularioGui {

	@NotEmpty(message = "El campo 'Código de ticket' no puede estar vacío.")
	String codOperacion;

	public SelfCheckoutFormularioConsultaTicketBean() {

	}

	public SelfCheckoutFormularioConsultaTicketBean(String codOperacion) {
		this.codOperacion = codOperacion;
	}

	public String getCodOperacion() {
		return codOperacion;
	}

	public void setCodOperacion(String codOperacion) {
		this.codOperacion = codOperacion != null ? codOperacion.trim() : codOperacion;
	}

	@Override
	public void limpiarFormulario() {
		codOperacion = "";
	}
}