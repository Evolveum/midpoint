/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.CatalogItemDetailsPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.TemplateChoicePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskCreationPopup extends BasePanel<ResourceObjectTypeDefinitionType> implements Popupable {

    private static final String ID_TEMPLATE_CHOICE_PANEL = "templateChoicePanel";
    private static final String ID_SIMULATE = "simulate";
    private static final String ID_BUTTON_CREATE_NEW_TASK = "createNewTask";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

//    private IModel<Boolean> simulate;
    private IModel<SynchronizationTaskFlavor> flavorModel;

    private Fragment footer;

    public TaskCreationPopup(String id, IModel<ResourceObjectTypeDefinitionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
//        simulate = new LoadableModel<>() {
//            @Override
//            protected Boolean load() {
//                return false;
//            }
//        };
        flavorModel = new LoadableModel<>() {
            @Override
            protected SynchronizationTaskFlavor load() {
                return null;
            }
        };

        initFooter();

    }

    private void initLayout() {

        TemplateChoicePanel templateChoicePanel = new TemplateChoicePanel(ID_TEMPLATE_CHOICE_PANEL) {

            @Override
            protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
                CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
                return profile.getObjectCollectionViews()
                        .stream()
                        .filter(view -> isSynchronizationTaskCollection(view.getArchetypeOid()))
                        .collect(Collectors.toList());
            }

            @Override
            protected void onTemplateChosePerformed(CompiledObjectCollectionView view, AjaxRequestTarget target) {
                flavorModel.setObject(determineTaskFlavour(view.getArchetypeOid()));
            }

            @Override
            protected QName getType() {
                return TaskType.COMPLEX_TYPE;
            }
        };
        add(templateChoicePanel);

        IModel<DisplayType> displayModel = () -> {
            DisplayType displayType = new DisplayType();
            displayType.setLabel(WebComponentUtil.createPolyFromOrigString("Simulation task"));
            displayType.setTooltip(WebComponentUtil.createPolyFromOrigString("Create in simulation mode"));
            return displayType;
        };

        ToggleCheckBoxPanel simulationPanel = new ToggleCheckBoxPanel(ID_SIMULATE,
                Model.of(false), displayModel);

        simulationPanel.setOutputMarkupId(true);
        add(simulationPanel);

    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxIconButton createNewTask = new AjaxIconButton(ID_BUTTON_CREATE_NEW_TASK, () -> "fa fa-arrow", createStringResource("TaskCreationPopup.createNewTask")) {

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

    private boolean isSynchronizationTaskCollection(String archetypeOid) {
        if (archetypeOid == null) {
            return false;
        }
        return archetypeOid.equals(SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())
                || archetypeOid.equals(SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())
                || archetypeOid.equals(SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
    }

    private SynchronizationTaskFlavor determineTaskFlavour(String archetypeOid) {
        SystemObjectsType taskType = SystemObjectsType.fromValue(archetypeOid);
        return switch (taskType) {
            case ARCHETYPE_RECONCILIATION_TASK -> SynchronizationTaskFlavor.RECONCILIATION;
            case ARCHETYPE_LIVE_SYNC_TASK -> SynchronizationTaskFlavor.LIVE_SYNC;
            case ARCHETYPE_IMPORT_TASK -> SynchronizationTaskFlavor.IMPORT;
            default -> null;
        };
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
}
