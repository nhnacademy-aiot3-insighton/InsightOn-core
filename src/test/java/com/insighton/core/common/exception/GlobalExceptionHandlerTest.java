package com.insighton.core.common.exception;

import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.exception.LocationAlreadyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("처리되지 않은 알 수 없는 예외 발생 시 500 Internal Server Error 반환")
    void handleUnhandledException_returns500() throws Exception {
        mockMvc.perform(get("/test/unhandled-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("NotFound 예외 발생 시 404 Not Found 반환")
    void handleNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("BadRequest 예외 발생 시 400 Bad Request 반환")
    void handleBadRequest_returns400() throws Exception {
        mockMvc.perform(get("/test/bad-request-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("AccessDenied 예외 발생 시 403 Forbidden 반환")
    void handleAccessDenied_returns403() throws Exception {
        mockMvc.perform(get("/test/access-denied-exception"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Conflict 예외 발생 시 409 Conflict 반환")
    void handleConflict_returns409() throws Exception {
        mockMvc.perform(get("/test/conflict-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/unhandled-exception")
        public void throwUnhandledException() {
            throw new RuntimeException("Unexpected Database Error");
        }

        @GetMapping("/test/not-found-exception")
        public void throwNotFoundException() {
            throw new GroupNotFoundException(999L);
        }

        @GetMapping("/test/bad-request-exception")
        public void throwBadRequestException() {
            throw new IllegalArgumentException("Invalid parameter value");
        }

        @GetMapping("/test/access-denied-exception")
        public void throwAccessDeniedException() {
            throw NoPermissionException.forAdmin(1L);
        }

        @GetMapping("/test/conflict-exception")
        public void throwConflictException() {
            throw new LocationAlreadyException("거실");
        }
    }
}
