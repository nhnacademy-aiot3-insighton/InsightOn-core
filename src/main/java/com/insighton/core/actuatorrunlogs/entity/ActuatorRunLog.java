package com.insighton.core.actuatorrunlogs.entity;


import com.insighton.core.actuators.entity.Actuator;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// 액추에이터 제어 명령이 실행될 때마다 남는 감사(audit) 이력 - 상태변경 성공 시 자동으로 생성됨
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
    private Long runLogId; // 실행 이력 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actuator_id", nullable = false)
    private Actuator actuator; // 어떤 액추에이터가 조작됐는지

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", length = 50, nullable = false)
    private CommandType commandType; // 무슨 종류의 명령이었는지 (전원/모드/온도 등)

    @Column(name = "command_value", length = 50, nullable = false)
    private String commandValue; // 실제 적용된 값 (ex. "ON", "24.0")

    @Enumerated(EnumType.STRING)
    @Column(name = "executed_by_type", length = 50, nullable = false)
    private ExecutedByType executedByType; // 누가 실행시켰는지 (USER/AI_SYSTEM/RULE_ENGINE) - 리포트의 조작 주체 비율 집계 원본

    @Column(name = "executed_by_user_id")
    private Long executedByUserId; // USER가 실행한 경우에만 채워짐, 시스템 실행이면 null

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt; // 실제 물리 집행이 완료된 시각

}