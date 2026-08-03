package com.banquito.core.admin.application.service;

import com.banquito.core.admin.api.dto.api.*;
import com.banquito.core.admin.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AdminMapper {
    public BranchResponse toBranchResponse(Sucursal s) {
        return new BranchResponse(s.getUuidSucursal(), s.getCodigoSucursal(), s.getNombre(), s.getCiudad(), s.getDireccion(), s.getEstado().name(), s.getFechaCreacion(), s.getFechaActualizacion());
    }
    public HolidayResponse toHolidayResponse(Feriado f) {
        return new HolidayResponse(f.getFechaFeriado(), f.getNombre(), f.getEsFinSemana(), f.getEstado().name());
    }
    public ParameterResponse toParameterResponse(ParametroCore p) {
        return new ParameterResponse(p.getCodigo(), p.getNombre(), p.getValorTexto(), p.getTipoDato().name(), p.getDescripcion(), p.getEstado().name());
    }
    public OperationalWindowResponse toOperationalWindowResponse(VentanaOperativa v) {
        return new OperationalWindowResponse(v.getCodigo(), v.getNombre(), v.getDominioOperativo().name(), v.getHoraInicio(), v.getHoraCorte(), v.getHoraFin(), v.getDiasAplica(), v.getTimezone(), v.getAccionDespuesCorte().name(), v.getEstado().name());
    }
    public FinancialInstitutionResponse toFinancialInstitutionResponse(InstitucionFinanciera i) {
        return new FinancialInstitutionResponse(i.getRoutingCode(), i.getNombre(), i.getPrefijoCuenta(), i.getEsBanquito(), i.getEstado().name());
    }
    public AccountSubtypeResponse toAccountSubtypeResponse(SubtipoCuenta s) {
        return new AccountSubtypeResponse(
                s.getCodigo(),
                s.getTipoBase().name(),
                s.getNombre(),
                s.getDescripcion(),
                splitCsv(s.getTiposClientePermitidos()),
                splitCsv(s.getPropositosPermitidos()),
                s.getSoportaPagosMasivos(),
                s.getSoportaCuentaFavorita(),
                s.getSaldoMinimoApertura(),
                s.getEstado().name()
        );
    }
    public TransactionSubtypeResponse toTransactionSubtypeResponse(SubtipoTransaccion s) {
        return new TransactionSubtypeResponse(s.getCodigo(), s.getNombre(), s.getTipoMovimientoBase().name(), s.getDescripcion(), s.getEstado().name());
    }
    public UserCoreResponse toUserCoreResponse(UsuarioCore u) {
        return new UserCoreResponse(u.getUuidUsuarioCore(), u.getUuidIdentidad(), u.getCodigoSucursal(), u.getNombreCompleto(), u.getCargo(), u.getEstadoOperativo().name());
    }
    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}

