package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.comerzzia.api.model.sales.ArticulosDevueltosBean;
import com.comerzzia.api.rest.client.tickets.ResponseGetTicketDev;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.BricodepotCabeceraTicket;
import com.comerzzia.brico.pos.selfcheckout.services.ticket.lineas.BricodepotLineaTicket;
import com.comerzzia.core.servicios.empresas.EmpresaException;
import com.comerzzia.core.servicios.tipodocumento.TipoDocumentoException;
import com.comerzzia.core.servicios.tipodocumento.TipoDocumentoNotFoundException;
import com.comerzzia.pos.core.dispositivos.Dispositivos;
import com.comerzzia.pos.core.dispositivos.dispositivo.fidelizacion.ConsultaTarjetaFidelizadoException;
import com.comerzzia.pos.gui.ventas.tickets.TicketManager;
import com.comerzzia.pos.persistence.articulos.ArticuloBean;
import com.comerzzia.pos.persistence.core.conceptosalmacen.ConceptoAlmacenBean;
import com.comerzzia.pos.persistence.core.documentos.tipos.TipoDocumentoBean;
import com.comerzzia.pos.persistence.core.usuarios.UsuarioBean;
import com.comerzzia.pos.persistence.fidelizacion.FidelizacionBean;
import com.comerzzia.pos.persistence.tickets.aparcados.TicketAparcadoBean;
import com.comerzzia.pos.services.core.conceptosalmacen.ConceptoNotFoundException;
import com.comerzzia.pos.services.core.conceptosalmacen.ConceptoService;
import com.comerzzia.pos.services.core.conceptosalmacen.ConceptoServiceException;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.documentos.Documentos;
import com.comerzzia.pos.services.core.usuarios.UsuarioNotFoundException;
import com.comerzzia.pos.services.core.usuarios.UsuariosService;
import com.comerzzia.pos.services.core.usuarios.UsuariosServiceException;
import com.comerzzia.pos.services.core.variables.VariablesServices;
import com.comerzzia.pos.services.mediospagos.MediosPagosService;
import com.comerzzia.pos.services.promociones.PromocionesServiceException;
import com.comerzzia.pos.services.ticket.TicketVenta;
import com.comerzzia.pos.services.ticket.TicketVentaAbono;
import com.comerzzia.pos.services.ticket.TicketsServiceException;
import com.comerzzia.pos.services.ticket.aparcados.TicketsAparcadosService;
import com.comerzzia.pos.services.ticket.lineas.LineaTicket;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketAbstract;
import com.comerzzia.pos.services.ticket.lineas.LineaTicketException;
import com.comerzzia.pos.services.ticket.pagos.PagoTicket;
import com.comerzzia.pos.services.ticket.promociones.PromocionLineaTicket;
import com.comerzzia.pos.util.bigdecimal.BigDecimalUtil;
import com.comerzzia.pos.util.i18n.I18N;
import com.comerzzia.pos.util.xml.MarshallUtil;

import javafx.stage.Stage;

@Scope("prototype")
@Primary
@Component
public class SelfCheckoutTicketManager extends TicketManager{
	
	public static final String PROPERTY_QR = "QR";
	public static final String PROPIEDAD_CODIGO_ANTICIPO = "POS.ARTICULO_ANTICIPO";
	
	protected String numeroTarjetaRegalo;

	@Autowired
	private ConceptoService conceptoService;
	
	@Autowired
	private Documentos documentosService;
	
	@Autowired
	private MediosPagosService mediosPagosService;
	
	@Autowired
    private UsuariosService usuariosService;
	
	@Autowired
	private TicketsAparcadosService ticketsAparcadosService;
	
	@Autowired
	private VariablesServices variablesServices;
	
	protected Boolean ventaIsAutorizada;
	
	public String getNumeroTarjetaRegalo() {
		return numeroTarjetaRegalo;
	}

	public void setNumeroTarjetaRegalo(String numeroTarjetaRegalo) {
		this.numeroTarjetaRegalo = numeroTarjetaRegalo;
	}
	
