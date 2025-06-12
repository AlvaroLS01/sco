package com.comerzzia.brico.pos.selfcheckout.persistence.motivos;

public class MotivoKey {
    private String uidActividad;

    private String codigo;

    public String getUidActividad() {
        return uidActividad;
    }

    public void setUidActividad(String uidActividad) {
        this.uidActividad = uidActividad == null ? null : uidActividad.trim();
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo == null ? null : codigo.trim();
    }
}