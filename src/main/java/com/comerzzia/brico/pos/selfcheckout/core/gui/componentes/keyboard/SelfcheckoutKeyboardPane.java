package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

import java.lang.reflect.Field;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.comtel2000.keyboard.control.KeyboardPane;

import com.comerzzia.pos.core.gui.POSApplication;

public class SelfcheckoutKeyboardPane extends KeyboardPane {

	private Logger log = Logger.getLogger(SelfcheckoutKeyboardPane.class);

	private String selfcheckoutKeyboardStyleURL;
	
	@Override
	public void load() throws Exception {
		super.load();

		eliminarMovimientoYZoomDeTeclado();

//		getStylesheets().clear();
//		getStylesheets().add(getCustomKeyBoardStyle());
	}

	private void eliminarMovimientoYZoomDeTeclado() {
		setOnZoom(null);

		setOnScroll(null);

		try {
			Field f = getClass().getSuperclass().getDeclaredField("_spaceKeyMove");
			f.setAccessible(true);
			f.set(this, false);
		}
		catch (Exception e) {
			log.error("SelfcheckoutKeyboardPane - configuracion erronea " + e.getMessage(), e);
		}
	}

	public String getCustomKeyBoardStyle() {
		if (StringUtils.isBlank(selfcheckoutKeyboardStyleURL)) {
            URL stylesheet = POSApplication.getInstance().getSkinResource("com/comerzzia/pos/core/gui/componentes/keyboard/BricoKeyboardButtonStyle.css");
            selfcheckoutKeyboardStyleURL = stylesheet != null ? stylesheet.toExternalForm() : null;
            setKeyBoardStyle(selfcheckoutKeyboardStyleURL);
        }
        return selfcheckoutKeyboardStyleURL;
	}
	
	@Override
	public String getUserAgentStylesheet() {
		return StringUtils.isNotBlank(getCustomKeyBoardStyle()) ? getCustomKeyBoardStyle() : super.getUserAgentStylesheet();
	}

}