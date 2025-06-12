package com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigo;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumerico;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.services.articulos.ArticuloNotFoundException;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketAbstract;
import com.comerzzia.pos.services.ticket.lineas.LineasTicketServices;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

@Component
public class SelfCheckoutIntroducirCodigoController extends WindowController {

	private Logger log = Logger.getLogger(SelfCheckoutIntroducirCodigoController.class);

	public static final String PARAM_CODART = "SelfCheckout_Codart";

	@FXML
	private TextField tfCodigo;
	
	@FXML
	private Button btAceptar, btCancelar;

	@FXML
	private TecladoNumerico tecladoNumerico;

	@FXML
	private Label lbDescripcionArticulo, lbImporteTotal, lbEuro, lbIntroCodArticulo, lbTituloArticulo;

	@Autowired
	private LineasTicketServices lineasTicketServices;

	private TicketManager ticketManager;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		tecladoNumerico.init(getScene());
		setShowKeyboard(false);
	}

	@Override
	public void initializeFocus() {
		tfCodigo.requestFocus();
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		configurarIdioma();
		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);

		if (ticketManager == null) {
			throw new InitializeGuiException(
					I18N.getTexto("No se ha podido abrir la pantalla. Contacte con el personal de tienda."));
		}
		limpiarFormulario();
	}

	@SuppressWarnings("rawtypes")
	public boolean buscarArticulo() {
		if (StringUtils.isNotBlank(tfCodigo.getText())) {
			try {
				LineaTicketAbstract lineaTicket = lineasTicketServices.createLineaArticulo(
						(TicketVenta) ticketManager.getTicket(), tfCodigo.getText(), null, null, BigDecimal.ONE, null,
						SpringContext.getBean(LineaTicket.class));

				String desart = lineaTicket.getArticulo().getDescripcion();
				String importe = FormatUtil.getInstance().formateaImporte(lineaTicket.getImporteTotalConDto());

				lbDescripcionArticulo.setText(desart);
				lbImporteTotal.setText(importe);
				lbEuro.setText("€");

				return true;
			} catch (ArticuloNotFoundException e) {
				VentanaDialogoComponent.crearVentanaError(
						I18N.getTexto("El artículo introducido no existe. Compruebe el código, por favor."),
						getStage());
			} catch (Exception e) {
				log.error("buscarArticulo() - Ha habido un error al buscar el artículo con código: " + e.getMessage(),
						e);
				VentanaDialogoComponent.crearVentanaError(getStage(), e.getMessage(), e);
			}
		} else {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("Introduzca el código del artículo, por favor."),
					getStage());
		}

		return false;
	}

	public void accionAceptar() {
		String codart = tfCodigo.getText();
		
		if (codart.startsWith("2") || codart.startsWith("9") || StringUtils.isNotBlank(codart) && buscarArticulo()) {
			getDatos().put(PARAM_CODART, codart);
			getStage().close();
		}
	}
	
	public void actionTfCodigoIntro(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			accionAceptar();
		}

	}

	protected void configurarIdioma() {
		lbIntroCodArticulo.setText(I18N.getTexto("Introduzca el código del artículo"));
		lbTituloArticulo.setText(I18N.getTexto("El artículo consultado es:"));
		btAceptar.setText(I18N.getTexto("Añadir a la compra"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
	}
	
	protected void limpiarFormulario() {
		tfCodigo.setText("");
		lbDescripcionArticulo.setText("");
		lbImporteTotal.setText("");
		lbEuro.setText("");
	}

}
