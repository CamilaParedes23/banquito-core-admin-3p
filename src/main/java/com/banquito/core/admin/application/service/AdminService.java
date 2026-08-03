package com.banquito.core.admin.application.service;

import com.banquito.core.admin.api.dto.api.*;
import com.banquito.core.admin.domain.enums.*;
import com.banquito.core.admin.domain.model.*;
import com.banquito.core.admin.domain.repository.*;
import com.banquito.core.admin.shared.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class AdminService {
    private static final Set<String> ALLOWED_CUSTOMER_TYPES = Set.of("NATURAL", "JURIDICO");
    private static final Set<String> ALLOWED_ACCOUNT_PURPOSES = Set.of("GENERAL", "OPERATIVA", "NOMINA", "IMPUESTOS");


    private final SucursalRepository sucursalRepository;
    private final FeriadoRepository feriadoRepository;
    private final ParametroCoreRepository parametroRepository;
    private final VentanaOperativaRepository ventanaRepository;
    private final InstitucionFinancieraRepository institucionRepository;
    private final SubtipoCuentaRepository subtipoCuentaRepository;
    private final SubtipoTransaccionRepository subtipoTransaccionRepository;
    private final UsuarioCoreRepository usuarioCoreRepository;
    private final AdminMapper mapper;
    private final AuditoriaAdminService auditoriaService;
    private final OutboxEventService outboxEventService;

    public AdminService(SucursalRepository sucursalRepository, FeriadoRepository feriadoRepository,
                        ParametroCoreRepository parametroRepository, VentanaOperativaRepository ventanaRepository,
                        InstitucionFinancieraRepository institucionRepository,
                        SubtipoCuentaRepository subtipoCuentaRepository,
                        SubtipoTransaccionRepository subtipoTransaccionRepository,
                        UsuarioCoreRepository usuarioCoreRepository, AdminMapper mapper,
                        AuditoriaAdminService auditoriaService, OutboxEventService outboxEventService) {
        this.sucursalRepository = sucursalRepository;
        this.feriadoRepository = feriadoRepository;
        this.parametroRepository = parametroRepository;
        this.ventanaRepository = ventanaRepository;
        this.institucionRepository = institucionRepository;
        this.subtipoCuentaRepository = subtipoCuentaRepository;
        this.subtipoTransaccionRepository = subtipoTransaccionRepository;
        this.usuarioCoreRepository = usuarioCoreRepository;
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
        this.outboxEventService = outboxEventService;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listarSucursales(String status) {
        if (status == null || status.isBlank()) return sucursalRepository.findAll().stream().map(mapper::toBranchResponse).toList();
        return sucursalRepository.findByEstadoOrderByNombreAsc(enumValue(EstadoSucursalEnum.class, status, "ADMIN_BRANCH_STATUS_INVALID")).stream().map(mapper::toBranchResponse).toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse obtenerSucursal(String codigo) {
        return mapper.toBranchResponse(findSucursal(codigo));
    }

    @Transactional
    public BranchResponse crearSucursal(BranchRequest request, String actorUuid) {
        if (sucursalRepository.existsByCodigoSucursal(request.code())) {
            throw new BusinessException("ADMIN_BRANCH_DUPLICATED", "Ya existe una sucursal con el código indicado", HttpStatus.CONFLICT);
        }
        Sucursal sucursal = sucursalRepository.save(Sucursal.crear(request.code(), request.name(), request.city(), request.address()));
        auditoriaService.registrar(actorUuid, "CREATE_BRANCH", "SUCURSAL", sucursal.getCodigoSucursal(), ResultadoAuditoriaAdminEnum.OK, null);
        outboxEventService.registrar("ADMIN_BRANCH_CREATED", "SUCURSAL", sucursal.getCodigoSucursal(), "{\"code\":\"" + sucursal.getCodigoSucursal() + "\"}");
        return mapper.toBranchResponse(sucursal);
    }

    @Transactional
    public BranchResponse actualizarSucursal(String codigo, BranchRequest request, String actorUuid) {
        Sucursal sucursal = findSucursal(codigo);
        sucursal.actualizar(request.name(), request.city(), request.address());
        auditoriaService.registrar(actorUuid, "UPDATE_BRANCH", "SUCURSAL", sucursal.getCodigoSucursal(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toBranchResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public BranchResponse cambiarEstadoSucursal(String codigo, ChangeStatusRequest request, String actorUuid) {
        Sucursal sucursal = findSucursal(codigo);
        sucursal.cambiarEstado(enumValue(EstadoSucursalEnum.class, request.status(), "ADMIN_BRANCH_STATUS_INVALID"));
        auditoriaService.registrar(actorUuid, "CHANGE_BRANCH_STATUS", "SUCURSAL", sucursal.getCodigoSucursal(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toBranchResponse(sucursalRepository.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> listarFeriados(String status) {
        if (status == null || status.isBlank()) return feriadoRepository.findAll().stream().map(mapper::toHolidayResponse).toList();
        return feriadoRepository.findByEstadoOrderByFechaFeriadoAsc(enumValue(EstadoRegistroEnum.class, status, "ADMIN_HOLIDAY_STATUS_INVALID")).stream().map(mapper::toHolidayResponse).toList();
    }

    @Transactional
    public HolidayResponse crearFeriado(HolidayRequest request, String actorUuid) {
        if (feriadoRepository.existsById(request.holidayDate())) {
            throw new BusinessException("ADMIN_HOLIDAY_DUPLICATED", "Ya existe un feriado configurado para esa fecha", HttpStatus.CONFLICT);
        }
        Feriado feriado = feriadoRepository.save(Feriado.crear(request.holidayDate(), request.name(), request.weekend()));
        auditoriaService.registrar(actorUuid, "CREATE_HOLIDAY", "FERIADO", feriado.getFechaFeriado().toString(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toHolidayResponse(feriado);
    }

    @Transactional
    public HolidayResponse actualizarFeriado(LocalDate fecha, UpdateHolidayRequest request, String actorUuid) {
        Feriado feriado = feriadoRepository.findById(fecha)
                .orElseThrow(() -> notFound("ADMIN_HOLIDAY_NOT_FOUND", "Feriado no encontrado"));
        feriado.actualizar(request.name().trim(), request.weekend());
        auditoriaService.registrar(actorUuid, "UPDATE_HOLIDAY", "FERIADO", fecha.toString(),
                ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toHolidayResponse(feriadoRepository.save(feriado));
    }

    @Transactional
    public HolidayResponse cambiarEstadoFeriado(LocalDate fecha, ChangeStatusRequest request, String actorUuid) {
        Feriado feriado = feriadoRepository.findById(fecha).orElseThrow(() -> notFound("ADMIN_HOLIDAY_NOT_FOUND", "Feriado no encontrado"));
        feriado.cambiarEstado(enumValue(EstadoRegistroEnum.class, request.status(), "ADMIN_HOLIDAY_STATUS_INVALID"));
        auditoriaService.registrar(actorUuid, "CHANGE_HOLIDAY_STATUS", "FERIADO", fecha.toString(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toHolidayResponse(feriadoRepository.save(feriado));
    }

    @Transactional(readOnly = true)
    public BusinessDayResponse obtenerDiaHabil(LocalDate fecha) {
        boolean weekend = fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY;
        Feriado feriado = feriadoRepository.findById(fecha).orElse(null);
        boolean holiday = feriado != null && feriado.getEstado() == EstadoRegistroEnum.ACTIVO;
        boolean businessDay = !weekend && !holiday;
        String description = holiday ? feriado.getNombre() : (weekend ? "Fin de semana" : "Día hábil");
        return new BusinessDayResponse(fecha, holiday, weekend, businessDay, description);
    }

    @Transactional(readOnly = true)
    public BusinessDayResponse obtenerSiguienteDiaHabil(LocalDate fecha) {
        LocalDate siguiente = fecha.plusDays(1);
        BusinessDayResponse evaluacion = obtenerDiaHabil(siguiente);
        while (!evaluacion.businessDay()) {
            siguiente = siguiente.plusDays(1);
            evaluacion = obtenerDiaHabil(siguiente);
        }
        return evaluacion;
    }

    @Transactional(readOnly = true)
    public List<ParameterResponse> listarParametros(String status) {
        if (status == null || status.isBlank()) return parametroRepository.findAll().stream().map(mapper::toParameterResponse).toList();
        return parametroRepository.findByEstadoOrderByCodigoAsc(enumValue(EstadoRegistroEnum.class, status, "ADMIN_PARAMETER_STATUS_INVALID")).stream().map(mapper::toParameterResponse).toList();
    }

    @Transactional(readOnly = true)
    public ParameterResponse obtenerParametro(String codigo) { return mapper.toParameterResponse(findParametro(codigo)); }

    @Transactional
    public ParameterResponse crearParametro(ParameterRequest request, String actorUuid) {
        if (parametroRepository.existsById(request.code())) throw new BusinessException("ADMIN_PARAMETER_DUPLICATED", "Ya existe el parámetro indicado", HttpStatus.CONFLICT);
        ParametroCore parametro = parametroRepository.save(ParametroCore.crear(request.code(), request.name(), request.value(), enumValue(TipoDatoParametroEnum.class, request.dataType(), "ADMIN_PARAMETER_TYPE_INVALID"), request.description()));
        auditoriaService.registrar(actorUuid, "CREATE_PARAMETER", "PARAMETRO_CORE", parametro.getCodigo(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toParameterResponse(parametro);
    }

    @Transactional
    public ParameterResponse actualizarParametro(String codigo, ParameterRequest request, String actorUuid) {
        ParametroCore parametro = findParametro(codigo);
        parametro.actualizar(request.name(), request.value(), enumValue(TipoDatoParametroEnum.class, request.dataType(), "ADMIN_PARAMETER_TYPE_INVALID"), request.description(), request.status() == null ? parametro.getEstado() : enumValue(EstadoRegistroEnum.class, request.status(), "ADMIN_PARAMETER_STATUS_INVALID"));
        auditoriaService.registrar(actorUuid, "UPDATE_PARAMETER", "PARAMETRO_CORE", codigo, ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toParameterResponse(parametroRepository.save(parametro));
    }

    @Transactional(readOnly = true)
    public List<OperationalWindowResponse> listarVentanas(String domain, String status) {
        EstadoVentanaOperativaEnum estado = status == null || status.isBlank() ? EstadoVentanaOperativaEnum.ACTIVA : enumValue(EstadoVentanaOperativaEnum.class, status, "ADMIN_WINDOW_STATUS_INVALID");
        if (domain != null && !domain.isBlank()) {
            return ventanaRepository.findByDominioOperativoAndEstadoOrderByCodigoAsc(enumValue(DominioOperativoEnum.class, domain, "ADMIN_WINDOW_DOMAIN_INVALID"), estado).stream().map(mapper::toOperationalWindowResponse).toList();
        }
        return ventanaRepository.findByEstadoOrderByCodigoAsc(estado).stream().map(mapper::toOperationalWindowResponse).toList();
    }

    @Transactional(readOnly = true)
    public OperationalWindowResponse obtenerVentana(String codigo) { return mapper.toOperationalWindowResponse(findVentana(codigo)); }

    @Transactional
    public OperationalWindowResponse crearVentana(OperationalWindowRequest request, String actorUuid) {
        if (ventanaRepository.existsByCodigo(request.code())) throw new BusinessException("ADMIN_WINDOW_DUPLICATED", "Ya existe la ventana operativa indicada", HttpStatus.CONFLICT);
        VentanaOperativa ventana = ventanaRepository.save(VentanaOperativa.crear(request.code(), request.name(), enumValue(DominioOperativoEnum.class, request.operationalDomain(), "ADMIN_WINDOW_DOMAIN_INVALID"), request.startTime(), request.cutoffTime(), request.endTime(), defaultString(request.applicableDays(), "LUN,MAR,MIE,JUE,VIE"), defaultString(request.timezone(), "America/Guayaquil"), enumValue(AccionDespuesCorteEnum.class, request.actionAfterCutoff(), "ADMIN_WINDOW_ACTION_INVALID")));
        auditoriaService.registrar(actorUuid, "CREATE_OPERATIONAL_WINDOW", "VENTANA_OPERATIVA", ventana.getCodigo(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toOperationalWindowResponse(ventana);
    }

    @Transactional
    public OperationalWindowResponse actualizarVentana(String codigo, OperationalWindowRequest request, String actorUuid) {
        VentanaOperativa ventana = findVentana(codigo);
        ventana.actualizar(request.name(), enumValue(DominioOperativoEnum.class, request.operationalDomain(), "ADMIN_WINDOW_DOMAIN_INVALID"), request.startTime(), request.cutoffTime(), request.endTime(), defaultString(request.applicableDays(), ventana.getDiasAplica()), defaultString(request.timezone(), ventana.getTimezone()), enumValue(AccionDespuesCorteEnum.class, request.actionAfterCutoff(), "ADMIN_WINDOW_ACTION_INVALID"), request.status() == null ? ventana.getEstado() : enumValue(EstadoVentanaOperativaEnum.class, request.status(), "ADMIN_WINDOW_STATUS_INVALID"));
        auditoriaService.registrar(actorUuid, "UPDATE_OPERATIONAL_WINDOW", "VENTANA_OPERATIVA", codigo, ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toOperationalWindowResponse(ventanaRepository.save(ventana));
    }

    @Transactional(readOnly = true)
    public List<FinancialInstitutionResponse> listarInstituciones(String status) {
        if (status == null || status.isBlank()) return institucionRepository.findAll().stream().map(mapper::toFinancialInstitutionResponse).toList();
        return institucionRepository.findByEstadoOrderByNombreAsc(enumValue(EstadoInstitucionFinancieraEnum.class, status, "ADMIN_INSTITUTION_STATUS_INVALID")).stream().map(mapper::toFinancialInstitutionResponse).toList();
    }

    @Transactional(readOnly = true)
    public FinancialInstitutionResponse obtenerInstitucion(String routingCode) { return mapper.toFinancialInstitutionResponse(findInstitucion(routingCode)); }

    @Transactional
    public FinancialInstitutionResponse crearInstitucion(FinancialInstitutionRequest request, String actorUuid) {
        if (institucionRepository.existsByRoutingCode(request.routingCode())) throw new BusinessException("ADMIN_INSTITUTION_DUPLICATED", "Ya existe la institución financiera indicada", HttpStatus.CONFLICT);
        InstitucionFinanciera institucion = institucionRepository.save(InstitucionFinanciera.crear(request.routingCode(), request.name(), request.accountPrefix(), request.banquito()));
        auditoriaService.registrar(actorUuid, "CREATE_FINANCIAL_INSTITUTION", "INSTITUCION_FINANCIERA", institucion.getRoutingCode(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toFinancialInstitutionResponse(institucion);
    }

    @Transactional
    public FinancialInstitutionResponse actualizarInstitucion(String routingCode, FinancialInstitutionRequest request, String actorUuid) {
        InstitucionFinanciera institucion = findInstitucion(routingCode);
        institucion.actualizar(request.name(), request.accountPrefix(), request.banquito(), request.status() == null ? institucion.getEstado() : enumValue(EstadoInstitucionFinancieraEnum.class, request.status(), "ADMIN_INSTITUTION_STATUS_INVALID"));
        auditoriaService.registrar(actorUuid, "UPDATE_FINANCIAL_INSTITUTION", "INSTITUCION_FINANCIERA", routingCode, ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toFinancialInstitutionResponse(institucionRepository.save(institucion));
    }

    @Transactional(readOnly = true)
    public List<AccountSubtypeResponse> listarSubtiposCuenta(String baseType, String status, String customerType) {
        EstadoRegistroEnum estado = status == null || status.isBlank()
                ? EstadoRegistroEnum.ACTIVO
                : enumValue(EstadoRegistroEnum.class, status, "ADMIN_ACCOUNT_SUBTYPE_STATUS_INVALID");
        String normalizedCustomerType = normalizeCustomerType(customerType);

        List<SubtipoCuenta> subtypes = baseType != null && !baseType.isBlank()
                ? subtipoCuentaRepository.findByTipoBaseAndEstadoOrderByNombreAsc(
                        enumValue(TipoBaseCuentaEnum.class, baseType, "ADMIN_ACCOUNT_BASE_TYPE_INVALID"), estado)
                : subtipoCuentaRepository.findByEstadoOrderByNombreAsc(estado);

        return subtypes.stream()
                .filter(subtype -> normalizedCustomerType == null
                        || csvContains(subtype.getTiposClientePermitidos(), normalizedCustomerType))
                .map(mapper::toAccountSubtypeResponse)
                .toList();
    }
    @Transactional(readOnly = true) public AccountSubtypeResponse obtenerSubtipoCuenta(String codigo) { return mapper.toAccountSubtypeResponse(findSubtipoCuenta(codigo)); }
    @Transactional
    public AccountSubtypeResponse crearSubtipoCuenta(AccountSubtypeRequest request, String actorUuid) {
        if (subtipoCuentaRepository.existsByCodigo(request.code())) {
            throw new BusinessException("ADMIN_ACCOUNT_SUBTYPE_DUPLICATED", "Ya existe el subtipo de cuenta", HttpStatus.CONFLICT);
        }
        SubtipoCuenta s = subtipoCuentaRepository.save(SubtipoCuenta.crear(
                request.code(),
                enumValue(TipoBaseCuentaEnum.class, request.baseType(), "ADMIN_ACCOUNT_BASE_TYPE_INVALID"),
                request.name(),
                request.description(),
                normalizeCsv(request.allowedCustomerTypes(), "NATURAL,JURIDICO", ALLOWED_CUSTOMER_TYPES, "ADMIN_ACCOUNT_SUBTYPE_CUSTOMER_TYPE_INVALID", "Tipo de cliente no permitido"),
                normalizeCsv(request.allowedPurposes(), "GENERAL,OPERATIVA,NOMINA,IMPUESTOS", ALLOWED_ACCOUNT_PURPOSES, "ADMIN_ACCOUNT_SUBTYPE_PURPOSE_INVALID", "Propósito de cuenta no permitido"),
                Boolean.TRUE.equals(request.supportsMassPayments()),
                request.supportsFavoritePaymentAccount() == null || request.supportsFavoritePaymentAccount(),
                normalizeMinimumBalance(request.minimumOpeningBalance())
        ));
        auditoriaService.registrar(actorUuid, "CREATE_ACCOUNT_SUBTYPE", "SUBTIPO_CUENTA", s.getCodigo(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toAccountSubtypeResponse(s);
    }

    @Transactional
    public AccountSubtypeResponse actualizarSubtipoCuenta(String codigo, AccountSubtypeRequest request, String actorUuid) {
        SubtipoCuenta s = findSubtipoCuenta(codigo);
        s.actualizar(
                enumValue(TipoBaseCuentaEnum.class, request.baseType(), "ADMIN_ACCOUNT_BASE_TYPE_INVALID"),
                request.name(),
                request.description(),
                request.allowedCustomerTypes() == null ? s.getTiposClientePermitidos() : normalizeCsv(request.allowedCustomerTypes(), "NATURAL,JURIDICO", ALLOWED_CUSTOMER_TYPES, "ADMIN_ACCOUNT_SUBTYPE_CUSTOMER_TYPE_INVALID", "Tipo de cliente no permitido"),
                request.allowedPurposes() == null ? s.getPropositosPermitidos() : normalizeCsv(request.allowedPurposes(), "GENERAL,OPERATIVA,NOMINA,IMPUESTOS", ALLOWED_ACCOUNT_PURPOSES, "ADMIN_ACCOUNT_SUBTYPE_PURPOSE_INVALID", "Propósito de cuenta no permitido"),
                request.supportsMassPayments() == null ? s.getSoportaPagosMasivos() : request.supportsMassPayments(),
                request.supportsFavoritePaymentAccount() == null ? s.getSoportaCuentaFavorita() : request.supportsFavoritePaymentAccount(),
                request.minimumOpeningBalance() == null ? s.getSaldoMinimoApertura() : normalizeMinimumBalance(request.minimumOpeningBalance()),
                request.status() == null ? s.getEstado() : enumValue(EstadoRegistroEnum.class, request.status(), "ADMIN_ACCOUNT_SUBTYPE_STATUS_INVALID")
        );
        auditoriaService.registrar(actorUuid, "UPDATE_ACCOUNT_SUBTYPE", "SUBTIPO_CUENTA", codigo, ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toAccountSubtypeResponse(subtipoCuentaRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<TransactionSubtypeResponse> listarSubtiposTransaccion(String baseMovementType, String status) {
        EstadoRegistroEnum estado = status == null || status.isBlank() ? EstadoRegistroEnum.ACTIVO : enumValue(EstadoRegistroEnum.class, status, "ADMIN_TRANSACTION_SUBTYPE_STATUS_INVALID");
        if (baseMovementType != null && !baseMovementType.isBlank()) return subtipoTransaccionRepository.findByTipoMovimientoBaseAndEstadoOrderByNombreAsc(enumValue(TipoMovimientoBaseEnum.class, baseMovementType, "ADMIN_MOVEMENT_TYPE_INVALID"), estado).stream().map(mapper::toTransactionSubtypeResponse).toList();
        return subtipoTransaccionRepository.findByEstadoOrderByNombreAsc(estado).stream().map(mapper::toTransactionSubtypeResponse).toList();
    }
    @Transactional(readOnly = true) public TransactionSubtypeResponse obtenerSubtipoTransaccion(String codigo) { return mapper.toTransactionSubtypeResponse(findSubtipoTransaccion(codigo)); }
    @Transactional public TransactionSubtypeResponse crearSubtipoTransaccion(TransactionSubtypeRequest request, String actorUuid) { if (subtipoTransaccionRepository.existsByCodigo(request.code())) throw new BusinessException("ADMIN_TRANSACTION_SUBTYPE_DUPLICATED", "Ya existe el subtipo de transacción", HttpStatus.CONFLICT); SubtipoTransaccion s = subtipoTransaccionRepository.save(SubtipoTransaccion.crear(request.code(), request.name(), enumValue(TipoMovimientoBaseEnum.class, request.baseMovementType(), "ADMIN_MOVEMENT_TYPE_INVALID"), request.description())); auditoriaService.registrar(actorUuid, "CREATE_TRANSACTION_SUBTYPE", "SUBTIPO_TRANSACCION", s.getCodigo(), ResultadoAuditoriaAdminEnum.OK, null); return mapper.toTransactionSubtypeResponse(s); }
    @Transactional public TransactionSubtypeResponse actualizarSubtipoTransaccion(String codigo, TransactionSubtypeRequest request, String actorUuid) { SubtipoTransaccion s = findSubtipoTransaccion(codigo); s.actualizar(request.name(), enumValue(TipoMovimientoBaseEnum.class, request.baseMovementType(), "ADMIN_MOVEMENT_TYPE_INVALID"), request.description(), request.status() == null ? s.getEstado() : enumValue(EstadoRegistroEnum.class, request.status(), "ADMIN_TRANSACTION_SUBTYPE_STATUS_INVALID")); auditoriaService.registrar(actorUuid, "UPDATE_TRANSACTION_SUBTYPE", "SUBTIPO_TRANSACCION", codigo, ResultadoAuditoriaAdminEnum.OK, null); return mapper.toTransactionSubtypeResponse(subtipoTransaccionRepository.save(s)); }


    @Transactional(readOnly = true)
    public CoreUserListResponse listarUsuariosCore(String branchCode, String status, String search, Integer page, Integer size) {
        EstadoUsuarioCoreEnum estado = status == null || status.isBlank() ? null : enumValue(EstadoUsuarioCoreEnum.class, status, "ADMIN_CORE_USER_STATUS_INVALID");
        Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page,
                size == null || size <= 0 ? 20 : Math.min(size, 100)
        );
        Page<UsuarioCore> result = usuarioCoreRepository.searchCoreUsers(blankToNull(branchCode), estado, blankToNull(search), pageable);
        return new CoreUserListResponse(
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getContent().stream().map(mapper::toUserCoreResponse).toList()
        );
    }

    @Transactional
    public UserCoreResponse crearUsuarioCore(UserCoreRequest request, String actorUuid) {
        UsuarioCore existing = usuarioCoreRepository.findByUuidIdentidad(request.identityUuid()).orElse(null);
        if (existing != null) {
            boolean samePayload = java.util.Objects.equals(blankToNull(existing.getCodigoSucursal()), blankToNull(request.branchCode()))
                    && java.util.Objects.equals(existing.getNombreCompleto(), request.fullName().trim())
                    && java.util.Objects.equals(blankToNull(existing.getCargo()), blankToNull(request.position()));
            if (!samePayload) {
                throw new BusinessException("ADMIN_CORE_USER_IDENTITY_CONFLICT",
                        "La identidad ya posee un perfil operativo con información diferente", HttpStatus.CONFLICT);
            }
            return mapper.toUserCoreResponse(existing);
        }
        if (request.branchCode() != null && !request.branchCode().isBlank()) findSucursal(request.branchCode());
        UsuarioCore usuario = usuarioCoreRepository.saveAndFlush(UsuarioCore.crear(
                request.identityUuid(), blankToNull(request.branchCode()), request.fullName().trim(), blankToNull(request.position())));
        auditoriaService.registrar(actorUuid, "CREATE_CORE_USER", "USUARIO_CORE", usuario.getUuidUsuarioCore(), ResultadoAuditoriaAdminEnum.OK, null);
        return mapper.toUserCoreResponse(usuario);
    }
    @Transactional(readOnly = true) public UserCoreResponse obtenerUsuarioCore(String uuid) { return mapper.toUserCoreResponse(usuarioCoreRepository.findByUuidUsuarioCore(uuid).orElseThrow(() -> notFound("ADMIN_CORE_USER_NOT_FOUND", "Usuario operativo no encontrado"))); }
    @Transactional public UserCoreResponse cambiarEstadoUsuarioCore(String uuid, ChangeStatusRequest request, String actorUuid) { UsuarioCore u = usuarioCoreRepository.findByUuidUsuarioCore(uuid).orElseThrow(() -> notFound("ADMIN_CORE_USER_NOT_FOUND", "Usuario operativo no encontrado")); u.cambiarEstado(enumValue(EstadoUsuarioCoreEnum.class, request.status(), "ADMIN_CORE_USER_STATUS_INVALID")); auditoriaService.registrar(actorUuid, "CHANGE_CORE_USER_STATUS", "USUARIO_CORE", uuid, ResultadoAuditoriaAdminEnum.OK, null); return mapper.toUserCoreResponse(usuarioCoreRepository.save(u)); }


    @Transactional(readOnly = true)
    public MetricsResponse obtenerMetricasAdministrativas() {
        return new MetricsResponse(
                sucursalRepository.count(),
                sucursalRepository.countByEstado(EstadoSucursalEnum.ACTIVA),
                feriadoRepository.count(),
                feriadoRepository.countByEstado(EstadoRegistroEnum.ACTIVO),
                parametroRepository.count(),
                parametroRepository.countByEstado(EstadoRegistroEnum.ACTIVO),
                ventanaRepository.count(),
                ventanaRepository.countByEstado(EstadoVentanaOperativaEnum.ACTIVA),
                institucionRepository.count(),
                institucionRepository.countByEstado(EstadoInstitucionFinancieraEnum.ACTIVA),
                subtipoCuentaRepository.count(),
                subtipoCuentaRepository.countByEstado(EstadoRegistroEnum.ACTIVO),
                subtipoTransaccionRepository.count(),
                subtipoTransaccionRepository.countByEstado(EstadoRegistroEnum.ACTIVO),
                usuarioCoreRepository.count(),
                usuarioCoreRepository.countByEstadoOperativo(EstadoUsuarioCoreEnum.ACTIVO),
                auditoriaService.contarTotal(),
                outboxEventService.contarPendientes()
        );
    }

    private Sucursal findSucursal(String codigo) { return sucursalRepository.findByCodigoSucursal(codigo).orElseThrow(() -> notFound("ADMIN_BRANCH_NOT_FOUND", "Sucursal no encontrada")); }
    private ParametroCore findParametro(String codigo) { return parametroRepository.findById(codigo).orElseThrow(() -> notFound("ADMIN_PARAMETER_NOT_FOUND", "Parámetro no encontrado")); }
    private VentanaOperativa findVentana(String codigo) { return ventanaRepository.findByCodigo(codigo).orElseThrow(() -> notFound("ADMIN_WINDOW_NOT_FOUND", "Ventana operativa no encontrada")); }
    private InstitucionFinanciera findInstitucion(String routingCode) { return institucionRepository.findByRoutingCode(routingCode).orElseThrow(() -> notFound("ADMIN_INSTITUTION_NOT_FOUND", "Institución financiera no encontrada")); }
    private String normalizeCsv(java.util.List<String> values,
                                String defaultValue,
                                Set<String> allowedValues,
                                String errorCode,
                                String errorMessage) {
        if (values == null || values.isEmpty()) return defaultValue;
        java.util.List<String> normalized = values.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
        if (normalized.isEmpty()) return defaultValue;
        normalized.stream()
                .filter(value -> !allowedValues.contains(value))
                .findFirst()
                .ifPresent(value -> {
                    throw new BusinessException(errorCode, errorMessage + ": " + value, HttpStatus.BAD_REQUEST);
                });
        return String.join(",", normalized);
    }

    private java.math.BigDecimal normalizeMinimumBalance(java.math.BigDecimal value) {
        if (value == null) return java.math.BigDecimal.ZERO.setScale(2);
        if (value.signum() < 0) {
            throw new BusinessException("ADMIN_ACCOUNT_SUBTYPE_MINIMUM_BALANCE_INVALID", "El saldo mínimo de apertura no puede ser negativo", HttpStatus.BAD_REQUEST);
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private SubtipoCuenta findSubtipoCuenta(String codigo) { return subtipoCuentaRepository.findByCodigo(codigo).orElseThrow(() -> notFound("ADMIN_ACCOUNT_SUBTYPE_NOT_FOUND", "Subtipo de cuenta no encontrado")); }
    private SubtipoTransaccion findSubtipoTransaccion(String codigo) { return subtipoTransaccionRepository.findByCodigo(codigo).orElseThrow(() -> notFound("ADMIN_TRANSACTION_SUBTYPE_NOT_FOUND", "Subtipo de transacción no encontrado")); }

    private String normalizeCustomerType(String customerType) {
        if (customerType == null || customerType.isBlank()) return null;
        String normalized = customerType.trim().toUpperCase();
        if (!ALLOWED_CUSTOMER_TYPES.contains(normalized)) {
            throw new BusinessException(
                    "ADMIN_CUSTOMER_TYPE_INVALID",
                    "Tipo de cliente inválido: " + customerType,
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private boolean csvContains(String csv, String expectedValue) {
        if (csv == null || csv.isBlank()) return false;
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .anyMatch(expectedValue::equalsIgnoreCase);
    }

    private BusinessException notFound(String code, String message) { return new BusinessException(code, message, HttpStatus.NOT_FOUND); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String defaultString(String value, String defaultValue) { return value == null || value.isBlank() ? defaultValue : value; }
    private static <E extends Enum<E>> E enumValue(Class<E> enumClass, String value, String code) { try { return Enum.valueOf(enumClass, value.toUpperCase()); } catch (Exception e) { throw new BusinessException(code, "Valor no permitido: " + value, HttpStatus.BAD_REQUEST); } }
}
