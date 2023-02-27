/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
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

/**
 * Created by Viliam Repan (lazyman).
 */
public interface SimulationPage extends IRequestablePage {

    String PAGE_PARAMETER_RESULT_OID = "RESULT_OID";
    String PAGE_PARAMETER_MARK_OID = "MARK_OID";
    String PAGE_PARAMETER_CONTAINER_ID = "CONTAINER_ID";

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

        if (!id.matches("[0-9]+")) {
            return null;
        }
        StringUtils.isNumeric(id);

        return Long.parseLong(id);
    }

    default SimulationResultType loadSimulationResult(PageBase page) {
        String resultOid = getPageParameterResultOid();

        if (!Utils.isPrismObjectOidValid(resultOid)) {
            throw new RestartResponseException(PageError404.class);
        }

        Task task = page.getPageTask();

        PrismObject<SimulationResultType> object = WebModelServiceUtils.loadObject(
                SimulationResultType.class, resultOid, page, task, task.getResult());
        if (object == null) {
            throw new RestartResponseException(PageError404.class);
        }

        return object.asObjectable();
    }
}
