package com.comerzzia.brico.pos.selfcheckout.core;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionUsuario;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.main.MainViewController;

import javafx.application.Platform;

@Component
public class SelfCheckoutMainViewController extends MainViewController {

	private static final long ID_ACCION_BIENVENIDA_SC = 700000L;

	private static final long ID_ACCION_VENTAS_SC = 700001L;

	private Logger log = Logger.getLogger(SelfCheckoutMainViewController.class);

	@Autowired
	private SelfCheckOutSesionUsuario sesionUsuario;

	private Long pantallaSelfCheckout;
	
	protected String cod;

	
	@Override
	public void initializeComponents() throws InitializeGuiException {
		super.initializeComponents();
		
	}
	
	@Override
	public void initializeForm() {
		super.initializeForm();

		actualizarMenu();

		pantallaSelfCheckout = POSApplication.getInstance().getMainView().getCurrentAccion().getIdAccion();
		cod = (String)getDatos().get("lecturaRecogida");
	}

	public void actualizarMenu() {
		if (sesionUsuario.isUsuarioSelfCheckout()) {
			quitarMenu();
		}
		else {
			mostrarMenu();
		}
	}

	public void mostrarMenu() {
		btnMostrarOcultar.setVisible(true);
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				accionMostrarMenu();
			}
		});
	}

	public void quitarMenu() {
		btnMostrarOcultar.setVisible(false);
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				accionOcultarMenu();
			}
		});
	}

	public void mostrarPantallaSelfCheckoutActual() {
		log.debug("mostrarPantallaSelfCheckoutActual() - Volviendo al modo Self Checkout");
		getApplication().getMainView().showActionView(pantallaSelfCheckout);
	}

	public void mostrarPantallaBienvenidaSelfCheckout() {
		log.debug("mostrarPantallaBienvenidaSelfCheckout() - Mostrando pantalla de bienvenida");
		pantallaSelfCheckout = ID_ACCION_BIENVENIDA_SC;
		getApplication().getMainView().showActionView(ID_ACCION_BIENVENIDA_SC);
	}

	public void mostrarPantallaVentaSelfCheckout() {
		log.debug("mostrarPantallaVentaSelfCheckout() - Mostrando pantalla de ventas");
		pantallaSelfCheckout = ID_ACCION_VENTAS_SC;
		getApplication().getMainView().showActionView(ID_ACCION_VENTAS_SC);
	}
	
	public void mostrarPantallaVentaSelfCheckout(String cod) {
		log.debug("mostrarPantallaVentaSelfCheckout() - Mostrando pantalla de ventas");
		pantallaSelfCheckout = ID_ACCION_VENTAS_SC;
		getDatos().put("lecturaRecogida", cod);
		getApplication().getMainView().showActionView(ID_ACCION_VENTAS_SC, getDatos());
	}

}
