/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface SimulationPage extends IRequestablePage {

    String PAGE_PARAMETER_RESULT_OID = "RESULT_OID"; // -> SimulationResultType
    String PAGE_PARAMETER_MARK_OID = "MARK_OID"; // -> SimulationResultMarkType
    String PAGE_PARAMETER_CONTAINER_ID = "CONTAINER_ID"; // -> SimulationResultProcessedObjectType

    default String getPageParameterResultOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_RESULT_OID).toString();
    }

    default String getPageParameterMarkOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_MARK_OID).toString();
    }

    default Long getPageParameterContainerId() {
        PageParameters params = getPageParameters();
        String id = params.get(PAGE_PARAMETER_CONTAINER_ID).toString();

        if (!StringUtils.isNumeric(id)) {
            return null;
        }

        return Long.parseLong(id);
    }

    default SimulationResultType loadSimulationResult(PageBase page) {
        String resultOid = getPageParameterResultOid();
        return SimulationsGuiUtil.loadSimulationResult(page, resultOid);
    }
}
