package com.insighton.core.groups.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("InsightOn-ruleEngine")
public interface EngineClient {

    @DeleteMapping("/internal/groups/{group-id}/engines")
    void deleteEnginesByGroupId(@PathVariable("group-id") Long groupId);
}
