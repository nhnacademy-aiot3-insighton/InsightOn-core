package com.insighton.core.common.exception;

import com.insighton.core.domain.actuators.exception.UnsupportedControlProviderException;
import com.insighton.core.domain.actuators.exception.*;
import com.insighton.core.domain.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.domain.gateway.exception.*;
import com.insighton.core.domain.groupmember.exception.*;
import com.insighton.core.domain.groupregistration.exception.AlreadyProcessedException;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import com.insighton.core.domain.groups.exception.*;
import com.insighton.core.domain.location.exception.EmptyValueException;
import com.insighton.core.domain.location.exception.LocationAlreadyException;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.exception.InvalidDateTimeFormatException;
import com.insighton.core.domain.widgets.exception.WidgetConfigNotFoundException;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 도메인이 늘어나면 이 클래스에 핸들러 메서드 추가.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({GatewayNotFoundException.class, GroupNotFoundException.class,
            InviteTokenNotFoundException.class, GroupMemberNotFoundException.class,
            LocationNotFoundException.class,
            SensorNotFoundException.class, ActuatorNotFoundException.class,
            MetricKeyNotFoundException.class, DashboardNotFoundException.class,
            WidgetNotFoundException.class, WidgetConfigNotFoundException.class,
            ActuatorLocationsActuatorTypeNotFound.class, GroupRegistrationNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler({GatewayAccessDeniedException.class, NoPermissionException.class,
            UnAuthorizedAccessException.class, InvalidServiceCredentialException.class,
            UnauthorizedGroupRegistrationAccessException.class
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler({AlreadyJoinedException.class, MetricKeyAlreadyExistsException.class, GatewayAlreadyExistsException.class,
            AlreadyRequestedException.class, AlreadyProcessedException.class, AlreadyDashboardSaveException.class,
            LocationAlreadyException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, SuperManagerCannotLeaveException.class, CannotKickSelfException.class,
            ManagerRoleRequiredForTransferException.class,
            InvalidActuatorValueException.class, InvalidSensorValueException.class,
            CouldNotAbleToUpdateByUserToSystem.class, EmptyValueException.class,
            InvalidGatewayConnectionConfigException.class, InvalidGatewayValueException.class,
            RegionNotFoundException.class, InvalidDateTimeFormatException.class,
            InvitationTokenMismatchException.class, WeatherApiException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            UnsupportedControlProviderException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception e) {
        String message = e.getMessage();
        if (e instanceof MethodArgumentNotValidException manve) {
            message = manve.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    // 액추에이터 공급자(SmartThings/LG ThinQ) 호출 실패 - CORE 버그가 아니라 외부 공급자 장애이므로 502
    @ExceptionHandler(ActuatorControlException.class)
    public ResponseEntity<ErrorResponse> handleActuatorControl(
            ActuatorControlException e) {
        log.warn("액추에이터 공급자 제어 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(feign.FeignException e) {
        log.warn("내부 서비스 Feign 통신 예외 발생 - status: {}, message: {}", e.status(), e.getMessage());
        int status = e.status() > 0 ? e.status() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        String message = "내부 서비스 통신 오류가 발생했습니다.";
        try {
            String body = e.contentUTF8();
            if (body != null && !body.isBlank()) {
                message = body;
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.status(status)
                .body(new ErrorResponse(status, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
        log.error("Unhandled Exception Occurred: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }
}
