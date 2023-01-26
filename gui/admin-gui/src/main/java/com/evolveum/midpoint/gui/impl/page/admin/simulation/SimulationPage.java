/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface SimulationPage extends IRequestablePage {

    String PAGE_PARAMETER_RESULT_OID = "RESULT_OID";
    String PAGE_PARAMETER_TAG_OID = "TAG_OID";
    String PAGE_PARAMETER_CONTAINER_ID = "CONTAINER_ID";

    String DOT_CLASS = SimulationPage.class.getName() + ".";

    String OPERATION_LOAD_USER = DOT_CLASS + "loadResult";

    default String getPageParameterResultOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_RESULT_OID).toString();
    }

    default String getPageParameterTagOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_TAG_OID).toString();
    }

    default String getPageParameterContainerId() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_CONTAINER_ID).toString();
    }
}
