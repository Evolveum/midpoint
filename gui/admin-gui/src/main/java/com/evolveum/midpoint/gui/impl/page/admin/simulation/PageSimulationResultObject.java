/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/object/?*"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/tag/${TAG_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/tag/?*/object/?*")
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

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResultObject.class);

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_CHANGES = "changes";

    private IModel<SimulationResultType> resultModel;

    private IModel<SimulationResultProcessedObjectType> objectModel;

    private IModel<VisualizationDto> changesModel;

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

        objectModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultProcessedObjectType load() {
                Task task = getPageTask();

                Long id = null;
                try {
                    id = Long.parseLong(getPageParameterContainerId());
                } catch (Exception ignored) {
                }

                if (id == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                ObjectQuery query = getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                        .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                        .ownerId(resultModel.getObject().getOid())
                        .and()
                        .id(id)
                        .build();

                List<SimulationResultProcessedObjectType> result = WebModelServiceUtils.searchContainers(SimulationResultProcessedObjectType.class,
                        query, null, task.getResult(), PageSimulationResultObject.this);

                if (result.isEmpty()) {
                    throw new RestartResponseException(PageError404.class);
                }

                return result.get(0);
            }
        };

        changesModel = new LoadableDetachableModel<>() {

            @Override
            protected VisualizationDto load() {
                Visualization visualization;
                try {
                    ObjectDelta delta = DeltaConvertor.createObjectDelta(objectModel.getObject().getDelta());

                    Task task = getPageTask();
                    OperationResult result = task.getResult();

                    visualization = getModelInteractionService().visualizeDelta(delta, task, result);
                } catch (SchemaException | ExpressionEvaluationException e) {
                    throw new SystemException(e);        // TODO
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Creating dto for deltas:\n{}", DebugUtil.debugDump(visualization));
                }

                final WrapperVisualization wrapper =
                        new WrapperVisualization(Arrays.asList(visualization), "PagePreviewChanges.primaryChangesOne", 1);

                return new VisualizationDto(wrapper);
            }
        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return () ->
                        WebComponentUtil.getOrigStringFromPoly(objectModel.getObject().getName())
                                + "(" + WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject()) + ")";
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObject.this.onBackPerformed(target);
            }
        };
        add(navigation);

        VisualizationPanel panel = new VisualizationPanel(ID_CHANGES, changesModel);
        add(panel);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        redirectBack();
    }
}
