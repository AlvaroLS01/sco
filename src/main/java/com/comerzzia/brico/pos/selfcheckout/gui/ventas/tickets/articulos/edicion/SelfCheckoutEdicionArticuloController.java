package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.articulos.edicion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.selfcheckout.gui.motivos.MotivoController;
import com.comerzzia.brico.pos.selfcheckout.gui.motivos.MotivoView;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationController;
import com.comerzzia.brico.pos.selfcheckout.gui.ventas.audit.RequestAuthorizationView;
import com.comerzzia.brico.pos.selfcheckout.persistence.motivos.Motivo;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.audit.TicketAuditEvent;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas.BricodepotLineaTicket;
import com.comerzzia.pos.gui.ventas.tickets.articulos.FacturacionArticulosController;
import com.comerzzia.pos.gui.ventas.tickets.articulos.edicion.EdicionArticuloController;
import com.comerzzia.pos.persistence.core.usuarios.UsuarioBean;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.util.format.FormatUtil;

import javafx.fxml.FXML;

@Component
@Primary
public class SelfCheckoutEdicionArticuloController extends EdicionArticuloController {

	public static final String PARAMETRO_TIPO_MOTIVO = "tipoMotivo";
	public static final String PARAMETRO_LINEA = "linea";
	
	protected String comentario;
	
	@Override
	public void initializeComponents() {
		super.initializeComponents();
	}
	// log
		private static final Logger log = Logger.getLogger(EdicionArticuloController.class.getName());

		public static final String TIPO_MOTIVO_CAMBIO_PRECIO_MANUAL = "cambioPrecioManual";
		
		@Autowired
		private Sesion sesion;

		@Override
		@FXML
		public void accionAceptar() {
			log.debug("accionAceptar()");
			BigDecimal precio;

			try {
				precio = FormatUtil.getInstance().desformateaBigDecimal(tfCantidad.getText(), 3);
				tfCantidad.setText(FormatUtil.getInstance().formateaNumero(precio, 3));
			} catch (Exception e) {
				//
			}

			frEdicionArticulo.setCantidad(tfCantidad.getText());

			if (!bVentaProfesional) {
				try {
					precio = FormatUtil.getInstance().desformateaBigDecimal(tfPrecio.getText(), 2);
					tfPrecio.setText(FormatUtil.getInstance().formateaImporte(precio));
				} catch (Exception e) {
					//
				}

				frEdicionArticulo.setPrecioFinalProfesional("0");
				frEdicionArticulo.setPrecioFinal(tfPrecio.getText());
			} else {
				try {
					precio = FormatUtil.getInstance().desformateaBigDecimal(tfPrecio.getText(), 4);
					tfPrecio.setText(FormatUtil.getInstance().formateaNumero(precio, 4));
				} catch (Exception e) {
					//
				}

				frEdicionArticulo.setPrecioFinal("0");
				frEdicionArticulo.setPrecioFinalProfesional(tfPrecio.getText());
			}

			frEdicionArticulo.setDescuento(tfDescuento.getText());
			frEdicionArticulo.setVendedor((UsuarioBean) cbVendedor.getSelectionModel().getSelectedItem());
			frEdicionArticulo.setDesArticulo(tfDescripcion.getText());

			if (validarFormularioEdicionArticulo() && sonNumerosSerieValidos()) {
				BigDecimal nuevaCantidad = frEdicionArticulo.getCantidadAsBD();

				BigDecimal precioSinImpuestos = sesion.getImpuestos().getPrecioSinImpuestos(
						linea.getCodImpuesto(), linea.getPrecioTotalSinDto(),
						ticketManager.getTicket().getIdTratImpuestos());

				List<TicketAuditEvent> events = new ArrayList<>();
				// CAMBIO PRECIO
				if (!precioSinImpuestos.equals(lineaOriginal.getPrecioSinDto())) {
					TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.CAMBIO_PRECIO,linea, sesion);
					events.add(auditEvent);
				}
				// NEGAR CANTIDAD
				if(nuevaCantidad.compareTo(new BigDecimal(0d))<0 && !nuevaCantidad.equals(lineaOriginal.getCantidad())) {
					TicketAuditEvent auditEvent = TicketAuditEvent.forEvent(TicketAuditEvent.Type.DEVOLUCION,linea, sesion);
					events.add(auditEvent);
				}else {
					lineaOriginal.setCantidad(
							ticketManager.tratarSignoCantidad(nuevaCantidad, linea.getCabecera().getCodTipoDocumento()));
				}
				
				if(events.size()>0) {
					abrirVentanaAutorizacion(events,getDatos()); 
					if(events.stream().anyMatch(e -> e.getType() == TicketAuditEvent.Type.DEVOLUCION)){
						if(getDatos().get(RequestAuthorizationController.PERMITIR_ACCION).equals(true)) {
							lineaOriginal.setCantidad(
									ticketManager.tratarSignoCantidad(nuevaCantidad, linea.getCabecera().getCodTipoDocumento()));
						}else {
							super.accionCancelar();
						}
					}
					if(events.stream().anyMatch(e -> e.getType() == TicketAuditEvent.Type.CAMBIO_PRECIO)){
						if (getDatos().get(RequestAuthorizationController.PERMITIR_ACCION).equals(true)) {

							if (!abrirVentanaMotivo()) {
								super.accionCancelar();
							}
							else {
								cambioPrecioManual();
								lineaOriginal.setPrecioSinDto(precioSinImpuestos);
								lineaOriginal.setPrecioTotalSinDto(linea.getPrecioTotalSinDto());
								lineaOriginal.setDescuentoManual(linea.getDescuentoManual());
							}
						}else {
							super.accionCancelar();
						}
					}
				}
				
				lineaOriginal.setVendedor(frEdicionArticulo.getVendedor());
				lineaOriginal.setDesArticulo(frEdicionArticulo.getDesArticulo());
				lineaOriginal.setNumerosSerie(linea.getNumerosSerie());
				lineaOriginal.recalcularImporteFinal();

				if(aplicarPromociones) {
	            	ticketManager.recalcularConPromociones();
	            }
	            if(lineaOriginal.tieneCambioPrecioManual()){
	            	cambioPrecioManual();
	            }
	            
	            if(lineaOriginal.tieneDescuentoManual()){
	            	cambioDescuentoManual();
	            	
	            }
				getStage().close();
			}
		}
		
		protected void abrirVentanaAutorizacion(List<TicketAuditEvent> auditEvent, HashMap<String, Object> datos) {
			datos.put(RequestAuthorizationController.AUDIT_EVENT, auditEvent);
			datos.put(FacturacionArticulosController.TICKET_KEY, ticketManager);
			getApplication().getMainView().showModalCentered(RequestAuthorizationView.class, datos, this.getStage());
		}

		private Boolean abrirVentanaMotivo() {
			HashMap<String, Object> datosVentana = new HashMap<String, Object>();
			datosVentana.put("linea", ((BricodepotLineaTicket) linea));
			// Motivo cambioPrecioManual
			datosVentana.put("tipoMotivo", 2);

			getApplication().getMainView().showModalCentered(MotivoView.class, datosVentana, getStage());
			
			Motivo motivo = (Motivo) datosVentana.get(MotivoController.PARAMETRO_MOTIVO);
			if (motivo != null) {
				((BricodepotLineaTicket) lineaOriginal).setMotivo(motivo);
			}
			else {
				return false;
			}

			return true;
		}
}
