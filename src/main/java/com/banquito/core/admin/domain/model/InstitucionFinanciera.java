package com.banquito.core.admin.domain.model;

import com.banquito.core.admin.domain.enums.EstadoInstitucionFinancieraEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "INSTITUCION_FINANCIERA")
public class InstitucionFinanciera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Integer id;

    @Column(name = "ROUTING_CODE", length = 20, nullable = false)
    private String routingCode;

    @Column(name = "NOMBRE", length = 150, nullable = false)
    private String nombre;

    @Column(name = "PREFIJO_CUENTA", length = 10)
    private String prefijoCuenta;

    @Column(name = "ES_BANQUITO", nullable = false)
    private Boolean esBanquito;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 15, nullable = false)
    private EstadoInstitucionFinancieraEnum estado;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    public InstitucionFinanciera() {}
    public InstitucionFinanciera(Integer id) { this.id = id; }

    public static InstitucionFinanciera crear(String routingCode, String nombre, Boolean esBanquito) {
        return crear(routingCode, nombre, null, esBanquito);
    }

    public static InstitucionFinanciera crear(String routingCode, String nombre, String prefijoCuenta, Boolean esBanquito) {
        InstitucionFinanciera i = new InstitucionFinanciera();
        i.routingCode = routingCode;
        i.nombre = nombre;
        i.prefijoCuenta = normalizarPrefijo(prefijoCuenta);
        i.esBanquito = esBanquito != null && esBanquito;
        i.estado = EstadoInstitucionFinancieraEnum.ACTIVA;
        return i;
    }
    public void actualizar(String nombre, String prefijoCuenta, Boolean esBanquito, EstadoInstitucionFinancieraEnum estado) {
        this.nombre = nombre;
        this.prefijoCuenta = normalizarPrefijo(prefijoCuenta);
        this.esBanquito = esBanquito;
        this.estado = estado;
    }
    private static String normalizarPrefijo(String prefijoCuenta) {
        return prefijoCuenta == null || prefijoCuenta.isBlank() ? null : prefijoCuenta.trim();
    }
    @PrePersist public void prePersist() { LocalDateTime now = LocalDateTime.now(); if (estado == null) estado = EstadoInstitucionFinancieraEnum.ACTIVA; if (esBanquito == null) esBanquito = false; if (fechaCreacion == null) fechaCreacion = now; if (fechaActualizacion == null) fechaActualizacion = now; }
    @PreUpdate public void preUpdate() { fechaActualizacion = LocalDateTime.now(); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof InstitucionFinanciera that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "InstitucionFinanciera{" + "id=" + id + ", routingCode='" + routingCode + '\'' + ", nombre='" + nombre + '\'' + ", estado=" + estado + '}'; }
}
