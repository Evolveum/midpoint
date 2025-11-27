/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.page;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultObjectPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResultProcessedObjectModel;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/object/?*"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/mark/${MARK_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/mark/?*/object/?*")
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
public class PageSimulationResultObject extends PageAdmin implements SimulationPage {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_PANEL = "panel";

    private IModel<SimulationResultType> resultModel;
    private IModel<SimulationResultProcessedObjectType> objectModel;

    public PageSimulationResultObject() {
        this(new PageParameters());
    }

    public PageSimulationResultObject(PageParameters parameters) {
        super(parameters);
        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResultObject.this);
            }
        };

        objectModel = loadSimulationResultProcessedObjectModel(
                PageSimulationResultObject.this,
                getPageParameterResultOid(), getPageParameterContainerId());
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull IModel<String> createTitleModel() {
                return PageSimulationResultObject.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObject.this.onBackPerformed();
            }
        };
        add(navigation);

        String pageParameterMarkOid = getPageParameterMarkOid();
        SimulationResultObjectPanel panel = new SimulationResultObjectPanel(ID_PANEL, resultModel, objectModel) {
            @Override
            protected String getMarkOid() {
                return pageParameterMarkOid;
            }
        };
        add(panel);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private void onBackPerformed() {
        redirectBack();
    }

    @Contract(pure = true)
    private @NotNull IModel<String> createTitleModel() {
        return () -> {
            String name = WebComponentUtil.getOrigStringFromPoly(objectModel.getObject().getName());

            if (StringUtils.isEmpty(name)) {
                SimulationResultProcessedObjectType object = objectModel.getObject();
                ProcessedObject<?> processedObject = SimulationsGuiUtil.parseProcessedObject(object, PageSimulationResultObject.this);
                name = SimulationsGuiUtil.getShadowNameFromAttribute(processedObject);
            }

            return name + " (" + WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject()) + ")";
        };
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }
}
