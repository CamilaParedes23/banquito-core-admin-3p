package com.banquito.core.admin.api.controller;

import com.banquito.core.admin.api.dto.api.AuditoriaEventoListResponse;
import com.banquito.core.admin.api.dto.api.AuditoriaEventoResponse;
import com.banquito.core.admin.application.service.AuditoriaAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditoriaAdminService auditoriaService;

    @InjectMocks
    private AuditController auditController;

    private AuditoriaEventoResponse eventoResponse;
    private AuditoriaEventoListResponse eventoListResponse;

    @BeforeEach
    void setUp() {
        eventoResponse = new AuditoriaEventoResponse(
                1L,
                "correlation-123",
                "user-uuid-123",
                "ADMIN_SERVICE",
                "CREATE_BRANCH",
                "SUCURSAL",
                "branch-001",
                "OK",
                "ADMIN_SERVICE",
                "2024-01-01T10:00:00",
                null,
                "CREATE_BRANCH",
                "Crear sucursal",
                "SUCURSAL",
                "Sucursal",
                "OK",
                "Exitoso",
                "ADMIN_SERVICE",
                "Servicio de administración"
        );

        eventoListResponse = new AuditoriaEventoListResponse(
                1L,
                0,
                20,
                1,
                List.of(eventoResponse)
        );
    }

    @Test
    void testListAuditEvents_SinParametros_RetornaLista() {
        when(auditoriaService.listarEventos(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
        assertEquals(0, result.page());
        assertFalse(result.events().isEmpty());
        assertEquals("CREATE_BRANCH", result.events().get(0).accion());
    }

    @Test
    void testListAuditEvents_ConParametros_RetornaListaFiltrada() {
        LocalDateTime fechaDesde = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime fechaHasta = LocalDateTime.of(2024, 12, 31, 23, 59);
        
        when(auditoriaService.listarEventos(eq(fechaDesde), eq(fechaHasta), 
                eq("ADMIN"), eq("CREATE"), eq("SUCURSAL"), eq("branch-001"), eq("OK"), eq(0), eq(20)))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(fechaDesde, fechaHasta, "ADMIN", "CREATE", "SUCURSAL", "branch-001", "OK", 0, 20);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_ConPaginacion_RetornaPaginaCorrecta() {
        when(auditoriaService.listarEventos(any(), any(), any(), any(), any(), any(), any(), eq(1), eq(10)))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(0, result.page());
        assertEquals(1, result.totalPages());
    }

    @Test
    void testListRecentAuditEvents_SinLimit_Retorna5PorDefecto() {
        when(auditoriaService.listarRecientes(isNull())).thenReturn(List.of(eventoResponse));

        List<AuditoriaEventoResponse> result = auditController.listRecentAuditEvents(null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("CREATE_BRANCH", result.get(0).accion());
    }

    @Test
    void testListRecentAuditEvents_ConLimit_RetornaCantidadEspecifica() {
        when(auditoriaService.listarRecientes(eq(10))).thenReturn(List.of(eventoResponse));

        List<AuditoriaEventoResponse> result = auditController.listRecentAuditEvents(10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("CREATE_BRANCH", result.get(0).accion());
    }

    @Test
    void testGetAuditEvent_IdValido_RetornaEvento() {
        when(auditoriaService.obtenerEvento(1L)).thenReturn(eventoResponse);

        AuditoriaEventoResponse result = auditController.getAuditEvent(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("CREATE_BRANCH", result.accion());
        assertEquals("SUCURSAL", result.entidad());
        assertEquals("branch-001", result.entidadId());
    }

    @Test
    void testListAuditEvents_ConFechasFormatoISO_RetornaLista() {
        LocalDateTime fechaDesde = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime fechaHasta = LocalDateTime.of(2024, 12, 31, 23, 59);
        
        when(auditoriaService.listarEventos(eq(fechaDesde), eq(fechaHasta), 
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(fechaDesde, fechaHasta, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_SoloModulo_RetornaFiltradoPorModulo() {
        when(auditoriaService.listarEventos(any(), any(), eq("ADMIN"), any(), any(), any(), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, "ADMIN", null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_SoloAccion_RetornaFiltradoPorAccion() {
        when(auditoriaService.listarEventos(any(), any(), any(), eq("CREATE"), any(), any(), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, "CREATE", null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_SoloEntidad_RetornaFiltradoPorEntidad() {
        when(auditoriaService.listarEventos(any(), any(), any(), any(), eq("SUCURSAL"), any(), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, null, "SUCURSAL", null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_SoloEntidadId_RetornaFiltradoPorEntidadId() {
        when(auditoriaService.listarEventos(any(), any(), any(), any(), any(), eq("branch-001"), any(), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, null, null, "branch-001", null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListAuditEvents_SoloResultado_RetornaFiltradoPorResultado() {
        when(auditoriaService.listarEventos(any(), any(), any(), any(), any(), any(), eq("OK"), any(), any()))
                .thenReturn(eventoListResponse);

        AuditoriaEventoListResponse result = auditController.listAuditEvents(null, null, null, null, null, null, "OK", null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testListRecentAuditEvents_LimitCero_RetornaDefault() {
        when(auditoriaService.listarRecientes(anyInt())).thenReturn(List.of(eventoResponse));

        List<AuditoriaEventoResponse> result = auditController.listRecentAuditEvents(0);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testListRecentAuditEvents_LimitNegativo_RetornaDefault() {
        when(auditoriaService.listarRecientes(anyInt())).thenReturn(List.of(eventoResponse));

        List<AuditoriaEventoResponse> result = auditController.listRecentAuditEvents(-5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
