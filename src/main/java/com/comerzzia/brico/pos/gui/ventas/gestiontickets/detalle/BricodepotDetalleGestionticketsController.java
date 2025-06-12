package com.comerzzia.brico.pos.gui.ventas.gestiontickets.detalle;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.services.impresion.SelfCheckoutServicioImpresion;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.SelfCheckoutTicketVentaAbono;
import com.comerzzia.core.util.base64.Base64Coder;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.gui.ventas.gestiontickets.detalle.DetalleGestionticketsController;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.tickets.TicketBean;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.ticket.ITicket;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.cabecera.CabeceraTicket;
import com.comerzzia.pos.services.ticket.cabecera.TotalesTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.profesional.TotalesTicketProfesional;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.i18n.I18N;
import com.comerzzia.pos.util.xml.MarshallUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.event.ActionEvent;

@Component
@Primary
public class BricodepotDetalleGestionticketsController extends DetalleGestionticketsController {

	private static final Logger log = Logger.getLogger(BricodepotDetalleGestionticketsController.class.getName());
	
	@SuppressWarnings("rawtypes")
	@Override
	public void refrescarDatosPantalla() throws InitializeGuiException {
        try {
            log.debug("refrescarDatosPantalla()");

            log.debug("Obtenemos el XML del ticket que queremos visualizar");
            
            this.ticket = tickets.get(posicionActual);

            TicketBean ticketConsultado = null;
            byte[] ticketXML = null;

        	ticketConsultado = ticketsService.consultarTicket(ticket.getUidTicket(), sesion.getAplicacion().getUidActividad());
        	ticketXML = ticketConsultado.getTicket();
            	
            TipoDocumentoBean documento = sesion.getAplicacion().getDocumentos().getDocumento(ticketConsultado.getIdTipoDocumento());
            if(documento.getFormatoImpresion().equals(TipoDocumentoBean.PROPIEDAD_FORMATO_IMPRESION_NO_CONFIGURADO)){
            	if(getStage() != null && getStage().isShowing()){
            		VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("No es posible visualizar este tipo de documento"), getStage());
            	}else{
            		Platform.runLater(new Runnable() {
						@Override
						public void run() {
							VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("No es posible visualizar este tipo de documento"), getStage());
						}
					});
            	}
            	setTicketText("<html><body></body></html>");
            	return;
            }
            
            ticketOperacion = (TicketVenta) MarshallUtil.leerXML(ticketXML, getTicketClasses(documento).toArray(new Class[] {}));
            
