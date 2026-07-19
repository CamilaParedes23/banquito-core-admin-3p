package com.banquito.core.admin.application.service;

import com.banquito.core.admin.domain.model.OutboxEvent;
import com.banquito.core.admin.domain.repository.OutboxEventRepository;
import com.banquito.core.admin.domain.enums.EstadoOutboxEventEnum;
import com.banquito.core.admin.shared.tracing.CorrelationIdHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void testRegistrar_EventoCreadoExitosamente() {
        try (MockedStatic<CorrelationIdHolder> mocked = mockStatic(CorrelationIdHolder.class)) {
            mocked.when(CorrelationIdHolder::get).thenReturn("correlation-123");
            
            when(repository.save(any(OutboxEvent.class))).thenReturn(OutboxEvent.crear("correlation-123", "TEST_EVENT", "TEST_TYPE", "123", "{}"));

            outboxEventService.registrar("TEST_EVENT", "TEST_TYPE", "123", "{}");

            verify(repository, times(1)).save(any(OutboxEvent.class));
            mocked.verify(CorrelationIdHolder::get, times(1));
        }
    }

    @Test
    void testContarPendientes_RetornaCantidad() {
        when(repository.countByEstado(EstadoOutboxEventEnum.PENDIENTE)).thenReturn(5L);

        long count = outboxEventService.contarPendientes();

        assertEquals(5L, count);
        verify(repository, times(1)).countByEstado(EstadoOutboxEventEnum.PENDIENTE);
    }

    @Test
    void testContarPendientes_SinPendientes_RetornaCero() {
        when(repository.countByEstado(EstadoOutboxEventEnum.PENDIENTE)).thenReturn(0L);

        long count = outboxEventService.contarPendientes();

        assertEquals(0L, count);
        verify(repository, times(1)).countByEstado(EstadoOutboxEventEnum.PENDIENTE);
    }

    @Test
    void testRegistrar_MultiplesEventos_CadaUnoGuardado() {
        try (MockedStatic<CorrelationIdHolder> mocked = mockStatic(CorrelationIdHolder.class)) {
            mocked.when(CorrelationIdHolder::get).thenReturn("correlation-123");
            
            when(repository.save(any(OutboxEvent.class))).thenReturn(OutboxEvent.crear("correlation-123", "TEST_EVENT", "TEST_TYPE", "123", "{}"));

            outboxEventService.registrar("EVENT_1", "TYPE_1", "1", "{\"data\":\"1\"}");
            outboxEventService.registrar("EVENT_2", "TYPE_2", "2", "{\"data\":\"2\"}");
            outboxEventService.registrar("EVENT_3", "TYPE_3", "3", "{\"data\":\"3\"}");

            verify(repository, times(3)).save(any(OutboxEvent.class));
        }
    }
}
