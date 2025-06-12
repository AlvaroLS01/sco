package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

import org.comtel2000.keyboard.control.DefaultLayer;
import org.comtel2000.keyboard.control.KeyBoardPopup;
import org.comtel2000.keyboard.control.KeyboardType;
import org.comtel2000.keyboard.robot.FXRobotHandler;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.keyboard.Keyboard;
import com.comerzzia.pos.core.gui.componentes.textField.TextFieldImporte;
import com.comerzzia.pos.util.config.AppConfig;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.stage.Screen;
import javafx.stage.Window;

@Primary
@Component
public class SelfCheckoutKeyboard extends Keyboard {
	
	private static final double escalaTeclado = 1.05;

	public SelfCheckoutKeyboard() throws IOException, URISyntaxException {
		super();
	}

	@Override
	protected void addFocusListener(Scene scene) {
		BricoSceneFocusChangeListener sceneFocusChangeListener = (BricoSceneFocusChangeListener) mapScenesInUse.get(scene);
		if (sceneFocusChangeListener == null) {
			sceneFocusChangeListener = new BricoSceneFocusChangeListener(scene.getWindow());
			mapScenesInUse.put(scene, sceneFocusChangeListener);
		}
		if (!sceneFocusChangeListener.isAttached()) {
			scene.focusOwnerProperty().addListener(sceneFocusChangeListener);
			sceneFocusChangeListener.setAttached(true);
		}


		for (Node nodo : getAllNodes(scene.getRoot())) {
			if(nodo instanceof TextInputControl) {
				nodo.setOnMouseClicked(new EventHandler<Event>(){
					@Override
                    public void handle(Event event) {
						boolean mostrarDebajoPantalla = false;
						boolean mostrar = true;
						if(nodo.getUserData() instanceof KeyboardDataDto) {					
							KeyboardDataDto keyboardDataDto = (KeyboardDataDto) nodo.getUserData();
							mostrarDebajoPantalla = keyboardDataDto.isPintarPiePantalla();
							mostrar = keyboardDataDto.isMostrar();
						}
						
						if(mostrar && ((TextInputControl) nodo).isEditable()) {
							setPopupVisible(true, (TextInputControl) nodo, scene.getWindow(), mostrarDebajoPantalla);
						}
						else {
							setPopupVisible(false, (TextInputControl) nodo, scene.getWindow(), mostrarDebajoPantalla);
						}
                    }
				});
			}
		}
	}

	protected class BricoSceneFocusChangeListener extends SceneFocusChangeListener implements ChangeListener<Node> {

		public BricoSceneFocusChangeListener(Window window) {
			super(window);
		}

		@Override
		public void changed(ObservableValue<? extends Node> value, Node n1, Node n2) {
			if (n2 != null && n2 instanceof TextInputControl) {
				if(((TextInputControl) n2).isEditable()) {
					boolean mostrarAlInicio = true;
					boolean mostrarDebajoPantalla = false;
					boolean mostrar = true;
					if(n2.getUserData() != null && n2.getUserData() instanceof KeyboardDataDto) {					
						KeyboardDataDto keyboardDataDto = (KeyboardDataDto) n2.getUserData();
						mostrarAlInicio = keyboardDataDto.isVisibleAlInicio();
						mostrarDebajoPantalla = keyboardDataDto.isPintarPiePantalla();
						mostrar = keyboardDataDto.isMostrar();
					}
					if(mostrarAlInicio && mostrar) {
						setPopupVisible(true, (TextInputControl) n2, this.window, mostrarDebajoPantalla);
					}
				}
			}
			else {
				close();
			}
		}

		public Boolean isAttached() {
			return isAttached;
		}

		public void setAttached(Boolean isAttached) {
			this.isAttached = isAttached;
		}
	}

