/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
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

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResults.class);

    private static final String DOT_CLASS = PageSimulationResults.class.getName() + ".";

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    public PageSimulationResults() {
        initLayout();
    }

    private void initLayout() {
        Form form = new MidpointForm(ID_FORM);
        add(form);

        // todo add "delete whole result" action and delete "processed objects (leave result)" action
        MainObjectListPanel<SimulationResultType> table = new MainObjectListPanel<>(ID_TABLE, SimulationResultType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_SIMULATION_RESULTS;
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, SimulationResultType object) {
                PageParameters params = new PageParameters();
                params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

                navigateToNext(PageSimulationResult.class, params);
            }
        };
        table.setOutputMarkupId(true);
        form.add(table);
    }
}