	public Boolean getVentaIsAutorizada() {
		return ventaIsAutorizada;
	}
	
	public void setVentaIsAutorizada(Boolean ventaIsAutorizada) {
		this.ventaIsAutorizada = ventaIsAutorizada;
	}

	@Override
	public void inicializarTicket() throws DocumentoException, PromocionesServiceException {
		super.inicializarTicket();
		
		setNumeroTarjetaRegalo(null);
		
		if(getVentaIsAutorizada() == null) {
			setVentaIsAutorizada(false);
		}
	}
	
	@Override
	public void finalizarTicket() {
		super.finalizarTicket();
		this.ventaIsAutorizada = null;
	}
	
	public void comprobarCantidadUnitaria(LineaTicketAbstract linea) throws LineaTicketException {
		ArticuloBean articulo = linea.getArticulo();
		if(articulo != null) {
			String balanzaTipoArticulo = articulo.getBalanzaTipoArticulo();
			if(StringUtils.isBlank(balanzaTipoArticulo) || TicketManager.UNITARIO_ARTICULO.equals(balanzaTipoArticulo.trim().toUpperCase())){
				try{
	        		linea.getCantidad().intValueExact();
	        	}catch(ArithmeticException e){
	        		throw new LineaTicketException(I18N.getTexto("Los artículos unitarios deben añadirse con una cantidad entera."));
	        	}
			}
		}
	}
	
	public boolean recuperarTicketConversion(String codigo, boolean controlarPlazoMaximoDevolucion)
	        throws TicketsServiceException, TipoDocumentoNotFoundException, TipoDocumentoException, EmpresaException {
		try {
			log.debug("recuperarTicketDevolucion() - Recuperando ticket...");
			byte[] xmlTicketOrigen = null;
			ResponseGetTicketDev datosDevolucion = null;
			TipoDocumentoBean tipoDocRecuperado = null;
			// Si es localizador
			// Obtenemos por localizador desde central
			try {
				tipoDocRecuperado = documentosService.getDocumento(Documentos.FACTURA_SIMPLIFICADA);

				xmlTicketOrigen = obtenerTicketDevolucionCentralLocalizador(codigo, true, tipoDocRecuperado.getIdTipoDocumento());
			}
			catch (LineaTicketException | DocumentoException e) {
				log.warn("recuperarTicketDevolucion() - Error al obtener ticket devolución desde central - " + e.getClass().getName() + " - " + e.getLocalizedMessage(), e);
			}

			if (xmlTicketOrigen != null) {
				// Si no null, buscamos datos devolucion
				tratarTicketRecuperado(xmlTicketOrigen);

				datosDevolucion = obtenerDatosDevolucion(ticketOrigen.getUidTicket());
			}

			// Si no tenemos ticket, consultamos como id de documento en lugar de como localizador
			if (xmlTicketOrigen == null) {
				// por codigo desde central
				try {
					xmlTicketOrigen = obtenerTicketDevolucionCentral(sesion.getAplicacion().getCodCaja(), sesion.getAplicacion().getCodAlmacen(), codigo, tipoDocRecuperado.getIdTipoDocumento());
				}
				catch (Exception e) {
					log.warn("recuperarTicketDevolucion() - Error al obtener ticket devolución desde central - " + e.getClass().getName() + " - " + e.getLocalizedMessage(), e);
				}

				if (xmlTicketOrigen != null) {
					// Si no null, buscamos datos devolucion
					tratarTicketRecuperado(xmlTicketOrigen);
					datosDevolucion = obtenerDatosDevolucion(ticketOrigen.getUidTicket());
				}
				else {
					throw new TicketsServiceException("No se ha encontrado ticket con codigo: " + codigo);
				}
			}
			
			if(datosDevolucion.getLineas() != null && !datosDevolucion.getLineas().isEmpty()) {
				for(ArticulosDevueltosBean line : datosDevolucion.getLineas()) {
					if(line.getCantidadDevuelta()> 0) {
						return true;
					}
				}
					asignarLineasDevueltas(datosDevolucion);
					descontarLineasNegativasTicketOrigen();
			}
			
		}
		catch (TicketsServiceException e) {
			log.error("recuperarTicketDevolucion() - " + e.getClass().getName() + " - " + e.getLocalizedMessage(), e);
			return false;
		}

		if (controlarPlazoMaximoDevolucion) {
			controlarPlazoMaximoDevolucion();
		}
    	
    	bloquearConversionArticulosAnticipo();

		return true;
	}

