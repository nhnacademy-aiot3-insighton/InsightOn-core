package com.insighton.core.gateway.repository;

import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.entity.ProtocolType;
import java.util.List;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {

    /**
 * 지정된 프로토콜 유형의 모든 게이트웨이를 조회한다.
 *
 * @param protocolType 조회할 프로토콜 유형
 * @return 지정된 프로토콜 유형에 해당하는 게이트웨이 목록
 */
    List<Gateway> findAllByProtocolType(ProtocolType protocolType);
    /**
 * 지정한 그룹에 속한 모든 게이트웨이를 조회한다.
 *
 * @param groupId 조회할 그룹의 ID
 * @return 해당 그룹에 속한 게이트웨이 목록
 */
List<Gateway> findAllByGroupsId(Long groupId);
    /**
 * 게이트웨이 ID로 게이트웨이를 조회합니다.
 *
 * @param gatewayId 조회할 게이트웨이 ID
 * @return 해당 ID의 게이트웨이를 포함한 {@code Optional}; 일치하는 게이트웨이가 없으면 빈 {@code Optional}
 */
Optional<Gateway> findByGatewayId(Long gatewayId);
}