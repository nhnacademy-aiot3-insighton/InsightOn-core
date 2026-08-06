package com.insighton.core.exception;

import com.insighton.core.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.actuators.exception.CouldNotAbleToUpdateByUserToSystem;
import com.insighton.core.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.device_attributes.exception.MetricKeyNotFoundException;
import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.groupmember.exception.*;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.location.exception.EmptyValueException;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.widgets.exception.WidgetConfigNotFoundException;
import com.insighton.core.widgets.exception.WidgetNotFoundException;
import com.insighton.core.sensors.exception.DeviceNotFoundException;
import com.insighton.core.sensors.exception.InvalidDeviceValueException;
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
            DeviceNotFoundException.class, ActuatorNotFoundException.class,
            MetricKeyNotFoundException.class, DashboardNotFoundException.class, 
            WidgetNotFoundException.class, WidgetConfigNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler({GatewayAccessDeniedException.class, NoPermissionException.class,
            UnAuthorizedAccessException.class, InvalidServiceCredentialException.class
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler(AlreadyJoinedException.class)
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, SuperManagerCannotLeaveException.class,
            NotJoinedAnyGroupException.class, ManagerRoleRequiredForTransferException.class,
            InvalidActuatorValueException.class, InvalidDeviceValueException.class,
            CouldNotAbleToUpdateByUserToSystem.class, EmptyValueException.class
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
