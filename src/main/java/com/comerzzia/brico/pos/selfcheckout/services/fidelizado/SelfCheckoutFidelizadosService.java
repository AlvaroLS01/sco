package com.comerzzia.brico.pos.selfcheckout.services.fidelizado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.api.model.loyalty.FidelizadoBean;
import com.comerzzia.api.model.loyalty.TarjetaBean;
import com.comerzzia.api.rest.client.exceptions.RestException;
import com.comerzzia.api.rest.client.exceptions.RestHttpException;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoDocumentoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoEmailRequestRest;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.fidelizado.IdentificacionFidelizadoController;
import com.comerzzia.core.util.fechas.Fecha;
import com.comerzzia.pos.persistence.tickets.TicketBean;
import com.comerzzia.pos.services.core.contadores.ServicioContadores;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.devices.DeviceException;
import com.comerzzia.pos.services.ticket.TicketsService;
import com.comerzzia.pos.servicios.impresion.ImpresionJasper;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;
import com.comerzzia.pos.util.xml.MarshallUtil;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.engine.util.JRLoader;
import rest.bean.enlace.Enlace;
import rest.client.fidelizados.enlaces.BricodepotEnlacesFidelizadosRest;

@Service
public class SelfCheckoutFidelizadosService {

	protected static final Logger log = Logger.getLogger(SelfCheckoutFidelizadosService.class.getName());

	public static final String ID_FIDELIZADO_CAP = "ID_FIDELIZADO_CAP";

	@Autowired
	protected Sesion sesion;
	@Autowired
	protected ServicioContadores contadoresService;
	@Autowired
	protected TicketsService ticketsService;
	@Autowired
	protected VariablesServices variablesServices;

	public void registrarTicketFidelizado(TicketFidelizadoCaptacion ticketFidelizado, FidelizadoBean fidelizado, byte[] firma) {
		TicketBean ticket;
		byte[] xmlTicketFidelizado = null;
		log.debug("registrarTicketFidelizado() - Registrando ticket de captación de fidelizado para el fidelizado " + fidelizado.getIdFidelizado());
		try {
			// Construimos objeto persistente
			ticket = new TicketBean();

			// uid documento
			String uidTicket = UUID.randomUUID().toString();
			ticket.setUidTicket(uidTicket);
			// id documento
			Long idTicket = contadoresService.obtenerValorContador(ID_FIDELIZADO_CAP, sesion.getAplicacion().getUidActividad());
			ticket.setIdTicket(idTicket);
			// serie documento
			String serieTicket = sesion.getAplicacion().getCodAlmacen() + "/" + sesion.getSesionCaja().getCajaAbierta().getCodCaja();
			ticket.setSerieTicket(serieTicket);
			// cod documento
			String codigoTicket = sesion.getAplicacion().getCodAlmacen() + "/" + sesion.getSesionCaja().getCajaAbierta().getCodCaja() + "/" + String.format("%08d", idTicket);
			ticket.setCodTicket(codigoTicket);
			// firma documento (no
			ticket.setFirma("*");
			// tipo documento
			ticket.setIdTipoDocumento(100000L);

			ticket.setCodAlmacen(sesion.getAplicacion().getCodAlmacen());
			ticket.setCodcaja(sesion.getSesionCaja().getCajaAbierta().getCodCaja());
			ticket.setFecha(new Date());

			// Formato localizador
			// yyMMdd[codAlm][idticket con padding][3 ultimos caracteres del uidTicket]
			SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
			String locator = format.format(new Date()) + sesion.getAplicacion().getCodAlmacen() + String.format("%06d", idTicket) + StringUtils.right(ticket.getUidTicket(), 3);
			ticket.setLocatorId(locator);

			/* Se codifica el pdf en base64 */
			byte[] pdfEncoded = Base64.getEncoder().encode(ticketFidelizado.getPdfFidelizado());
			ticketFidelizado.setPdfFidelizado(pdfEncoded);

			String fechaAlta = Fecha.getFecha(new Date()).getString("YYYMMdd");
			ticketFidelizado.setFechaAlta(fechaAlta);

			generaPDF(ticketFidelizado, fidelizado, firma);

			xmlTicketFidelizado = MarshallUtil.crearXML(ticketFidelizado);
			ticket.setTicket(xmlTicketFidelizado);

			log.debug("registrarTicketFidelizado() - Guardando ticket de fidelizado " + fidelizado.getIdFidelizado());
			ticketsService.insertarTicket(null, ticket, false);
		}
		catch (Exception e) {
			log.error("registrarTicketFidelizado() - Error saving document: " + e.getMessage());
		}

	}

