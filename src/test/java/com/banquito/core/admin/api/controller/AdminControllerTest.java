package com.banquito.core.admin.api.controller;

import com.banquito.core.admin.api.dto.api.*;
import com.banquito.core.admin.application.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private BranchResponse branchResponse;
    private BranchRequest branchRequest;
    private HolidayResponse holidayResponse;
    private HolidayRequest holidayRequest;
    private ParameterResponse parameterResponse;
    private ParameterRequest parameterRequest;
    private UserCoreResponse userCoreResponse;
    private UserCoreRequest userCoreRequest;
    private MetricsResponse metricsResponse;

    @BeforeEach
    void setUp() {
        branchResponse = new BranchResponse("uuid-123", "SUC001", "Sucursal Principal", "Quito", "Av. Principal 123", "ACTIVA", LocalDateTime.now(), LocalDateTime.now());
        branchRequest = new BranchRequest("SUC001", "Sucursal Principal", "Quito", "Av. Principal 123");
        
        holidayResponse = new HolidayResponse(LocalDate.of(2024, 12, 25), "Navidad", false, "ACTIVO");
        holidayRequest = new HolidayRequest(LocalDate.of(2024, 12, 25), "Navidad", false);
        
        parameterResponse = new ParameterResponse("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO");
        parameterRequest = new ParameterRequest("PARAM001", "Parámetro Test", "100", "NUMERICO", "Descripción", "ACTIVO");
        
        userCoreResponse = new UserCoreResponse("uuid-123", "identity-123", "SUC001", "Juan Perez", "Gerente", "ACTIVO");
        userCoreRequest = new UserCoreRequest("identity-123", "SUC001", "Juan Perez", "Gerente", "ACTIVO");
        
        metricsResponse = new MetricsResponse(
                10L, 8L, 15L, 12L, 20L, 18L, 5L, 4L, 30L, 25L, 
                12L, 10L, 8L, 7L, 50L, 45L, 1000L, 5L
        );
    }

    // Tests para Sucursales
    @Test
    void testListBranches_SinStatus_RetornaTodas() {
        when(adminService.listarSucursales(null)).thenReturn(List.of(branchResponse));

        List<BranchResponse> result = adminController.listBranches(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SUC001", result.get(0).code());
        assertEquals("Sucursal Principal", result.get(0).name());
    }

    @Test
    void testListBranches_ConStatus_RetornaFiltradas() {
        when(adminService.listarSucursales("ACTIVA")).thenReturn(List.of(branchResponse));

        List<BranchResponse> result = adminController.listBranches("ACTIVA");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVA", result.get(0).status());
    }

    @Test
    void testGetBranch_CodigoValido_RetornaSucursal() {
        when(adminService.obtenerSucursal("SUC001")).thenReturn(branchResponse);

        BranchResponse result = adminController.getBranch("SUC001");

        assertNotNull(result);
        assertEquals("SUC001", result.code());
        assertEquals("Sucursal Principal", result.name());
    }

    @Test
    void testCreateBranch_DatosValidos_RetornaCreated() {
        when(adminService.crearSucursal(branchRequest, null)).thenReturn(branchResponse);

        ResponseEntity<BranchResponse> result = adminController.createBranch(branchRequest, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("SUC001", result.getBody().code());
    }

    @Test
    void testUpdateBranch_DatosValidos_RetornaSucursalActualizada() {
        when(adminService.actualizarSucursal("SUC001", branchRequest, null))
                .thenReturn(branchResponse);

        BranchResponse result = adminController.updateBranch("SUC001", branchRequest, null);

        assertNotNull(result);
        assertEquals("SUC001", result.code());
    }

    @Test
    void testChangeBranchStatus_DatosValidos_RetornaSucursalActualizada() {
        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVA");
        when(adminService.cambiarEstadoSucursal("SUC001", request, null))
                .thenReturn(branchResponse);

        BranchResponse result = adminController.changeBranchStatus("SUC001", request, null);

        assertNotNull(result);
        assertEquals("SUC001", result.code());
    }

    // Tests para Feriados
    @Test
    void testListHolidays_SinStatus_RetornaTodos() {
        when(adminService.listarFeriados(null)).thenReturn(List.of(holidayResponse));

        List<HolidayResponse> result = adminController.listHolidays(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2024, 12, 25), result.get(0).holidayDate());
        assertEquals("Navidad", result.get(0).name());
    }

    @Test
    void testListHolidays_ConStatus_RetornaFiltrados() {
        when(adminService.listarFeriados("ACTIVO")).thenReturn(List.of(holidayResponse));

        List<HolidayResponse> result = adminController.listHolidays("ACTIVO");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVO", result.get(0).status());
    }

    @Test
    void testCreateHoliday_DatosValidos_RetornaCreated() {
        when(adminService.crearFeriado(holidayRequest, null)).thenReturn(holidayResponse);

        ResponseEntity<HolidayResponse> result = adminController.createHoliday(holidayRequest, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(LocalDate.of(2024, 12, 25), result.getBody().holidayDate());
    }

    @Test
    void testUpdateHoliday_DatosValidos_RetornaFeriadoActualizado() {
        UpdateHolidayRequest request = new UpdateHolidayRequest("Navidad Actualizada", false);
        when(adminService.actualizarFeriado(LocalDate.of(2024, 12, 25), request, null))
                .thenReturn(holidayResponse);

        HolidayResponse result = adminController.updateHoliday(LocalDate.of(2024, 12, 25), request, null);

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 12, 25), result.holidayDate());
    }

    @Test
    void testChangeHolidayStatus_DatosValidos_RetornaFeriadoActualizado() {
        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        when(adminService.cambiarEstadoFeriado(LocalDate.of(2024, 12, 25), request, null))
                .thenReturn(holidayResponse);

        HolidayResponse result = adminController.changeHolidayStatus(LocalDate.of(2024, 12, 25), request, null);

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 12, 25), result.holidayDate());
    }

    // Tests para Calendario de Negocios
    @Test
    void testGetBusinessDay_DiaHabil_RetornaDiaHabil() {
        BusinessDayResponse response = new BusinessDayResponse(LocalDate.of(2024, 12, 2), false, false, true, "Día hábil");
        when(adminService.obtenerDiaHabil(LocalDate.of(2024, 12, 2))).thenReturn(response);

        BusinessDayResponse result = adminController.getBusinessDay(LocalDate.of(2024, 12, 2));

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 12, 2), result.date());
        assertTrue(result.businessDay());
        assertEquals("Día hábil", result.description());
    }

    @Test
    void testGetNextBusinessDay_DiaHabil_RetornaSiguienteDiaHabil() {
        BusinessDayResponse response = new BusinessDayResponse(LocalDate.of(2024, 12, 3), false, false, true, "Día hábil");
        when(adminService.obtenerSiguienteDiaHabil(LocalDate.of(2024, 12, 2))).thenReturn(response);

        BusinessDayResponse result = adminController.getNextBusinessDay(LocalDate.of(2024, 12, 2));

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 12, 3), result.date());
        assertTrue(result.businessDay());
    }

    // Tests para Parámetros
    @Test
    void testListParameters_SinStatus_RetornaTodos() {
        when(adminService.listarParametros(null)).thenReturn(List.of(parameterResponse));

        List<ParameterResponse> result = adminController.listParameters(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PARAM001", result.get(0).code());
        assertEquals("Parámetro Test", result.get(0).name());
    }

    @Test
    void testListParameters_ConStatus_RetornaFiltrados() {
        when(adminService.listarParametros("ACTIVO")).thenReturn(List.of(parameterResponse));

        List<ParameterResponse> result = adminController.listParameters("ACTIVO");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVO", result.get(0).status());
    }

    @Test
    void testGetParameter_CodigoValido_RetornaParametro() {
        when(adminService.obtenerParametro("PARAM001")).thenReturn(parameterResponse);

        ParameterResponse result = adminController.getParameter("PARAM001");

        assertNotNull(result);
        assertEquals("PARAM001", result.code());
        assertEquals("Parámetro Test", result.name());
    }

    @Test
    void testCreateParameter_DatosValidos_RetornaCreated() {
        when(adminService.crearParametro(parameterRequest, null)).thenReturn(parameterResponse);

        ResponseEntity<ParameterResponse> result = adminController.createParameter(parameterRequest, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("PARAM001", result.getBody().code());
    }

    @Test
    void testUpdateParameter_DatosValidos_RetornaParametroActualizado() {
        when(adminService.actualizarParametro("PARAM001", parameterRequest, null))
                .thenReturn(parameterResponse);

        ParameterResponse result = adminController.updateParameter("PARAM001", parameterRequest, null);

        assertNotNull(result);
        assertEquals("PARAM001", result.code());
    }

    // Tests para Ventanas Operativas
    @Test
    void testListOperationalWindows_SinFiltros_RetornaTodas() {
        OperationalWindowResponse response = new OperationalWindowResponse("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        when(adminService.listarVentanas(null, null)).thenReturn(List.of(response));

        List<OperationalWindowResponse> result = adminController.listOperationalWindows(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VENT001", result.get(0).code());
    }

    @Test
    void testListOperationalWindows_ConFiltros_RetornaFiltradas() {
        OperationalWindowResponse response = new OperationalWindowResponse("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        when(adminService.listarVentanas("TRANSFERS", "ACTIVA")).thenReturn(List.of(response));

        List<OperationalWindowResponse> result = adminController.listOperationalWindows("TRANSFERS", "ACTIVA");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOperationalWindow_CodigoValido_RetornaVentana() {
        OperationalWindowResponse response = new OperationalWindowResponse("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        when(adminService.obtenerVentana("VENT001")).thenReturn(response);

        OperationalWindowResponse result = adminController.getOperationalWindow("VENT001");

        assertNotNull(result);
        assertEquals("VENT001", result.code());
    }

    @Test
    void testCreateOperationalWindow_DatosValidos_RetornaCreated() {
        OperationalWindowResponse response = new OperationalWindowResponse("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        OperationalWindowRequest request = new OperationalWindowRequest("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        when(adminService.crearVentana(request, null)).thenReturn(response);

        ResponseEntity<OperationalWindowResponse> result = adminController.createOperationalWindow(request, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("VENT001", result.getBody().code());
    }

    @Test
    void testUpdateOperationalWindow_DatosValidos_RetornaVentanaActualizada() {
        OperationalWindowResponse response = new OperationalWindowResponse("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        OperationalWindowRequest request = new OperationalWindowRequest("VENT001", "Ventana Test", "TRANSFERS", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(17, 0), "LUN,MAR,MIE,JUE,VIE", "America/Guayaquil", "QUEUE", "ACTIVA");
        when(adminService.actualizarVentana("VENT001", request, null)).thenReturn(response);

        OperationalWindowResponse result = adminController.updateOperationalWindow("VENT001", request, null);

        assertNotNull(result);
        assertEquals("VENT001", result.code());
    }

    // Tests para Instituciones Financieras
    @Test
    void testListFinancialInstitutions_SinStatus_RetornaTodas() {
        FinancialInstitutionResponse response = new FinancialInstitutionResponse("123456789", "Banco Test", false, "ACTIVA");
        when(adminService.listarInstituciones(null)).thenReturn(List.of(response));

        List<FinancialInstitutionResponse> result = adminController.listFinancialInstitutions(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("123456789", result.get(0).routingCode());
    }

    @Test
    void testGetFinancialInstitution_RoutingCodeValido_RetornaInstitucion() {
        FinancialInstitutionResponse response = new FinancialInstitutionResponse("123456789", "Banco Test", false, "ACTIVA");
        when(adminService.obtenerInstitucion("123456789")).thenReturn(response);

        FinancialInstitutionResponse result = adminController.getFinancialInstitution("123456789");

        assertNotNull(result);
        assertEquals("123456789", result.routingCode());
    }

    @Test
    void testCreateFinancialInstitution_DatosValidos_RetornaCreated() {
        FinancialInstitutionResponse response = new FinancialInstitutionResponse("123456789", "Banco Test", false, "ACTIVA");
        FinancialInstitutionRequest request = new FinancialInstitutionRequest("123456789", "Banco Test", false, "ACTIVA");
        when(adminService.crearInstitucion(request, null)).thenReturn(response);

        ResponseEntity<FinancialInstitutionResponse> result = adminController.createFinancialInstitution(request, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("123456789", result.getBody().routingCode());
    }

    // Tests para Subtipos de Cuenta
    @Test
    void testListAccountSubtypes_SinFiltros_RetornaTodos() {
        AccountSubtypeResponse response = new AccountSubtypeResponse("AHORROS", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL", "JURIDICO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        when(adminService.listarSubtiposCuenta(null, null, null)).thenReturn(List.of(response));

        List<AccountSubtypeResponse> result = adminController.listAccountSubtypes(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AHORROS", result.get(0).code());
    }

    @Test
    void testGetAccountSubtype_CodigoValido_RetornaSubtipo() {
        AccountSubtypeResponse response = new AccountSubtypeResponse("AHORROS", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL", "JURIDICO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        when(adminService.obtenerSubtipoCuenta("AHORROS")).thenReturn(response);

        AccountSubtypeResponse result = adminController.getAccountSubtype("AHORROS");

        assertNotNull(result);
        assertEquals("AHORROS", result.code());
    }

    @Test
    void testCreateAccountSubtype_DatosValidos_RetornaCreated() {
        AccountSubtypeResponse response = new AccountSubtypeResponse("AHORROS", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL", "JURIDICO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        AccountSubtypeRequest request = new AccountSubtypeRequest("AHORROS", "AHORROS", "Cuenta Ahorros", "Descripción", List.of("NATURAL", "JURIDICO"), List.of("GENERAL"), true, true, BigDecimal.valueOf(100.00), "ACTIVO");
        when(adminService.crearSubtipoCuenta(request, null)).thenReturn(response);

        ResponseEntity<AccountSubtypeResponse> result = adminController.createAccountSubtype(request, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("AHORROS", result.getBody().code());
    }

    // Tests para Subtipos de Transacción
    @Test
    void testListTransactionSubtypes_SinFiltros_RetornaTodos() {
        TransactionSubtypeResponse response = new TransactionSubtypeResponse("TRANSF", "DEBITO", "Transferencia", "Descripción", "ACTIVO");
        when(adminService.listarSubtiposTransaccion(null, null)).thenReturn(List.of(response));

        List<TransactionSubtypeResponse> result = adminController.listTransactionSubtypes(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TRANSF", result.get(0).code());
    }

    @Test
    void testGetTransactionSubtype_CodigoValido_RetornaSubtipo() {
        TransactionSubtypeResponse response = new TransactionSubtypeResponse("TRANSF", "DEBITO", "Transferencia", "Descripción", "ACTIVO");
        when(adminService.obtenerSubtipoTransaccion("TRANSF")).thenReturn(response);

        TransactionSubtypeResponse result = adminController.getTransactionSubtype("TRANSF");

        assertNotNull(result);
        assertEquals("TRANSF", result.code());
    }

    @Test
    void testCreateTransactionSubtype_DatosValidos_RetornaCreated() {
        TransactionSubtypeResponse response = new TransactionSubtypeResponse("TRANSF", "DEBITO", "Transferencia", "Descripción", "ACTIVO");
        TransactionSubtypeRequest request = new TransactionSubtypeRequest("TRANSF", "DEBITO", "Transferencia", "Descripción", "ACTIVO");
        when(adminService.crearSubtipoTransaccion(request, null)).thenReturn(response);

        ResponseEntity<TransactionSubtypeResponse> result = adminController.createTransactionSubtype(request, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("TRANSF", result.getBody().code());
    }

    // Tests para Métricas
    @Test
    void testGetMetrics_RetornaMetricasCompletas() {
        when(adminService.obtenerMetricasAdministrativas()).thenReturn(metricsResponse);

        MetricsResponse result = adminController.getMetrics();

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

    // Tests para Usuarios Core
    @Test
    void testListCoreUsers_SinFiltros_RetornaPaginacion() {
        CoreUserListResponse response = new CoreUserListResponse(1L, 0, 20, 1, List.of(userCoreResponse));
        when(adminService.listarUsuariosCore(null, null, null, null, null)).thenReturn(response);

        CoreUserListResponse result = adminController.listCoreUsers(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1L, result.total());
        assertEquals(0, result.page());
        assertFalse(result.users().isEmpty());
    }

    @Test
    void testListCoreUsers_ConFiltros_RetornaFiltrados() {
        CoreUserListResponse response = new CoreUserListResponse(1L, 0, 20, 1, List.of(userCoreResponse));
        when(adminService.listarUsuariosCore("SUC001", "ACTIVO", "Juan", 0, 20)).thenReturn(response);

        CoreUserListResponse result = adminController.listCoreUsers("SUC001", "ACTIVO", "Juan", 0, 20);

        assertNotNull(result);
        assertEquals(1L, result.total());
    }

    @Test
    void testCreateCoreUser_DatosValidos_RetornaCreated() {
        when(adminService.crearUsuarioCore(userCoreRequest, null)).thenReturn(userCoreResponse);

        ResponseEntity<UserCoreResponse> result = adminController.createCoreUser(userCoreRequest, null);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("uuid-123", result.getBody().userCoreUuid());
    }

    @Test
    void testGetCoreUser_UuidValido_RetornaUsuario() {
        when(adminService.obtenerUsuarioCore("uuid-123")).thenReturn(userCoreResponse);

        UserCoreResponse result = adminController.getCoreUser("uuid-123");

        assertNotNull(result);
        assertEquals("uuid-123", result.userCoreUuid());
        assertEquals("Juan Perez", result.fullName());
    }

    @Test
    void testChangeCoreUserStatus_DatosValidos_RetornaUsuarioActualizado() {
        ChangeStatusRequest request = new ChangeStatusRequest("INACTIVO");
        when(adminService.cambiarEstadoUsuarioCore("uuid-123", request, null))
                .thenReturn(userCoreResponse);

        UserCoreResponse result = adminController.changeCoreUserStatus("uuid-123", request, null);

        assertNotNull(result);
        assertEquals("uuid-123", result.userCoreUuid());
    }
}
