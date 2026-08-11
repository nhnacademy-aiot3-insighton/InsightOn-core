package com.insighton.core.common.exception;

import com.insighton.core.domain.actuators.exception.*;
import com.insighton.core.domain.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.domain.gateway.exception.*;
import com.insighton.core.domain.groupmember.exception.*;
import com.insighton.core.domain.groupregistration.exception.AlreadyProcessedException;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.location.exception.EmptyValueException;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.exception.InvalidDateTimeFormatException;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import com.insighton.core.domain.widgets.exception.WidgetConfigNotFoundException;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 도메인이 늘어나면 이 클래스에 핸들러 메서드 추가.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({GatewayNotFoundException.class, GroupNotFoundException.class,
            InviteTokenNotFoundException.class, GroupMemberNotFoundException.class,
            UserIdNotFoundException.class, LocationNotFoundException.class,
            SensorNotFoundException.class, ActuatorNotFoundException.class,
            MetricKeyNotFoundException.class, DashboardNotFoundException.class,
            WidgetNotFoundException.class, WidgetConfigNotFoundException.class,
            ActuatorLocationsActuatorTypeNotFound.class,
            GroupRegistrationNotFoundException.class
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
            AlreadyRequestedException.class, AlreadyProcessedException.class, AlreadyDashboardSaveException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, SuperManagerCannotLeaveException.class,
            NotJoinedAnyGroupException.class, ManagerRoleRequiredForTransferException.class,
            InvalidActuatorValueException.class, InvalidSensorValueException.class,
            CouldNotAbleToUpdateByUserToSystem.class, EmptyValueException.class,
            InvalidGatewayConnectionConfigException.class, InvalidGatewayValueException.class,
            RegionNotFoundException.class, InvalidDateTimeFormatException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }
}
