package com.banquito.core.admin.infrastructure.grpc;

import com.banquito.core.admin.application.service.AdminService;
import com.banquito.core.admin.shared.exception.BusinessException;
import com.banquito.core.admin.infrastructure.grpc.generated.AccountSubtypeCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.AccountSubtypeResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.AdminCatalogServiceGrpc;
import com.banquito.core.admin.infrastructure.grpc.generated.BranchCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.BranchResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.BusinessDayRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.BusinessDayResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.DecimalValueResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.EmptyRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.FinancialInstitutionResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.OperationalWindowCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.OperationalWindowDomainRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.OperationalWindowListResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.OperationalWindowResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.ParameterCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.ParameterResponse;
import com.banquito.core.admin.infrastructure.grpc.generated.RoutingCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.TransactionSubtypeCodeRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.TransactionSubtypeResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AdminCatalogGrpcService extends AdminCatalogServiceGrpc.AdminCatalogServiceImplBase {
    private final AdminService adminService;

    public AdminCatalogGrpcService(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void getBranchByCode(BranchCodeRequest request, StreamObserver<BranchResponse> observer) {
        try {
            var r = adminService.obtenerSucursal(request.getCode());
            observer.onNext(BranchResponse.newBuilder()
                    .setBranchUuid(nvl(r.branchUuid()))
                    .setCode(nvl(r.code()))
                    .setName(nvl(r.name()))
                    .setCity(nvl(r.city()))
                    .setStatus(nvl(r.status()))
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getParameterByCode(ParameterCodeRequest request, StreamObserver<ParameterResponse> observer) {
        try {
            var r = adminService.obtenerParametro(request.getCode());
            observer.onNext(ParameterResponse.newBuilder()
                    .setCode(nvl(r.code()))
                    .setName(nvl(r.name()))
                    .setValue(nvl(r.value()))
                    .setDataType(nvl(r.dataType()))
                    .setStatus(nvl(r.status()))
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getIvaRate(EmptyRequest request, StreamObserver<DecimalValueResponse> observer) {
        try {
            var r = adminService.obtenerParametro("IVA_PORCENTAJE");
            observer.onNext(DecimalValueResponse.newBuilder().setValue(nvl(r.value())).build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getOperationalWindowByCode(OperationalWindowCodeRequest request, StreamObserver<OperationalWindowResponse> observer) {
        try {
            observer.onNext(toGrpc(adminService.obtenerVentana(request.getCode())));
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getOperationalWindowByDomain(OperationalWindowDomainRequest request, StreamObserver<OperationalWindowListResponse> observer) {
        try {
            OperationalWindowListResponse.Builder builder = OperationalWindowListResponse.newBuilder();
            adminService.listarVentanas(request.getDomain(), "ACTIVA").forEach(window -> builder.addWindows(toGrpc(window)));
            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getInstitutionByRoutingCode(RoutingCodeRequest request, StreamObserver<FinancialInstitutionResponse> observer) {
        try {
            var r = adminService.obtenerInstitucion(request.getRoutingCode());
            observer.onNext(FinancialInstitutionResponse.newBuilder()
                    .setRoutingCode(nvl(r.routingCode()))
                    .setName(nvl(r.name()))
                    .setAccountPrefix(nvl(r.accountPrefix()))
                    .setBanquito(Boolean.TRUE.equals(r.banquito()))
                    .setStatus(nvl(r.status()))
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getAccountSubtype(AccountSubtypeCodeRequest request, StreamObserver<AccountSubtypeResponse> observer) {
        try {
            var r = adminService.obtenerSubtipoCuenta(request.getCode());
            observer.onNext(AccountSubtypeResponse.newBuilder()
                    .setCode(nvl(r.code()))
                    .setBaseType(nvl(r.baseType()))
                    .setName(nvl(r.name()))
                    .setStatus(nvl(r.status()))
                    .addAllAllowedCustomerTypes(r.allowedCustomerTypes())
                    .addAllAllowedPurposes(r.allowedPurposes())
                    .setSupportsMassPayments(Boolean.TRUE.equals(r.supportsMassPayments()))
                    .setSupportsFavoritePaymentAccount(Boolean.TRUE.equals(r.supportsFavoritePaymentAccount()))
                    .setMinimumOpeningBalance(r.minimumOpeningBalance() == null ? "0.00" : r.minimumOpeningBalance().toPlainString())
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void getTransactionSubtype(TransactionSubtypeCodeRequest request, StreamObserver<TransactionSubtypeResponse> observer) {
        try {
            var r = adminService.obtenerSubtipoTransaccion(request.getCode());
            observer.onNext(TransactionSubtypeResponse.newBuilder()
                    .setCode(nvl(r.code()))
                    .setName(nvl(r.name()))
                    .setBaseMovementType(nvl(r.baseMovementType()))
                    .setStatus(nvl(r.status()))
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    @Override
    public void isBusinessDay(BusinessDayRequest request, StreamObserver<BusinessDayResponse> observer) {
        try {
            var r = adminService.obtenerDiaHabil(LocalDate.parse(request.getDate()));
            observer.onNext(BusinessDayResponse.newBuilder()
                    .setDate(r.date().toString())
                    .setHoliday(Boolean.TRUE.equals(r.holiday()))
                    .setWeekend(Boolean.TRUE.equals(r.weekend()))
                    .setBusinessDay(Boolean.TRUE.equals(r.businessDay()))
                    .setDescription(nvl(r.description()))
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            fail(observer, ex);
        }
    }

    private OperationalWindowResponse toGrpc(com.banquito.core.admin.api.dto.api.OperationalWindowResponse r) {
        return OperationalWindowResponse.newBuilder()
                .setCode(nvl(r.code()))
                .setName(nvl(r.name()))
                .setOperationalDomain(nvl(r.operationalDomain()))
                .setStartTime(r.startTime() == null ? "" : r.startTime().toString())
                .setCutoffTime(r.cutoffTime() == null ? "" : r.cutoffTime().toString())
                .setEndTime(r.endTime() == null ? "" : r.endTime().toString())
                .setActionAfterCutoff(nvl(r.actionAfterCutoff()))
                .setStatus(nvl(r.status()))
                .build();
    }

    private static <T> void fail(StreamObserver<T> observer, RuntimeException ex) {
        if (ex instanceof BusinessException be) {
            observer.onError(Status.FAILED_PRECONDITION.withDescription(be.getCode() + "|" + be.getMessage()).asRuntimeException());
        } else {
            observer.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
