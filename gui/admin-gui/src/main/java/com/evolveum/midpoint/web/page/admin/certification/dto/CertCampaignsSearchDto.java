/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class CertCampaignsSearchDto implements Serializable, DebugDumpable {
	private static final long serialVersionUID = 1L;

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