			if (ticketOperacion != null) {
				ticketOperacion.getCabecera().setDocumento(sesion.getAplicacion().getDocumentos().getDocumento(ticketOperacion.getCabecera().getTipoDocumento()));
				if (sesion.getAplicacion().getDocumentos().getDocumento(ticketOperacion.getCabecera().getTipoDocumento()).getPermiteTicketRegalo()) {
					btnTicketRegalo.setDisable(false);
				}
				else {
					btnTicketRegalo.setDisable(true);
				}
				try {
					Map<String, Object> mapaParametros = new HashMap<String, Object>();
					mapaParametros.put("ticket", (SelfCheckoutTicketVentaAbono) ticketOperacion);
					mapaParametros.put("BRICO_CABECERA", (BricodepotCabeceraTicket) ticketOperacion.getCabecera());
					mapaParametros.put("urlQR", variablesService.getVariableAsString("TPV.URL_VISOR_DOCUMENTOS"));
					mapaParametros.put("esGestion", true);

					// Hay que obtener el resultado de mostrar en pantalla el ticket y mostrarlo en taTicket
					addQR(ticketOperacion, mapaParametros);
					aniadirLogoParametrosImprimir(mapaParametros);
					
					//Se añade para evitar la impresion del A4 en la previsualizacion
					mapaParametros.put("esImpresionA4", false);

					String previsualizacion = ServicioImpresion.imprimirPantalla(ticketOperacion.getCabecera().getFormatoImpresion(), mapaParametros);
					setTicketText(previsualizacion);
				}
				catch (Exception e) {
					VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Lo sentimos, ha ocurrido un error al imprimir."), e);
					throw new InitializeGuiException(false);
        		}
            }
            else {
                log.error("refrescarDatosPantalla()- Error leyendo ticket otriginal");
                VentanaDialogoComponent.crearVentanaError(I18N.getTexto("Error leyendo información de ticket."), getStage());
                throw new InitializeGuiException(false);
            }

        }
        catch (TicketsServiceException ex) {
            log.error("refrescarDatosPantalla() - " + ex.getMessage());
            VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Error leyendo información de ticket"), ex);
        } catch (DocumentoException e) {
			log.error("Error recuperando el tipo de documento del ticket.",e);
		}
    }
	
	public List<Class<?>> getTicketClasses(TipoDocumentoBean tipoDocumento) {
		List<Class<?>> classes = new LinkedList<>();
		
		// Obtenemos la clase root
		Class<?> clazz = SpringContext.getBean(getTicketClass(tipoDocumento)).getClass();
		
		// Generamos lista de clases "ancestras" de la principal
		Class<?> superClass = clazz.getSuperclass();
		while (!superClass.equals(Object.class)) {
			classes.add(superClass);
			superClass = superClass.getSuperclass();
		}
		// Las ordenamos descendentemente
		Collections.reverse(classes);
		
		//Añadimos la clase principal y otras necesarias
		classes.add(clazz);
		classes.add(SpringContext.getBean(LineaTicket.class).getClass());
		classes.add(SpringContext.getBean(CabeceraTicket.class).getClass());
		classes.add(SpringContext.getBean(TotalesTicket.class).getClass());
		classes.add(SpringContext.getBean(TotalesTicketProfesional.class).getClass());

		return classes;
	}
	
	private void aniadirLogoParametrosImprimir(Map<String, Object> mapaParametros) throws IOException {
		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			InputStream is = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			mapaParametros.put("LOGO", is);
			is.close();
		}
	}

	@SuppressWarnings("rawtypes")
	private void addQR(ITicket ticketOrigen, Map<String, Object> parameters) throws Exception, IOException {
		if (ticketOrigen.getCabecera() instanceof BricodepotCabeceraTicket) {

			if (ticketOrigen.getCabecera().getFiscalData() != null) {
				log.debug("addQr() - La información fiscal ya viene en el ticket.");
				if (!ticketOrigen.getCabecera().getFiscalData().getProperties().isEmpty() && ticketOrigen.getCabecera().getFiscalData().getProperty(SelfCheckoutTicketManager.PROPERTY_QR) != null) {

					String data = ticketOrigen.getCabecera().getFiscalData().getProperty(SelfCheckoutTicketManager.PROPERTY_QR).getValue();

					log.debug("refrescarDatosPantalla() - Generando imagen del QR de Portugal");

					Base64Coder coder = new Base64Coder(Base64Coder.UTF8);
					String qr = coder.decodeBase64(data);
					BufferedImage qrImage = generateQRCodeImage(qr);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ImageIO.write(qrImage, "jpeg", os);
					InputStream is = new ByteArrayInputStream(os.toByteArray());
					parameters.put("QR_PORTUGAL", is);
				}
			}
			else {
				log.debug("addQr() - La información fiscal no viene en el ticket.");
			}
		}
		else {
			log.debug("addQr() - La información fiscal no viene en el ticket.");
		}
	}

	private BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);

		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
	
	
	@Override
	protected void accionImprimirCopia(ActionEvent event) {
		log.debug("accionImprimirCopia()");
	      try {
            // Se reimprime la misma
    	  boolean hayPagosTarjeta = false;
			for (Object pago : ticketOperacion.getPagos()) {
				if (pago instanceof PagoTicket && ((PagoTicket) pago).getDatosRespuestaPagoTarjeta() != null) {
					hayPagosTarjeta = true;
					break;
				}
			}
			Map<String, Object> mapaParametros = SelfCheckoutServicioImpresion.setearParametrosImpresion(hayPagosTarjeta, ticketOperacion, sesion);

			if (ticketOperacion.getCabecera().getCodTipoDocumento().equals("FT")) {
				mapaParametros.put("esImpresionA4", true);
			}
			else {
				mapaParametros.put("esImpresionA4", false);
			}
			mapaParametros.put("esGestionTicket", true);
            mapaParametros.put("ticket",ticketOperacion);
            mapaParametros.put("urlQR", variablesService.getVariableAsString("TPV.URL_VISOR_DOCUMENTOS"));
            mapaParametros.put("esDuplicado", true);
            
            SelfCheckoutServicioImpresion.imprimir(ticketOperacion.getCabecera().getFormatoImpresion(), mapaParametros);
        }
        catch (Exception ex) {
            VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto("Fallo al imprimir ticket."), getStage());
        }
	}
}
