package com.evops.secrecy.controller;

import com.evops.common.ApiResponse;
import com.evops.secrecy.dto.BoxRequest;
import com.evops.secrecy.entity.SecrecyBox;
import com.evops.secrecy.entity.SecrecyBoxMove;
import com.evops.secrecy.service.SecrecyBoxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 卷箱：建档与轨迹查询；出入跟人员进出挂钩（随刷证登记创建、随复核联动），不单独开放出入接口。
 */
@RestController
@RequestMapping("/api/secrecy/boxes")
public class SecrecyBoxController {

    private final SecrecyBoxService boxService;

    public SecrecyBoxController(SecrecyBoxService boxService) {
        this.boxService = boxService;
    }

    @PostMapping
    public ApiResponse<SecrecyBox> create(@Valid @RequestBody BoxRequest request) {
        return ApiResponse.ok(boxService.createBox(request));
    }

    @GetMapping
    public ApiResponse<List<SecrecyBox>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(boxService.listBoxes(status));
    }

    /** 卷箱出入轨迹（随人员登记挂钩的全部记录）。 */
    @GetMapping("/{id}/moves")
    public ApiResponse<List<SecrecyBoxMove>> moves(@PathVariable Long id) {
        return ApiResponse.ok(boxService.listMoves(id));
    }
}
