/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.page;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultPanel;

import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULT_URL,
                        label = "PageSimulationResults.auth.simulationResult.label",
                        description = "PageSimulationResults.auth.simulationResult.description")
        }
)
public class PageSimulationResult extends PageAdmin implements SimulationPage {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RESULT_PANEL = "resultPanel";
    private static final String ID_NAVIGATION = "navigation";
    private IModel<SimulationResultType> resultModel;

    public PageSimulationResult() {
        this(new PageParameters());
    }

    public PageSimulationResult(PageParameters parameters) {
        super(parameters);
        initModels();
        initLayout();
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Contract(pure = true)
            @Override
            protected @NotNull IModel<String> createTitleModel() {
                return PageSimulationResult.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResult.this.onBackPerformed();
            }

            @Override
            protected @NotNull AjaxLink<?> createNextButton(String id, IModel<String> nextTitle) {
                AjaxIconButton next = new AjaxIconButton(id, () -> "fa-solid fa-magnifying-glass mr-2",
                        () -> getString("PageSimulationResult.viewProcessedObjects")) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onViewAllPerformed();
                    }
                };
                next.showTitleAsLabel(true);
                next.add(AttributeAppender.append("class", "btn btn-primary"));

                return next;
            }
        };
        add(navigation);

        SimulationResultPanel resultPanel = new SimulationResultPanel(PageSimulationResult.ID_RESULT_PANEL, resultModel);
        resultPanel.setOutputMarkupId(true);
        add(resultPanel);
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {
            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResult.this);
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    @Contract(pure = true)
    private @NotNull IModel<String> createTitleModel() {
        return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
    }

    protected void onBackPerformed() {
        PageSimulationResult.this.redirectBack();
    }

    private void onViewAllPerformed() {
        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());

        navigateToNext(PageSimulationResultObjects.class, params);
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }
}
