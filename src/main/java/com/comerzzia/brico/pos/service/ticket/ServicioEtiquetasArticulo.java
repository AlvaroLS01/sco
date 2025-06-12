package com.comerzzia.brico.pos.service.ticket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.comerzzia.core.model.etiquetas.categorias.EtiquetaBean;
import com.comerzzia.core.model.etiquetas.categorias.EtiquetaExample;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceBean;
import com.comerzzia.core.model.etiquetas.enlaces.EtiquetaEnlaceKey;
import com.comerzzia.core.persistencia.etiquetas.EtiquetaMapper;
import com.comerzzia.core.persistencia.etiquetas.enlaces.EtiquetaEnlaceMapper;
import com.comerzzia.pos.services.core.sesion.Sesion;

@Service
@Component
public class ServicioEtiquetasArticulo {

	protected Logger log = Logger.getLogger(ServicioEtiquetasArticulo.class);
	@Autowired
	protected EtiquetaEnlaceMapper etiquetaArticuloMapper;
	@Autowired
	protected EtiquetaMapper etiquetaMapper;
	@Autowired
	protected Sesion sesion;
	public static final String ID_CLASE_ETIQUETA_TIPOLOGIA = "D_ARTICULOS_TBL.CODART";

	/**
	 * Consulta las etiquetas tipologías
	 * 
	 * @return listaEtiquetas
	 */
	private List<EtiquetaBean> consultarEtiquetasTipologias() {
		List<EtiquetaBean> listaEtiquetas = null;
		try {
			List<String> etiquetasTipologiasList = new ArrayList<String>(Arrays.asList("producto precursor explosivo regulado", "aire acondicionado", "producto biocida uso profesional"));
			EtiquetaExample example = new EtiquetaExample();
			example.or().andEtiquetaIn(etiquetasTipologiasList);
			listaEtiquetas = etiquetaMapper.selectByExample(example);
		}
		catch (Exception e) {
			log.error("Ha ocurrido un error consultando las etiquetas tipologias: " + e.getMessage(), e);
		}
		return listaEtiquetas;
	}

	/**
	 * Consultamos el enlace de etiquetas y articulos para obtener la tipología
	 * 
	 * @param codArticulo
	 * @return tag.getEtiqueta()
	 */
	public String consultarEtiquetaArticulo(String codArticulo) {
		try {
			List<EtiquetaBean> etiquetasTipologias = consultarEtiquetasTipologias();
			if(etiquetasTipologias!=null && !etiquetasTipologias.isEmpty()) {
				for (EtiquetaBean tag : etiquetasTipologias) {
					EtiquetaEnlaceKey key = new EtiquetaEnlaceKey();
					key.setUidActividad(sesion.getAplicacion().getUidActividad());
					key.setIdObjeto(codArticulo);
					key.setUidEtiqueta(tag.getUidEtiqueta());
					key.setIdClase(ID_CLASE_ETIQUETA_TIPOLOGIA);
					EtiquetaEnlaceBean etiquetaArticulo = etiquetaArticuloMapper.selectByPrimaryKey(key);
					if (etiquetaArticulo != null) {
						return tag.getEtiqueta();
					}
				}
			}
		}
		catch (Exception e) {
			log.error("consultarEtiquetaArticulo() - Ha ocurrido un error obteniendo las etiquetas" + e.getMessage(), e);
		}

		return null;
	}
}
