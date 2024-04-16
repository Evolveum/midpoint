/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractTemplateChoicePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;

import java.io.Serializable;

public abstract class TaskCreationPopup<T extends Serializable> extends BasePanel<T> implements Popupable {

    private static final String ID_TEMPLATE_CHOICE_PANEL = "templateChoicePanel";
    private static final String ID_SIMULATE = "simulate";
    private static final String ID_BUTTON_CREATE_NEW_TASK = "createNewTask";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private IModel<SynchronizationTaskFlavor> flavorModel = Model.of();

    private Fragment footer;

    public TaskCreationPopup(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        initFooter();
    }

    private void initLayout() {

        AbstractTemplateChoicePanel<T> templateChoicePanel = createChoicePanel(ID_TEMPLATE_CHOICE_PANEL);
        add(templateChoicePanel);

        ToggleCheckBoxPanel simulationPanel = new ToggleCheckBoxPanel(ID_SIMULATE,
                Model.of(getDefaultSimulationTag()),
                createStringResource("TaskCreationPopup.simulate.label"),
                createStringResource("TaskCreationPopup.simulate.tooltip"));
        simulationPanel.setOutputMarkupId(true);
        add(simulationPanel);

    }

    protected boolean getDefaultSimulationTag() {
        return false;
    }

    @SuppressWarnings("all")
    protected abstract AbstractTemplateChoicePanel<T> createChoicePanel(String id);

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxIconButton createNewTask = new AjaxIconButton(ID_BUTTON_CREATE_NEW_TASK,
                () -> "fa-solid fa-circle-plus",
                createStringResource("TaskCreationPopup.createNewTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCreateTask(target);
            }
        };
        createNewTask.showTitleAsLabel(true);
        footer.add(createNewTask);

        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });

    }

    private void onCreateTask(AjaxRequestTarget target) {
        ToggleCheckBoxPanel toggleCheckBoxPanel = (ToggleCheckBoxPanel) get(ID_SIMULATE);
        createNewTaskPerformed(flavorModel.getObject(), toggleCheckBoxPanel.getValue(), target);
    }

    protected void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean simulate, AjaxRequestTarget target) {

    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return TaskCreationPopup.this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected final IModel<SynchronizationTaskFlavor> getFlavorModel() {
        return flavorModel;
    }

    protected SynchronizationTaskFlavor determineTaskFlavour(String archetypeOid) {
        SystemObjectsType taskType = SystemObjectsType.fromValue(archetypeOid);
        return switch (taskType) {
            case ARCHETYPE_RECONCILIATION_TASK -> SynchronizationTaskFlavor.RECONCILIATION;
            case ARCHETYPE_LIVE_SYNC_TASK -> SynchronizationTaskFlavor.LIVE_SYNC;
            case ARCHETYPE_IMPORT_TASK -> SynchronizationTaskFlavor.IMPORT;
            case ARCHETYPE_SHADOW_RECLASSIFICATION_TASK -> SynchronizationTaskFlavor.SHADOW_RECLASSIFICATION;
            default -> null;
        };
    }
}