	@SuppressWarnings("unchecked")
	private void bloquearConversionArticulosAnticipo() throws TicketsServiceException {
		String codAnticipo = variablesServices.getVariableAsString(PROPIEDAD_CODIGO_ANTICIPO);
		for (LineaTicket linea : (List<LineaTicket>) ticketOrigen.getLineas()) {
			if (linea.getCodArticulo().equals(codAnticipo)) {
				throw new TicketsServiceException(I18N.getTexto("El ticket contiene un ANTICIPO y no se puede transformar. Pase por caja central para su transformación manual."));
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void addLineas() throws DocumentoException, LineaTicketException {
		log.debug("addLineas() - Añadiendo lineas al ticket de devolución");

		if (ticketOrigen == null || ticketOrigen.getLineas() == null) {
	        log.debug("addLineas() - El documento recuperado no tiene líneas");
	        return;
	    }

	    inicializarDocumentoDevolucion();

	    List<LineaTicket> lineasClonadas = clonarLineas(ticketOrigen.getLineas());
	    tratarLineasParaDevolucion(lineasClonadas);

	    ((TicketVentaAbono) getTicket()).getCantidadTotal();
	    completaLineaDevolucionPunto();
	}
	
	private void inicializarDocumentoDevolucion() throws DocumentoException {
	    setDocumentoActivo(sesion.getAplicacion().getDocumentos().getDocumento(Documentos.NOTA_CREDITO));
	    setEsDevolucion(true);
	    getTicket().getLineas().clear();
	}

	private List<LineaTicket> clonarLineas(List<LineaTicket> lineasOriginales) {
	    return lineasOriginales.stream()
	            .map(LineaTicket::clone)
	            .collect(Collectors.toList());
	}

	private void tratarLineasParaDevolucion(List<LineaTicket> lineasDevolucion) throws LineaTicketException {
	    for (LineaTicket original : lineasDevolucion) {
	        if (esAnticipo(original) && original.getImporteTotalConDto().compareTo(BigDecimal.ZERO) < 0) {
	            original.setCantidad(original.getCantidad().abs());
	            LineaTicket nuevaLinea = clonarLineaParaDevolucion(original);
	            nuevaLinea.setCantidad(original.getCantidad());
	        } else if (BigDecimalUtil.isMayorACero(original.getCantidadDisponibleDevolver())) {
	            clonarLineaParaDevolucion(original);
	        }
	    }
	}

	private boolean esAnticipo(LineaTicketAbstract linea) {
	    String codigoAnticipo = variablesServices.getVariableAsString(PROPIEDAD_CODIGO_ANTICIPO);
	    return linea.getCodArticulo().equals(codigoAnticipo);
	}
	

	private LineaTicket clonarLineaParaDevolucion(LineaTicket devolucion) throws LineaTicketException {
	    LineaTicket nuevaLinea = nuevaLineaArticulo(
	        devolucion.getCodArticulo(),
	        devolucion.getDesglose1(),
	        devolucion.getDesglose2(),
	        devolucion.getCantidadDisponibleDevolver(),
	        devolucion.getIdLinea()
	    );

	    devolucion.setCantidadADevolver(BigDecimal.ZERO);
	    nuevaLinea.setLineaDocumentoOrigen(devolucion.getIdLinea());

	    if (nuevaLinea instanceof BricodepotLineaTicket && devolucion instanceof BricodepotLineaTicket) {
	        BricodepotLineaTicket nuevaLineaBrico = (BricodepotLineaTicket) nuevaLinea;
	        BricodepotLineaTicket lineaOrigenBrico = (BricodepotLineaTicket) devolucion;

	        nuevaLineaBrico.setConversionAFT(lineaOrigenBrico.isConversionAFT());
	        nuevaLineaBrico.setPorcentajeIvaConversion(lineaOrigenBrico.getPorcentajeIvaConversion());
	        nuevaLineaBrico.setIsAnticipo(true);
	    }

	    nuevaLinea.setPrecioTarifaOrigen(devolucion.getPrecioTarifaOrigen());
	    nuevaLinea.setPrecioTotalTarifaOrigen(devolucion.getPrecioTotalTarifaOrigen());

	    return nuevaLinea;
	}
	
	@Override
	public void completaLineaDevolucionPunto() {
		for (LineaTicketAbstract linea : (List<LineaTicketAbstract>) getTicket().getLineas()) {
			if (getTicketOrigen() != null) {
				BigDecimal puntosConcedidos = getPuntosConcedidosLinea(linea.getCodArticulo(), linea.getLineaDocumentoOrigen(), linea.getCantidad());
				double puntos = (puntosConcedidos != null) ? puntosConcedidos.doubleValue() : 0.0;
				linea.setPuntosADevolver(puntos);
			}
		}
	}
	
	@Override
	protected BigDecimal getPuntosConcedidosLinea(String codArticulo, Integer lineaOrigen, BigDecimal cantidadLineaDevolucion) {
		List<LineaTicketAbstract> lineasOriginales = getTicketOrigen().getLinea(codArticulo);
		for (LineaTicketAbstract lineaOriginal : lineasOriginales) {
			if (lineaOriginal.getIdLinea().equals(lineaOrigen) && lineaOriginal.getCantidadDevuelta().compareTo(lineaOriginal.getCantidad()) <= 0) {
				BigDecimal puntosLinea = BigDecimal.ZERO;
				if (((LineaTicket) lineaOriginal).getDocumentoOrigen() != null) {
					for (PromocionLineaTicket promo : ((LineaTicket) lineaOriginal).getDocumentoOrigen().getPromociones()) {
						if (promo.getPuntos() == null) {
							promo.setPuntos(BigDecimal.ZERO);
						}
						if (promo.getPuntos().compareTo(BigDecimal.ZERO) > 0) {
							puntosLinea = puntosLinea.add(promo.getPuntos());
						}
					}
				}
				for (PromocionLineaTicket promo : lineaOriginal.getPromociones()) {
					if (promo.getPuntos() == null) {
						promo.setPuntos(BigDecimal.ZERO);
					}
					if (promo.getPuntos().compareTo(BigDecimal.ZERO) > 0) {
						puntosLinea = puntosLinea.add(promo.getPuntos());
					}
				}
				BigDecimal factor = cantidadLineaDevolucion.divide(lineaOriginal.getCantidad(), 4, RoundingMode.HALF_UP).abs();
				return puntosLinea.multiply(factor);
			}
		}
		return null;
	}


	@SuppressWarnings({ "unchecked" })
	public void addPagos() throws ConceptoServiceException, ConceptoNotFoundException {
		log.debug("addPagos() - Añadiendo pagos al ticket de devolución");

		if (getTicketOrigen() == null || getTicketOrigen().getPagos() == null) {
			log.debug("addPagos() - El documento recuperado no tiene pagos");
		}

		getTicket().setPagos(getTicketOrigen().getPagos());
		BigDecimal factorSigno = obtenFactorSigno();
		if (!getTicket().getPagos().isEmpty()) {
			for (PagoTicket pago : (List<PagoTicket>) getTicket().getPagos()) {
				pago.setImporte(pago.getImporte().multiply(factorSigno));
			}
		}
	}

	private BigDecimal obtenFactorSigno() throws ConceptoServiceException, ConceptoNotFoundException { // SOS-155
		log.debug("obtenFactorSigno() - Obteniendo signo para documento de devolución");

		BigDecimal factorSigno = BigDecimal.ONE.negate();
		ConceptoAlmacenBean cocAlm = conceptoService.consultarConcepto(sesion.getAplicacion().getUidActividad(), getDocumentoActivo().getCodaplicacion(), getDocumentoActivo().getCodconalm());
		if (StringUtils.isNotBlank(cocAlm.getSigno()) && cocAlm.getSigno().equals("+")) {
			factorSigno = factorSigno.negate();
		}

		log.debug("obtenFactorSigno() Signo: " + (BigDecimalUtil.isMenorACero(factorSigno) ? " - " : " + "));

		return factorSigno;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void recuperarTicket(Stage stage, TicketAparcadoBean ticketAparcado) throws TicketsServiceException, PromocionesServiceException, DocumentoException, LineaTicketException {
		log.debug("recuperarTicket() - Recuperando ticket...");
        
        nuevoTicket();
        // Realizamos el unmarshall
        log.debug("Ticket recuperado:\n"+new String(ticketAparcado.getTicket()));
        @SuppressWarnings("rawtypes")
		TicketVenta ticketRecuperado = (TicketVentaAbono) MarshallUtil.leerXML(ticketAparcado.getTicket(), getTicketClasses(documentoActivo).toArray(new Class[]{}));

        ticketPrincipal.getCabecera().setIdTicket(ticketRecuperado.getIdTicket());
        ticketPrincipal.getCabecera().setUidTicket(ticketRecuperado.getUidTicket());
        ticketPrincipal.getCabecera().setCodTicket(ticketRecuperado.getCabecera().getCodTicket());
        ticketPrincipal.getCabecera().setSerieTicket(ticketRecuperado.getCabecera().getSerieTicket());
        
        String tipoDocumentoFacturaDirecta = getDocumentoActivo().getTipoDocumentoFacturaDirecta();
		if(ticketRecuperado.getCabecera().getCodTipoDocumento().equals(tipoDocumentoFacturaDirecta)) {
        	 setDocumentoActivo(sesion.getAplicacion().getDocumentos().getDocumento(tipoDocumentoFacturaDirecta));
        }
        
        if(ticketAparcado.getUsuario() == null || !ticketAparcado.getUsuario().equals("FASTPOS")){
        	// Recuperamos el cliente del ticket aparcado
        	ticketPrincipal.getCabecera().setCliente(ticketRecuperado.getCabecera().getCliente());
        }
	    String uidDiarioCaja = sesion.getSesionCaja().getUidDiarioCaja();
        ticketPrincipal.getCabecera().setUidDiarioCaja(uidDiarioCaja);
        
        recuperarDatosPersonalizados(ticketRecuperado);

        List<LineaTicket> lineas = ticketRecuperado.getLineas();
        for (LineaTicket lineaRecuperada : lineas) {
			String codigo = lineaRecuperada.getCodigoBarras();
			String desglose1 = lineaRecuperada.getDesglose1();
			String desglose2 = lineaRecuperada.getDesglose2();
			if(StringUtils.isBlank(codigo)) {
				codigo = lineaRecuperada.getCodArticulo();
			}
			else {
				desglose1 = null;
				desglose2 = null;
			}
			LineaTicket nuevaLineaArticulo = nuevaLineaArticulo(codigo, desglose1, desglose2, lineaRecuperada.getCantidad(), null, null, false, false);
			
			nuevaLineaArticulo.setDocumentoOrigen(lineaRecuperada.getDocumentoOrigen());
			
			nuevaLineaArticulo.setDesArticulo(lineaRecuperada.getDesArticulo());
			nuevaLineaArticulo.setDescuentoManual(lineaRecuperada.getDescuentoManual());
			BigDecimal nuevoPrecio = lineaRecuperada.getPrecioTotalSinDto();
			nuevaLineaArticulo.setPrecioTotalSinDto(nuevoPrecio);
			BigDecimal precioSinDto = lineaRecuperada.getPrecioSinDto();
			nuevaLineaArticulo.setPrecioSinDto(precioSinDto);
			nuevaLineaArticulo.setCodigoBarras(lineaRecuperada.getCodigoBarras());
			nuevaLineaArticulo.setNumerosSerie(lineaRecuperada.getNumerosSerie());
			nuevaLineaArticulo.setEditable(lineaRecuperada.isEditable());
			
			String sellerName = lineaRecuperada.getVendedor().getUsuario();
			try {
				UsuarioBean seller = usuariosService.consultarUsuario(sellerName);
				nuevaLineaArticulo.setVendedor(seller);
			} catch (UsuarioNotFoundException e) {
				// active user
				log.warn("recuperarTicket() - No se ha encontrado el usuario: " + sellerName);
			} catch (UsuariosServiceException e) {
				// active user
				log.warn("recuperarTicket() - Se ha producido un error al consultar el: " + sellerName);
			}
			recuperarDatosPersonalizadosLinea(lineaRecuperada, nuevaLineaArticulo);
		}
        
        FidelizacionBean datosFidelizado = ticketRecuperado.getCabecera().getDatosFidelizado();
		if(datosFidelizado!=null){
        	try {
				FidelizacionBean tarjetaFidelizado = Dispositivos.getInstance().getFidelizacion().consultarTarjetaFidelizado(stage, datosFidelizado.getNumTarjetaFidelizado(), ticketPrincipal.getCabecera().getUidActividad());
				ticketPrincipal.getCabecera().setDatosFidelizado(tarjetaFidelizado);
			} catch (ConsultaTarjetaFidelizadoException e) {
				log.debug("recuperarTicket() - Error al consultar fidelizado", e);
				FidelizacionBean fidelizacionBean = new FidelizacionBean();
				fidelizacionBean.setNumTarjetaFidelizado(datosFidelizado.getNumTarjetaFidelizado());
				ticketPrincipal.getCabecera().setDatosFidelizado(fidelizacionBean);
			}
        }
		
		for(PagoTicket pago : (List<PagoTicket>) ticketRecuperado.getPagos()) {
			pago.setMedioPago(mediosPagosService.getMedioPago(pago.getCodMedioPago()));
			if(pago.getGiftcards()!=null && !pago.getGiftcards().isEmpty()) {
				pago.getExtendedData().put("PARAM_TARJETA", pago.getGiftcards().get(0));
			}
			ticketPrincipal.getPagos().add(pago);
		}
        
        recalcularConPromociones();
        
        // Establecemos el contador
        contadorLinea = ticketPrincipal.getLineas().size()+1;
        //Eliminamos el ticket recuperado de la lista de tickets aparcados.
        ticketsAparcadosService.eliminarTicket(ticketAparcado.getUidTicket());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void descontarLineasNegativasTicketOrigen() {
		List<LineaTicket> lineasNegativas = new ArrayList<LineaTicket>();
    	 
    	Iterator<LineaTicket> it = ticketOrigen.getLineas().iterator();
    	while(it.hasNext()) {
    		LineaTicket linea = it.next();
    		if(BigDecimalUtil.isMenorACero(linea.getImporteTotalConDto()) || linea.getLineaDocumentoOrigen() != null)  {
    			if (!Boolean.TRUE.equals(((BricodepotCabeceraTicket) ticketOrigen.getCabecera()).getFsToFt())
    				    && !variablesServices.getVariableAsString(PROPIEDAD_CODIGO_ANTICIPO).equals(linea.getCodArticulo())) {
    				    lineasNegativas.add(linea);
    				    it.remove();
    				}
    		}
    	}
	}
	
	@Override
	public void actualizarCantidadesOrigenADevolver(LineaTicketAbstract linea, BigDecimal cantidad) {
        BigDecimal oldCantidadADevolver = linea.getCantidadADevolver();
        linea.setCantidadADevolver(cantidad.abs());
        BigDecimal cantidadADevolver = linea.getCantidadADevolver();
        if(BigDecimalUtil.isMenorACero(cantidadADevolver) && !linea.getCodArticulo().equals(variablesServices.getVariableAsString(PROPIEDAD_CODIGO_ANTICIPO))){
        	log.error("actualizarCantidadesOrigenADevolver() - Ha habido un error al ser la cantidad a devolver de la linea " + linea.toString() + " inferior a 0.");
        	linea.setCantidadADevolver(oldCantidadADevolver);
        	//Error en la programación, nunca debería ser menor a 0, hay que validar antes
        	throw new RuntimeException();
        }
    }
}
