package com.comerzzia.brico.pos.selfcheckout.services.colectivos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.comerzzia.brico.pos.selfcheckout.persistence.colectivos.Colectivos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ColectivosParser {

	public List<Colectivos> parsearColectivos(String resourcePath) throws IOException {
		URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath);

        // Validar que el recurso no sea nulo
        if (resourceUrl == null) {
            throw new IOException("No se pudo encontrar el archivo: " + resourcePath);
        }

        // Convertir el URL a un archivo
        File file = new File(resourceUrl.getFile());

        // Usar Jackson para parsear el archivo JSON
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(file, new TypeReference<List<Colectivos>>() {});
	}
}