	public void generaPDF(TicketFidelizadoCaptacion ticketFidelizado, FidelizadoBean fidelizado, byte[] firma) throws JRException, IOException {
		log.debug("generaPDF() - Inicio del proceso de generacion de pdf");
		Locale locale = new Locale(AppConfig.idioma, AppConfig.pais);
		FormatUtil.getInstance().init(locale);

		URL url = Thread.currentThread().getContextClassLoader().getResource("plantillas/jasper/fidelizados/formulariofidelizado.jasper");
		log.debug("generaPDF() - Ruta inicial de comerzzia - " + url.getPath());

		File reportFile = new File(url.getPath());
		JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportFile);
		jasperReport.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);

		Map<String, Object> parametros = new HashMap<String, Object>();
		if (fidelizado != null) {
			if (fidelizado.getTipoContacto("MOVIL") != null) {
				parametros.put("MOVIL", fidelizado.getTipoContacto("MOVIL").getValor());
				parametros.put("MOVIL_NOTIF", fidelizado.getTipoContacto("MOVIL").getRecibeNotificaciones() ? I18N.getTexto("Si") : I18N.getTexto("No"));
			}
			else {
				parametros.put("MOVIL", "");
				parametros.put("MOVIL_NOTIF", "-");
			}
			if (fidelizado.getTipoContacto("EMAIL") != null) {
				parametros.put("EMAIL", fidelizado.getTipoContacto("EMAIL").getValor());
				parametros.put("EMAIL_NOTIF", fidelizado.getTipoContacto("EMAIL").getRecibeNotificaciones() ? I18N.getTexto("Si") : I18N.getTexto("No"));
			}
			else {
				parametros.put("EMAIL", "");
				parametros.put("EMAIL_NOTIF", "-");
			}

		}
		else { // Para que no aparezca 'null' en 'Permite notificaciones'
			parametros.put("MOVIL_NOTIF", "");
			parametros.put("EMAIL_NOTIF", "");
		}
		parametros.put("DESEMP", sesion.getAplicacion().getEmpresa().getDesEmpresa());
		parametros.put("DOMICILIO", sesion.getAplicacion().getEmpresa().getDomicilio());
		parametros.put("CP", sesion.getAplicacion().getEmpresa().getCp());
		parametros.put("PROVINCIA", sesion.getAplicacion().getEmpresa().getProvincia());

		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			parametros.put("LOGO", bis);
		}
		/* Se pasa por parametro la imagen de la firma */
		if (firma != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(firma);
			parametros.put("FIRMA", bis);
		}
		List<FidelizadoBean> fidelizados = new ArrayList<FidelizadoBean>();
		fidelizados.add(fidelizado);
		JRDataSource dataSource = new JRBeanCollectionDataSource(fidelizados);

		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

		ticketFidelizado.setPdfFidelizado(outputStream.toByteArray());
		log.debug("generaPDF() - Generacion correcta del PDF");
	}

	@SuppressWarnings({ "unlikely-arg-type", "unused" })
	public void accionImprimir(FidelizadoBean fidelizado) {
		log.debug("accionImprimir() - Imprimiendo formulario de fidelizado");
		List<FidelizadoBean> fidelizados = new ArrayList<FidelizadoBean>();
		HashMap<String, Object> parametros = new HashMap<String, Object>();

		// Se edita temporalmente el domicilio y documento del fidelizado para la impresión por si fuera necesario
		// ocultarlo
		String domicilioCopia = fidelizado.getDomicilio();
		String documentoCopia = fidelizado.getDocumento();
		fidelizado.setDomicilio(imprimeDatoSensible(fidelizado.getDomicilio()));
		fidelizado.setDocumento(imprimeDatoSensible(fidelizado.getDocumento()));

		fidelizados.add(fidelizado);
		parametros.put(ImpresionJasper.LISTA, fidelizados);
		if (fidelizado != null) {
			if (fidelizado.getTipoContacto("MOVIL") != null) {
				parametros.put("MOVIL", imprimeDatoSensible(fidelizado.getTipoContacto("MOVIL").getValor()));
				parametros.put("MOVIL_NOTIF", "S".equals(fidelizado.getTipoContacto("MOVIL").getRecibeNotificaciones()) ? "Sí" : "No");
			}
			else {
				parametros.put("MOVIL", "");
				parametros.put("MOVIL_NOTIF", "-");
			}
			if (fidelizado.getTipoContacto("EMAIL") != null) {
				parametros.put("EMAIL", imprimeDatoSensible(fidelizado.getTipoContacto("EMAIL").getValor()));
				parametros.put("EMAIL_NOTIF", "S".equals(fidelizado.getTipoContacto("EMAIL").getRecibeNotificaciones()) ? "Sí" : "No");
			}
			else {
				parametros.put("EMAIL", "");
				parametros.put("EMAIL_NOTIF", "-");
			}

		}
		else { // Para que no aparezca 'null' en 'Permite notificaciones'
			parametros.put("MOVIL_NOTIF", "");
			parametros.put("EMAIL_NOTIF", "");
		}
		parametros.put("DESEMP", sesion.getAplicacion().getEmpresa().getDesEmpresa());
		parametros.put("DOMICILIO", sesion.getAplicacion().getEmpresa().getDomicilio());
		parametros.put("CP", sesion.getAplicacion().getEmpresa().getCp());
		parametros.put("PROVINCIA", sesion.getAplicacion().getEmpresa().getProvincia());

		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			parametros.put("LOGO", bis);
		}

		try {
			ServicioImpresion.imprimir("jasper/fidelizados/formulariofidelizado", parametros);
		}
		catch (DeviceException e) {
			log.error("accionImprimir() - Ha ocurrido un error al imprimir el informe ", e);
		}
		finally {
			fidelizado.setDomicilio(domicilioCopia);
			fidelizado.setDocumento(documentoCopia);
		}
	}

	protected String imprimeDatoSensible(String valor) {
		String res = valor;
		if (StringUtils.isNotBlank(valor)) {
			String sustituir = res.substring(1, res.length() - 1);
			String car = sustituir.replaceAll(".", "*");
			res = res.replace(sustituir, car);
		}
		return res;
	}

	public String getNumTarjetaFidelizado(Long idFidelizado) {
		log.debug("getNumTarjetaFidelizado() - recuperando tarjeta del fidelizado " + idFidelizado);
		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();

		ConsultarFidelizadoRequestRest consultaRest = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
		consultaRest.setIdFidelizado(idFidelizado.toString());

		try {
			List<TarjetaBean> tarjetas = FidelizadosRest.getTarjetasFidelizado(consultaRest);
			if (!tarjetas.isEmpty()) {
				return tarjetas.get(0).getNumeroTarjeta();
			}

		}
		catch (RestException | RestHttpException e) {
			log.error("getNumTarjetaFidelizado() - No se ha podido recuperar la tarjeta del fidelizado " + idFidelizado + " : " + e.getMessage(), e);
		}
		return null;
	}

	public List<String> getNumTarjetasFidelizados(List<FidelizadoBean> fidelizados) {
		log.debug("getNumTarjetasFidelizados() - Obteniendo los números de tarjetas de los fidelizados");

		Set<String> numTarjetas = new HashSet<>();
		for (FidelizadoBean fidelizado : fidelizados) {
			if (fidelizado.getNumeroTarjeta() != null && !fidelizado.getNumeroTarjeta().isEmpty()) {
				numTarjetas.add(fidelizado.getNumeroTarjeta());
			}
		}

		log.debug("getNumTarjetasFidelizados() - Tarjetas obtenidas: " + numTarjetas.toString());

		return new ArrayList<String>(numTarjetas);
	}

	public Boolean compruebaEmailNoRepetido(String email) {
		log.debug("compruebaEmailNoRepetido() - Comprobando que no existe otro fidelizado con email: " + email);

		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		
		ConsultarFidelizadoEmailRequestRest request = new ConsultarFidelizadoEmailRequestRest(apiKey, uidActividad, email);
		try {
			FidelizadoBean fidelizadoEmail = FidelizadosRest.getFidelizadoPorEmail(request);
			return fidelizadoEmail != null && fidelizadoEmail.getIdFidelizado() != null;
		}
		catch (RestException | RestHttpException e) {
			String errorMsg = "Error buscando fidelizados por email: " + email;
			log.error("compruebaEmailNoRepetido() - " + errorMsg + " : " + e.getMessage(), e);
			return false;
		}
	}
	
	public String compruebaEmailNoRepetido(String email, String documentoCabecera) {
		log.debug("compruebaEmailNoRepetido() - Comprobando que no existe otro fidelizado con email: " + email);

		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		ConsultarFidelizadoRequestRest req = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
		req.setEmail(email);

		try {
			List<FidelizadoBean> fidelizadosPorEmail = FidelizadosRest.getFidelizadosDatos(req);

			if (fidelizadosPorEmail == null || fidelizadosPorEmail.isEmpty()) {
				return IdentificacionFidelizadoController.EMAIL_NO_REPETIDO;
			}
			else {
				List<String> numTarjetas = getNumTarjetasFidelizados(fidelizadosPorEmail);
				if (StringUtils.isNotBlank(documentoCabecera)) {
					if (numTarjetas.contains(documentoCabecera)) {
						return IdentificacionFidelizadoController.EMAIL_NO_REPETIDO;
					}
				}

				String msgError = I18N.getTexto("Este Email ya está asignado al fidelizado con Número de Tarjeta: " + numTarjetas);
				return msgError;
			}

		}
		catch (RestException | RestHttpException e) {
			String errorMsg = "Error buscando fidelizados por email: " + email;
			log.error("compruebaEmailNoRepetido() - " + errorMsg + " : " + e.getMessage(), e);
			return null;
		}
	}

	public Boolean compruebaDocumentoNoRepetido(String documento) {
		log.debug("compruebaEmailNoRepetido() - Comprobando que no existe otro fidelizado con documento: " + documento);
		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();

		ConsultarFidelizadoDocumentoRequestRest request = new ConsultarFidelizadoDocumentoRequestRest(apiKey, uidActividad, documento);
		try {
			FidelizadoBean fidelizadoDocumento = FidelizadosRest.getFidelizadoPorDocumento(request);
			return fidelizadoDocumento != null && fidelizadoDocumento.getIdFidelizado() != null;
		}
		catch (RestException | RestHttpException e) {
			String errorMsg = "Error buscando fidelizados por documento: " + documento;
			log.error("compruebaDocumentoNoRepetido() - " + errorMsg + " : " + e.getMessage(), e);
			return false;
		}
	}
	
	public String compruebaDocumentoNoRepetido(String documento, String documentoCabecera) {
		log.debug("compruebaEmailNoRepetido() - Comprobando que no existe otro fidelizado con documento: " + documento);

		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		ConsultarFidelizadoRequestRest req = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
		req.setDocumento(documento);

		try {
			List<FidelizadoBean> fidelizadosPorEmail = FidelizadosRest.getFidelizadosDatos(req);

			if (fidelizadosPorEmail == null || fidelizadosPorEmail.isEmpty()) {
				return IdentificacionFidelizadoController.DOCUMENTO_NO_REPETIDO;
			}
			else {
				List<String> numTarjetas = getNumTarjetasFidelizados(fidelizadosPorEmail);

				if (StringUtils.isNotBlank(documentoCabecera)) {
					if (numTarjetas.contains(documentoCabecera)) {
						return IdentificacionFidelizadoController.DOCUMENTO_NO_REPETIDO;
					}
				}

				String msgError = I18N.getTexto("Este Documento ya está asignado al fidelizado con Número de Tarjeta: " + numTarjetas);
				return msgError;
			}

		}
		catch (RestException | RestHttpException e) {
			String errorMsg = "Error buscando fidelizados por documento: " + documento;
			log.error("compruebaDocumentoNoRepetido() - " + errorMsg + " : " + e.getMessage(), e);
			return null;
		}
	}
	
	
	public void registrarEnlacesColectivoFidelizado(FidelizadoBean fidelizado) {
		log.debug("registrarEnlacesColectivoFidelizado() - Registrando enlace de colectivo para el fidelizado: " + fidelizado.getIdFidelizado());
		String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
		String uidActividad = sesion.getAplicacion().getUidActividad();
		 try {
			 Enlace enlace = new Enlace();
			 enlace.setCodAlmacen(sesion.getAplicacion().getCodAlmacen());
			 enlace.setFechaAlta(new Date());
			 enlace.setIdFidelizado(fidelizado.getIdFidelizado());
			 enlace.setIdUsuario(sesion.getSesionUsuario().getUsuario().getIdUsuario());
			 enlace.setUidActividad(uidActividad);
			 enlace.setApiKey(apiKey);
			 BricodepotEnlacesFidelizadosRest.insertEnlacesColectivoDeFidelizado(enlace);
		}
		catch (RestHttpException | RestException e) {
			//NO es determinante para el alta, no debemos dar error en el proceso.
			log.error("No se ha podido registrar los enlaces de los colectivos para el id fidelizado "+ fidelizado.getIdFidelizado());
		}

		
	}

}
