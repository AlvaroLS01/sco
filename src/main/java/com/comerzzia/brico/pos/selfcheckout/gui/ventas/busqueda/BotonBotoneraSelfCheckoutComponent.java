package com.comerzzia.brico.pos.selfcheckout.gui.ventas.busqueda;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.botonaccion.normal.BotonBotoneraNormalComponent;
import com.comerzzia.pos.util.i18n.I18N;

public class BotonBotoneraSelfCheckoutComponent extends BotonBotoneraNormalComponent {
	
	// Componentes internos de botón
	private HBox linea1, linea2, linea3, linea4;

	private ImageView imagen;
	private Label lbTexto;
	private Label lbTexto2;
	private Label lbTeclaAccesoRapido;
	
	private final String GIFTCARDVALE = "GIFTCARD/VALE";

	/**
	 * Constructor de botón con imagen y dos etiquetas
	 */
	public BotonBotoneraSelfCheckoutComponent() {
		super();

		panelBoton.setMinHeight(80.0);
		panelBoton.setMaxHeight(80.0);
		panelBoton.setMinWidth(60.0);
		panelBoton.setMaxWidth(400.0);

		btAccion.setMinHeight(80.0);
		btAccion.setMaxHeight(80.0);
		btAccion.setMinWidth(60.0);
		btAccion.setMaxWidth(400.0);

		linea1 = new HBox();
		linea2 = new HBox();
		linea3 = new HBox();

		imagen = new ImageView();
		imagen.setFitHeight(40);
		imagen.setFitWidth(40);
		imagen.setLayoutX(10);

		lbTexto = new Label();
		lbTexto.setAlignment(Pos.CENTER);
		lbTexto.setContentDisplay(ContentDisplay.CENTER);

		lbTeclaAccesoRapido = new Label();
		lbTeclaAccesoRapido.setAlignment(Pos.CENTER);
		lbTeclaAccesoRapido.setContentDisplay(ContentDisplay.CENTER);

		linea1.getChildren().add(imagen);
		linea1.setAlignment(Pos.CENTER);
		linea2.getChildren().add(lbTeclaAccesoRapido);
		linea2.setAlignment(Pos.CENTER);
		linea3.getChildren().add(lbTexto);
		linea3.setAlignment(Pos.CENTER);

		HBox.setHgrow(imagen, Priority.ALWAYS);
		HBox.setHgrow(lbTexto, Priority.ALWAYS);
		HBox.setHgrow(lbTeclaAccesoRapido, Priority.ALWAYS);

		panelInterno.setMinHeight(80.0);
		panelInterno.setMaxHeight(80.0);
		panelInterno.setMinWidth(60.0);
		panelInterno.setMaxWidth(400.0);
		panelInterno.setPickOnBounds(true);
		panelInterno.getChildren().addAll(linea1, linea2, linea3);

		AnchorPane.setTopAnchor(linea1, 0.0);
		AnchorPane.setBottomAnchor(linea1, 0.0);
		AnchorPane.setLeftAnchor(linea1, 0.0);
		AnchorPane.setRightAnchor(linea1, 0.0);

		AnchorPane.setTopAnchor(linea2, 45.0);
		AnchorPane.setBottomAnchor(linea2, 15.0);
		AnchorPane.setLeftAnchor(linea2, 0.0);
		AnchorPane.setRightAnchor(linea2, 0.0);

		AnchorPane.setTopAnchor(linea3, 60.0);
		AnchorPane.setBottomAnchor(linea3, 0.0);
		AnchorPane.setLeftAnchor(linea3, 0.0);
		AnchorPane.setRightAnchor(linea3, 0.0);

		AnchorPane.setTopAnchor(btAccion, 0.0);
		AnchorPane.setBottomAnchor(btAccion, 0.0);
		AnchorPane.setLeftAnchor(btAccion, 0.0);
		AnchorPane.setRightAnchor(btAccion, 0.0);

		btAccion.setPickOnBounds(true);

		AnchorPane.setTopAnchor(this, 0.0d);
		AnchorPane.setBottomAnchor(this, 0.0d);
		AnchorPane.setLeftAnchor(this, 0.0d);
		AnchorPane.setRightAnchor(this, 0.0d);
		AnchorPane.setTopAnchor(panelBoton, 0.0d);
		AnchorPane.setBottomAnchor(panelBoton, 0.0d);
		AnchorPane.setLeftAnchor(panelBoton, 0.0d);
		AnchorPane.setRightAnchor(panelBoton, 0.0d);
		AnchorPane.setTopAnchor(panelInterno, 0.0d);
		AnchorPane.setBottomAnchor(panelInterno, 0.0d);
		AnchorPane.setLeftAnchor(panelInterno, 0.0d);
		AnchorPane.setRightAnchor(panelInterno, 0.0d);

		btAccion.setGraphic(panelInterno);
		
		lbTexto.getStyleClass().add("lb-boton-compuesto");
	}

	@Override
	protected void inicializaComponentesPersonalizados(Double ancho, Double alto) {

		String rutaImagen = configuracion.getRutaImagen();
		if (rutaImagen != null) {
			Image image = POSApplication.getInstance().createImage(rutaImagen);
			
			imagen.setImage(image);
			imagen.setFitWidth(ancho);
			imagen.setFitHeight(alto);
		}

		// Buscamos el texto del botón en las properties
		if (GIFTCARDVALE.equals(configuracion.getTexto())) {
			lbTexto.setText(I18N.getTexto("GIFTCARD"));
			lbTexto.setPrefWidth(ancho);
			
			lbTexto2 = new Label();
			lbTexto2.setAlignment(Pos.CENTER);
			lbTexto2.setContentDisplay(ContentDisplay.CENTER);
			lbTexto2.getStyleClass().add("lb-boton-compuesto");
			
			HBox.setHgrow(lbTexto2, Priority.ALWAYS);
			
			linea4 = new HBox();
			linea4.getChildren().add(lbTexto2);
			linea4.setAlignment(Pos.CENTER);
			
			AnchorPane.setTopAnchor(linea1, -20.0);
			AnchorPane.setBottomAnchor(linea1, 0.0);
			AnchorPane.setLeftAnchor(linea1, 0.0);
			AnchorPane.setRightAnchor(linea1, 0.0);

			AnchorPane.setTopAnchor(linea3, 50.0);
			AnchorPane.setBottomAnchor(linea3, 0.0);
			AnchorPane.setLeftAnchor(linea3, 0.0);
			AnchorPane.setRightAnchor(linea3, 0.0);

			AnchorPane.setTopAnchor(linea4, 80.0);
			AnchorPane.setBottomAnchor(linea4, 0.0);
			AnchorPane.setLeftAnchor(linea4, 0.0);
			AnchorPane.setRightAnchor(linea4, 0.0);
			
			panelInterno.getChildren().add(linea4);
			
			lbTexto2.setText(I18N.getTexto("VALE"));
			lbTexto2.setPrefWidth(ancho);
		}
		else {
			lbTexto.setText(I18N.getTexto(configuracion.getTexto()));
			lbTexto.setPrefWidth(ancho);
		}
		
		

		// Establecemos el texto de acceso rápido
		lbTeclaAccesoRapido.setText(configuracion.getTeclaAccesoRapido());
		
		if(configuracion.getTipo().equals("navegarCategoria")) {
			btAccion.getStyleClass().add("bt-categoria");
		}
		else {
			btAccion.getStyleClass().add("bt-articulo");
		}
	}
	
	

}
