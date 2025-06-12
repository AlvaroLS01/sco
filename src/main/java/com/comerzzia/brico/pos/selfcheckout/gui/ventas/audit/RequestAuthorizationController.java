package com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.comerzzia.brico.pos.selfcheckout.core.gui.login.SelfCheckoutLoginController;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.controllers.WindowController;
import com.comerzzia.pos.core.gui.login.LoginFormularioBean;
import com.comerzzia.pos.core.gui.validation.ValidationUI;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.persistence.core.usuarios.UsuarioBean;
import com.comerzzia.pos.services.core.perfiles.ServicioPerfiles;
import com.comerzzia.pos.services.core.permisos.ServicioPermisos;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.sesion.SesionUsuario;
import com.comerzzia.pos.services.core.usuarios.UsuarioInvalidLoginException;
import com.comerzzia.pos.services.core.usuarios.UsuariosService;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.util.config.SpringContext;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

@Controller
public class RequestAuthorizationController extends WindowController {

	private static final Logger log = Logger.getLogger(RequestAuthorizationController.class.getName());

	public static final String PERMITIR_ACCION = "PERMITIR_ACCION";
	public static final String AUDIT_EVENT = "AUDIT_EVENT";
	public static final String AUTORIZAR_TIPOLOGIA = "autorizarTipologia";
	public static final String MENSAJE_INFO = "mensajeInfo";
	
	UsuarioBean supervisor;

	List<TicketAuditEvent> auditEvent;

	TicketManager ticketManager;
	
	@FXML
	protected AnchorPane panelTipologia;
	
	@FXML
	protected TextField tfUsuario;
	@FXML
	protected PasswordField tfPassword;
	@FXML
	protected Button btAceptar;
	@FXML
	protected Button btCancelar;
	@FXML
	protected Label lbError;
	@FXML
	protected Label lbInformacion;
	@FXML
	protected Label lbUser, lbPassword;
	@FXML
	protected Label lbAutorizacion;

	protected Runnable accionAceptar;
	
	@FXML
	protected Label textoInfoTipologia;

	// Formulario de logín -> funcion submit accionFormularioLoginSubmit()
	protected LoginFormularioBean formularioLogin;

