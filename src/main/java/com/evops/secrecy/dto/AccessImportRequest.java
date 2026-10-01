package com.evops.secrecy.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 纸面存量导入：先 preview 看影响范围，确认后再 execute。
 */
@Data
public class AccessImportRequest {
    @NotEmpty(message = "导入条目不能为空")
    @Valid
    private List<AccessImportItem> items;
}
