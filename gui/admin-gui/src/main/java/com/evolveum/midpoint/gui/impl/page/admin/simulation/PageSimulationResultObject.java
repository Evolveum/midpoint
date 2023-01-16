/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/object/${CONTAINER_ID}"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/tag/${TAG_OID}/object/${CONTAINER_ID}")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECT_URL,
                        label = "PageSimulationResultObject.auth.simulationProcessedObject.label",
                        description = "PageSimulationResultObject.auth.simulationProcessedObject.description")
        }
)
public class PageSimulationResultObject extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String PAGE_PARAMETER_RESULT_OID = "RESULT_OID";
    private static final String PAGE_PARAMETER_TAG_OID = "TAG_OID";

    private static final String ID_TABLE = "table";

    public PageSimulationResultObject() {
        this(new PageParameters());
    }

    public PageSimulationResultObject(PageParameters parameters) {
        super(parameters);

        initLayout();
    }

    private void initLayout() {

    }

    private String getPageParameterResultOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_RESULT_OID).toString();
    }

    private String getPageParameterTagOid() {
        PageParameters params = getPageParameters();
        return params.get(PAGE_PARAMETER_TAG_OID).toString();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> {
            String oid = getPageParameterResultOid();

            if (!Utils.isPrismObjectOidValid(oid)) {
                throw new RestartResponseException(PageError404.class);
            }

            String name = WebModelServiceUtils.resolveReferenceName(
                    new ObjectReferenceType().oid(oid).type(SimulationResultType.COMPLEX_TYPE), this);

            return getString("PageSimulationResultObjects.title", name);
        };
    }
}
