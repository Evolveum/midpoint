/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionProcessType;

public interface AnalysisDetectionOption {

    Integer getMinRolesOccupancy();

    Integer getMinUserOccupancy();

    RangeType getFrequencyRange();

    RoleAnalysisDetectionProcessType getDetectionProcessMode();

}
