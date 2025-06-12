package com.comerzzia.brico.pos.selfcheckout.core.gui;

import org.apache.log4j.Logger;

import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.simboloCargando.SCOVentanaCargando;
import com.comerzzia.pos.core.gui.BackgroundTask;

import javafx.stage.Stage;

public abstract class SCOBackgroundTask<V> extends BackgroundTask<V> {
	
	protected Stage stage;

	
	private static Logger log = Logger.getLogger(SCOBackgroundTask.class);
	
	public void start(Stage stageR) {
		
		this.stage = stageR;
		currentThread = new Thread(this);
		currentThread.setName(this.getClass().toString());
		currentThread.start();
		
		mostrarVentanaCargando();

    }
	
	@Override
	protected void mostrarVentanaCargando(){
    	if(mostrarVentanaCargando){
    		SCOVentanaCargando.crearVentanaCargando(stage);
            SCOVentanaCargando.mostrar();
            
    	}
    }
	
	@Override
	protected void cerrarVentanaCargando(){
    	log.trace("cerrarVentanaCargando()");
    	if(mostrarVentanaCargando){
    		SCOVentanaCargando.cerrar();
    	}
    }
}
