package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.api.rest.client.clientes.ResponseGetClienteRest;
import com.comerzzia.api.rest.client.fidelizados.ConsultarFidelizadoRequestRest;
import com.comerzzia.api.rest.client.fidelizados.FidelizadosRest;
import com.comerzzia.api.rest.client.fidelizados.ResponseGetFidelizadoRest;
import com.comerzzia.brico.pos.selfcheckout.core.gui.componentes.keyboard.SelfCheckoutKeyboard;
import com.comerzzia.brico.pos.selfcheckout.gui.conversion.ConversionController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.factura.lpd.LeyProteccionDatosView;
import com.comerzzia.brico.pos.selfcheckout.services.cliente.SelfCheckoutClientesService;
import com.comerzzia.brico.pos.selfcheckout.services.core.sesion.SelfCheckOutSesionAplicacion;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.pos.core.gui.BackgroundTask;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.POSApplication;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.factura.FacturaController;
import com.comerzzia.pos.persistence.clientes.ClienteBean;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.tickets.datosfactura.DatosFactura;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.documentos.Documentos;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import rest.client.clientes.BricodepotClientesRest;
import rest.client.clientes.BricodepotConsultarClienteRequestRest;

@Primary
@Component
@SuppressWarnings("rawtypes")
public class SelfCheckoutFacturaController extends FacturaController {

	public static final String DUPLICADO = "DUPLICADO";
	public static final String DATOS_CLIENTE_CONVERSION = "localizadorConversion";
	public static final String CLIENTE_CONVERSION = "clienteConversion";
	
	@Autowired
	private Sesion sesion;

	@Autowired
	private VariablesServices variablesServices;

	@Autowired
	private SelfCheckoutClientesService clientesService;
	
	@FXML
	private Label lbConversion, lbLocalizador;
	

	@FXML
	private SelfCheckoutKeyboard keyboard;

	private boolean recuperado;
	private boolean valido;
	private boolean solicitarAutorizacion;
	private Integer identificacion;

	@Override
	public void initializeForm() {
		super.initializeForm();
		
		tfDesCliente.setText("");
		tfNumDocIdent.setDisable(false);
		btBusquedaCentral.setDisable(false);
		bloquearFormularioCliente(true);
		identificacion = 0;
		try {

			if (getDatos() != null && getDatos().containsKey(DATOS_CLIENTE_CONVERSION) && getDatos().containsKey(CLIENTE_CONVERSION)) {
				lbLocalizador.setText((String) getDatos().get(DATOS_CLIENTE_CONVERSION));
				String apiKey = variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY);
				String uidActividad = sesion.getAplicacion().getUidActividad();

				ConsultarFidelizadoRequestRest consultaRest = new ConsultarFidelizadoRequestRest(apiKey, uidActividad);
				consultaRest.setNumeroTarjeta((String) getDatos().get(CLIENTE_CONVERSION));
				ResponseGetFidelizadoRest fidelizado = FidelizadosRest.getFidelizado(consultaRest);
				tfNumDocIdent.setText(fidelizado.getDocumento());
				cbTipoDocIdent.getSelectionModel().getSelectedItem().setCodTipoIden(fidelizado.getCodTipoIden());
				cargarClienteCentral();

			}
			else {
				lbConversion.setVisible(false);
				lbLocalizador.setVisible(false);
			}
			
			solicitarAutorizacion = getDatos().containsKey(ConversionController.REQUIERE_AUTORIZACION) && (boolean) getDatos().get(ConversionController.REQUIERE_AUTORIZACION);
			
		}
		catch (Exception e) {
			log.error("initializeForm() - Error al inicializar la pantalla: " + e.getMessage(), e);
		}
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {
		try {
			keyboard = new SelfCheckoutKeyboard();
		} catch (IOException | URISyntaxException e) {
			log.error("Error cargando teclado");
		}
		keyboard.onController(this);
		keyboard.setPopupVisible(true, tfNumDocIdent, this.getStage(), Boolean.TRUE);
		recuperado = Boolean.TRUE;
		valido = Boolean.FALSE;

	}

