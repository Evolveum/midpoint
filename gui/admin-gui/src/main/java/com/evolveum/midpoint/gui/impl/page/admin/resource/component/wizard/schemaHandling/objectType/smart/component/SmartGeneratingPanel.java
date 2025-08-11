/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class SmartGeneratingPanel extends BasePanel<SmartGeneratingDto> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_ELAPSED_TIME = "elapsedTime";

    private static final String ID_LIST_VIEW_CONTAINER = "listViewContainer";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_LIST_ITEM_ICON = "icon";
    private static final String ID_LIST_ITEM_TEXT = "text";

    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_BUTTONS = "buttons";

    public SmartGeneratingPanel(String id, IModel<SmartGeneratingDto> model) {
        super(id, model);
        add(AttributeModifier.append("class", "p-0"));
        initLayout();
    }

    private void initLayout() {
        final WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label elapsedTime = new Label(ID_ELAPSED_TIME, () -> getModelObject().getTimeElapsed());
        elapsedTime.setOutputMarkupId(true);
        container.add(elapsedTime);

        WebMarkupContainer listViewContainer = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        listViewContainer.setOutputMarkupId(true);
        listViewContainer.add(new VisibleBehaviour(() -> {
            List<SmartGeneratingDto.StatusRow> rows = getSafeRows();
            return rows != null && !rows.isEmpty();
        }));
        container.add(listViewContainer);

        ListView<SmartGeneratingDto.StatusRow> listView = createStatusListView();
        listView.setReuseItems(false);
        listView.setOutputMarkupId(true);
        listViewContainer.add(listView);

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        container.add(buttonsContainer);

        RepeatingView buttonsView = new RepeatingView(ID_BUTTONS);
        buttonsView.setOutputMarkupId(true);

        createButtons(buttonsView);
        buttonsContainer.add(buttonsView);

        container.add(new AbstractAjaxTimerBehavior(getRefreshInterval()) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                final SmartGeneratingDto dto = getModelObject();
                final boolean finished = dto.getOperationResultStatus() == OperationResultStatus.SUCCESS
                        || dto.getOperationResultStatus() == OperationResultStatus.FATAL_ERROR;
                if (finished) {
                    stop(target);
                }
                target.add(container);
            }
        });
    }

    private @NotNull ListView<SmartGeneratingDto.StatusRow> createStatusListView() {
        return new ListView<>(SmartGeneratingPanel.ID_LIST_VIEW, () -> getModelObject().getStatusRows()) {
            @Override
            protected void populateItem(@NotNull ListItem<SmartGeneratingDto.StatusRow> item) {
                SmartGeneratingDto.StatusRow row = item.getModelObject();

                item.add(new Label(ID_LIST_ITEM_TEXT, row.text()));

                WebMarkupContainer icon = new WebMarkupContainer(ID_LIST_ITEM_ICON);
                icon.setOutputMarkupId(true);
                String iconCss = row.done()
                        ? "fa fa-check text-success"
                        : "spinner-border spinner-border-sm text-muted";
                icon.add(AttributeModifier.replace("class", iconCss));
                item.add(icon);

                item.add(AttributeModifier.append("class", "status-row"));
            }
        };
    }

    /** Null-safe accessor for rows. */
    private List<SmartGeneratingDto.StatusRow> getSafeRows() {
        SmartGeneratingDto dto = getModelObject();
        return dto != null ? dto.getStatusRows() : Collections.emptyList();
    }

    /** Polling interval; override if you want a different cadence. */
    protected Duration getRefreshInterval() {
        return Duration.ofSeconds(1);
    }

    /**
     * Override this method to create custom buttons.
     * The default implementation creates a close button that does nothing.
     */
    protected void createButtons(@NotNull RepeatingView buttonsView) {
        AjaxIconButton actionButton = new AjaxIconButton(
                buttonsView.newChildId(),
                () -> {
                    SmartGeneratingDto dto = getModelObject();
                    boolean finished = dto.isFinished();
                    return finished ? "fa fa-check text-success" : "fa fa-stop";
                },
                () -> {
                    SmartGeneratingDto dto = getModelObject();
                    return dto.isFinished()
                            ? getString("SmartGeneratingPanel.finished")
                            : getString("SmartGeneratingPanel.stop");
                }) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
//                TaskOperationUtils.suspendTasks()
                //TODO: implement the action for the button
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                SmartGeneratingDto dto = SmartGeneratingPanel.this.getModelObject();
                boolean finished = dto.isFinished();
                setEnabled(!finished);
            }
        };

        actionButton.setOutputMarkupId(true);
        actionButton.showTitleAsLabel(true);
        buttonsView.add(actionButton);
    }

}
