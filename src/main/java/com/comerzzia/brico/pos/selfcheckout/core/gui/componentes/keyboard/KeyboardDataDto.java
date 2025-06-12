package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

public class KeyboardDataDto {

	private boolean visibleAlInicio;

	private boolean pintarPiePantalla;
	
	private boolean pintarSignoNegativo;
	
	private boolean mostrar;

	public KeyboardDataDto() {
		super();

		this.visibleAlInicio = true;
		this.mostrar = true;
	}

	public boolean isVisibleAlInicio() {
		return visibleAlInicio;
	}

	public void setVisibleAlInicio(boolean visibleAlInicio) {
		this.visibleAlInicio = visibleAlInicio;
	}

	public boolean isPintarPiePantalla() {
		return pintarPiePantalla;
	}

	public void setPintarPiePantalla(boolean pintarPiePantalla) {
		this.pintarPiePantalla = pintarPiePantalla;
	}
	
    public boolean isPintarSignoNegativo() {
    	return pintarSignoNegativo;
    }
	
    public void setPintarSignoNegativo(boolean pintarSignoNegativo) {
    	this.pintarSignoNegativo = pintarSignoNegativo;
    }

	public boolean isMostrar() {
		return mostrar;
	}
	
    public void setMostrar(boolean mostrar) {
    	this.mostrar = mostrar;
    }

}
