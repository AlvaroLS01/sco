package com.comerzzia.brico.pos.selfcheckout.services.impresion;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.SelfCheckoutTicketManager;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.core.util.base64.Base64Coder;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.impresora.IPrinter;
import com.comerzzia.pos.dispositivo.impresora.ImpresoraHTML;
import com.comerzzia.pos.dispositivo.impresora.ImpresoraPantalla;
import com.comerzzia.pos.dispositivo.impresora.parser.PrintParser;
import com.comerzzia.pos.dispositivo.impresora.parser.PrintParserException;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.documentos.Documentos;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.sesion.SesionCaja;
import com.comerzzia.pos.services.core.sesion.SesionImpuestos;
import com.comerzzia.pos.services.core.sesion.SesionUsuario;
import com.comerzzia.pos.services.devices.DeviceException;
import com.comerzzia.pos.services.ticket.ITicket;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.pagos.tarjeta.DatosPeticionPagoTarjeta;
import com.comerzzia.pos.services.ticket.pagos.tarjeta.DatosRespuestaPagoTarjeta;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.servicios.impresion.util.ModificadorXmlImpresion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.text.TextUtils;
import com.comerzzia.pos.velocity.VelocityException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SelfCheckoutServicioImpresion {

	private static final Logger log = Logger.getLogger(ServicioImpresion.class.getName());
    private static final String PATH_PLANTILLAS = "./plantillas/";

    public static final String PLANTILLA_TICKET = "factura_simplificada";
    public static final String PLANTILLAS_CIERRE_CAJA = "cierrecaja";
    public static final String PLANTILLA_TICKET_REGALO = "ticket_regalo";
    public static final String PLANTILLA_MOVIMIENTO_CAJA = "movimiento_caja";
    
    private static final String COD_PAIS_PORTUGAL = "PT";
	private static final String COD_PAIS_ESPANA = "ES";

    private static final Map<String, Template> templateCache = new HashMap<>();
    private static VelocityEngine velocityEngine = null;

    public static void imprimir(String plantilla, String idObjeto, Object objeto) throws DeviceException {
        HashMap<String, Object> parametros = new HashMap<String, Object>(1);
        parametros.put(idObjeto, objeto);
        imprimir(plantilla, parametros);
    }

    public static void imprimir(String plantilla, Map<String, Object> parametros) throws DeviceException {
        IPrinter printer = null;
        imprimir(printer, plantilla, parametros);
    }

    public static void imprimir(IPrinter printer, String plantilla, Map<String, Object> parametros) throws DeviceException {
        Sesion sesion = SpringContext.getBean(Sesion.class);
        PrintParser parser = null;
        try {
            parser = new PrintParser();

            parametros.put("salida", (printer instanceof ImpresoraPantalla ? "pantalla" : "impresora"));
            parametros.put("cajaAbierta", sesion.getSesionCaja().getCajaAbierta());
            parametros.put("aplicacion", sesion.getAplicacion());
            parametros.put("usuario", sesion.getSesionUsuario().getUsuario());

            parser.setParametros(parametros);
            
            reemplazarCaracteresConflictivosTicket(parametros);
			log.debug("imprimir() - Inicio impresión: plantilla=" + plantilla + ", parámetros=" + parametros.toString() + ", impresora=" + (printer != null ? printer.toString() : "default"));

           if(printer != null) {
               log.debug("imprimir() - Detalles de la impresora proporcionada: " + printer.toString());
           } else {
               log.debug("imprimir() - No se proporcionó impresora, se usará la impresora por defecto.");
           }
            
            //String documento a imprimir
            String plantillaRellena = getPrintDocument(parametros, plantilla);
            
            IPrinter printerTest = printer;
            if(printerTest == null) {
            	printerTest = Dispositivos.getInstance().getImpresora1();
            }

            if(printerTest.isReady()) {
            	parser.print(plantillaRellena, printer);
            }
            else {
            	List<String> errors = printerTest.getAvailabilityErrors();
            	
            	String textoError = "";
            	
            	if(errors != null && !errors.isEmpty()) {
	            	for(String error : errors) {
	            		textoError = textoError + error + System.lineSeparator();
	            	}
            	}
            	
            	throw new DeviceException(textoError);
            }

            parametros.remove("salida");
            parametros.remove("cajaAbierta");
            parametros.remove("aplicacion");
            parametros.remove("usuario");
        } catch (PrintParserException e) {
            log.error("imprimir() - Error imprimiendo documento: " + e.getMessage(), e);
            throw new DeviceException(e);
        } catch (VelocityException e) {
            log.error("imprimir() - Error imprimiendo documento: " + e.getMessage(), e);
            throw new DeviceException(e);
        } catch (Throwable e) {
            log.error("imprimir() - Error imprimiendo documento: " + e.getMessage(), e);
            throw new DeviceException(e);
        } finally {
            try {
                parser.cerrarImpresoras();
            } catch (Exception e) {
            }
        }
    }

	protected static void reemplazarCaracteresConflictivosTicket(Map<String, Object> parametros) {
	    Object ticket = parametros.get("ticket");
	    if(ticket != null) {
	    	SpringContext.getBean(ModificadorXmlImpresion.class).reemplazarCaracteresConflictivosObjeto(ticket);
	    }
    }

	public static String imprimirPantalla(String plantilla, Map<String, Object> parametros) throws DeviceException {
        log.debug("imprimirPantalla() - Previsualizando por pantalla: " + plantilla);
        ImpresoraHTML htmlPrinter = ImpresoraHTML.getInstance();
        htmlPrinter.inicializar();

        htmlPrinter.setPrevisualizacion(true);
        imprimir(htmlPrinter, plantilla, parametros);
        htmlPrinter.setPrevisualizacion(false);

        return htmlPrinter.getPrevisualizacion();
    }

    public static String getPrintDocument(Map<String, Object> parametros, String plantilla) throws VelocityException {
        try {
            log.debug("getPrintDocument() - Generando documento a partir de plantilla velocity: " + plantilla);

            SesionImpuestos sesionImpuestos = SpringContext.getBean(SesionImpuestos.class); 
         	SesionCaja sesionCaja = SpringContext.getBean(SesionCaja.class); 
         	SesionUsuario sesionUsuario = SpringContext.getBean(SesionUsuario.class); 
            //Buscamos la plantilla. Primero con el locale y si no existe, por defecto.
            Template template = getTemplate(plantilla);

            Context context = new VelocityContext();
            // Pasamos a la plantilla los parámetros que la alimentan
            context.put("esc", new EscapeTool());
            context.put("fmt", FormatUtil.getInstance());
            context.put("sesionImpuestos", sesionImpuestos); 
         	context.put("sesionCaja", sesionCaja); 
         	context.put("sesionUsuario", sesionUsuario);
         	context.put("textUtils", TextUtils.getInstance());
         	context.put("base64Coder", new Base64Coder(Base64Coder.UTF8));
            for (String key : parametros.keySet()) {
                context.put(key, parametros.get(key));
            }

            // Aplicamos a la plantilla las variables
            try (StringWriter writer = new StringWriter()) {
                template.merge(context, writer);
                return writer.toString();
            }
        } catch (Exception e) {
            log.error("getPrintDocument() - Error generando documento con velocity: " + e.getMessage(), e);
            throw new VelocityException(e);
        }
    }

    protected static Template getTemplate(String plantilla) throws Exception {
        Template template = templateCache.get(plantilla);
        if (template != null && !AppConfig.modoDesarrollo) {
            log.debug("getTemplate() - Devolviendo plantilla cacheada para " + plantilla);
            return template;
        }

        long time = System.currentTimeMillis();

        VelocityEngine ve = getVelocityEngine();
        String plantillaPais = null;
        try {
        	/* Para el SCO se usa el codPais en lugar del idioma */
            plantillaPais = PATH_PLANTILLAS + plantilla + "_" + AppConfig.pais.toUpperCase() + ".xml";
        } catch (Exception e) {
            log.error("getPrintDocument() - Ha habido un error al obtener el país de la tienda: " + e.getMessage(), e);
        }
      
        String plantillaDefault = PATH_PLANTILLAS + plantilla + ".xml";

        if (plantillaPais != null && ve.resourceExists(plantillaPais)) {
            log.debug("getPrintDocument() - Usando: " + plantillaPais);
            template = ve.getTemplate(plantillaPais, "UTF-8");
        }else {
            log.debug("getPrintDocument() - No existe plantilla " + plantillaPais + ". Se usará plantilla por defecto. ");
            log.debug("getPrintDocument() - Usando: " + plantillaDefault);
            template = ve.getTemplate(plantillaDefault, "UTF-8");
        }

        Long timeEllapsed = System.currentTimeMillis() - time;
        log.debug(String.format("getTemplate() - Plantilla '%s' cargada en %sms", plantilla, timeEllapsed.toString()));

        templateCache.put(plantilla, template);
        return template;
    }

    protected static VelocityEngine getVelocityEngine() throws Exception {
        if (velocityEngine != null) {
            return velocityEngine;
        }

        velocityEngine = new VelocityEngine();

        // Establecemos propiedades
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", log.getName());
        velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.Log4JLogChute");
        velocityEngine.setProperty("velocimacro.library", "plantillas/velocimacros.vm");

        // Inicializamos
        velocityEngine.init();
        return velocityEngine;
    }

    public static class EscapeTool {

        public String xml(Object string) {
            if (string == null) return null;
            return StringEscapeUtils.escapeXml(string.toString());
        }

    }
    
    public static Map<String, Object> setearParametrosImpresion(boolean hayPagosTarjeta, ITicket ticket, Sesion sesion) throws DocumentoException, Exception, IOException {
		Map<String, Object> mapaParametros = new HashMap<String, Object>();
		mapaParametros.put("ticket", ticket);

		if (hayPagosTarjeta) {
			mapaParametros.put("listaPagosTarjeta", getPagosTarjetas(ticket));
			mapaParametros.put("listaPagosTarjetaDatosPeticion", getPagosTarjetasDatosPeticion(ticket));
		}
		
		FidelizacionBean datosFidelizado = ticket.getCabecera().getDatosFidelizado();
//		mapaParametros.put("paperLess", datosFidelizado != null && datosFidelizado.getPaperLess() != null && datosFidelizado.getPaperLess());
		if (ticket.getCabecera().getCodTipoDocumento().equals(sesion.getAplicacion().getDocumentos().getDocumento(Documentos.FACTURA_COMPLETA).getCodtipodocumento())) {
			mapaParametros.put("empresa", sesion.getAplicacion().getEmpresa());
		}
		
		addQR(ticket, mapaParametros);
		aniadirLogoParametrosImprimir(mapaParametros, sesion);

		if (sesion.getAplicacion().getTienda().getCliente().getCodpais().equals(COD_PAIS_PORTUGAL)) {
			AppConfig.pais = "pt";
			aniadirLogoParametrosImprimir(mapaParametros, sesion);
			addQR(ticket, mapaParametros);
		}
		else if (sesion.getAplicacion().getTienda().getCliente().getCodpais().equals(COD_PAIS_ESPANA) && ticket.getCabecera().getCodTipoDocumento().equals("NC")) {
			mapaParametros.put("esCopia", true);
			aniadirLogoParametrosImprimir(mapaParametros, sesion);
		}
		mapaParametros.put("esGestionTicket", false);
		
		return mapaParametros;
	}
    

	protected static List<DatosRespuestaPagoTarjeta> getPagosTarjetas(ITicket ticket) {
		log.debug("getPagosTarjetas");
		List<DatosRespuestaPagoTarjeta> listaPagosTarjeta = new ArrayList<DatosRespuestaPagoTarjeta>();
		List<PagoTicket> listaPagos = ticket.getPagos();
		for (PagoTicket pago : listaPagos) {
			if (pago.getDatosRespuestaPagoTarjeta() != null) {
				DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta = pago.getDatosRespuestaPagoTarjeta();
				listaPagosTarjeta.add(datosRespuestaPagoTarjeta);
			}
		}
		return listaPagosTarjeta;
	}


	protected static List<DatosPeticionPagoTarjeta> getPagosTarjetasDatosPeticion(ITicket ticket) {
		log.debug("getPagosTarjetasDatosPeticion()");
		List<DatosPeticionPagoTarjeta> listaPagosTarjeta = new ArrayList<DatosPeticionPagoTarjeta>();
		List<PagoTicket> listaPagos = ticket.getPagos();
		for (PagoTicket pago : listaPagos) {
			if (pago.getDatosRespuestaPagoTarjeta() != null) {
				DatosRespuestaPagoTarjeta datosRespuestaPagoTarjeta = pago.getDatosRespuestaPagoTarjeta();
				DatosPeticionPagoTarjeta datosPeticion = datosRespuestaPagoTarjeta.getDatosPeticion();
				listaPagosTarjeta.add(datosPeticion);
			}
		}
		return listaPagosTarjeta;
	}


	private static void aniadirLogoParametrosImprimir(Map<String, Object> mapaParametros, Sesion sesion) throws IOException {
		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			InputStream is = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			mapaParametros.put("LOGO", is);
			is.close();
		}
	}

	private static void addQR(ITicket ticketOrigen, Map<String, Object> parameters) throws Exception, IOException {
		if (ticketOrigen.getCabecera() instanceof BricodepotCabeceraTicket) {
			log.debug("addQr() - La información fiscal ya viene en el ticket.");

			if (ticketOrigen.getCabecera().getFiscalData() != null) {
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

	private static BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);

		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
}
