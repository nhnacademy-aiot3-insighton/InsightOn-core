package com.insighton.core.actuator_run_logs.entity;


import com.insighton.core.actuators.entity.Actuator;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "actuator_run_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActuatorRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simulator_run_log_id")
    private Long runLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actuator_id", nullable = false)
    private Actuator actuatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", length = 50, nullable = false)
    private CommandType commandType;

    @Column(name = "command_value", length = 50, nullable = false)
    private String commandValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "executed_by_type", length = 50, nullable = false)
    private ExecutedByType executedByType;

    @Column(name = "executed_by_user_id")
    private Long executedByUserId;

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;


}
