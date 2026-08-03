package com.banquito.core.admin.application.service;

import com.banquito.core.admin.api.dto.api.*;
import com.banquito.core.admin.domain.enums.*;
import com.banquito.core.admin.domain.model.*;
import com.banquito.core.admin.domain.repository.*;
import com.banquito.core.admin.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private FeriadoRepository feriadoRepository;

    @Mock
    private ParametroCoreRepository parametroRepository;

    @Mock
    private VentanaOperativaRepository ventanaRepository;

    @Mock
    private InstitucionFinancieraRepository institucionRepository;

    @Mock
    private SubtipoCuentaRepository subtipoCuentaRepository;

    @Mock
    private SubtipoTransaccionRepository subtipoTransaccionRepository;

    @Mock
    private UsuarioCoreRepository usuarioCoreRepository;

    @Mock
    private AdminMapper mapper;

    @Mock
    private AuditoriaAdminService auditoriaService;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private AdminService adminService;

    private Sucursal sucursal;
    private Feriado feriado;
    private ParametroCore parametro;
    private BranchRequest branchRequest;
    private HolidayRequest holidayRequest;

    @BeforeEach
    void setUp() {
        sucursal = Sucursal.crear("SUC001", "Sucursal Principal", "Quito", "Av. Principal 123");
        feriado = Feriado.crear(LocalDate.of(2024, 12, 25), "Navidad", false);
        parametro = ParametroCore.crear("PARAM001", "Parámetro Test", "100", TipoDatoParametroEnum.INTEGER, "Descripción");
        branchRequest = new BranchRequest("SUC001", "Sucursal Principal", "Quito", "Av. Principal 123");
        holidayRequest = new HolidayRequest(LocalDate.of(2024, 12, 25), "Navidad", false);
    }

    // Tests para Sucursales
    @Test
    void testListarSucursales_SinStatus_RetornaTodas() {
        when(sucursalRepository.findAll()).thenReturn(List.of(sucursal));
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "ACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        List<BranchResponse> result = adminService.listarSucursales(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sucursalRepository).findAll();
    }

    @Test
    void testListarSucursales_ConStatus_RetornaFiltradas() {
        when(sucursalRepository.findByEstadoOrderByNombreAsc(EstadoSucursalEnum.ACTIVA)).thenReturn(List.of(sucursal));
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "ACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        List<BranchResponse> result = adminService.listarSucursales("ACTIVA");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sucursalRepository).findByEstadoOrderByNombreAsc(EstadoSucursalEnum.ACTIVA);
    }

    @Test
    void testListarSucursales_StatusInvalido_LanzaExcepcion() {
        assertThrows(BusinessException.class, () -> adminService.listarSucursales("INVALIDO"));
    }

    @Test
    void testObtenerSucursal_CodigoValido_RetornaSucursal() {
        when(sucursalRepository.findByCodigoSucursal("SUC001")).thenReturn(Optional.of(sucursal));
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "ACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        BranchResponse result = adminService.obtenerSucursal("SUC001");

        assertNotNull(result);
        assertEquals("SUC001", result.code());
        verify(sucursalRepository).findByCodigoSucursal("SUC001");
    }

    @Test
    void testObtenerSucursal_CodigoInvalido_LanzaExcepcion() {
        when(sucursalRepository.findByCodigoSucursal("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerSucursal("INVALID"));

        assertEquals("ADMIN_BRANCH_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCrearSucursal_CodigoDuplicado_LanzaExcepcion() {
        when(sucursalRepository.existsByCodigoSucursal("SUC001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSucursal(branchRequest, "user-123"));

        assertEquals("ADMIN_BRANCH_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testCrearSucursal_DatosValidos_CreaExitosamente() {
        when(sucursalRepository.existsByCodigoSucursal("SUC001")).thenReturn(false);
        when(sucursalRepository.save(any(Sucursal.class))).thenReturn(sucursal);
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "ACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        BranchResponse result = adminService.crearSucursal(branchRequest, "user-123");

        assertNotNull(result);
        verify(sucursalRepository).save(any(Sucursal.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CREATE_BRANCH"), eq("SUCURSAL"), eq("SUC001"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
        verify(outboxEventService).registrar(eq("ADMIN_BRANCH_CREATED"), eq("SUCURSAL"), eq("SUC001"), anyString());
    }

    @Test
    void testActualizarSucursal_DatosValidos_ActualizaExitosamente() {
        when(sucursalRepository.findByCodigoSucursal("SUC001")).thenReturn(Optional.of(sucursal));
        when(sucursalRepository.save(any(Sucursal.class))).thenReturn(sucursal);
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Actualizada", "Quito", "Av. Nueva 456", "ACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        BranchResponse result = adminService.actualizarSucursal("SUC001", branchRequest, "user-123");

        assertNotNull(result);
        verify(sucursalRepository).save(any(Sucursal.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("UPDATE_BRANCH"), eq("SUCURSAL"), eq("SUC001"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testCambiarEstadoSucursal_DatosValidos_CambiaEstadoExitosamente() {
        when(sucursalRepository.findByCodigoSucursal("SUC001")).thenReturn(Optional.of(sucursal));
        when(sucursalRepository.save(any(Sucursal.class))).thenReturn(sucursal);
        when(mapper.toBranchResponse(any(Sucursal.class))).thenReturn(new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "INACTIVA", LocalDateTime.now(), LocalDateTime.now()));

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVA");
        BranchResponse result = adminService.cambiarEstadoSucursal("SUC001", request, "user-123");

        assertNotNull(result);
        verify(sucursalRepository).save(any(Sucursal.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CHANGE_BRANCH_STATUS"), eq("SUCURSAL"), eq("SUC001"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    // Tests para Feriados
    @Test
    void testListarFeriados_SinStatus_RetornaTodos() {
        when(feriadoRepository.findAll()).thenReturn(List.of(feriado));
        when(mapper.toHolidayResponse(any(Feriado.class))).thenReturn(new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad", false, "ACTIVO"));

        List<HolidayResponse> result = adminService.listarFeriados(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(feriadoRepository).findAll();
    }

    @Test
    void testCrearFeriado_FechaDuplicada_LanzaExcepcion() {
        when(feriadoRepository.existsById(LocalDate.of(2024, 12, 25))).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearFeriado(holidayRequest, "user-123"));

        assertEquals("ADMIN_HOLIDAY_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testCrearFeriado_DatosValidos_CreaExitosamente() {
        when(feriadoRepository.existsById(LocalDate.of(2024, 12, 25))).thenReturn(false);
        when(feriadoRepository.save(any(Feriado.class))).thenReturn(feriado);
        when(mapper.toHolidayResponse(any(Feriado.class))).thenReturn(new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad", false, "ACTIVO"));

        HolidayResponse result = adminService.crearFeriado(holidayRequest, "user-123");

        assertNotNull(result);
        verify(feriadoRepository).save(any(Feriado.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CREATE_HOLIDAY"), eq("FERIADO"), eq(LocalDate.of(2024, 12, 25).toString()), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testActualizarFeriado_FechaNoExistente_LanzaExcepcion() {
        when(feriadoRepository.findById(LocalDate.of(2024, 12, 25))).thenReturn(Optional.empty());

        UpdateHolidayRequest request = new UpdateHolidayRequest("Navidad Actualizada", false);
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.actualizarFeriado(LocalDate.of(2024, 12, 25), request, "user-123"));

        assertEquals("ADMIN_HOLIDAY_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCambiarEstadoFeriado_DatosValidos_CambiaEstadoExitosamente() {
        when(feriadoRepository.findById(LocalDate.of(2024, 12, 25))).thenReturn(Optional.of(feriado));
        when(feriadoRepository.save(any(Feriado.class))).thenReturn(feriado);
        when(mapper.toHolidayResponse(any(Feriado.class))).thenReturn(new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad", false, "INACTIVO"));

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        HolidayResponse result = adminService.cambiarEstadoFeriado(LocalDate.of(2024, 12, 25), request, "user-123");

        assertNotNull(result);
        verify(feriadoRepository).save(any(Feriado.class));
    }

    // Tests para Días Hábiles
    @Test
    void testObtenerDiaHabil_DiaHabil_RetornaDiaHabil() {
        LocalDate fecha = LocalDate.of(2024, 12, 2); // Lunes
        when(feriadoRepository.findById(fecha)).thenReturn(Optional.empty());

        BusinessDayResponse result = adminService.obtenerDiaHabil(fecha);

        assertNotNull(result);
        assertTrue(result.businessDay());
        assertFalse(result.holiday());
        assertFalse(result.weekend());
        assertEquals("Día hábil", result.description());
    }

    @Test
    void testObtenerDiaHabil_FinDeSemana_RetornaNoHabil() {
        LocalDate fecha = LocalDate.of(2024, 12, 1); // Domingo
        when(feriadoRepository.findById(fecha)).thenReturn(Optional.empty());

        BusinessDayResponse result = adminService.obtenerDiaHabil(fecha);

        assertNotNull(result);
        assertFalse(result.businessDay());
        assertFalse(result.holiday());
        assertTrue(result.weekend());
        assertEquals("Fin de semana", result.description());
    }

    @Test
    void testObtenerDiaHabil_Feriado_RetornaNoHabil() {
        LocalDate fecha = LocalDate.of(2024, 12, 25);
        when(feriadoRepository.findById(fecha)).thenReturn(Optional.of(feriado));

        BusinessDayResponse result = adminService.obtenerDiaHabil(fecha);

        assertNotNull(result);
        assertFalse(result.businessDay());
        assertTrue(result.holiday());
        assertFalse(result.weekend());
        assertEquals("Navidad", result.description());
    }

    @Test
    void testObtenerSiguienteDiaHabil_DiaHabil_RetornaMismoDia() {
        LocalDate fecha = LocalDate.of(2024, 12, 2); // Lunes
        when(feriadoRepository.findById(any(LocalDate.class))).thenReturn(Optional.empty());

        BusinessDayResponse result = adminService.obtenerSiguienteDiaHabil(fecha);

        assertNotNull(result);
        assertTrue(result.businessDay());
    }

    // Tests para Parámetros
    @Test
    void testListarParametros_SinStatus_RetornaTodos() {
        when(parametroRepository.findAll()).thenReturn(List.of(parametro));
        when(mapper.toParameterResponse(any(ParametroCore.class))).thenReturn(new ParameterResponse("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO"));

        List<ParameterResponse> result = adminService.listarParametros(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(parametroRepository).findAll();
    }

    @Test
    void testCrearParametro_CodigoDuplicado_LanzaExcepcion() {
        when(parametroRepository.existsById("PARAM001")).thenReturn(true);

        ParameterRequest request = new ParameterRequest("PARAM001", "Parámetro Test", "100", "INTEGER", "Descripción", "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearParametro(request, "user-123"));

        assertEquals("ADMIN_PARAMETER_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testCrearParametro_DatosValidos_CreaExitosamente() {
        when(parametroRepository.existsById("PARAM001")).thenReturn(false);
        when(parametroRepository.save(any(ParametroCore.class))).thenReturn(parametro);
        when(mapper.toParameterResponse(any(ParametroCore.class))).thenReturn(new ParameterResponse("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO"));

        ParameterRequest request = new ParameterRequest("PARAM001", "Parámetro Test", "100", "INTEGER", "Descripción", "ACTIVO");
        ParameterResponse result = adminService.crearParametro(request, "user-123");

        assertNotNull(result);
        verify(parametroRepository).save(any(ParametroCore.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CREATE_PARAMETER"), eq("PARAMETRO_CORE"), eq("PARAM001"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testObtenerParametro_CodigoValido_RetornaParametro() {
        when(parametroRepository.findById("PARAM001")).thenReturn(Optional.of(parametro));
        when(mapper.toParameterResponse(any(ParametroCore.class))).thenReturn(new ParameterResponse("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO"));

        ParameterResponse result = adminService.obtenerParametro("PARAM001");

        assertNotNull(result);
        assertEquals("PARAM001", result.code());
        verify(parametroRepository).findById("PARAM001");
    }

    @Test
    void testObtenerParametro_CodigoInvalido_LanzaExcepcion() {
        when(parametroRepository.findById("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerParametro("INVALID"));

        assertEquals("ADMIN_PARAMETER_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para Métricas
    @Test
    void testObtenerMetricasAdministrativas_RetornaMetricas() {
        when(sucursalRepository.count()).thenReturn(10L);
        when(sucursalRepository.countByEstado(EstadoSucursalEnum.ACTIVA)).thenReturn(8L);
        when(feriadoRepository.count()).thenReturn(15L);
        when(feriadoRepository.countByEstado(EstadoRegistroEnum.ACTIVO)).thenReturn(12L);
        when(parametroRepository.count()).thenReturn(20L);
        when(parametroRepository.countByEstado(EstadoRegistroEnum.ACTIVO)).thenReturn(18L);
        when(ventanaRepository.count()).thenReturn(5L);
        when(ventanaRepository.countByEstado(EstadoVentanaOperativaEnum.ACTIVA)).thenReturn(4L);
        when(institucionRepository.count()).thenReturn(30L);
        when(institucionRepository.countByEstado(EstadoInstitucionFinancieraEnum.ACTIVA)).thenReturn(25L);
        when(subtipoCuentaRepository.count()).thenReturn(12L);
        when(subtipoCuentaRepository.countByEstado(EstadoRegistroEnum.ACTIVO)).thenReturn(10L);
        when(subtipoTransaccionRepository.count()).thenReturn(8L);
        when(subtipoTransaccionRepository.countByEstado(EstadoRegistroEnum.ACTIVO)).thenReturn(7L);
        when(usuarioCoreRepository.count()).thenReturn(50L);
        when(usuarioCoreRepository.countByEstadoOperativo(EstadoUsuarioCoreEnum.ACTIVO)).thenReturn(45L);
        when(auditoriaService.contarTotal()).thenReturn(1000L);
        when(outboxEventService.contarPendientes()).thenReturn(5L);

        MetricsResponse result = adminService.obtenerMetricasAdministrativas();

        assertNotNull(result);
        assertEquals(10L, result.totalBranches());
        assertEquals(8L, result.activeBranches());
        assertEquals(15L, result.totalHolidays());
        assertEquals(12L, result.activeHolidays());
        assertEquals(20L, result.totalParameters());
        assertEquals(18L, result.activeParameters());
        assertEquals(5L, result.totalOperationalWindows());
        assertEquals(4L, result.activeOperationalWindows());
        assertEquals(30L, result.totalFinancialInstitutions());
        assertEquals(25L, result.activeFinancialInstitutions());
        assertEquals(12L, result.totalAccountSubtypes());
        assertEquals(10L, result.activeAccountSubtypes());
        assertEquals(8L, result.totalTransactionSubtypes());
        assertEquals(7L, result.activeTransactionSubtypes());
        assertEquals(50L, result.totalCoreUsers());
        assertEquals(45L, result.activeCoreUsers());
        assertEquals(1000L, result.totalAuditEvents());
        assertEquals(5L, result.pendingOutboxEvents());
    }

    // Tests para Subtipos de Cuenta
    @Test
    void testCrearSubtipoCuenta_CodigoDuplicado_LanzaExcepcion() {
        when(subtipoCuentaRepository.existsByCodigo("SUB001")).thenReturn(true);

        AccountSubtypeRequest request = new AccountSubtypeRequest("SUB001", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL", "JURIDICO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSubtipoCuenta(request, "user-123"));

        assertEquals("ADMIN_ACCOUNT_SUBTYPE_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testCrearSubtipoCuenta_TipoClienteInvalido_LanzaExcepcion() {
        when(subtipoCuentaRepository.existsByCodigo("SUB001")).thenReturn(false);

        AccountSubtypeRequest request = new AccountSubtypeRequest("SUB001", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("INVALIDO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSubtipoCuenta(request, "user-123"));

        assertEquals("ADMIN_ACCOUNT_SUBTYPE_CUSTOMER_TYPE_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testCrearSubtipoCuenta_SaldoMinimoNegativo_LanzaExcepcion() {
        when(subtipoCuentaRepository.existsByCodigo("SUB001")).thenReturn(false);

        AccountSubtypeRequest request = new AccountSubtypeRequest("SUB001", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL"), List.of("GENERAL"), true, true, BigDecimal.valueOf(-100.00), "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSubtipoCuenta(request, "user-123"));

        assertEquals("ADMIN_ACCOUNT_SUBTYPE_MINIMUM_BALANCE_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para Usuarios Core
    @Test
    void testListarUsuariosCore_SinFiltros_RetornaPaginacion() {
        org.springframework.data.domain.Page<UsuarioCore> page = new org.springframework.data.domain.PageImpl<>(List.of());
        when(usuarioCoreRepository.searchCoreUsers(any(), any(), any(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        CoreUserListResponse result = adminService.listarUsuariosCore(null, null, null, null, null);

        assertNotNull(result);
        verify(usuarioCoreRepository).searchCoreUsers(any(), any(), any(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void testCrearUsuarioCore_UsuarioExistenteMismoPayload_RetornaExistente() {
        UsuarioCore existingUser = UsuarioCore.crear("uuid-123", "SUC001", "Juan Perez", "Gerente");
        when(usuarioCoreRepository.findByUuidIdentidad("identity-123")).thenReturn(Optional.of(existingUser));
        when(mapper.toUserCoreResponse(any(UsuarioCore.class))).thenReturn(new UserCoreResponse("uuid-123", "identity-123", "SUC001", "Juan Perez", "Gerente", "ACTIVO"));

        UserCoreRequest request = new UserCoreRequest("identity-123", "SUC001", "Juan Perez", "Gerente", "ACTIVO");
        UserCoreResponse result = adminService.crearUsuarioCore(request, "user-123");

        assertNotNull(result);
        verify(usuarioCoreRepository, never()).save(any(UsuarioCore.class));
    }

    @Test
    void testCrearUsuarioCore_UsuarioExistenteDiferentePayload_LanzaExcepcion() {
        UsuarioCore existingUser = UsuarioCore.crear("uuid-123", "SUC001", "Juan Perez", "Gerente");
        when(usuarioCoreRepository.findByUuidIdentidad("identity-123")).thenReturn(Optional.of(existingUser));

        UserCoreRequest request = new UserCoreRequest("identity-123", "SUC002", "Juan Perez Diferente", "Gerente", "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearUsuarioCore(request, "user-123"));

        assertEquals("ADMIN_CORE_USER_IDENTITY_CONFLICT", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testObtenerUsuarioCore_UuidValido_RetornaUsuario() {
        UsuarioCore usuario = UsuarioCore.crear("uuid-123", "SUC001", "Juan Perez", "Gerente");
        when(usuarioCoreRepository.findByUuidUsuarioCore("uuid-123")).thenReturn(Optional.of(usuario));
        when(mapper.toUserCoreResponse(any(UsuarioCore.class))).thenReturn(new UserCoreResponse("uuid-123", "identity-123", "SUC001", "Juan Perez", "Gerente", "ACTIVO"));

        UserCoreResponse result = adminService.obtenerUsuarioCore("uuid-123");

        assertNotNull(result);
        assertEquals("uuid-123", result.userCoreUuid());
        verify(usuarioCoreRepository).findByUuidUsuarioCore("uuid-123");
    }

    @Test
    void testObtenerUsuarioCore_UuidInvalido_LanzaExcepcion() {
        when(usuarioCoreRepository.findByUuidUsuarioCore("invalid-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerUsuarioCore("invalid-uuid"));

        assertEquals("ADMIN_CORE_USER_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCambiarEstadoUsuarioCore_DatosValidos_CambiaEstadoExitosamente() {
        UsuarioCore usuario = UsuarioCore.crear("uuid-123", "SUC001", "Juan Perez", "Gerente");
        when(usuarioCoreRepository.findByUuidUsuarioCore("uuid-123")).thenReturn(Optional.of(usuario));
        when(usuarioCoreRepository.save(any(UsuarioCore.class))).thenReturn(usuario);
        when(mapper.toUserCoreResponse(any(UsuarioCore.class))).thenReturn(new UserCoreResponse("uuid-123", "identity-123", "SUC001", "Juan Perez", "Gerente", "INACTIVO"));

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        UserCoreResponse result = adminService.cambiarEstadoUsuarioCore("uuid-123", request, "user-123");

        assertNotNull(result);
        verify(usuarioCoreRepository).save(any(UsuarioCore.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CHANGE_CORE_USER_STATUS"), eq("USUARIO_CORE"), eq("uuid-123"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    // Tests adicionales para aumentar coverage
    @Test
    void testActualizarFeriado_DatosValidos_ActualizaExitosamente() {
        when(feriadoRepository.findById(LocalDate.of(2024, 12, 25))).thenReturn(Optional.of(feriado));
        when(feriadoRepository.save(any(Feriado.class))).thenReturn(feriado);
        when(mapper.toHolidayResponse(any(Feriado.class))).thenReturn(new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad Actualizada", false, "ACTIVO"));

        UpdateHolidayRequest request = new UpdateHolidayRequest("Navidad Actualizada", false);
        HolidayResponse result = adminService.actualizarFeriado(LocalDate.of(2024, 12, 25), request, "user-123");

        assertNotNull(result);
        verify(feriadoRepository).save(any(Feriado.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("UPDATE_HOLIDAY"), eq("FERIADO"), eq(LocalDate.of(2024, 12, 25).toString()), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testActualizarParametro_DatosValidos_ActualizaExitosamente() {
        when(parametroRepository.findById("PARAM001")).thenReturn(Optional.of(parametro));
        when(parametroRepository.save(any(ParametroCore.class))).thenReturn(parametro);
        when(mapper.toParameterResponse(any(ParametroCore.class))).thenReturn(new ParameterResponse("PARAM001", "Parámetro Actualizado", "200", "NUMERICO", "Descripción Actualizada", "ACTIVO"));

        ParameterRequest request = new ParameterRequest("PARAM001", "Parámetro Actualizado", "200", "INTEGER", "Descripción Actualizada", "ACTIVO");
        ParameterResponse result = adminService.actualizarParametro("PARAM001", request, "user-123");

        assertNotNull(result);
        verify(parametroRepository).save(any(ParametroCore.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("UPDATE_PARAMETER"), eq("PARAMETRO_CORE"), eq("PARAM001"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testActualizarParametro_CodigoInvalido_LanzaExcepcion() {
        when(parametroRepository.findById("INVALID")).thenReturn(Optional.empty());

        ParameterRequest request = new ParameterRequest("INVALID", "Parámetro", "100", "INTEGER", "Descripción", "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.actualizarParametro("INVALID", request, "user-123"));

        assertEquals("ADMIN_PARAMETER_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testListarFeriados_ConStatus_RetornaFiltradas() {
        when(feriadoRepository.findByEstadoOrderByFechaFeriadoAsc(EstadoRegistroEnum.ACTIVO)).thenReturn(List.of(feriado));
        when(mapper.toHolidayResponse(any(Feriado.class))).thenReturn(new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad", false, "ACTIVO"));

        List<HolidayResponse> result = adminService.listarFeriados("ACTIVO");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(feriadoRepository).findByEstadoOrderByFechaFeriadoAsc(EstadoRegistroEnum.ACTIVO);
    }

    @Test
    void testListarParametros_ConStatus_RetornaFiltrados() {
        when(parametroRepository.findByEstadoOrderByCodigoAsc(EstadoRegistroEnum.ACTIVO)).thenReturn(List.of(parametro));
        when(mapper.toParameterResponse(any(ParametroCore.class))).thenReturn(new ParameterResponse("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO"));

        List<ParameterResponse> result = adminService.listarParametros("ACTIVO");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(parametroRepository).findByEstadoOrderByCodigoAsc(EstadoRegistroEnum.ACTIVO);
    }

    @Test
    void testActualizarSucursal_CodigoInvalido_LanzaExcepcion() {
        when(sucursalRepository.findByCodigoSucursal("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.actualizarSucursal("INVALID", branchRequest, "user-123"));

        assertEquals("ADMIN_BRANCH_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCambiarEstadoSucursal_CodigoInvalido_LanzaExcepcion() {
        when(sucursalRepository.findByCodigoSucursal("INVALID")).thenReturn(Optional.empty());

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVA");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.cambiarEstadoSucursal("INVALID", request, "user-123"));

        assertEquals("ADMIN_BRANCH_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCambiarEstadoFeriado_FechaInvalida_LanzaExcepcion() {
        when(feriadoRepository.findById(LocalDate.of(2024, 12, 25))).thenReturn(Optional.empty());

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.cambiarEstadoFeriado(LocalDate.of(2024, 12, 25), request, "user-123"));

        assertEquals("ADMIN_HOLIDAY_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCambiarEstadoUsuarioCore_UuidInvalido_LanzaExcepcion() {
        when(usuarioCoreRepository.findByUuidUsuarioCore("invalid-uuid")).thenReturn(Optional.empty());

        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.cambiarEstadoUsuarioCore("invalid-uuid", request, "user-123"));

        assertEquals("ADMIN_CORE_USER_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCrearSubtipoCuenta_PropositoInvalido_LanzaExcepcion() {
        when(subtipoCuentaRepository.existsByCodigo("SUB001")).thenReturn(false);

        AccountSubtypeRequest request = new AccountSubtypeRequest("SUB001", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL"), List.of("INVALIDO"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSubtipoCuenta(request, "user-123"));

        assertEquals("ADMIN_ACCOUNT_SUBTYPE_PURPOSE_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para Instituciones Financieras
    @Test
    void testListarInstituciones_SinStatus_RetornaTodas() {
        InstitucionFinanciera institucion = InstitucionFinanciera.crear("123456789", "Banco Test", true);
        when(institucionRepository.findAll()).thenReturn(List.of(institucion));
        when(mapper.toFinancialInstitutionResponse(any(InstitucionFinanciera.class))).thenReturn(new FinancialInstitutionResponse("123456789", "Banco Test", "10101", true, "ACTIVA"));

        List<FinancialInstitutionResponse> result = adminService.listarInstituciones(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(institucionRepository).findAll();
    }

    @Test
    void testListarInstituciones_ConStatus_RetornaFiltradas() {
        InstitucionFinanciera institucion = InstitucionFinanciera.crear("123456789", "Banco Test", true);
        when(institucionRepository.findByEstadoOrderByNombreAsc(EstadoInstitucionFinancieraEnum.ACTIVA)).thenReturn(List.of(institucion));
        when(mapper.toFinancialInstitutionResponse(any(InstitucionFinanciera.class))).thenReturn(new FinancialInstitutionResponse("123456789", "Banco Test", "10101", true, "ACTIVA"));

        List<FinancialInstitutionResponse> result = adminService.listarInstituciones("ACTIVA");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(institucionRepository).findByEstadoOrderByNombreAsc(EstadoInstitucionFinancieraEnum.ACTIVA);
    }

    @Test
    void testObtenerInstitucion_CodigoValido_RetornaInstitucion() {
        InstitucionFinanciera institucion = InstitucionFinanciera.crear("123456789", "Banco Test", true);
        when(institucionRepository.findByRoutingCode("123456789")).thenReturn(Optional.of(institucion));
        when(mapper.toFinancialInstitutionResponse(any(InstitucionFinanciera.class))).thenReturn(new FinancialInstitutionResponse("123456789", "Banco Test", "10101", true, "ACTIVA"));

        FinancialInstitutionResponse result = adminService.obtenerInstitucion("123456789");

        assertNotNull(result);
        assertEquals("123456789", result.routingCode());
        verify(institucionRepository).findByRoutingCode("123456789");
    }

    @Test
    void testObtenerInstitucion_CodigoInvalido_LanzaExcepcion() {
        when(institucionRepository.findByRoutingCode("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerInstitucion("INVALID"));

        assertEquals("ADMIN_INSTITUTION_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCrearInstitucion_CodigoDuplicado_LanzaExcepcion() {
        when(institucionRepository.existsByRoutingCode("123456789")).thenReturn(true);

        FinancialInstitutionRequest request = new FinancialInstitutionRequest("123456789", "Banco Test", "10101", true, "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearInstitucion(request, "user-123"));

        assertEquals("ADMIN_INSTITUTION_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void testCrearInstitucion_DatosValidos_CreaExitosamente() {
        when(institucionRepository.existsByRoutingCode("123456789")).thenReturn(false);
        InstitucionFinanciera institucion = InstitucionFinanciera.crear("123456789", "Banco Test", true);
        when(institucionRepository.save(any(InstitucionFinanciera.class))).thenReturn(institucion);
        when(mapper.toFinancialInstitutionResponse(any(InstitucionFinanciera.class))).thenReturn(new FinancialInstitutionResponse("123456789", "Banco Test", "10101", true, "ACTIVA"));

        FinancialInstitutionRequest request = new FinancialInstitutionRequest("123456789", "Banco Test", "10101", true, "ACTIVO");
        FinancialInstitutionResponse result = adminService.crearInstitucion(request, "user-123");

        assertNotNull(result);
        verify(institucionRepository).save(any(InstitucionFinanciera.class));
        verify(auditoriaService).registrar(eq("user-123"), eq("CREATE_FINANCIAL_INSTITUTION"), eq("INSTITUCION_FINANCIERA"), eq("123456789"), eq(ResultadoAuditoriaAdminEnum.OK), isNull());
    }

    @Test
    void testActualizarInstitucion_CodigoInvalido_LanzaExcepcion() {
        when(institucionRepository.findByRoutingCode("INVALID")).thenReturn(Optional.empty());

        FinancialInstitutionRequest request = new FinancialInstitutionRequest("INVALID", "Banco Test", "10101", true, "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.actualizarInstitucion("INVALID", request, "user-123"));

        assertEquals("ADMIN_INSTITUTION_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para Subtipos de Cuenta
    @Test
    void testListarSubtiposCuenta_SinFiltros_RetornaTodos() {
        when(subtipoCuentaRepository.findByEstadoOrderByNombreAsc(EstadoRegistroEnum.ACTIVO)).thenReturn(List.of());

        List<AccountSubtypeResponse> result = adminService.listarSubtiposCuenta(null, null, null);

        assertNotNull(result);
        verify(subtipoCuentaRepository).findByEstadoOrderByNombreAsc(EstadoRegistroEnum.ACTIVO);
    }

    @Test
    void testObtenerSubtipoCuenta_CodigoInvalido_LanzaExcepcion() {
        when(subtipoCuentaRepository.findByCodigo("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerSubtipoCuenta("INVALID"));

        assertEquals("ADMIN_ACCOUNT_SUBTYPE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para Subtipos de Transacción
    @Test
    void testListarSubtiposTransaccion_SinFiltros_RetornaTodos() {
        when(subtipoTransaccionRepository.findByEstadoOrderByNombreAsc(EstadoRegistroEnum.ACTIVO)).thenReturn(List.of());

        List<TransactionSubtypeResponse> result = adminService.listarSubtiposTransaccion(null, null);

        assertNotNull(result);
        verify(subtipoTransaccionRepository).findByEstadoOrderByNombreAsc(EstadoRegistroEnum.ACTIVO);
    }

    @Test
    void testObtenerSubtipoTransaccion_CodigoInvalido_LanzaExcepcion() {
        when(subtipoTransaccionRepository.findByCodigo("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerSubtipoTransaccion("INVALID"));

        assertEquals("ADMIN_TRANSACTION_SUBTYPE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para Ventanas Operativas
    @Test
    void testListarVentanas_SinFiltros_RetornaTodas() {
        when(ventanaRepository.findByEstadoOrderByCodigoAsc(EstadoVentanaOperativaEnum.ACTIVA)).thenReturn(List.of());

        List<OperationalWindowResponse> result = adminService.listarVentanas(null, null);

        assertNotNull(result);
        verify(ventanaRepository).findByEstadoOrderByCodigoAsc(EstadoVentanaOperativaEnum.ACTIVA);
    }

    @Test
    void testObtenerVentana_CodigoInvalido_LanzaExcepcion() {
        when(ventanaRepository.findByCodigo("INVALID")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminService.obtenerVentana("INVALID"));

        assertEquals("ADMIN_WINDOW_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests adicionales para subtipos de transacción
    @Test
    void testCrearSubtipoTransaccion_CodigoDuplicado_LanzaExcepcion() {
        when(subtipoTransaccionRepository.existsByCodigo("SUB001")).thenReturn(true);

        TransactionSubtypeRequest request = new TransactionSubtypeRequest("SUB001", "Débito", "DEBITO", "Descripción", "ACTIVO");
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                adminService.crearSubtipoTransaccion(request, "user-123"));

        assertEquals("ADMIN_TRANSACTION_SUBTYPE_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
