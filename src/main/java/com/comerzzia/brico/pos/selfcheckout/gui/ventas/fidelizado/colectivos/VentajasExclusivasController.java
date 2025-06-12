package com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos.sectoractividad.SectorActividadView;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

@Component
public class VentajasExclusivasController extends WindowController {

	protected static final Logger log = Logger.getLogger(VentajasExclusivasController.class);

	public static final String CANCELAR = "CANCELAR";

	public static final String CODCOLECTIVO = "CODCOLECTIVO";
	public static final String CODSECTOR = "CODSECTOR";
	
	private static final String COD_CLUB = "0002";


	@FXML
	private Label lbTitulo;

	@FXML
	private Button btCancelar;

	@Override
	public void initialize(URL url, ResourceBundle rb) {}

	@Override
	public void initializeComponents() throws InitializeGuiException {}

	@Override
	public void initializeForm() throws InitializeGuiException {
		lbTitulo.setText(I18N.getTexto("VENTAJAS EXCLUSIVAS PARA AMANTES DEL BRICOLAJE"));
		btCancelar.setText(I18N.getTexto("Cancelar"));

	}

	@Override
	public void initializeFocus() {}

	@FXML
	private void accionCancelar(ActionEvent event) {
		getDatos().put(CANCELAR, true);
		getStage().close();
	}

	@FXML
	private void unirseDYI() {
		getDatos().put(CODCOLECTIVO, COD_CLUB);
		getStage().close();
	}

	@FXML
	private void unirsePRO() {
		ventanaColectivo();
		if (!getDatos().containsKey(CANCELAR)) {
			if (getDatos().containsKey("sector")) {
				getDatos().put(CODSECTOR, getDatos().get("sector").toString());
			}
		}
		getStage().close();
	}

	private void ventanaColectivo() {
		getApplication().getMainView().showModalCentered(SectorActividadView.class, getDatos(), getStage());
	}
}
