package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.validation.FormularioGui;

@Component
@Scope("prototype")
public class FormularioDatosIdentificacionFidelizadoBean extends FormularioGui {

	@NotEmpty(message = "Debe rellenar el nombre del cliente.")
	private String nombre;

	@NotEmpty(message = "Debe rellenar los apellidos del cliente.")
	private String apellidos;

	@NotEmpty(message = "Debe rellenar el campo de identificación del cliente.")
	@Size(max = 20, message = "La longitud del campo número de documento no puede superar los {max} caracteres")
	private String numDocIdent;

	@NotEmpty(message = "Debe rellenar el correo del cliente.")
	private String correo;

	@NotEmpty(message = "Debe rellenar el código postal del cliente.")
	private String cPostal;

	@NotEmpty(message = "Debe seleccionar el país del cliente.")
	private String pais;

	public FormularioDatosIdentificacionFidelizadoBean() {

	}

	public FormularioDatosIdentificacionFidelizadoBean(String nombre, String apellidos, String numDocIdent, String correo, String cPostal, String pais) {
		super();
		this.nombre = nombre;
		this.apellidos = apellidos;
		this.numDocIdent = numDocIdent;
		this.correo = correo;
		this.cPostal = cPostal;
		this.pais = pais;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellidos() {
		return apellidos;
	}

	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	public String getNumDocIdent() {
		return numDocIdent;
	}

	public void setNumDocIdent(String numDocIdent) {
		this.numDocIdent = numDocIdent;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getcPostal() {
		return cPostal;
	}

	public void setcPostal(String cPostal) {
		this.cPostal = cPostal;
	}

	public String getPais() {
		return pais;
	}

	public void setPais(String pais) {
		this.pais = pais;
	}

	@Override
	public void limpiarFormulario() {
		nombre = "";
		apellidos = "";
		numDocIdent = "";
		correo = "";
		cPostal = "";
		pais = "";
	}

}
