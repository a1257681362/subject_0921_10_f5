package com.evops.secrecy.controller;

import com.evops.common.ApiResponse;
import com.evops.exam.vo.ActionPreview;
import com.evops.secrecy.dto.AccessImportRequest;
import com.evops.secrecy.dto.ReviewRequest;
import com.evops.secrecy.dto.SupplementRequest;
import com.evops.secrecy.dto.SwipeRequest;
import com.evops.secrecy.entity.SecrecyAccess;
import com.evops.secrecy.entity.SecrecyAccessSupplement;
import com.evops.secrecy.entity.SecrecyAccessVersion;
import com.evops.secrecy.service.SecrecyAccessService;
import com.evops.secrecy.vo.AccessDetailView;
import com.evops.secrecy.vo.InRoomView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 出入登记模块：进出刷证 → 第二人复核确认生效；登记落库不可改，纠错走补充说明；
 * 在室清单实时可查；纸面存量两步导入。
 */
@RestController
@RequestMapping("/api/secrecy/access")
public class SecrecyAccessController {

    private final SecrecyAccessService accessService;

    public SecrecyAccessController(SecrecyAccessService accessService) {
        this.accessService = accessService;
    }

    // ---------- 刷证登记 / 第二人复核 ----------

    /** 刷证登记（可随带卷箱）：生成待复核登记，异常时段自动打标并标记给保卫。 */
    @PostMapping("/swipe")
    public ApiResponse<AccessDetailView> swipe(@Valid @RequestBody SwipeRequest request) {
        return ApiResponse.ok(accessService.swipe(request));
    }

    /** 第二人复核确认：登记生效（在室清单与卷箱状态按生效口径联动）。 */
    @PostMapping("/{id}/confirm")
    public ApiResponse<AccessDetailView> confirm(@PathVariable Long id,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(accessService.confirm(id, request));
    }

    /** 第二人复核拒绝：登记作废（终态），卷箱出入联动作废。 */
    @PostMapping("/{id}/reject")
    public ApiResponse<AccessDetailView> reject(@PathVariable Long id,
                                                @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(accessService.reject(id, request));
    }

    // ---------- 查询 ----------

    @GetMapping
    public ApiResponse<List<SecrecyAccess>> list(
            @RequestParam(required = false) String personCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Boolean offHours,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(accessService.list(personCode, status, direction, offHours, dateFrom, dateTo));
    }

    /** 在室清单：已生效进出实时推导（待复核不计入，单列计数）。 */
    @GetMapping("/in-room")
    public ApiResponse<InRoomView> inRoom() {
        return ApiResponse.ok(accessService.inRoom());
    }

    /** 登记详情：本体 + 版本链 + 补充说明 + 随带卷箱。 */
    @GetMapping("/{id}")
    public ApiResponse<AccessDetailView> detail(@PathVariable Long id) {
        return ApiResponse.ok(accessService.detail(id));
    }

    /** 出入登记版本链（登记 / 复核确认 / 复核拒绝全程只追加）。 */
    @GetMapping("/{id}/versions")
    public ApiResponse<List<SecrecyAccessVersion>> versions(@PathVariable Long id) {
        return ApiResponse.ok(accessService.versions(id));
    }

    // ---------- 补充说明（登记不可改，纠错唯一入口） ----------

    @PostMapping("/{id}/supplements")
    public ApiResponse<SecrecyAccessSupplement> addSupplement(@PathVariable Long id,
                                                              @Valid @RequestBody SupplementRequest request) {
        return ApiResponse.ok(accessService.addSupplement(id, request));
    }

    @GetMapping("/{id}/supplements")
    public ApiResponse<List<SecrecyAccessSupplement>> supplements(@PathVariable Long id) {
        return ApiResponse.ok(accessService.listSupplements(id));
    }

    // ---------- 纸面存量导入（两步） ----------

    /** 第一步：预检每条可导入 / 跳过与原因，不落库。 */
    @PostMapping("/import/preview")
    public ApiResponse<ActionPreview> previewImport(@Valid @RequestBody AccessImportRequest request) {
        return ApiResponse.ok(accessService.previewImport(request));
    }

    /** 第二步：确认导入。结构性问题整批拒收；导入即为已生效历史，保留原始登记时间。 */
    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importAccess(@Valid @RequestBody AccessImportRequest request) {
        return ApiResponse.ok(accessService.importAccess(request));
    }
}
