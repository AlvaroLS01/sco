package com.comerzzia.brico.pos.selfcheckout.dispositivo.comun.tarjeta;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.validation.ValidationUI;
import com.comerzzia.pos.dispositivo.comun.tarjeta.CodigoTarjetaController;
import com.comerzzia.pos.dispositivo.comun.tarjeta.FormularioCodigoTarjetaBean;
import com.comerzzia.pos.util.config.SpringContext;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

@Primary
@Component
public class SelfCheckoutCodigoTarjetaController extends CodigoTarjetaController {
	
	public static final Boolean mostrarTeclado = Boolean.TRUE;
	
    @FXML
    private TextField tfNumero;    
    
    protected FormularioCodigoTarjetaBean frCodTarjeta;
	
    @Autowired
	protected SelfCheckoutTicketManager selfCheckoutTicketManager;
	@FXML 
	private SelfCheckoutKeyboard keyboard; 
	
    @Override
    public void initialize(URL url, ResourceBundle rb) {
       
        frCodTarjeta = SpringContext.getBean(FormularioCodigoTarjetaBean.class);
        frCodTarjeta.setFormField("codTarjRegalo", tfNumero);
    }
    @Override
    public void initializeFocus() {
    		tfNumero.requestFocus();
    }
	@Override
	public void initializeComponents() {
		try {
			keyboard =  new SelfCheckoutKeyboard();
		}
		catch (IOException | URISyntaxException e) {
			log.error("Error cargando teclado");
		}
		keyboard.onController(this);
		keyboard.setPopupVisible(mostrarTeclado, tfNumero, this.getStage(), Boolean.FALSE);
		registrarAccionCerrarVentanaEscape();
	}
	
	@Override
    public void initializeForm() throws InitializeGuiException {        
		super.initializeForm();
		keyboard.setPopupVisible(mostrarTeclado, tfNumero, this.getStage(), Boolean.FALSE);

    }
	
	@Override
    @FXML
    public void accionAceptar(){
        
		frCodTarjeta.setCodTarjRegalo(tfNumero.getText());
        
        // Validamos el formulario 
        Set<ConstraintViolation<FormularioCodigoTarjetaBean>> constraintViolations = ValidationUI.getInstance().getValidator().validate(frCodTarjeta);
        if (constraintViolations.size() >= 1) {
            ConstraintViolation<FormularioCodigoTarjetaBean> next = constraintViolations.iterator().next();
            frCodTarjeta.setErrorStyle(next.getPropertyPath(), true);
            frCodTarjeta.setFocus(next.getPropertyPath());         
            VentanaDialogoComponent.crearVentanaError(next.getMessage(), this.getStage() );
        }
        else{
            getDatos().put(PARAMETRO_NUM_TARJETA, tfNumero.getText());
            selfCheckoutTicketManager.setNumeroTarjetaRegalo(tfNumero.getText());
            getStage().close();
        }
        
    }

}
