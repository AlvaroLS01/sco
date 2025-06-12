package com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.simboloCargando;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import org.apache.log4j.Logger;

import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SCOVentanaCargando extends Stage {

    private static Logger log = Logger.getLogger(SCOVentanaCargando.class);

    static SCOVentanaCargando ventana;
    protected Scene scene;
    protected Timer timer;
    protected int timeoutVentana = 0;
    protected BorderPane panelInterno;
    protected VBox vBoxMensaje;
    protected static Label lbMensaje = new Label();
    protected static Label lbMensaje2 = new Label();
//    protected static ImageView imageView = new ImageView();

    public static void crearVentanaCargando(Stage stageOwner) {
        ventana = new SCOVentanaCargando();
        ventana.initOwner(stageOwner);
        ventana.setResizable(false);

        /* Timer */
        ventana.timer = new Timer(10000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Platform.runLater(() -> {
                    if (ventana.isShowing()) {
                        ventana.timeoutVentana++;
                        log.warn("Ventana progreso activa (" + ventana.timeoutVentana + ")");
                    }
                    
//                    if(ventana.timeoutVentana >= 10) {
//                    	SCOVentanaCargando.cerrar();
//                    }
                });
            }
        });

        /* Configuración de la ventana */
        ventana.centerOnScreen();
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setIconified(false);
        ventana.initStyle(StageStyle.TRANSPARENT);

        /* Configuración de la imagen */
        String imagePath = "pinpadbrico.png";
        Image imagen = new Image(SCOVentanaCargando.class.getResourceAsStream(imagePath));
//        imageView.setImage(imagen);
//        imageView.setFitWidth(100);
//        imageView.setFitHeight(100);

        /* Configuración del panel superior para el mensaje */
        VBox vBoxTop = new VBox();
        vBoxTop.setStyle("-fx-background-color: red; -fx-alignment: center; -fx-background-radius: 5 5 0 0; -fx-border-width:5;");
        lbMensaje.setText(I18N.getTexto("Utiliza el Pin Pad para completar el pago"));
//        lbMensaje.setText("Utiliza el Pin Pad para completar el pago");
        lbMensaje.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        vBoxTop.getChildren().add(lbMensaje);
        vBoxTop.setPadding(new Insets(10));

        /* Configuración del mensaje y la imagen dentro de un VBox */
        VBox vBoxCenter = new VBox(20);
        vBoxCenter.setAlignment(Pos.CENTER);
        lbMensaje2.setText(I18N.getTexto("Por favor, sigue las instrucciones del Pin Pad"));
        lbMensaje2.setStyle("-fx-font-size: 16px; ");
//        vBoxCenter.getChildren().addAll(lbMensaje2, imageView);

        /* Ajustes finales del panel interno */
        ventana.panelInterno = new BorderPane();
        ventana.panelInterno.setTop(vBoxTop);
        ventana.panelInterno.setCenter(vBoxCenter);
        ventana.panelInterno.setStyle("-fx-background-color: white; -fx-background-radius: 10 10 10 10; -fx-border-width:1; -fx-border-color: rgba(160,160,160); -fx-border-radius: 5px");

        ventana.scene = new Scene(ventana.panelInterno, 600, 200);
        ventana.scene.setFill(Color.TRANSPARENT);
        ventana.setScene(ventana.scene);

        // Iniciar el timer
        ventana.timer.start();
    }

    public static void mostrar() {
        if (ventana != null) {
//            Platform.runLater(() -> {
                ventana.show();
//            });
        }
    }

    public static void cerrar() {
        if (ventana != null) {
//            Platform.runLater(() -> {
                ventana.timer.stop();
                ventana.close();
//            });
        }
    }
}