package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

import java.lang.reflect.Field;

import org.comtel2000.keyboard.control.KeyBoardBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import com.comerzzia.pos.services.core.sesion.Sesion;

public class SelfcheckoutKeyboardBuilder extends KeyBoardBuilder {
	@Autowired
	protected Sesion sesion;

	public static KeyBoardBuilder create() {
		return new SelfcheckoutKeyboardBuilder();
	}

	protected SelfcheckoutKeyboardBuilder() {
		try {
			Field f = getClass().getSuperclass().getDeclaredField("kb");
			f.setAccessible(true);
			f.set(this, new SelfcheckoutKeyboardPane());
			SelfcheckoutKeyboardPane pane = (SelfcheckoutKeyboardPane) f.get(this);
//			pane.setKeyBoardStyle(POSApplication.getInstance().getSkinResource("/com/comerzzia/pos/core/gui/componentes/keyboard/BricoKeyboardButtonStyle.css").getPath());
			pane.setSpaceKeyMove(Boolean.FALSE);

		}
		catch (Exception e) {

		}
	}

}
