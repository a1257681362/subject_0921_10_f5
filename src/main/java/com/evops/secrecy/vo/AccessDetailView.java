package com.evops.secrecy.vo;

import com.evops.secrecy.entity.SecrecyAccess;
import com.evops.secrecy.entity.SecrecyAccessSupplement;
import com.evops.secrecy.entity.SecrecyAccessVersion;
import com.evops.secrecy.entity.SecrecyBoxMove;
import lombok.Data;

import java.util.List;

/**
 * 出入登记详情：登记本体 + 版本链 + 补充说明 + 随带卷箱。
 */
@Data
public class AccessDetailView {
    private SecrecyAccess access;
    private List<SecrecyAccessVersion> versions;
    private List<SecrecyAccessSupplement> supplements;
    private List<SecrecyBoxMove> boxMoves;
}
