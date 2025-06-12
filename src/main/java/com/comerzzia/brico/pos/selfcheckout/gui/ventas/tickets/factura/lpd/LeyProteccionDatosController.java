package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.lpd;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.firma.FirmaController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.firma.FirmaView;
import com.comerzzia.brico.pos.selfcheckout.services.cliente.SelfcheckoutClienteService;
import com.comerzzia.brico.pos.selfcheckout.services.cliente.TicketClienteCaptacion;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.core.util.base.Estado;
import com.comerzzia.core.util.fechas.Fecha;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.controllers.Controller;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.persistence.clientes.ClienteBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.devices.DeviceException;
import com.comerzzia.pos.servicios.impresion.ImpresionJasper;
import com.comerzzia.pos.servicios.impresion.ServicioImpresion;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;

@Component
public class LeyProteccionDatosController extends Controller{

	public static final Logger log = Logger.getLogger(LeyProteccionDatosController.class.getName());
	public static final String LPD = "Ley de Protección de Datos";
	public static final String IMPRIMIR_LPD = "Imprimir Documento";
	public static final String CHECK_LPD = "checkLPD";
	
	@FXML
	protected Label lbTitulo;
	
	@FXML
	protected CheckBox checkBox;
	
	@FXML
	protected WebView wvTexto;
	
	@Autowired 
	protected Sesion sesion;
	
	protected byte[] firma;
	
	@Autowired
	protected SelfcheckoutClienteService scoClienteService;
	
	protected ClienteBean cliente;
	
	protected TicketManager ticketManager;
	
	protected DatosFactura factura;
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		lbTitulo.setText(I18N.getTexto(LPD));
		checkBox.setText(I18N.getTexto(IMPRIMIR_LPD));
		ticketManager = (TicketManager)getDatos().get(FacturacionArticulosController.TICKET_KEY);
		cliente =  ticketManager.getTicket().getCabecera().getCliente();
		factura = ticketManager.getTicket().getCabecera().getCliente().getDatosFactura();

	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		refrescarDatosPantalla();
	}

	@Override
	public void initializeFocus() {

	}

	public void refrescarDatosPantalla() {
		log.debug("refrescarDatosPantalla()");
		try {
			cargarTexto();
		}
		catch (IOException e) {
		}
	}

	@FXML
	public void accionAceptar() {
		getDatos().put(CHECK_LPD, checkBox.isSelected());
		
		if(sesion.getAplicacion().getTienda().getCliente().getCodpais().toUpperCase().equals("ES")) {
			pantallaFirma();
		}
		if(firma!=null) {
			getDatos().put(FirmaController.FIRMA, firma);
		}
		registrarTicketCliente();
		getStage().close();

	}

	private void pantallaFirma() {
		getApplication().getMainView().showModalCentered(FirmaView.class, getDatos(), getStage());
		firma = (byte[])getDatos().get(FirmaController.FIRMA);
//		getDatos().put(FirmaController.FIRMA, firma);
	}

	@FXML
	public void accionRechazar() {
		datos.put("cancela", true);
		getStage().close();
	}

	private void cargarTexto() throws FileNotFoundException, IOException {
		log.debug("cargarTexto()");
		URL url = Thread.currentThread().getContextClassLoader().getResource("textos_legales/Clientes" + AppConfig.pais.toUpperCase() + ".htm");

		try (FileInputStream inputStream = new FileInputStream(url.getPath())) {
			log.debug("cargarTexto() - Url del fichero de proteccion de datos: " + url.getPath());

			String everything = IOUtils.toString(inputStream, "UTF-8");

			wvTexto.getEngine().loadContent(everything);
		}
	}
	
	private void registrarTicketCliente() {
		log.debug("registrarTicketCliente()");
		
		ClienteBean clienteNuevo = scoClienteService.datosFacturaCliente(factura, cliente);

		TicketClienteCaptacion ticketCliente = new TicketClienteCaptacion();
		ticketCliente.setPdfCliente(firma != null ? firma : new byte[0]);
		ticketCliente.setCif(clienteNuevo.getDatosFactura().getCif());
		ticketCliente.setOperacion("ALTA"); // ALTA o MOD(ificación)
		ticketCliente.setFechaAlta(Fecha.getFecha(new Date()).getString("dd/MM/YYYY"));
		scoClienteService.registrarTicketCliente(ticketCliente, clienteNuevo, firma);

		if(checkBox.isSelected())
			accionImprimir();

	}
	
	public void accionImprimir() {
		log.debug("accionImprimir()");
		
		List<ClienteBean> clientes = new ArrayList<ClienteBean>();
		HashMap<String, Object> parametros = new HashMap<String, Object>();

		//Muestra check de alta o modificación dependiendo de lo que es
		parametros.put("ES_ALTA",Boolean.valueOf(cliente.getEstadoBean()==Estado.NUEVO));
		
		String cif = factura.getCif();
		String nombre = factura.getNombre();
		cliente.setCif(cif);
		cliente.setDesCliente(nombre);
	
		clientes.add(cliente);
		parametros.put(ImpresionJasper.LISTA, clientes);
		parametros.put("factura",factura);
		

		if (sesion.getAplicacion().getEmpresa().getLogotipo() != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(sesion.getAplicacion().getEmpresa().getLogotipo());
			parametros.put("LOGO", bis);
		}
		
		/* Se pasa por parametro la imagen de la firma */
		if (firma != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(firma);
			parametros.put("FIRMA", bis);
		}
		
		try {
			String idioma = ((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getCodIdiomaCliente();
			if (idioma.equals("pt")) {
				ServicioImpresion.imprimir("jasper/clientes/formulariocliente_PT", parametros);
			}else {
				ServicioImpresion.imprimir("jasper/clientes/formulariocliente", parametros);

			}
			
	
		}
		catch (DeviceException e) {
			log.error("Ha ocurrido un error al imprimir el informe ", e);
		}
	}

}


