package com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos.sectoractividad;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.fidelizado.colectivos.VentajasExclusivasController;
import com.comerzzia.brico.pos.selfcheckout.persistence.colectivos.Colectivos;
import com.comerzzia.brico.pos.selfcheckout.services.colectivos.ColectivosParser;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@Component
public class SectorActividadController extends WindowController {

	public static final Logger log = Logger.getLogger(SectorActividadController.class.getName());

	@FXML
	private Label lbTitulo;

	@FXML
	private VBox vbColectivos;

	private List<CheckBox> checkBoxes = new ArrayList<>();
	
	private List<Colectivos> colectivos = new ArrayList<>();
	
	private Map<String, String> descripcionCodigoMap = new HashMap<>();
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {

	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		ColectivosParser parser = new ColectivosParser();
		try {
			colectivos = parser.parsearColectivos("entities/colectivos_alta.json");
			for (Colectivos colectivo : colectivos) {
				CheckBox checkBox = new CheckBox(I18N.getTexto(colectivo.getDescripcion()));
				checkBox.setOnAction(event -> handleCheckBoxAction(checkBox,colectivo));
				vbColectivos.getChildren().add(checkBox);
				checkBoxes.add(checkBox);

			}
		} catch (IOException e) {
			log.error("Ha ocurrido un error: " + e.getMessage(), e);
			 getDatos().put(VentajasExclusivasController.CANCELAR, true);
			 throw new InitializeGuiException("El JSON está vacío. Por favor, solicite ayuda en caja central");
			
		}
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		lbTitulo.setText(I18N.getTexto("SELECCIONA TU COLECTIVO"));

		limpiarFormulario();
		
		for (int i = 0; i < colectivos.size(); i++) {
            Colectivos colectivo = colectivos.get(i);
            CheckBox checkBox = checkBoxes.get(i);
            checkBox.setText(I18N.getTexto(colectivo.getDescripcion()));
            descripcionCodigoMap.put(colectivo.getCodigo(), I18N.getTexto(colectivo.getDescripcion()));
		}
	}

	private void limpiarFormulario() {
		for (CheckBox checkBox : checkBoxes) {
			checkBox.setSelected(false);
		}
	}

	@Override
	public void initializeFocus() {}
	
	private void handleCheckBoxAction(CheckBox checkBox, Colectivos colectivo) {
        if (checkBox.isSelected()) {
        	String codigo = "";
        	String descripcion = checkBox.getText();
        	
        	for (Map.Entry<String, String> entry : descripcionCodigoMap.entrySet()) {
                if (entry.getValue().equals(descripcion)) {
                    codigo = entry.getKey();
                    break;
                }
            }
            if (codigo != null) {
                getDatos().put("sector", codigo);
            }
            descripcionCodigoMap.clear();
            getStage().close();
        }
    }

}
