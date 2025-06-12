package com.comerzzia.brico.pos.selfcheckout.services.intervenciones;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comerzzia.brico.pos.gui.ventas.tickets.SesionTicketManager;
import com.comerzzia.core.model.tiposdocumentos.TipoDocumentoBean;
import com.comerzzia.core.servicios.empresas.EmpresaException;
import com.comerzzia.core.servicios.sesion.DatosSesionBean;
import com.comerzzia.core.servicios.tipodocumento.TipoDocumentoException;
import com.comerzzia.core.servicios.tipodocumento.TipoDocumentoNotFoundException;
import com.comerzzia.core.servicios.tipodocumento.TiposDocumentosService;
import com.comerzzia.core.util.mybatis.session.SqlSession;
import com.comerzzia.pos.persistence.core.usuarios.UsuarioBean;
import com.comerzzia.pos.persistence.mybatis.SessionFactory;
import com.comerzzia.pos.persistence.tickets.TicketBean;
import com.comerzzia.pos.services.core.contadores.ServicioContadores;
import com.comerzzia.pos.services.core.documentos.DocumentoException;
import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.promociones.PromocionesServiceException;
import com.comerzzia.pos.services.ticket.TicketsService;
import com.comerzzia.pos.util.xml.MarshallUtil;

@SuppressWarnings("deprecation")
@Component
public class IntervencionesService {
	
	private Logger log = Logger.getLogger(IntervencionesService.class);
	
	private static final Long ID_LOGIN_SCO_DOCUMENTO = 700000L; 
	public static final String LOGIN_SCO = "LOGIN_SCO";
	
	@Autowired
	private Sesion sesion;
	
	@Autowired
	protected SesionTicketManager sesionTicketManager;

	@Autowired
	private TicketsService ticketsService;
	
	@Autowired
	private ServicioContadores servicioContadores;
	
	@Autowired
	private TiposDocumentosService tiposDocumentosService;
	
	public void crearIntervencion() throws EmpresaException, TipoDocumentoNotFoundException, TipoDocumentoException, DocumentoException, PromocionesServiceException {
		log.debug("crearIntervencion()");
		
		TicketBean ticket = new TicketBean();
		IntervencionesDto inter = new IntervencionesDto();
		DatosSesionBean datosSesion = new DatosSesionBean();
		
		String uidActividad = sesion.getAplicacion().getUidActividad();
		datosSesion.setUidActividad(uidActividad);		
		inter.setUidActividad(uidActividad);
		ticket.setUidActividad(uidActividad);

		TipoDocumentoBean docLoginSCO =  tiposDocumentosService.consultar(datosSesion, ID_LOGIN_SCO_DOCUMENTO);
		ticket.setIdTipoDocumento(docLoginSCO.getIdTipoDocumento());
		
		String uidTicket = sesionTicketManager.getSesionTicketManager().getTicket().getCabecera().getUidTicket();
		inter.setUidTicket(uidTicket);
		ticket.setUidTicket(uidTicket);
		ticket.setLocatorId(uidTicket);
		
		String codalm = sesion.getAplicacion().getCodAlmacen();
		inter.setCodalm(codalm);
		ticket.setCodAlmacen(codalm);
		
		String codcaja = sesion.getAplicacion().getCodCaja();
		inter.setCodcaja(codcaja);
		ticket.setCodcaja(codcaja);
		
		Date fecha = new Date();
		inter.setFecha(fecha);
		ticket.setFecha(fecha);
		
		UsuarioBean usuario = sesion.getSesionUsuario().getUsuario();
		inter.setIdUsuario(usuario.getIdUsuario());
		
		ticket.setCodTicket("*");
		ticket.setFirma("*");
		ticket.setSerieTicket("*");
		
		log.debug("crearIntervencion() - Guardando intervención en BBDD");
		
		SqlSession sqlSession = new SqlSession();
		
		try {
			ticket.setIdTicket(servicioContadores.obtenerValorContador(LOGIN_SCO, uidActividad));
			
			byte[] xml = MarshallUtil.crearXML(inter);
			log.debug("guardarAuditoria() - XML de intervención: " + new String(xml));
			ticket.setTicket(xml);
			
			sqlSession.openSession(SessionFactory.openSession());
			ticketsService.insertarTicket(sqlSession, ticket, false);
			sqlSession.commit();

		}catch(Exception e) {
			log.error("crearIntervencion() - Ha habido un error al guardar la intervención: " + e.getMessage(), e);
			sqlSession.rollback();
		}
		finally {
			sqlSession.close();
		}
	}

}
