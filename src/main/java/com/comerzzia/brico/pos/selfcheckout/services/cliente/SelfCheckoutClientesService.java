package com.comerzzia.brico.pos.selfcheckout.services.cliente;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.core.util.mybatis.session.SqlSession;
import com.comerzzia.pos.persistence.clientes.ClienteBean;
import com.comerzzia.pos.persistence.clientes.ClienteExample;
import com.comerzzia.pos.persistence.clientes.ClienteExample.Criteria;
import com.comerzzia.pos.persistence.mybatis.SessionFactory;
import com.comerzzia.pos.services.clientes.ClientesService;
import com.comerzzia.pos.services.clientes.ClientesServiceException;

@Service
@Primary
public class SelfCheckoutClientesService extends ClientesService {

	
	 public List<ClienteBean> consultarClientes(String codTipoIdent, String ident) throws ClientesServiceException {
	        log.debug("consultarClientesDatosObligatorios() - consultando clientes con codTipoIdent: " + codTipoIdent + " y ident:" + ident);
	        List<ClienteBean> res = null;
	        SqlSession sqlSession = new SqlSession();
	        try {
	        	if(StringUtils.isNotBlank(ident)) {
		            sqlSession.openSession(SessionFactory.openSession());
		            String uidActividad = sesion.getAplicacion().getUidActividad();
		            ClienteExample example = new ClienteExample();
		            
		            Criteria or = example.createCriteria();
		            or.andUidActividadEqualTo(uidActividad);
		            or.andCifEqualTo(ident.trim());
		            
		            res = clienteMapper.selectByExample(example);
		            Integer idGrupoImpuestos = sesion.getImpuestos().getGrupoImpuestos().getIdGrupoImpuestos();
		            for (ClienteBean cliente : res) {
		            	cliente.setIdGrupoImpuestos(idGrupoImpuestos);
					}
	            }
	        }
	        catch (Exception e) {
	            String msg = "Se ha producido un error consultando clientes:" + e.getMessage();
	            log.error("consultarClientes() - " + msg, e);
	            throw new ClientesServiceException(e);
	        } finally {
	            sqlSession.close();
	        }
	        
	        
	        return res;   
	    }
	
}
