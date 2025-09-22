/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.time.Duration;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;

import com.evolveum.midpoint.util.exception.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class ResourceTestPanel extends BasePanel<String> {

    private static final String ID_PANEL_CONTAINER = "panelContainer";
    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_BODY_CONTAINER = "bodyContainer";
    private static final String ID_ELAPSED_TIME = "elapsedTime";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceTestPanel.class);

    private Long startMillis = System.currentTimeMillis();
    private State state = State.RUNNING;

    private enum State {
        RUNNING,
        SUCCESS,
        FAILED
    }

    public ResourceTestPanel(String id, IModel<String> resourceOidModel) {
        super(id, resourceOidModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        add(panelContainer);

        initIntroPart(panelContainer);

        WebMarkupContainer bodyContainer = new WebMarkupContainer(ID_BODY_CONTAINER);
        bodyContainer.setOutputMarkupId(true);
        panelContainer.add(bodyContainer);

        initCorePart(bodyContainer);

        bodyContainer.add(createSuggestionAjaxTimerBehavior());
    }

    private AbstractAjaxTimerBehavior createSuggestionAjaxTimerBehavior() {

        return new AbstractAjaxTimerBehavior(getRefreshInterval()) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                try {
                    boolean failed;
                    Task task = getPageBase().createSimpleTask("testResource");
                    OperationResult result = task.getResult();
                    try {
                        getPageBase().getModelService().testResource(getModelObject(), task, result);
                    } catch (CommonException e) {
                        state = State.FAILED;
                        stop(target);
                        return;
                    }

                    failed = result.isError();

                    if (!failed) {
                        try {
                            onFinishActionPerform(target);
                            state = State.SUCCESS;
                        } catch (Exception e) {
                            LOGGER.error("Error during finishing resource test", e);
                            state = State.FAILED;
                        } finally {
                            stop(target);
                        }
                    } else {
                        state = State.FAILED;
                        stop(target);
                    }

                } finally {
                    target.add(get(ID_PANEL_CONTAINER));
                }
            }
        };
    }

    private void initCorePart(@NotNull WebMarkupContainer bodyContainer) {
        Label elapsedTime = new Label(ID_ELAPSED_TIME, () -> SmartIntegrationUtils.formatElapsedTime(startMillis, null));
        elapsedTime.setOutputMarkupId(true);
        elapsedTime.add(new VisibleBehaviour(() -> state == State.RUNNING));
        bodyContainer.add(elapsedTime);
    }

    private void initIntroPart(@NotNull WebMarkupContainer panelContainer) {
        WebMarkupContainer titleIcon = new WebMarkupContainer(ID_TITLE_ICON);
        titleIcon.setOutputMarkupId(true);
        titleIcon.add(AttributeModifier.append("class", getIconCssModel()));
        panelContainer.add(titleIcon);

        Label title = new Label(ID_TEXT, getTitleModel());
        title.setOutputMarkupId(true);
        panelContainer.add(title);

        Label subTitle = new Label(ID_SUBTEXT, getSubTitleModel());
        subTitle.setOutputMarkupId(true);
        panelContainer.add(subTitle);
    }

    /** Polling interval; override if you want a different cadence. */
    protected Duration getRefreshInterval() {
        return Duration.ofSeconds(1);
    }

    protected void onFinishActionPerform(AjaxRequestTarget target) {
    }

    protected IModel<String> getIconCssModel() {
        return () -> {
            switch (state){
                case SUCCESS -> {
                    return "fa fa-check-circle text-success";
                }
                case FAILED -> {
                    return "fa fa-times-circle text-dangerous";
                }
                default -> {
                    return "fa fa-tower-broadcast text-primary";
                }
            }
        };
    }

    protected IModel<String> getTitleModel() {
        return createTextModel("PageConnectorDevelopment.wizard.step.resourceTest.text");
    }

    protected IModel<String> getSubTitleModel() {
        return createTextModel("PageConnectorDevelopment.wizard.step.resourceTest.subText");
    }

    private @NotNull IModel<String> createTextModel(String keyPrefix) {
        return () -> {
            String key = keyPrefix;
            switch (state){
                case SUCCESS -> key += ".success";
                case FAILED -> key += ".fails";
            }
            return createStringResource(key).getString();
        };
    }
}
