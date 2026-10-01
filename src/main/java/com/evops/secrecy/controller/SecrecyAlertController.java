package com.evops.secrecy.controller;

import com.evops.common.ApiResponse;
import com.evops.secrecy.entity.SecrecyAlert;
import com.evops.secrecy.service.SecrecyAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 异常标记：异常时段进出自动标记给保卫；保卫签收闭环。
 */
@RestController
@RequestMapping("/api/secrecy/alerts")
public class SecrecyAlertController {

    private final SecrecyAlertService alertService;

    public SecrecyAlertController(SecrecyAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<List<SecrecyAlert>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(alertService.list(status));
    }

    /** 保卫签收：NEW → ACKED，记签收人与钟点。 */
    @PostMapping("/{id}/ack")
    public ApiResponse<SecrecyAlert> ack(@PathVariable Long id) {
        return ApiResponse.ok(alertService.ack(id));
    }
}
