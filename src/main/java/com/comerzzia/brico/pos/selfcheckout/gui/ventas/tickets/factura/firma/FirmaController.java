package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.firma;

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

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

@Primary
@Component
public class FirmaController extends Controller{
	
	protected static final Logger log = Logger.getLogger(FirmaController.class.getName());

	public static final String FIRMA = "firmaCliente";
	
	private GraphicsContext gcB, gcF;
	double startX, startY, lastX, lastY, oldX, oldY;
	double hg;
	
	@FXML
	private Canvas TheCanvas, canvasGo;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		gcB = TheCanvas.getGraphicsContext2D();
		gcF = canvasGo.getGraphicsContext2D();
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		refrescarDatosPantalla();
	}

	@Override
	public void initializeFocus() {
		// TODO Auto-generated method stub
		
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

}
