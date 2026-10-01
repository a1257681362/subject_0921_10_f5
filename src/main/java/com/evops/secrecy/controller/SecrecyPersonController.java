package com.evops.secrecy.controller;

import com.evops.common.ApiResponse;
import com.evops.secrecy.dto.PersonRequest;
import com.evops.secrecy.dto.TimeRuleRequest;
import com.evops.secrecy.entity.SecrecyPerson;
import com.evops.secrecy.entity.SecrecyTimeRule;
import com.evops.secrecy.service.SecrecyPersonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 保密室建档：准入人员（刷证）与进出时段规则（异常时段判定口径）。
 */
@RestController
@RequestMapping("/api/secrecy")
public class SecrecyPersonController {

    private final SecrecyPersonService personService;

    public SecrecyPersonController(SecrecyPersonService personService) {
        this.personService = personService;
    }

    // ---------- 准入人员 ----------

    @PostMapping("/persons")
    public ApiResponse<SecrecyPerson> createPerson(@Valid @RequestBody PersonRequest request) {
        return ApiResponse.ok(personService.createPerson(request));
    }

    @GetMapping("/persons")
    public ApiResponse<List<SecrecyPerson>> listPersons(@RequestParam(required = false) String status) {
        return ApiResponse.ok(personService.listPersons(status));
    }

    // ---------- 进出时段规则 ----------

    @PostMapping("/time-rules")
    public ApiResponse<SecrecyTimeRule> createTimeRule(@Valid @RequestBody TimeRuleRequest request) {
        return ApiResponse.ok(personService.createTimeRule(request));
    }

    @GetMapping("/time-rules")
    public ApiResponse<List<SecrecyTimeRule>> listTimeRules() {
        return ApiResponse.ok(personService.listTimeRules());
    }
}
