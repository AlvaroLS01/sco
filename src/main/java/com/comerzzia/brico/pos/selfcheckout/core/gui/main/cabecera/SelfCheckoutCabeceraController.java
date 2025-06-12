package com.comerzzia.brico.pos.selfcheckout.core.gui.main.cabecera;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.core.SelfCheckoutMainViewController;
import com.comerzzia.brico.pos.selfcheckout.core.gui.login.SelfCheckoutLoginView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.SelfCheckoutFacturacionArticulosController;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionUsuario;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.BotonBotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonaccion.simple.BotonBotoneraSimpleComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.BotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.ConfiguracionBotonBean;
import com.comerzzia.pos.core.gui.componentes.botonera.PanelBotoneraBean;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.exception.CargarPantallaException;
import com.comerzzia.pos.core.gui.login.LoginController;
import com.comerzzia.pos.core.gui.login.seleccionUsuarios.SeleccionUsuarioController;
import com.comerzzia.pos.core.gui.login.seleccionUsuarios.SeleccionUsuariosView;
import com.comerzzia.pos.core.gui.main.cabecera.CabeceraController;
import com.comerzzia.pos.core.gui.view.View;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.i18n.I18N;
import com.comerzzia.pos.util.xml.MarshallUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

@Primary
@Component
public class SelfCheckoutCabeceraController extends CabeceraController {

	private Logger log = Logger.getLogger(SelfCheckoutCabeceraController.class);

	@Autowired
	private SelfCheckOutSesionUsuario sesionUsuario;

	@Autowired
	private SelfCheckoutMainViewController mainViewController;
	
	@Autowired
	private SelfCheckoutFacturacionArticulosController manager;
	
	@Autowired
	protected Sesion sesion;
	
	@Override
	public void initializeComponents() {
		super.initializeComponents();
		
		String cssName = View.getCSSName(getView().getClass(), AppConfig.skin);
		cssName = cssName.replace("cabecera.css", "cabecera_cs.css");
		
		getView().getScene().getStylesheets().add(cssName);
	}

	@Override
	public void initializeForm() {
		super.initializeForm();
		cargarBotonera();
	}

	protected void cargarBotonera() {
		String rutaXML = null;
		if (sesionUsuario.isUsuarioSelfCheckout()) {
			rutaXML = "/skins/bricodepot/com/comerzzia/brico/pos/core/gui/main/cabecera/cabecera_self_checkout_panel.xml";
		}

		try {
			PanelBotoneraBean panelBotoneraBean = null;
			if (StringUtils.isNotBlank(rutaXML)) {
				InputStream is = SelfCheckoutCabeceraController.class.getResourceAsStream(rutaXML);
				panelBotoneraBean = (PanelBotoneraBean) MarshallUtil.leerXML(is, PanelBotoneraBean.class);
			}
			else {
				panelBotoneraBean = getView().loadBotonera();

				ConfiguracionBotonBean botonSelfCheckout = new ConfiguracionBotonBean("", null, null, "gotoSelfCheckout", "METODO");
				panelBotoneraBean.getLineasBotones().get(0).getLineaBotones().add(0, botonSelfCheckout);
			}
			botonera = new BotoneraComponent(panelBotoneraBean, panelCabeceraBotonera.getPrefWidth(), panelCabeceraBotonera.getPrefHeight(), this, BotonBotoneraSimpleComponent.class);
			panelCabeceraBotonera.getChildren().clear();
			panelCabeceraBotonera.getChildren().add(botonera);
			for (Entry<ConfiguracionBotonBean, BotonBotoneraComponent> entry : botonera.getMapConfiguracionesBotones().entrySet()) {
				ConfiguracionBotonBean config = (ConfiguracionBotonBean) entry.getKey();
				String clave = config.getClave();
				Button boton = entry.getValue().getBtAccion();
				boton.getStyleClass().add(clave);
			}
		}
		catch (InitializeGuiException e) {
			log.error("cargarBotonera() - Error al crear botonera en la cabecera: " + e.getMessage(), e);
		}
		catch (CargarPantallaException e) {
			log.error("cargarBotonera() - Error al crear botonera en la cabecera: " + e.getMessage(), e);
		}
		catch (JAXBException e) {
			log.error("cargarBotonera() - Error al crear botonera en la cabecera: " + e.getMessage(), e);
		}
	}

	public void gotoLoginSelfCheckout() {
		gotoLogin();
		if(!sesionUsuario.isUsuarioSelfCheckout()) {
			cargarBotonera();
			mainViewController.actualizarMenu();
			getApplication().getMainView().showActionView(201L);
		}

	}
	
	@FXML
    @Override
    public void gotoLogin() {
//		if(manager.ticketManager!=null) {
//			getDatos().put("ticket", manager.ticketManager.getTicket());			
//		}
//    	
        HashMap<String, Object> parametros = new HashMap<>();
//        if(getDatos().containsKey("ticket")) {
//        	TicketVenta ticket = (TicketVenta)getDatos().get("ticket");
//        	parametros.put("ticket", ticket);        	
//        }
        
    	parametros.put(LoginController.PARAMETRO_ENTRADA_CAMBIO_USUARIO, "true");
        if(AppConfig.loginBotonera){
        	parametros.put(SeleccionUsuarioController.PARAMETRO_MODO_PANTALLA_CAJERO, "S");
            getApplication().getMainView().showModal(SeleccionUsuariosView.class, parametros);
        }
        else{
        	parametros.put(LoginController.PARAMETRO_ENTRADA_ES_MODO_SELECCION_CAJERO, "S");
            getApplication().getMainView().showModal(SelfCheckoutLoginView.class, parametros);
        }
    }

	public void gotoSelfCheckout(HashMap<String, String> params) {
		log.debug("gotoSelfCheckout() - Volvemos a modo SelfCheckout");
		try {
			log.debug("gotoSelfCheckout() - Cambiando el idioma a " + AppConfig.idioma + " para el SCO");
			Locale locale = new Locale(((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getCodIdiomaCliente(), AppConfig.pais);
			Locale.setDefault(locale);
			
			sesionUsuario.initSelfCheckout();

			cargarBotonera();
			
			mainViewController.actualizarMenu();
			
			if(manager.getSesionTicketManager()!= null && manager.getSesionTicketManager().getSesionTicketManager().getTicket()!=null) {
				mainViewController.mostrarPantallaVentaSelfCheckout();
			}else {
				mainViewController.mostrarPantallaBienvenidaSelfCheckout();
			}
			
//			mainViewController.mostrarPantallaSelfCheckoutActual();
		}
		catch (Exception e) {
			log.error("gotoSelfCheckout() - Ha habido un problema al volver al Self Checkout: " + e.getMessage(), e);
			VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("No se ha podido volver a la pantalla de Self Checkout, contacte con el administrador."), e);
		}
	}

}
