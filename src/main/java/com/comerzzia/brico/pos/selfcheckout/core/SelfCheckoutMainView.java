package com.comerzzia.brico.pos.selfcheckout.core;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionUsuario;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.main.MainView;
import com.comerzzia.pos.core.gui.main.MainViewController;
import com.comerzzia.pos.core.gui.view.ModalView;
import com.comerzzia.pos.core.gui.view.View;

import javafx.stage.Stage;

@Primary
@Component
public class SelfCheckoutMainView extends MainView {

	private Logger log = Logger.getLogger(SelfCheckoutMainView.class);

	@Autowired
	private SelfCheckOutSesionUsuario sesionUsuario;

	@Override
	protected String getFXMLName() {
		return getFXMLName(MainView.class);
	}

	@Override
	public Object loadCustomController() {
		return new SelfCheckoutMainViewController();
	}

	@Override
	protected void showModalInternal(Class<? extends ModalView> clazz, HashMap<String, Object> datosVentana, Stage stage, boolean centered) {
		try {
			ModalView view = (ModalView) View.loadView(clazz, stage);
			view.loadAndInitialize(datosVentana, true);
			if (centered) {
				view.showCentered();
			}
			else {
				if (!sesionUsuario.isUsuarioSelfCheckout()) {
					((MainViewController) POSApplication.getInstance().getMainView().getController()).ocultarCabecera();
					((MainViewController) POSApplication.getInstance().getMainView().getController()).ocultarBotonMenu();
				}

				view.show();

				if (!sesionUsuario.isUsuarioSelfCheckout()) {
					((MainViewController) POSApplication.getInstance().getMainView().getController()).mostrarCabecera();
					((MainViewController) POSApplication.getInstance().getMainView().getController()).mostrarBotonMenu();
				}
			}
		}
		catch (InitializeGuiException e) {
			if (e.isMostrarError()) {
				log.error("showModal() - Error abriendo ventana para la clase: " + clazz.getName(), e);
				VentanaDialogoComponent.crearVentanaError(POSApplication.getInstance().getStage(), e.getMessageI18N(), e);
			}
		}
	}

}
