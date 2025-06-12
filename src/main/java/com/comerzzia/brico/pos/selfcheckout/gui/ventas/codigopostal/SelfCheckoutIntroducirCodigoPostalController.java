package com.comerzzia.brico.pos.selfcheckout.gui.ventas.codigopostal;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumerico;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@Component
public class SelfCheckoutIntroducirCodigoPostalController extends WindowController {

	public static final String PARAM_CP = "cp";

	@FXML
	private TextField tfCodigo;
	
	@FXML
	private Button btAceptar, btCancelar;

	@FXML
	private TecladoNumerico tecladoNumerico;

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
		
		tfCodigo.clear();
		
		tfCodigo.lengthProperty().addListener(new ChangeListener<Number>(){

			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number valorAnterior, Number valorActual) {
				if (valorActual.intValue() > valorAnterior.intValue()) {
					  String input = tfCodigo.getText();
			            if (input.length() > 8) {
			            	tfCodigo.setText(input.substring(0, 8));
			                return;
			            }
			            Pattern permitido=null;
			            Matcher mpermitido=null;
			            
					if (input.length() == 1) {
						permitido = Pattern.compile("^[0-9A]$");
						mpermitido = permitido.matcher(tfCodigo.getText().substring(valorAnterior.intValue()));
					}
					if (input.length() == 2) {
						permitido = Pattern.compile("^[0-9D]$");
						mpermitido = permitido.matcher(tfCodigo.getText().substring(valorAnterior.intValue()));
					}
					if (input.length() == 3 || input.length() == 4 || input.length() == 6 || input.length() == 7
							|| input.length() == 8) {
						permitido = Pattern.compile("^[0-9]$");
						mpermitido = permitido.matcher(tfCodigo.getText().substring(valorAnterior.intValue()));
					}

					if (input.length() == 5) {
						permitido = Pattern.compile("^[0-9-]$");
						mpermitido = permitido.matcher(tfCodigo.getText().substring(valorAnterior.intValue()));
					}
					if(tfCodigo.getText().substring(valorAnterior.intValue()).equals("-")) {
						return;	
					}
					if (!mpermitido.find()) {
						// caracter no permitido, borrarlo
						tfCodigo.setText(tfCodigo.getText().substring(0, valorAnterior.intValue()));
						return;
					}
				}
			}
		});
	}

	public void actionButonOKTecladoNumerico(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			accionAceptar();
		}
	}
	
	public void accionAceptar() {
		if (validarCodigoPostal(tfCodigo.getText())) {
			getDatos().put(PARAM_CP, tfCodigo.getText().trim());
			getStage().close();
		} else {
			VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Formato incorrecto"), getStage());
			tfCodigo.requestFocus();
		}
	}

	protected void configurarIdioma() {
		btAceptar.setText(I18N.getTexto("Aceptar"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
	}
	
	protected void limpiarFormulario() {
		tfCodigo.setText("");
	}
	
	@FXML
	public void accionCancelar() {
		
		//si esta fidelizado lo manda de forma automatica
		getDatos().put(PARAM_CP, "");
		getStage().close();
	}
	
	public boolean validarCodigoPostal(String cp) {
		boolean validado = false;
		
		if (cp.length() ==5 ) {
			//ANDORRA
			Pattern patron = Pattern.compile("^A[D]\\d{3}$");
			Matcher matcher = patron.matcher(cp);
			if (matcher.matches()) {
				validado=true;
			}
			//ESPAÑA
			if(!validado) {
				 patron = Pattern.compile("\\d{5}");
				 matcher = patron.matcher(cp);
				 if (matcher.matches()) {
						validado=true;
					}
			}
		}
		//PORTUGAL
		 if (cp.length() == 8 ) {
			 Pattern patron = Pattern.compile("^[0-9]{4}-[0-9]{3}$");
				Matcher matcher = patron.matcher(cp);
				if (matcher.matches()) {
					validado=true;
				}
		}

		return validado;
	}

}
