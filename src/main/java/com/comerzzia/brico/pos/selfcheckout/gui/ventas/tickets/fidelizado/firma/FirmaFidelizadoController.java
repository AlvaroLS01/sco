package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.firma;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.controllers.Controller;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

@Primary
@Component
public class FirmaFidelizadoController extends Controller {

	protected static final Logger log = Logger.getLogger(FirmaFidelizadoController.class.getName());

	public static final String FIRMA = "firmaFidelizado";

	private GraphicsContext gcB, gcF;
	double startX, startY, lastX, lastY, oldX, oldY;
	double hg;
	
	@FXML
	protected Button btFirmar, btBorrarFirma;
	
	@FXML
	protected Label lbTitulo;

	@FXML
	private Canvas TheCanvas, canvasGo;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		gcB = TheCanvas.getGraphicsContext2D();
		gcF = canvasGo.getGraphicsContext2D();
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {

	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		refrescarDatosPantalla();
		configurarIdioma();
	}

	@Override
	public void initializeFocus() {

	}

	@FXML
	private void onMousePressedListener(MouseEvent e) {
		this.startX = e.getX();
		this.startY = e.getY();
		this.oldX = e.getX();
		this.oldY = e.getY();
	}

	@FXML
	private void onMouseDraggedListener(MouseEvent e) {
		this.lastX = e.getX();
		this.lastY = e.getY();

		freeDrawing();
	}

	@FXML
	private void onMouseReleaseListener(MouseEvent e) {
	}

	@FXML
	private void onMouseExitedListener(MouseEvent event) {
	}

	private void freeDrawing() {
		gcB.setStroke(Color.BLACK);
		gcB.strokeLine(oldX, oldY, lastX, lastY);
		oldX = lastX;
		oldY = lastY;
	}

	@FXML
	public void accionFirmar() {
		log.debug("accionFirmar()");
		SnapshotParameters parameters = new SnapshotParameters();
		WritableImage wi = new WritableImage(800, 480);

		WritableImage snapshot = TheCanvas.snapshot(parameters, wi);
		try {
			BufferedImage bImage = SwingFXUtils.fromFXImage(snapshot, null);
			ByteArrayOutputStream s = new ByteArrayOutputStream();
			ImageIO.write(bImage, "png", s);
			byte[] res = s.toByteArray();
			getDatos().put(FIRMA, res);
			s.close();
		}
		catch (IOException e) {
			log.debug("accionFirmar() - Ha ocurrido un error al realizar el parse de WritableImage a byte[]." + e.getMessage());
		}

		getStage().close();

	}

	@FXML
	public void accionBorrarFirma() {
		log.debug("accionBorrarFirma()");

		gcB.clearRect(0, 0, TheCanvas.getWidth(), TheCanvas.getHeight());
		gcF.clearRect(0, 0, TheCanvas.getWidth(), TheCanvas.getHeight());
	}

	public void refrescarDatosPantalla() {
		log.debug("refrescarDatosPantalla()");
		accionBorrarFirma();
	}
	
	private void configurarIdioma() {
		btFirmar.setText(I18N.getTexto("Aceptar"));
		btBorrarFirma.setText(I18N.getTexto("Borrar Firma"));
		lbTitulo.setText(I18N.getTexto("Firme aquí"));
	}
}
