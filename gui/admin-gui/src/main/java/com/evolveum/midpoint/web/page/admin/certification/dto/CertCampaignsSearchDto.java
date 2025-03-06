/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class CertCampaignsSearchDto implements Serializable, DebugDumpable {
    @Serial private static final long serialVersionUID = 1L;

    public static final String F_STATE_FILTER = "stateFilter";

    @NotNull private CertCampaignStateFilter stateFilter = CertCampaignStateFilter.ALL;

    @NotNull
    public CertCampaignStateFilter getStateFilter() {
        return stateFilter;
    }

    public void setStateFilter(@NotNull CertCampaignStateFilter stateFilter) {
        this.stateFilter = stateFilter;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("CertCampaignSearchDto\n");
        DebugUtil.debugDumpWithLabelLn(sb, "stateFilter", stateFilter.toString(), indent+1);
        return sb.toString();
    }
}
