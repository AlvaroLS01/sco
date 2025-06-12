package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.dialogos;

import org.apache.log4j.Logger;

import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;

import javafx.stage.Window;


public class SelfCheckoutVentanaDialogoComponent extends VentanaDialogoComponent {

	// <editor-fold desc="Declaración de variables">   
    private static final Logger log = Logger.getLogger(SelfCheckoutVentanaDialogoComponent.class.getName());
    
    
    public static boolean crearVentanaConfirmacionUnBotonAceptar(String mensaje, Window ventanaPadre){
      	 log.debug("crearVentanaConfirmacionUnBotonAceptar()" );
           VentanaDialogoComponent ventana = crearVentana(null, mensaje, TIPO_CONFIRMACION, false, ventanaPadre, null, false, null, null);
           return ventana.isPulsadoAceptar();
       }
}
