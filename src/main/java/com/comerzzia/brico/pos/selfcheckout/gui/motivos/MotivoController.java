package com.comerzzia.brico.pos.selfcheckout.gui.motivos;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.articulos.edicion.SelfCheckoutEdicionArticuloController;
import com.comerzzia.brico.pos.selfcheckout.persistence.motivos.Motivo;
import com.comerzzia.brico.pos.selfcheckout.persistence.motivos.MotivoExample;
import com.comerzzia.brico.pos.selfcheckout.persistence.motivos.MotivoMapper;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas.BricodepotLineaTicket;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.util.StringConverter;

@Controller
public class MotivoController extends WindowController {

	private static final Logger log = Logger.getLogger(MotivoController.class.getName());
	
	public static final String PARAMETRO_MOTIVO = "motivo";

	@Autowired
	protected MotivoMapper mapper;

	@FXML
	protected ComboBox<Motivo> cbMotivo;

	@FXML
	protected Label lbCodigo;
	@FXML
	protected Label lbArticulo;
	@FXML
	protected Label lbCantidad;
	@FXML
	protected Label lbPrecio;
	@FXML
	protected TextArea taComentario;

	protected BricodepotLineaTicket lineaOriginal;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

	}

	@Override
	public void initializeComponents() throws InitializeGuiException {

	}

	@Override
	public void initializeFocus() {

	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		Integer tipoMotivo = (Integer) getDatos().get(SelfCheckoutEdicionArticuloController.PARAMETRO_TIPO_MOTIVO);
		MotivoExample example = new MotivoExample();
		example.createCriteria().andCodigoTipoEqualTo(String.valueOf(tipoMotivo));

		List<Motivo> list = mapper.selectByExample(example);
		ObservableList<Motivo> ob = FXCollections.observableArrayList(list);
		cbMotivo.setConverter(new StringConverter<Motivo>(){

			@Override
			public String toString(Motivo arg0) {
				return arg0.getDescripcion();
			}

			@Override
			public Motivo fromString(String arg0) {
				return null;
			}
		});

		LineaTicket lineaSeleccionada = (LineaTicket) getDatos().get(SelfCheckoutEdicionArticuloController.PARAMETRO_LINEA);
		setLinea(lineaSeleccionada);
		cbMotivo.setItems(ob);
		lbCodigo.setText(lineaOriginal.getCodArticulo());
		lbArticulo.setText(lineaOriginal.getDesArticulo());
		lbCantidad.setText(lineaOriginal.getCantidadAsString());
		lbPrecio.setText(lineaOriginal.getPrecioTotalConDtoAsString());
		taComentario.setText("");
	}

	private void setLinea(LineaTicket linea) {
		this.lineaOriginal = (BricodepotLineaTicket) linea;
	}

	@FXML
	public void accionGuardar() {
		log.debug("accionGuardar() - Guardando motivo en la línea");
		Motivo mot = cbMotivo.getValue();
		if (mot != null) {
			mot.setComentario(taComentario.getText());
			mot.setPrecioSinDtoOriginal(lineaOriginal.getPrecioTotalTarifaOrigen());
			mot.setPrecioSinDtoAplicado(lineaOriginal.getPrecioTotalSinDto());
			lineaOriginal.setMotivo(mot);
			getDatos().put("comentario", taComentario);
			getDatos().put(PARAMETRO_MOTIVO, mot);
			getStage().close();
		}
		else {
			taComentario.setText("");
			log.debug("accionGuardar() - No se ha seleccionado ningún motivo");
			getStage().close();
		}
	}

}
