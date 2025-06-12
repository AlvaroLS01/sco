package com.comerzzia.brico.pos.selfcheckout.services.ticket.audit;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.persistence.core.usuarios.UsuarioBean;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.ticket.lineas.ILineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.util.i18n.I18N;

/**
 * 
 * @author jbn@tier1.es
 *
 */
@Component
@Scope("prototype")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "evento")
@XmlType(propOrder = { "uidActividad", "uidTicketVenta", "codAlmacen", "moment", "type", "resolution","successful",
		"idUsuario", "desUsuario",  "idUsuarioSupervisor", "desUsuarioSupervisor", "codArticulo",
		"desArticulo", "cantidad", "precioSinDtoOriginal", "precioSinDtoAplicado","lineaReferencia" })
public class TicketAuditEvent {

	public enum Type {
		CAMBIO_PRECIO, ANULACION_TICKET, ANULACION_LINEA, DEVOLUCION, GESTO_COMERCIAL, MODIFICACION_CLIENTE, VALIDAR_COMPRA, SOLICITAR_AUTORIZACION
	}
	
	public static Map<Type,String> Types = new HashMap<>();
	static {
		Types.put(Type.CAMBIO_PRECIO, "cambio de precio");
		Types.put(Type.ANULACION_TICKET, "anulación ticket");
		Types.put(Type.ANULACION_LINEA, "anulación linea");
		Types.put(Type.DEVOLUCION, "devolución");
		Types.put(Type.GESTO_COMERCIAL, "gesto comercial");
		Types.put(Type.MODIFICACION_CLIENTE, "modificacion cliente");
		Types.put(Type.VALIDAR_COMPRA, "validar la compra");
		Types.put(Type.SOLICITAR_AUTORIZACION, "solicitar autorizacion");
	}

	@XmlElement(name = "tipo")
	protected Type type;
	// actividad
	@XmlElement(name = "uid_actividad")
	private String uidActividad;
	// ticket stuff
	@XmlElement(name = "uid_ticket_venta")
	private String uidTicketVenta;

	// user stuff
	@XmlElement(name = "id_usuario")
	private Long idUsuario;

	@XmlElement(name = "des_usuario")
	private String desUsuario;
	// supervisor stuff
	@XmlElement(name = "id_supervisor")
	protected Long idUsuarioSupervisor;

	@XmlElement(name = "des_supervisor")
	protected String desUsuarioSupervisor;
	// alamcen
	@XmlElement(name = "cod_almacen")
	private String codAlmacen;
	// fecha
	@XmlElement(name = "fecha")
	protected Date moment;
	// (OPTIONAL) ticket line stuff
	@XmlElement(name = "cod_articulo")
	protected String codArticulo;
	@XmlElement(name = "des_articulo")
	protected String desArticulo;
	@XmlElement(name = "can_articulo")
	protected BigDecimal cantidad;
	@XmlElement(name = "precio_articulo_original")
	protected BigDecimal precioSinDtoOriginal;
	@XmlElement(name = "precio_articulo_aplicado")
	protected BigDecimal precioSinDtoAplicado;
	@XmlElement(name = "procede")
	protected Boolean successful;

	@XmlElement(name = "des_evento")
	protected String resolution;
	
	@XmlElement(name = "linea_referencia")
	protected Integer lineaReferencia; 

	private TicketAuditEvent(Type type) {
		super();
		this.type = type;
		this.moment = new Date();
	}

	public TicketAuditEvent() {
		super();
	}

	/**
	 * Crea evento auditoria a partir de un tipo y sesion. Este metodo se usa si hay un articulo asociado al evento
	 * 
	 * @param type		Tipo de evento (enum)
	 * @param linea		ILineaTicket
	 * @param sesion	Sesion
	 * @return			TicketAuditEvent
	 */
	public static TicketAuditEvent forEvent(Type type,ILineaTicket linea, Sesion sesion) {
		TicketAuditEvent res = new TicketAuditEvent(type);
		// uid actividad
		res.setUidActividad(sesion.getAplicacion().getUidActividad());
		// user stuff
		res.setIdUsuario(sesion.getSesionUsuario().getUsuario().getIdUsuario());
		res.setDesUsuario(sesion.getSesionUsuario().getUsuario().getDesusuario());

		res.setCodAlmacen(sesion.getAplicacion().getCodAlmacen());

		// line stuff
		LineaTicket lineaTicket = (LineaTicket) linea;
		res.setCodArticulo(lineaTicket.getCodArticulo());
		res.setDesArticulo(lineaTicket.getArticulo().getDesArticulo());
		res.setCantidad(lineaTicket.getCantidad());
		res.setPrecioSinDtoOriginal(lineaTicket.getPrecioTotalTarifaOrigen());
		if (type.equals(TicketAuditEvent.Type.CAMBIO_PRECIO)) {
			res.setPrecioSinDtoAplicado(lineaTicket.getPrecioTotalSinDto());
		}
		res.setLineaReferencia(linea.getIdLinea());

		return res;
	}