	@Autowired
	protected ServicioPermisos servicioPermisos;
	@Autowired
	protected SesionUsuario sesionUsuario;
	@Autowired
	protected UsuariosService usuariosService;
	@Autowired
	protected VariablesServices variablesService;
	@Autowired
	protected ServicioPerfiles servicioPerfiles;
	@Autowired
	protected Sesion sesion;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		log.debug("initialize()");
		// Inicializamos el formulario de login
		formularioLogin = SpringContext.getBean(LoginFormularioBean.class);
		// Asignamos un componente a cada elemento del formulario. (Para establecer foco
		// o estilos de error)
		formularioLogin.setFormField("usuario", tfUsuario);
		formularioLogin.setFormField("password", tfPassword);
	}

	@Override
	public void initializeComponents() throws InitializeGuiException {

		lbError.setText("");
		lbInformacion.setText("");
		lbAutorizacion.setText("");
		btCancelar.setDisable(false);
		textoInfoTipologia.setText("");
		this.accionLimpiarFormulario();
	}

	/**
	 * Captura y consume evento de soltar tecla enter
	 * 
	 * @param e KeyEvent
	 */
	@FXML
	public void accionAceptarIntro(KeyEvent e) {
		log.debug("accionAceptarIntro()");

		if (e.getCode() == KeyCode.ENTER) {
			accionAceptar();
			e.consume();
		}
	}

	@FXML
	public void accionCancelarEsc(KeyEvent e) {
		log.debug("accionCancelarEsc()");

		if (e.getCode() == KeyCode.ESCAPE) {
			accionCancelar();
			e.consume();
		}
	}

	@Override
	public void initializeFocus() {
		tfUsuario.requestFocus();
	}

	@Override
	public void initializeForm() throws InitializeGuiException {
		configurarIdioma();
		panelTipologia.setVisible(false);
		auditEvent = null;
		ticketManager = null;
		supervisor = null;
		lbError.setText("");
		accionLimpiarFormulario();
		initializeFocus();
		auditEvent = castToList(getDatos().get("AUDIT_EVENT"));
		ticketManager = (TicketManager) getDatos().get(FacturacionArticulosController.TICKET_KEY);
		
		if(getDatos().containsKey("autorizarTipologia")) {
			panelTipologia.setVisible(true);
			textoInfoTipologia.setText((String) getDatos().get("autorizarTipologia"));
		}
		
		// si hay una parametro permitir accion quitarlo
		try {
			getDatos().remove(RequestAuthorizationController.PERMITIR_ACCION);
		} catch (Exception ignore) {

		}
		// asumir que ambos eventos son para la misma linea del ticket
		StringBuilder eventType = new StringBuilder();
		if (auditEvent.size() > 1) {
			for (int i = 0; i < auditEvent.size(); i++) {
				eventType.append(auditEvent.get(i).getTypeAsString());
				if (i < auditEvent.size() - 1) {
					eventType.append(" & ");
				}
			}
		} else {
			eventType.append(auditEvent.get(0).getTypeAsString());
		}
		log.debug("auditAction() - Captured auditable action of type " + eventType);
		String tipoEvento = I18N.getTexto(eventType.toString());
		String inf= I18N.getTexto("Se necesita autorización para");
		
		String mensajeInfo = getDatos().containsKey(MENSAJE_INFO) ? getDatos().get(MENSAJE_INFO).toString() : inf + " " + tipoEvento;
		
		lbInformacion.setText(I18N.getTexto(mensajeInfo) + (auditEvent.get(0).getDesArticulo() == null ? "" : (" de \n" + auditEvent.get(0).getDesArticulo())));
		
		lbAutorizacion.setText(I18N.getTexto("Acción requiere autorización"));
		
		lbUser.setText(I18N.getTexto("Usuario"));
		lbPassword.setText(I18N.getTexto("Contraseña"));
		
		//Anular botón cancelar hasta que no se supervise, solo desde cantidades
		btCancelar.setVisible(!getDatos().containsKey("notCancel"));
		btCancelar.setDisable(getDatos().containsKey("notCancel"));
		btCancelar.setManaged(!getDatos().containsKey("notCancel"));
	}

	@SuppressWarnings("unchecked")
	public static <T extends List<?>> T castToList(Object obj) {
		return (T) obj;
	}

	@Override
	@FXML
	public void accionCancelar() {
		StringBuilder eventType = new StringBuilder();
		if (auditEvent.size() > 1) {
			for (int i = 0; i < auditEvent.size(); i++) {
				eventType.append(auditEvent.get(i).getTypeAsString());
				if (i < auditEvent.size() - 1) {
					eventType.append(" & ");
				}
			}
		} else {
			eventType.append(auditEvent.get(0).getTypeAsString());
		}
		
		log.debug("actionBtCancelar() - Action(s) " + eventType + " cancelled or unauthorized :(");
		getDatos().put(PERMITIR_ACCION, Boolean.FALSE);
		getDatos().remove("AUDIT_EVENT");
		super.accionCancelar();
	}

	@FXML
	public void accionAceptar() {
		log.debug("accionAceptar()");
		if (accionFrLoginSubmit()) {
			try {
				if(formularioLogin.getUsuario().equals(SelfCheckoutLoginController.USUARIO_SCO)) {
					throw new UsuarioInvalidLoginException();
				}
				UsuarioBean usuarioBean = usuariosService.login(formularioLogin.getUsuario(),
						formularioLogin.getPassword());
				this.supervisor = usuarioBean;


				for (TicketAuditEvent e : auditEvent) {
					e.logSupervisor(this.supervisor);
					
					log.info("accionAceptar() - Action " + e.getTypeAsString() + " authorized by "
							+ e.getDesUsuarioSupervisor());
					
					if (ticketManager != null) {
						((BricodepotCabeceraTicket) ticketManager.getTicket().getCabecera()).addAuditEvent(e);
					}
					getDatos().remove("AUDIT_EVENT");
					getDatos().put(PERMITIR_ACCION, Boolean.TRUE);
					
					getStage().close();
				}

			} catch (UsuarioInvalidLoginException ex) {
				log.error("accionAceptar() - Error - " + ex.getMessage(), ex);
				lbError.setText(I18N.getTexto("Las credenciales introducidas son incorrectas."));
				accionLimpiarFormulario();
				initializeFocus();

			} catch (Exception ex) {
				log.error("accionAceptar() - Error no controlado -" + ex.getMessage(), ex);
				VentanaDialogoComponent.crearVentanaError(getStage(), ex);
			}
		}
	}

	protected void accionLimpiarFormulario() {
		log.debug("accionLimpiarFormulario()");
		tfUsuario.setText("");
		tfPassword.setText("");
		formularioLogin.limpiarFormulario();
	}

	public boolean accionFrLoginSubmit() {
		formularioLogin.setUsuario(tfUsuario.getText());
		formularioLogin.setPassword(tfPassword.getText());
		return accionValidarFrLogin();
	}

	protected boolean accionValidarFrLogin() {
		// Limpiamos los errores que pudiese tener el formulario
		formularioLogin.clearErrorStyle();

		// Validamos el formulario de login
		Set<ConstraintViolation<LoginFormularioBean>> constraintViolations = ValidationUI.getInstance().getValidator()
				.validate(formularioLogin);
		if (constraintViolations.size() >= 1) {
			ConstraintViolation<LoginFormularioBean> next = constraintViolations.iterator().next();
			formularioLogin.setErrorStyle(next.getPropertyPath(), true);
			formularioLogin.setFocus(next.getPropertyPath());
			if (next.getConstraintDescriptor().getAnnotation().annotationType().equals(org.hibernate.validator.constraints.NotBlank.class)) {
		         lbError.setText(I18N.getTexto("Debe introducir las credenciales de supervisor para continuar."));
		    } else {
		         lbError.setText(next.getMessage());
		    }
			return false;
		}
		return true;
	}
	
	private void configurarIdioma() {
		btAceptar.setText(I18N.getTexto("Aceptar"));
		btCancelar.setText(I18N.getTexto("Cancelar"));
		
	}

}
