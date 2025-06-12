package com.comerzzia.brico.pos.selfcheckout.gui.bienvenida;

import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.gui.ventas.tickets.SesionTicketManager;
import com.comerzzia.brico.pos.selfcheckout.core.SelfCheckoutMainViewController;
import com.comerzzia.brico.pos.selfcheckout.gui.conversion.ConversionView;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.Controller;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.tickets.aparcados.TicketAparcadoBean;
import com.comerzzia.pos.services.core.documentos.Documentos;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.copiaSeguridad.CopiaSeguridadTicketService;
import com.comerzzia.pos.util.config.AppConfig;
import com.comerzzia.pos.util.format.FormatUtil;
import com.comerzzia.pos.util.i18n.I18N;
import com.comerzzia.pos.util.xml.MarshallUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@SuppressWarnings("rawtypes")
@Component
public class SelfCheckoutBienvenidaController extends Controller {

	private Logger log = Logger.getLogger(SelfCheckoutBienvenidaController.class);

	@Autowired
	private Sesion sesion;

	@FXML
	private Label lbError;
	@FXML
	private ImageView imgFT;

	@Autowired
	private TicketManager ticketManager;
	
	@Autowired
	protected CopiaSeguridadTicketService copiaSeguridadTicketService;
	
	@Autowired
	protected SesionTicketManager sesionTicketManager;
	
	Boolean hayTicketsRecuperados = Boolean.FALSE;
	
	@FXML
	protected TextField tfCodigoIntro;
	
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		setShowKeyboard(false);
		tfCodigoIntro.setText("");
		tfCodigoIntro.requestFocus();
		configurarIdioma(AppConfig.idioma);
	}

	@Override
	public void initializeFocus() {
		tfCodigoIntro.requestFocus();
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		tfCodigoIntro.setText("");
		tfCodigoIntro.requestFocus();
		try {
			configurarIdioma(AppConfig.idioma);
			if (!sesion.getSesionCaja().isCajaAbierta()) {
				sesion.getSesionCaja().abrirCajaAutomatica();	
			}
		}
		catch (Exception e) {
			log.error("initializeForm() - No se ha podido abrir la caja: " + e.getMessage(), e);
			throw new InitializeGuiException(I18N.getTexto("NO SE HA PODIDO ABRIR LA CAJA. CONTACTE CON EL PERSONAL DE TIENDA."));
		}

		if (!ticketManager.comprobarCierreCajaDiarioObligatorio()) {
			lbError.setText(I18N.getTexto("LA CAJA NO ESTÁ ABIERTA. NO SE PUEDE COMPRAR."));
		}
		else {
			lbError.setText("");
		}
		
//	    String rutaImagen = "/factura_completa_" + AppConfig.idioma + ".png";
//	    imgFT.setImage(new Image(getClass().getResourceAsStream("/skins/bricodepot/com/comerzzia/brico/pos/selfcheckout/gui/bienvenida" + rutaImagen)));
	    
		comprobarTicketSeguridad();
	}

	public void iniciarVentaEsp() {
		iniciarVenta("es");
	}

	public void iniciarVentaIng() {
		iniciarVenta("en");
	}

	public void iniciarVentaAle() {
		iniciarVenta("de");
	}

	public void iniciarVentaPor() {
		iniciarVenta("pt");
	}
	
	public void iniciarVentaCat() {
		iniciarVenta("ca");
	}

	public void iniciarVenta(String idioma) {
		log.debug("iniciarVenta() - Iniciamos el proceso de venta");
		try {
			if (comprobarCierreCaja()) {
				configurarIdioma(idioma);

				if(StringUtils.isNotBlank(tfCodigoIntro.getText())) {
					((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaVentaSelfCheckout(tfCodigoIntro.getText());
				}else {
					((SelfCheckoutMainViewController) getApplication().getMainView().getController()).mostrarPantallaVentaSelfCheckout();					
				}
			}
		}
		catch (Exception e) {
			log.error("iniciarVenta() - Ha habido un error al abrir la pantalla de ventas: " + e.getMessage(), e);
			VentanaDialogoComponent.crearVentanaError(getStage(), I18N.getTexto("Ha habido un error al abrir la pantalla de ventas. Contacte con el personal de tienda para solucionar el problema."),
			        e);
		}
	}

	protected boolean comprobarCierreCaja() {
		try {
		
			if (!ticketManager.comprobarCierreCajaDiarioObligatorio()) {
				if (!((ticketManager.getTicket() != null && ticketManager.getTicket().getLineas().size() > 0) || ticketManager.countTicketsAparcados() > 0)) {
					String fechaCaja = FormatUtil.getInstance().formateaFecha(sesion.getSesionCaja().getCajaAbierta().getFechaApertura());
					String fechaActual = FormatUtil.getInstance().formateaFecha(new Date());
					VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se puede realizar la venta. El día de apertura de la caja {0} no coincide con el del sistema {1}" + System.lineSeparator()
					        + System.lineSeparator() + I18N.getTexto("Contacte con el personal de tienda."), fechaCaja, fechaActual), getStage());
					return false;
				}
			}
			return true;
		}
		catch (Exception e) {
			log.error("comprobarCierreCaja() - Ha habido un error al comprobar el cierre de caja: " + e.getMessage(), e);
			return false;
		}
	}

	private void comprobarTicketSeguridad() throws InitializeGuiException{
		TipoDocumentoBean tipoDocumentoActivo = null;
		TicketAparcadoBean res = null;
		TicketVenta ticket = null;
		try {
			tipoDocumentoActivo = sesion.getAplicacion().getDocumentos().getDocumento(Documentos.FACTURA_SIMPLIFICADA);
			res = copiaSeguridadTicketService.consultarCopiaSeguridadTicket(tipoDocumentoActivo);
			if(res!=null) {
		        ticket = (TicketVentaAbono) MarshallUtil.leerXML(res.getTicket(), ticketManager.getTicketClasses(tipoDocumentoActivo).toArray(new Class[]{}));
		        if(ticket.getIdTicket() != null) {
		        	throw new InitializeGuiException();
		        }
			}
		}
		catch (InitializeGuiException e) {
			log.error("comprobarTicketSeguridad() - error recuperando/parsear el ticket" + e.getMessage(), e);
			throw new InitializeGuiException(I18N.getTexto("HAY TICKETS APARCADOS CON PAGOS ASOCIADOS, POR FAVOR, CONTACTE UN ADMINISTRADOR."));
		}
		catch (Exception e) {
			log.error("comprobarTicketSeguridad() - error recuperando/parsear el ticket" + e.getMessage(), e);
		}
	}

	protected void configurarIdioma(String idioma) {
		log.debug("configurarIdioma() - Configurando el idioma del cliente a " + idioma);

		Locale locale = new Locale(idioma, AppConfig.pais);
		Locale.setDefault(locale);
		((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).setCodIdiomaCliente(idioma);
	}
	
	public void actionTfCodigoIntro(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			iniciarVenta(AppConfig.idioma.toLowerCase());
		}
	}
	
	@FXML
	public void convertirFactura() {
		log.debug("convertirFactura");
		POSApplication.getInstance().getMainView().showModalCentered(ConversionView.class, getDatos(), getStage());
	}

	
}
