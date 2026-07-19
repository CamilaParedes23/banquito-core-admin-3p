package com.banquito.core.admin.application.service;

import com.banquito.core.admin.api.dto.api.AuditoriaEventoListResponse;
import com.banquito.core.admin.api.dto.api.AuditoriaEventoResponse;
import com.banquito.core.admin.domain.enums.ResultadoAuditoriaAdminEnum;
import com.banquito.core.admin.domain.model.AuditoriaAdminEvento;
import com.banquito.core.admin.domain.repository.AuditoriaAdminEventoRepository;
import com.banquito.core.admin.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaAdminServiceTest {

    @Mock
    private AuditoriaAdminEventoRepository repository;

    @InjectMocks
    private AuditoriaAdminService auditoriaService;

    private AuditoriaAdminEvento evento;

    @BeforeEach
    void setUp() {
        evento = AuditoriaAdminEvento.crear(
                "correlation-123",
                "user-uuid-123",
                "CREATE_BRANCH",
                "SUCURSAL",
                "branch-001",
                ResultadoAuditoriaAdminEnum.OK,
                null
        );
    }

    @Test
    void testRegistrar_EventoCreadoExitosamente() {
        when(repository.save(any(AuditoriaAdminEvento.class))).thenReturn(evento);

        auditoriaService.registrar("user-uuid-123", "CREATE_BRANCH", "SUCURSAL", "branch-001", ResultadoAuditoriaAdminEnum.OK, null);

        verify(repository, times(1)).save(any(AuditoriaAdminEvento.class));
    }

    @Test
    void testListarEventos_SinFiltros_RetornaListaPaginada() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        AuditoriaEventoListResponse response = auditoriaService.listarEventos(null, null, null, null, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.total());
        assertEquals(0, response.page());
        assertEquals(1, response.totalPages());
        assertFalse(response.events().isEmpty());
    }

    @Test
    void testListarEventos_ConFiltros_RetornaListaFiltrada() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        LocalDateTime fechaDesde = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime fechaHasta = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(repository.searchAuditEvents(eq(fechaDesde), eq(fechaHasta), eq("ADMIN"), eq("CREATE"), 
                eq("SUCURSAL"), eq("branch-001"), eq(ResultadoAuditoriaAdminEnum.OK), any(Pageable.class)))
                .thenReturn(page);

        AuditoriaEventoListResponse response = auditoriaService.listarEventos(fechaDesde, fechaHasta, "ADMIN", "CREATE", "SUCURSAL", "branch-001", "OK", 0, 20);

        assertNotNull(response);
        assertEquals(1, response.total());
    }

    @Test
    void testListarEventos_FechaInicialMayorAFinal_LanzaExcepcion() {
        LocalDateTime fechaDesde = LocalDateTime.of(2024, 12, 31, 23, 59);
        LocalDateTime fechaHasta = LocalDateTime.of(2024, 1, 1, 0, 0);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                auditoriaService.listarEventos(fechaDesde, fechaHasta, null, null, null, null, null, null, null));

        assertEquals("ADMIN_AUDIT_DATE_RANGE_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("fecha inicial no puede ser mayor"));
    }

    @Test
    void testListarEventos_ResultadoInvalido_LanzaExcepcion() {
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                auditoriaService.listarEventos(null, null, null, null, null, null, "INVALIDO", null, null));

        assertEquals("ADMIN_AUDIT_RESULT_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testListarEventos_ResultadoExitoso_NormalizadoAOK() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.searchAuditEvents(any(), any(), any(), any(), any(), any(), 
                eq(ResultadoAuditoriaAdminEnum.OK), any(Pageable.class)))
                .thenReturn(page);

        AuditoriaEventoListResponse response = auditoriaService.listarEventos(null, null, null, null, null, null, "EXITOSO", null, null);

        assertNotNull(response);
        verify(repository).searchAuditEvents(any(), any(), any(), any(), any(), any(), 
                eq(ResultadoAuditoriaAdminEnum.OK), any(Pageable.class));
    }

    @Test
    void testListarRecientes_SinLimit_Retorna5PorDefecto() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        List<AuditoriaEventoResponse> response = auditoriaService.listarRecientes(null);

        assertNotNull(response);
        assertFalse(response.isEmpty());
        verify(repository).findAll(any(PageRequest.class));
    }

    @Test
    void testListarRecientes_ConLimit_RetornaCantidadEspecificada() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        List<AuditoriaEventoResponse> response = auditoriaService.listarRecientes(10);

        assertNotNull(response);
        verify(repository).findAll(any(PageRequest.class));
    }

    @Test
    void testListarRecientes_LimitExcedeMaximo_UsaMaximo20() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        List<AuditoriaEventoResponse> response = auditoriaService.listarRecientes(50);

        assertNotNull(response);
        verify(repository).findAll(any(PageRequest.class));
    }

    @Test
    void testListarRecientes_LimitNegativo_UsaDefault5() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        List<AuditoriaEventoResponse> response = auditoriaService.listarRecientes(-5);

        assertNotNull(response);
        verify(repository).findAll(any(PageRequest.class));
    }

    @Test
    void testObtenerEvento_EventoExistente_RetornaEvento() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(evento));

        AuditoriaEventoResponse response = auditoriaService.obtenerEvento(1L);

        assertNotNull(response);
        assertEquals("CREATE_BRANCH", response.accion());
        assertEquals("SUCURSAL", response.entidad());
        assertEquals("branch-001", response.entidadId());
    }

    @Test
    void testObtenerEvento_EventoNoExistente_LanzaExcepcion() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                auditoriaService.obtenerEvento(999L));

        assertEquals("ADMIN_AUDIT_EVENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Evento de auditoría no encontrado"));
    }

    @Test
    void testContarTotal_RetornaCantidad() {
        when(repository.count()).thenReturn(100L);

        long count = auditoriaService.contarTotal();

        assertEquals(100L, count);
        verify(repository, times(1)).count();
    }

    @Test
    void testToResponse_MapeoCorrecto() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(evento));

        AuditoriaEventoResponse response = auditoriaService.obtenerEvento(1L);

        assertNotNull(response);
        assertEquals("Crear sucursal", response.actionName());
        assertEquals("Sucursal", response.entityName());
        assertEquals("Exitoso", response.resultName());
        assertEquals("ADMIN", response.modulo());
    }

    @Test
    void testActionName_CodigoDesconocido_RetornaCodigoFormateado() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(evento));

        AuditoriaEventoResponse response = auditoriaService.obtenerEvento(1L);

        assertNotNull(response);
    }

    @Test
    void testPageable_ValoresNulos_UsaDefaults() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        auditoriaService.listarEventos(null, null, null, null, null, null, null, null, null);

        verify(repository).searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void testPageable_PageNegativo_UsaCero() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        auditoriaService.listarEventos(null, null, null, null, null, null, null, -1, 20);

        verify(repository).searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void testPageable_SizeExcedeMaximo_UsaMaximo() {
        Page<AuditoriaAdminEvento> page = new PageImpl<>(List.of(evento));
        when(repository.searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        auditoriaService.listarEventos(null, null, null, null, null, null, null, 0, 200);

        verify(repository).searchAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
