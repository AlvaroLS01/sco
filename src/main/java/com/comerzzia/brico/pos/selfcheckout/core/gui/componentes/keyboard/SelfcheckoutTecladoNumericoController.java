package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.componentes.tecladonumerico.TecladoNumericoController;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;

@Primary
@Component
public class SelfcheckoutTecladoNumericoController extends TecladoNumericoController {
	
		@FXML
		protected Button btLimpiar, btBorrar;
		
		 @Override
		 public void initializeForm() {
			 super.initializeForm();
			 btLimpiar.setText(I18N.getTexto("Limpiar"));
			 btBorrar.setText(I18N.getTexto("Borrar"));
		 }
		
		@Override
		@FXML
	    public void actionButtonABC(ActionEvent event) {
	        robot.keyPress(KeyCode.CONTROL);
	        robot.keyPress(KeyCode.A);
	        robot.keyRelease(KeyCode.CONTROL);
	        robot.keyPress(KeyCode.BACK_SPACE);
	    }
		
		@FXML
		public void actionButtonMenos(ActionEvent event) {
			robot.keyType(KeyCode.MINUS, "-");
		}
}
