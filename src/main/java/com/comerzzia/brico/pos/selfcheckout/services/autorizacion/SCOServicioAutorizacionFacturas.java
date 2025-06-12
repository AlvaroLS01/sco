package com.comerzzia.brico.pos.selfcheckout.services.autorizacion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.comerzzia.pos.services.core.sesion.Sesion;
import com.comerzzia.pos.services.core.variables.VariablesServices;

@Service
public class SCOServicioAutorizacionFacturas {

	protected Logger log = Logger.getLogger(SCOServicioAutorizacionFacturas.class);

	private static final String VARIABLE_VALIDACION_CREAR_FACTURA = "X_SCO.VALIDACION_CREAR_FACTURA";
	private static final String SEPARADOR_PAISES = ",";

	@Autowired
	private VariablesServices variablesServices;

	@Autowired
	private Sesion sesion;

	/**
	 * Verifica si el país de la tienda actual requiere autorización para facturas
	 * 
	 * @return true si requiere autorización, false en caso contrario
	 */
	public boolean requiereAutorizacionPais() {
		List<String> paisesNecesarioAutorizacion = getPaisesNecesariaAutorizacion();
		String paisTienda = sesion.getAplicacion().getTienda().getCliente().getCodpais();
		
		log.debug("requiereAutorizacionPais() - Verificando autorización: país de la tienda = " + paisTienda + " | Lista de países autorizados: " + paisesNecesarioAutorizacion);
		
		return !paisesNecesarioAutorizacion.isEmpty() && paisesNecesarioAutorizacion.contains(paisTienda);
	}

	private List<String> getPaisesNecesariaAutorizacion() {
		String paisesNecesarioAutorizacion = variablesServices.getVariableAsString(VARIABLE_VALIDACION_CREAR_FACTURA);
		if (paisesNecesarioAutorizacion == null || paisesNecesarioAutorizacion.isEmpty()) {
			log.debug("No hay países configurados que requieran autorización");
			return Collections.emptyList();
		}

		return Arrays.stream(paisesNecesarioAutorizacion.split(SEPARADOR_PAISES))
				.map(String::trim)
				.map(String::toUpperCase)
				.collect(Collectors.toList());
	}

}