	/**
	 * Crea evento auditoria a partir de un tipo y sesion; solo se usa para anular el ticket
	 * 
	 * @param type		Tipo de evento de auditoria (enum)
	 * @param sesion	Sesion
	 * @return			TicketAuditEvent
	 */
	public static TicketAuditEvent forEvent(Type type, Sesion sesion) {
		TicketAuditEvent res = new TicketAuditEvent(type);
		// uid actividad
		res.setUidActividad(sesion.getAplicacion().getUidActividad());
		// user stuff
		res.setIdUsuario(sesion.getSesionUsuario().getUsuario().getIdUsuario());
		res.setDesUsuario(sesion.getSesionUsuario().getUsuario().getDesusuario());
		// ticket stuff
		res.setCodAlmacen(sesion.getAplicacion().getCodAlmacen());
		
		if(type == Type.ANULACION_TICKET) {
			res.setResolution(I18N.getTexto("Ticket descartado por")+" "+res.getDesUsuario());
			res.setSuccessful(true);
		}

		return res;
	}

	/**
	 * Incluye los datos de un usuario supervisor en un evento de auditoria
	 * 
	 * @param supervisor	UsuarioBean
	 */
	public void logSupervisor(UsuarioBean supervisor) {
		this.setIdUsuarioSupervisor(supervisor.getIdUsuario());
		this.setDesUsuarioSupervisor(supervisor.getDesusuario());
		this.close(true);
	}


	public void logUnauthorized() {
		this.close(false);
	}

	private String close(Boolean success) {
		String _resolution = I18N.getTexto(Types.get(this.type));
		if (success) {
			_resolution = _resolution + " "+I18N.getTexto("autorizado por")+" " + this.getDesUsuarioSupervisor();
		} else if (!success) {
			_resolution = _resolution +  " "+I18N.getTexto("denegado");
		} else {
			_resolution = _resolution + " "+I18N.getTexto("se quedo abierto");
		}
		_resolution = _resolution.substring(0, 1).toUpperCase() + _resolution.substring(1);
		setSuccessful(success);
		setResolution(_resolution);
		return resolution;
	}

	public Boolean isSuccessful() {
		return successful;
	}

	public Type getType() {
		return type;
	}

	public String getTypeAsString() {
		return I18N.getTexto(Types.get(this.type));
	}

	public String getUidActividad() {
		return uidActividad;
	}


	public Long getIdUsuario() {
		return idUsuario;
	}

	public String getDesUsuario() {
		return desUsuario;
	}

	public String getCodAlmacen() {
		return codAlmacen;
	}

	public Date getMoment() {
		return moment;
	}

	public Long getIdUsuarioSupervisor() {
		return idUsuarioSupervisor;
	}

	public String getDesUsuarioSupervisor() {
		return desUsuarioSupervisor;
	}

	public Boolean getsuccessful() {
		return successful;
	}

	public String getResolution() {
		return resolution;
	}

	public String getCodArticulo() {
		return codArticulo;
	}

	public String getDesArticulo() {
		return desArticulo;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioSinDtoOriginal() {
		return precioSinDtoOriginal;
	}

	public BigDecimal getPrecioSinDtoAplicado() {
		return precioSinDtoAplicado;
	}

	private void setCodArticulo(String codArticulo) {
		this.codArticulo = codArticulo;
	}

	private void setDesArticulo(String desArticulo) {
		this.desArticulo = desArticulo;
	}

	private void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	private void setPrecioSinDtoOriginal(BigDecimal precioSinDtoOriginal) {
		this.precioSinDtoOriginal = precioSinDtoOriginal;
	}

	private void setPrecioSinDtoAplicado(BigDecimal precioSinDtoAplicado) {
		this.precioSinDtoAplicado = precioSinDtoAplicado;
	}

	private void setUidActividad(String uidActividad) {
		this.uidActividad = uidActividad;
	}

	private void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	private void setDesUsuario(String desUsuario) {
		this.desUsuario = desUsuario;
	}

	private void setCodAlmacen(String codAlmacen) {
		this.codAlmacen = codAlmacen;
	}

	private void setIdUsuarioSupervisor(Long idUsuarioSupervisor) {
		this.idUsuarioSupervisor = idUsuarioSupervisor;
	}

	private void setDesUsuarioSupervisor(String desUsuarioSupervisor) {
		this.desUsuarioSupervisor = desUsuarioSupervisor;
	}

	private void setSuccessful(Boolean successful) {
		this.successful = successful;
	}

	private void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getUidTicketVenta() {
		return uidTicketVenta;
	}

	public void setUidTicketVenta(String uidTicketVenta) {
		this.uidTicketVenta = uidTicketVenta;
	}

	
	public Integer getLineaReferencia() {
		return lineaReferencia;
	}

	
	public void setLineaReferencia(Integer lineaReferencia) {
		this.lineaReferencia = lineaReferencia;
	}
	
}
