/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.page;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultObjectsPanel;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/objects"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/mark/${MARK_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/mark/?*/objects")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECTS_URL,
                        label = "PageSimulationResultObjects.auth.simulationProcessedObjects.label",
                        description = "PageSimulationResultObjects.auth.simulationProcessedObjects.description")
        }
)
public class PageSimulationResultObjects extends PageAdmin implements SimulationPage {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PAGE_QUERY_PARAMETER = "state";

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_PANEL = "panel";

    private IModel<SimulationResultType> resultModel;

    public PageSimulationResultObjects() {
        this(new PageParameters());
    }

    public PageSimulationResultObjects(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
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
                return PageSimulationResultObjects.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObjects.this.onBackPerformed();
            }
        };
        add(navigation);

        SimulationResultObjectsPanel panel = new SimulationResultObjectsPanel(ID_PANEL, resultModel) {

            @Override
            protected ObjectProcessingStateType getStateQueryParameter() {
                return PageSimulationResultObjects.this.getStateQueryParameter();
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private @Nullable ObjectProcessingStateType getStateQueryParameter() {
        PageParameters params = getPageParameters();
        String state = params.get(PAGE_QUERY_PARAMETER).toString();
        if (StringUtils.isEmpty(state)) {
            return null;
        }

        try {
            return ObjectProcessingStateType.fromValue(state);
        } catch (Exception ex) {
            return null;
        }
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResultObjects.this);
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(PageSimulationResultObjects.super.createPageTitleModel(), this.getClass(), getPageParameters()));
    }

    @Contract(pure = true)
    private @NotNull IModel<String> createTitleModel() {
        return () -> getString("PageSimulationResultObjects.title");
    }

    private void onBackPerformed() {
        if (canRedirectBack()) {
            redirectBack();
            return;
        }

        clearBreadcrumbs();

        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, resultModel.getObject().getOid());

        navigateToNext(PageSimulationResult.class, params);
    }
}
