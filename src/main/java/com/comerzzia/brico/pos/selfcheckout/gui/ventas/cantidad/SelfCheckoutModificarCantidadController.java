package com.comerzzia.brico.pos.selfcheckout.gui.ventas.cantidad;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumerico;
import com.comerzzia.pos.core.gui.componentes.textField.TextFieldImporte;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketException;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@Component
public class SelfCheckoutModificarCantidadController extends WindowController {
	
	private Logger log = Logger.getLogger(SelfCheckoutModificarCantidadController.class);

	public static final String PARAMETRO_LINEA = "Linea_Ticket";
	public static final String PARAMETRO_CANTIDAD = "Cantidad";
	
	@FXML
	private Label lbDescripcionArticulo, lbIntroCantidad;

	@FXML
	private TecladoNumerico tecladoNumerico;
	
	@FXML
	private TextFieldImporte tfCantidad;
	
	@FXML
	private Button btAceptar, btCancelar;
	
	protected LineaTicket linea;
	
	protected BigDecimal cantidad;
	
	protected BigDecimal cantidadOriginal;
	
	@Autowired
	protected SelfCheckoutTicketManager selfCheckoutTicketManager;

	@Override
    public void initialize(URL url, ResourceBundle rb) {
    }

	@Override
    public void initializeComponents() throws InitializeGuiException {
	    tecladoNumerico.init(getScene());
	    setShowKeyboard(false);
	    
	    tfCantidad.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if(oldValue){
					try {
						limpiarFormulario();
						BigDecimal cantidadNueva = FormatUtil.getInstance().desformateaBigDecimal(tfCantidad.getText(), 2);
						if (cantidadNueva == null) {
							cantidadNueva = BigDecimal.ZERO;
						}
						tfCantidad.setText(FormatUtil.getInstance().formateaNumero(cantidadNueva, 2));
					}
					catch (Exception e) {
						marcarErrorFormulario();
					}
                }
            }
        });
    }

	@Override
    public void initializeFocus() {
		tfCantidad.requestFocus();
    }

	@Override
    public void initializeForm() throws InitializeGuiException {
		configurarIdioma();
		linea = (LineaTicket) getDatos().get(PARAMETRO_LINEA);
		cantidadOriginal = linea.getCantidad();
		
		if(linea == null) {
			throw new InitializeGuiException(I18N.getTexto("No se ha encontrado la línea a la que se desea modificar la cantidad."));
		}
		else {
			try {
	            lbDescripcionArticulo.setText(linea.getArticulo().getDesArticulo());
	            tfCantidad.setText(FormatUtil.getInstance().formateaNumero(linea.getCantidad()));
	            tfCantidad.selectAll();
            }
            catch (Exception e) {
            	log.error("initializeForm() - Ha habido un error al inicializar la pantalla: " + e.getMessage(), e);
            	throw new InitializeGuiException(e);
            }
			
		}
    }
	
	protected void configurarIdioma() {
		lbIntroCantidad.setText(I18N.getTexto("Modifique la cantidad del artículo"));
		btAceptar.setText(I18N.getTexto("Aceptar"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
		lbDescripcionArticulo.setText(I18N.getTexto("Articulo"));

	}
	public void accionAceptar() {
		if(validarFormulario()) {
			log.debug("accionAceptar() - Cambiando cantidad del artículo " + linea.getArticulo().getCodArticulo() + " de " + cantidadOriginal + " a " + cantidad + " unidades");
			getDatos().put(PARAMETRO_CANTIDAD, cantidad);
			getStage().close();
		}
	}
	
	private boolean validarFormulario() {
		limpiarFormulario();
		try {
			cantidad = FormatUtil.getInstance().desformateaBigDecimal(tfCantidad.getText());
			linea.setCantidad(cantidad);
			selfCheckoutTicketManager.comprobarCantidadUnitaria(linea);
			
			if(BigDecimalUtil.isMenorACero(cantidad)) {
				marcarErrorFormulario(I18N.getTexto("La cantidad introducida no es correcta"));
			}else if(BigDecimalUtil.isIgualACero(cantidad)) {
				marcarErrorFormulario(I18N.getTexto("No se puede introducir un artículo con cantidad 0"));
//			}else if(BigDecimalUtil.isMayor(cantidad, new BigDecimal(1000))){
//				marcarErrorFormulario();
			}else {
				return true;
			}
			// Si el valor del dígito no es correcto restauramos el precio al original
			restaurarPrecioOriginal();
			return false;
		}
	    catch(LineaTicketException e) {
	    	log.debug("validarFormulario() - Error al validar formulario: " + e.getMessage(), e);
	    	marcarErrorFormulario(e.getMessage());
	    	return false;
	    }
	    catch(Exception e) {
	    	log.debug("validarFormulario() - Error al validar formulario: " + e.getMessage(), e);
	    	marcarErrorFormulario();
	    	return false;
	    }
    }

	protected void restaurarPrecioOriginal() {
		cantidad = cantidadOriginal;
		linea.setCantidad(cantidad);
	}

	private void marcarErrorFormulario() {
		tfCantidad.getStyleClass().add("error-formulario");
	    VentanaDialogoComponent.crearVentanaError(I18N.getTexto("La cantidad introducida no es correcta"), getStage());
	    tfCantidad.requestFocus();
    }
	
	private void marcarErrorFormulario(String textoPantalla) {
		tfCantidad.getStyleClass().add("error-formulario");
	    VentanaDialogoComponent.crearVentanaError(I18N.getTexto(textoPantalla), getStage());
	    tfCantidad.requestFocus();
    }

	private void limpiarFormulario() {
		tfCantidad.getStyleClass().remove("error-formulario");
    }

	@FXML
	public void accionCancelar() {
		restaurarPrecioOriginal();
		getDatos().remove(PARAMETRO_CANTIDAD);
		getStage().close();
	}
	
	public void actionButonOKTecladoNumerico(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			accionAceptar();
		}
	}
}