	public ArrayList<Node> getAllNodes(Parent root) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		addAllDescendents(root, nodes);
		return nodes;
	}

	private void addAllDescendents(Parent parent, ArrayList<Node> nodes) {
		for (Node node : parent.getChildrenUnmodifiable()) {
			nodes.add(node);
			if (node instanceof Parent)
				addAllDescendents((Parent) node, nodes);
		}
	}
	
	@Override
	protected void calcularTamañoPantalla(boolean show, TextInputControl textNode, boolean printBottomScreen) {
		if (show) {
			// FIX BUG BRICO-466
			Object userData = null;
			if(textNode != null) {
				userData = textNode.getUserData();
			}

			// FIX BUG BRICO-466
			if(userData != null && userData instanceof KeyboardDataDto && ((KeyboardDataDto) userData).isPintarSignoNegativo()) {
				getKeyboardPopup().getKeyBoard().setKeyboardType(KeyboardType.EMAIL);
			}
			else if(textNode instanceof TextFieldImporte) {
				getKeyboardPopup().getKeyBoard().setKeyboardType(KeyboardType.NUMERIC);
			}
			else {
				getKeyboardPopup().getKeyBoard().setKeyboardType(KeyboardType.TEXT_SHIFT);							
			}
			
			if (textNode != null && textNode.getScene() != null) {
				Window window = textNode.getScene().getWindow();
				Rectangle2D textNodeBounds = new Rectangle2D(
						window.getX() + textNode.getLocalToSceneTransform().getTx(), 
						window.getY() + textNode.getLocalToSceneTransform().getTy(), 
						textNode.getWidth(), 
						textNode.getHeight());

				Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
				if (textNodeBounds.getMinX() + getKeyboardPopup().getWidth() > screenBounds.getMaxX()) {
					getKeyboardPopup().setX(screenBounds.getMaxX() - getKeyboardPopup().getWidth());
					if(textNode.getId().equals("tfNumDocIdent")) {
						printBottomScreen = Boolean.TRUE;
						getKeyboardPopup().setX(0);
					}
				} else {
					if(textNode.getId().equals("tfNumTarjetaRegalo")) {
						getKeyboardPopup().setX(textNodeBounds.getMinX()-135);
						printBottomScreen = Boolean.FALSE;
					}
					else if(textNode.getId().equals("tfInsertarEmail")){
						getKeyboardPopup().setX(textNodeBounds.getMinX());
						printBottomScreen = Boolean.FALSE;
					}
					else {
						getKeyboardPopup().setX(textNodeBounds.getMinX());						
					}
				}
				
				if (textNodeBounds.getMaxY() + getKeyboardPopup().getHeight() > screenBounds.getMaxY()) {
					getKeyboardPopup().setY(textNodeBounds.getMinY() - getKeyboardPopup().getHeight() - 40);
					
					if(textNode.getId().equals("tfNumDocIdent")) {
						getKeyboardPopup().setY(textNodeBounds.getMaxY() + 20);
					}
				} else {
					if(textNode.getId().equals("tfNumTarjetaRegalo") || textNode.getId().equals("tfInsertarEmail")) {
						getKeyboardPopup().setY(textNodeBounds.getMaxY() + 10);
						printBottomScreen = Boolean.FALSE;
					}
//					else if(textNode.getId().equals("tfInsertarEmail")) {
//						getKeyboardPopup().setY(textNodeBounds.getMaxY() + 10);
//						printBottomScreen = Boolean.FALSE;
//					}
					else {
						getKeyboardPopup().setY(textNodeBounds.getMaxY() + 20);						
					}
				}

				if(printBottomScreen) {
					getKeyboardPopup().show(window);
					double widthKeyboard = getKeyboardPopup().getWidth();
					double x = (screenBounds.getWidth() - widthKeyboard) / 2;
					getKeyboardPopup().setX(x);
					getKeyboardPopup().setY(window.getY() + window.getHeight());
				}
			}
		}
	}

	protected KeyBoardPopup createKeyBoardPopup() throws IOException, URISyntaxException {
		Locale locale = POSApplication.getInstance().getLocale();
		
		//Buscamos el recurso para el idioma concreto
		URL skinResource = POSApplication.getInstance().getSkinResource("/skins/" + AppConfig.skin + "/com/comerzzia/pos/core/gui/componentes/keyboard/" + locale.getLanguage());
		if (skinResource == null) {
			skinResource = POSApplication.class.getResource("/skins/" + AppConfig.DEFAULT_SKIN + "/com/comerzzia/pos/core/gui/componentes/keyboard/");
		}
		
		//Si no existe usaremos idioma inglés
    	if (skinResource == null) {
    		locale = Locale.UK;
    		skinResource = POSApplication.getInstance().getSkinResource("com/comerzzia/pos/core/gui/componentes/keyboard/" + locale.getLanguage());
    	}
		
    	Path path = null;
    	String resourceStr = skinResource.toURI().toString();
		if (resourceStr.startsWith("jar")) {
			File tempDir = createTempDirectory();
			tempDir.deleteOnExit();
    		path = tempDir.toPath();
    		String substring = resourceStr.substring(resourceStr.indexOf("!")+1, resourceStr.lastIndexOf("/"));
    		copyFromJar(substring, skinResource.toURI(), path);    			

    	} else {
    		File file = urlToFile(skinResource);
    		path = file.getParentFile().toPath();
    	}
				
		KeyBoardPopup popup = SelfcheckoutKeyboardPopupBuilder.create()
        		.initScale(escalaTeclado)
        		.initLocale(locale)
        		.addIRobot(new FXRobotHandler())
        		.layerPath(path)
        		.layer(DefaultLayer.NUMBLOCK)
        		.build();

//		popup.getKeyBoard().getStylesheets();
//		popup.getKeyBoard().getStylesheets().clear();
			
		return popup;
    }

}
