package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

import java.lang.reflect.Field;

import org.comtel2000.keyboard.control.KeyBoardPopupBuilder;

public class SelfcheckoutKeyboardPopupBuilder extends KeyBoardPopupBuilder {

	public static KeyBoardPopupBuilder create() {
		return new SelfcheckoutKeyboardPopupBuilder();
	}
	
	public SelfcheckoutKeyboardPopupBuilder() {
		try {
			Field f = getClass().getSuperclass().getDeclaredField("kb");
			f.setAccessible(true);
			f.set(this, SelfcheckoutKeyboardBuilder.create());

		}
		catch (Exception e) {
		}
	}

}
