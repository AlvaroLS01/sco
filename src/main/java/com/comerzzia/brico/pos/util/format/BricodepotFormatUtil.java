package com.comerzzia.brico.pos.util.format;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.log4j.Logger;

import com.comerzzia.pos.util.format.FormatUtil;

public class BricodepotFormatUtil extends FormatUtil{

	protected static final Logger log = Logger.getLogger(BricodepotFormatUtil.class);
	private static final DateTimeFormatter dfTicket = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	private static BricodepotFormatUtil instance;


	public static synchronized BricodepotFormatUtil getInstance() {
        if (instance == null) {
            instance = new BricodepotFormatUtil();
        }
        return instance;
    }
	
	@Override
	public Date desformateaFechaHoraTicket(String date) {
		try {
			log.debug("desformateaFechaHoraTicket() - Desformateando fecha: " + date);
            LocalDateTime localDateTime = LocalDateTime.parse(date, dfTicket);
            
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("desformateaFechaHoraTicket() - Error al desformatear la fecha: " + date + " " + e.getMessage());
            return null;
        }
	}
}