	@Override
	public void cargarClienteCentral() {
		log.debug("cargarClienteCentral() - haciendo busqueda de clientes");
		if (validarFormularioBusquedaCentral()) {
			String codPais = null;
			String codTipoIden = null;
			
			if (cbTipoDocIdent.getSelectionModel().getSelectedItem() != null
					&& StringUtils.isNotBlank(cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden())) {
				codTipoIden = cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden();
				if (StringUtils.isNotBlank(tfCodPais.getText())) {
					codPais = tfCodPais.getText();
				} else {
					codPais = sesion.getAplicacion().getTienda().getCliente().getCodpais();
				}
			}

			try {
				List<ClienteBean> consultarClientes = clientesService.consultarClientes(codTipoIden,
						tfNumDocIdent.getText().toUpperCase());
				if (consultarClientes != null && !consultarClientes.isEmpty()) {
					log.debug("cargarClienteCentral() - recuperados de local");
					if (consultarClientes.size() == 1) {
						bloquearFormularioCliente(true);
						rellenarDatosFacturacion(consultarClientes.get(0));
						recuperado = true;
					} else {
						VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central"),
								getStage());
					}
				} else {
					log.debug("cargarClienteCentral() - recuperando datos de central");
					new SelfCheckoutBuscarClienteEnCentralTask(codPais).start();

				}
			} catch (Exception e) {
				log.error(
						"cargarClienteCentral() - Ha habido un error al buscar en la base de datos el cliente para facturar: "
								+ e.getMessage(),
						e);
				new SelfCheckoutBuscarClienteEnCentralTask(codPais).start();
			}
		}
	}
	
	protected class SelfCheckoutBuscarClienteEnCentralTask extends BackgroundTask<ResponseGetClienteRest> {

		private String codPais;

		public SelfCheckoutBuscarClienteEnCentralTask(String codPais) {
			super();
			this.codPais = codPais;
		}

		@Override
		protected ResponseGetClienteRest call() throws Exception {

			BricodepotConsultarClienteRequestRest consultaCliente = new BricodepotConsultarClienteRequestRest(
					variablesServices.getVariableAsString(VariablesServices.WEBSERVICES_APIKEY),
			        sesion.getAplicacion().getUidActividad(), 
			        null, 
			        tfNumDocIdent.getText().toUpperCase(),
			        null,
			        codPais
			        );

			return BricodepotClientesRest.getClientePais(consultaCliente);
		}

		@Override
		protected void succeeded() {
			super.succeeded();

			try {
				ResponseGetClienteRest res = get();

				if (res == null) {
					VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central"), getStage());
					limpiarCampos();
				}
				else if (res.getCodCliente() == null) {
					boolean confirmacion = VentanaDialogoComponent.crearVentanaConfirmacion(I18N.getTexto("No ha sido posible recuperar tu información. ¿Deseas introducir tus datos manualmente?"),
					        getStage());
					if (confirmacion) {
						//guardamos el campo para poder recuperarlo luego
						String numDoc = tfNumDocIdent.getText();

						identificacion = cbTipoDocIdent.getSelectionModel().getSelectedIndex();
						bloquearFormularioCliente(false);
						limpiarCampos();
						tfNumDocIdent.setDisable(true);
						tfNumDocIdent.setText(numDoc);
						btBusquedaCentral.setDisable(true);
						recuperado = Boolean.FALSE;
						if (identificacion != null) {
							cbTipoDocIdent.getSelectionModel().select(identificacion);
							clienteTicket.setTipoIdentificacion(cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden());
							identificacion = null;
						}
					}
					else {
						bloquearFormularioCliente(true);
						return;
					}
				}
				else if (res.getCodCliente().equals(DUPLICADO)) {
					VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central"), getStage());
					limpiarCampos();
				}
				else if (res.getCodError() != 0) {
					VentanaDialogoComponent.crearVentanaAviso(res.getMensaje(), getStage());

				}
				else if (!res.isActivo()) {
					VentanaDialogoComponent.crearVentanaError(I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central"), getStage());
					limpiarCampos();
				}
				else {
					limpiarCampos();
					rellenarDatosFacturacion(res);
				}
			}
			catch (Exception e) {
				log.error("BuscarClienteEnCentralTask() - Ha habido un error al recuperar el cliente de la central: " + e.getMessage(), e);
			}
		}

		@Override
		protected void failed() {
			super.failed();

			log.error("BuscarClienteEnCentralTask() - Error consultando el cliente en central: "
					+ getException().getMessage(), getException());
			String msg = getException().getMessage();

			if (msg == null || msg.isEmpty()) {
				msg = I18N.getTexto("La petición no se procesó correctamente.");
			} else if (msg.contains("HTTP 404")) {
				msg = (I18N.getTexto("Error. Dirección de servicio rest no encontrada (HTTP 404)"));
			} else if (msg.contains("HTTP 4")) {
				msg = (I18N.getTexto("No se pudo conectar con los servicios REST (HTTP 400)"));
			} else if (msg.contains("HTTP 5")) {
				msg = (I18N.getTexto("No se pudo conectar con los servicios REST (HTTP 500)"));
			} else if (msg.contains("Connection refused")) {
				msg = (I18N.getTexto("No se pudo conectar con los servicios REST de la central"));
			} else if (msg.equals("El cliente consultado no existe en el sistema")) {
				if (cbTipoDocIdent.getSelectionModel().getSelectedItem() != null && StringUtils
						.isNotBlank(cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden())) {
					msg = I18N.getTexto(
							"El cliente con el documento {0}, el tipo de documento {1}, no se encuentra registrado en la BBDD. Para obtener su factura completa diríjase a una de nuestras cajas asistidas",
							tfNumDocIdent.getText(),
							cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden(), codPais);
				} else {
					msg = I18N.getTexto("El cliente con el documento {0} no existe en el sistema",
							tfNumDocIdent.getText());
				}
			} else if (msg.equals("Error al consultar cliente en el sistema")) {
				msg = I18N.getTexto("No se ha podido recuperar su información. Por favor, solicite ayuda en caja central");
			}
			boolean confirmacion = VentanaDialogoComponent.crearVentanaConfirmacion(
					I18N.getTexto(
							"No ha sido posible recuperar tu información. ¿Deseas introducir tus datos manualmente?"),
					getStage());
			if (confirmacion) {
				//guardamos el campo para poder recuperarlo luego
				String numDoc = tfNumDocIdent.getText();
				identificacion = cbTipoDocIdent.getSelectionModel().getSelectedIndex();
				bloquearFormularioCliente(false);
				limpiarCampos();
				tfNumDocIdent.setDisable(true);
				tfNumDocIdent.setText(numDoc);
				cbTipoDocIdent.setDisable(true);
				btBusquedaCentral.setDisable(true);
				recuperado = Boolean.FALSE;
				if (identificacion != null) {
					cbTipoDocIdent.getSelectionModel().select(identificacion);
					clienteTicket.setTipoIdentificacion(cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden());
					identificacion = null;
				}

			} else {
				bloquearFormularioCliente(true);
				return;
			}
		}
	}

	@Override
	protected void establecerClienteFactura(Event event) {

		log.debug("establecerClienteFactura()");

		frDatosFactura.setNumDocIdent(frDatosFactura.trimTextField(tfNumDocIdent));
		frDatosFactura.setDomicilio(frDatosFactura.trimTextField(tfDomicilio));
		frDatosFactura.setProvincia(frDatosFactura.trimTextField(tfProvincia));
		frDatosFactura.setLocalidad(frDatosFactura.trimTextField(tfLocalidad));
		frDatosFactura.setPoblacion(frDatosFactura.trimTextField(tfPoblacion));
		frDatosFactura.setRazonSocial(frDatosFactura.trimTextField(tfRazonSocial));
		frDatosFactura.setcPostal(frDatosFactura.trimTextField(tfCP));
		frDatosFactura.setTelefono(frDatosFactura.trimTextField(tfTelefono));
		frDatosFactura.setPais(frDatosFactura.trimTextField(tfCodPais));
		frDatosFactura.setBanco(frDatosFactura.trimTextField(tfBanco));
		frDatosFactura.setBancoDomicilio(frDatosFactura.trimTextField(tfBancoDomicilio));
		frDatosFactura.setBancoPoblacion(frDatosFactura.trimTextField(tfBancoPoblacion));
		frDatosFactura.setBancoCCC(frDatosFactura.trimTextField(tfBancoCCC));
		valido = validarFormularioDatosFactura();
		if (valido) {

			DatosFactura datosFactura = new DatosFactura();
			datosFactura.setCif(tfNumDocIdent.getText().toUpperCase());
			datosFactura.setCp(tfCP.getText());
			datosFactura.setDomicilio(tfDomicilio.getText());
			datosFactura.setProvincia(tfProvincia.getText());
			datosFactura.setTelefono(tfTelefono.getText());
			datosFactura.setNombre(tfRazonSocial.getText());
			datosFactura.setPoblacion(tfPoblacion.getText());
			datosFactura.setLocalidad(tfLocalidad.getText());
			datosFactura.setPais(tfCodPais.getText());
			datosFactura.setBanco(tfBanco.getText());
			datosFactura.setBancoDomicilio(tfBancoDomicilio.getText());
			datosFactura.setBancoPoblacion(tfBancoPoblacion.getText());
			datosFactura.setCcc(tfBancoCCC.getText());

			if (cbTipoDocIdent.getSelectionModel().getSelectedItem() != null) {
				datosFactura.setTipoIdentificacion(cbTipoDocIdent.getSelectionModel().getSelectedItem().getCodTipoIden());
			}

			((TicketVenta) ticketManager.getTicket()).setDatosFacturacion(datosFactura);
			try {
				if (!ticketManager.getDocumentoActivo().getCodtipodocumento().equals(Documentos.FACTURA_COMPLETA)) {
					ticketManager.setDocumentoActivo(sesion.getAplicacion().getDocumentos()
							.getDocumento(ticketManager.getDocumentoActivo().getTipoDocumentoFacturaDirecta()));
				}
				// getStage().close();
			} catch (DocumentoException ex) {
				log.error("No se pudo establecer el tipo documento factura", ex);
				VentanaDialogoComponent.crearVentanaAviso(
						I18N.getTexto("Error al establecer el tipo de documento factura."), this.getStage());
			}
		}
		String frNumDocIdent = frDatosFactura.getNumDocIdent();
		FidelizacionBean fid = ticketManager.getTicket().getCabecera().getDatosFidelizado();

		if (fid != null && !frNumDocIdent.equals(fid.getDocumento())) {
			((TicketVenta) ticketManager.getTicket()).getDatosFacturacion().setCif(StringUtils.isNotBlank(fid.getDocumento()) ? fid.getDocumento() : frNumDocIdent);
		}
		if(!valido) {
			datos.put("cancela", true);
		}
		
	}

	private void bloquearFormularioCliente(boolean valor) {
		String documento = tfNumDocIdent.getText();
		if(ticketManager.getTicket().getCabecera().getDatosFidelizado() == null && ticketManager.getTicket().getCabecera().getCliente().getDatosFactura() == null){
			limpiarCampos();			
		}
		tfCodCliente.setDisable(true);
		tfDesCliente.setDisable(valor);
		tfRazonSocial.setDisable(valor);
		tfDomicilio.setDisable(valor);
		tfPoblacion.setDisable(valor);
		tfProvincia.setDisable(valor);
		tfLocalidad.setDisable(valor);
		tfCP.setDisable(valor);
		cbTipoDocIdent.setDisable(valor);
		tfTelefono.setDisable(valor);
		tfCodPais.setDisable(true);
		tfDesPais.setDisable(true);
		btBuscar.setVisible(false);
		if (!valor) {
			tfNumDocIdent.setText(documento);
		}

	}

	@Override
	protected void limpiarCampos() {
		tfRazonSocial.setText("");
		tfRazonSocial.setEditable(true);
		tfProvincia.setText("");
		tfDomicilio.setText("");
		tfLocalidad.setText("");
		tfTelefono.setText("");
		tfNumDocIdent.setText("");
//		tfCodPais.setText("");I
//		codPais = "";
//		tfDesPais.setText("");
		tfPoblacion.setText("");
		tfCP.setText("");
		if (clienteTicket != null && clienteTicket.getTipoIdentificacion() == null) {
			updateSelectedTipoIdentificacion();
		}

		tfBanco.setText("");
		tfBancoDomicilio.setText("");
		tfBancoPoblacion.setText("");
		tfBancoCCC.setText("");
		tfDesCliente.setText("");
	}

	@Override
	@FXML
	public void accionAceptar(ActionEvent event) {

		String nombreFactura = frDatosFactura.trimTextField(tfRazonSocial);
		String domicilioFactura = frDatosFactura.trimTextField(tfDomicilio);
		String cpFactura = frDatosFactura.trimTextField(tfCP);
		String paisFactura = frDatosFactura.trimTextField(tfCodPais);
		String datosFaltan = I18N.getTexto("Debes rellenar:");

		log.debug("aceptar() - Acción aceptar");
		getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);

		if (StringUtils.isBlank(nombreFactura) || StringUtils.isBlank(domicilioFactura) || StringUtils.isBlank(cpFactura) || StringUtils.isBlank(paisFactura)) {
			
			if (StringUtils.isBlank(nombreFactura)) {
				datosFaltan = datosFaltan + I18N.getTexto("Nombre,");
			}
			if (StringUtils.isBlank(domicilioFactura)) {
				datosFaltan = datosFaltan + I18N.getTexto("Domicilio,");
			}
			if (StringUtils.isBlank(cpFactura)) {
				datosFaltan = datosFaltan + I18N.getTexto("CP,");
			}
			if (StringUtils.isBlank(paisFactura)) {
				datosFaltan = datosFaltan + I18N.getTexto("País,");
			}
			// Eliminar la última coma si existe
			if (datosFaltan.endsWith(", ")) {
				datosFaltan = datosFaltan.substring(0, datosFaltan.length() - 2);
			}
			int lastIndex = datosFaltan.lastIndexOf(", ");

	        if (lastIndex != -1) {
	            // Reemplazar la última ", " por " y "
	        	datosFaltan = datosFaltan.substring(0, lastIndex) + I18N.getTexto("y") + datosFaltan.substring(lastIndex + 2);
	        }
			VentanaDialogoComponent.crearVentanaInfo(I18N.getTexto(datosFaltan), getStage());

		} else {
			establecerClienteFactura(event);
			
			comprobarRecuperacionFidelizado(nombreFactura, domicilioFactura, cpFactura);
			
			if (valido && !recuperado) {
				String idiomaElegido = ((SelfCheckOutSesionAplicacion) sesion.getAplicacion()).getCodIdiomaCliente();
				
				if (!idiomaElegido.equals("pt")) {
					ventanaLPD();
				}			
			}
			
			if (solicitarAutorizacion) {
				abrirVentanaAutorizacion();
			}
			getStage().close();
		}

	}

	private void comprobarRecuperacionFidelizado(String nombreFactura, String domicilioFactura, String cpFactura) {
		if (StringUtils.isNotBlank(nombreFactura) || StringUtils.isNotBlank(domicilioFactura) || StringUtils.isNotBlank(cpFactura)) {
			if (tfRazonSocial.isDisabled() || tfDomicilio.isDisabled() || tfCP.isDisabled()) {
				recuperado = Boolean.TRUE;
			}
		}
	}

	@Override
	public void accionCancelar() {
		log.debug("cancelar() - Cancelar");

		POSApplication.getInstance().activarTimer();
		getDatos().put("cancela", true);
		getStage().close();
		clienteTicket.setTipoIdentificacion(null);

	}

	private void ventanaLPD() {
		getApplication().getMainView().showModalCentered(LeyProteccionDatosView.class, getDatos(), getStage());
	}
	
	protected void abrirVentanaAutorizacion() {
		log.debug("abrirVentanaAutorizacion()- abriendo ventana de autorizacion");
		List<TicketAuditEvent> events = new ArrayList<>();
		TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.SOLICITAR_AUTORIZACION, sesion);
		events.add(auditEvent);
		getDatos().put(RequestAuthorizationController.AUDIT_EVENT, events);
		getDatos().put(FacturacionArticulosController.TICKET_KEY, ticketManager);
		getDatos().put(RequestAuthorizationController.MENSAJE_INFO, "Debes firmar el documento de devolución que se generará a continuacion. Por favor, contacte con el supervisor para que acuda a caja y autorice la operación.");
		
		getApplication().getMainView().showModalCentered(RequestAuthorizationView.class, getDatos(), this.getStage());
	}
	
}
