/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.page;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.SimulationResultsPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/results",
                        matchUrlForSecurity = "/admin/simulations/results")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULTS_URL,
                        label = "PageSimulationResults.auth.simulationResults.label",
                        description = "PageSimulationResults.auth.simulationResults.description")
        }
)
@CollectionInstance(identifier = "allSimulationResults", applicableForType = SimulationResultType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.simulations", singularLabel = "ObjectType.SimulationResultType", icon = GuiStyleConstants.CLASS_SIMULATION_RESULT))
public class PageSimulationResults extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    public PageSimulationResults() {
        initLayout();
    }

    private void initLayout() {
        if (!isNativeRepo()) {
            warn(getString("PageSimulationResults.nonNativeRepositoryWarning"));
        }

        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected @NotNull VisibleEnableBehaviour getBackVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return PageSimulationResults.super.createPageTitleModel();
            }
        };
        add(navigation);

        Form form = new MidpointForm(ID_FORM);
        form.add(new VisibleBehaviour(() -> isNativeRepo()));
        add(form);

        SimulationResultsPanel table = new SimulationResultsPanel(ID_TABLE, null);
        table.setOutputMarkupId(true);
        form.add(table);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(PageSimulationResults.super.createPageTitleModel(), this.getClass(), getPageParameters()));
    }
}
