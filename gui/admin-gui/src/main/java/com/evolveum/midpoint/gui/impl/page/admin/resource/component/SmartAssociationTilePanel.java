/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.io.Serial;
import java.util.Objects;

import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

public abstract class SmartAssociationTilePanel
        extends TilePanel<SmartAssociationTileModel, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TILE_CONTENT = "tileContent";
    private static final String ID_SELECT_CHECKBOX = "selectCheckbox";

    private static final String ID_NAME = "name";
    private static final String ID_TAG = "tag";

    private static final String ID_SUBJECT_NAME = "subjectName";
    private static final String ID_SUBJECT_OBJECT_CLASS = "subjectObjectClass";
    private static final String ID_OBJECT_NAME = "objectName";
    private static final String ID_OBJECT_OBJECT_CLASS = "objectObjectClass";

    private static final String ID_ARROW_TEXT_LINE_CONTAINER = "arrowTextLine";
    private static final String ID_ARROW_TEXT_LABEL = "arrowTextLineLabel";
    private static final String ID_ARROW_TEXT_VALUE = "arrowTextLineValue";

    private static final String ID_LIFECYCLE_STATE = "lifecycleState";
    private static final String ID_DETAILS_PANEL = "detailsPanel";

    private static final String ID_ACCEPT = "accept";
    private static final String ID_DISMISS = "dismiss";
    private static final String ID_EDIT = "edit";
    private static final String ID_DETAILS = "details";

    private LoadableModel<StatusInfo<?>> statusModel;
    private boolean isDetailedView = false;

    public SmartAssociationTilePanel(@NotNull String id, @NotNull IModel<SmartAssociationTileModel> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);

        customizeTileItemCss();
        initStatusModel();

        WebMarkupContainer tile = createTileContainer();
        add(tile);

        initCheckBox(tile);
        initLifecycleState(tile);
        initTag(tile);
        initBasicLabels(tile);
        initSubjectSection(tile);
        initObjectSection(tile);
        initSuggestionButtons(tile);
        initEditButton(tile);
        initDetailsButton(tile);

        initArrowText(tile);

        add(new WebMarkupContainer(ID_DETAILS_PANEL).add(new VisibleBehaviour(() -> isDetailedView)));
    }

    private void initArrowText(@NotNull WebMarkupContainer tile) {
        String associationObjectObjectClass = getModelObject().getAssociationObjectObjectClass();

        WebMarkupContainer arrowTextLine = new WebMarkupContainer(ID_ARROW_TEXT_LINE_CONTAINER);
        arrowTextLine.setOutputMarkupId(true);
        arrowTextLine.add(new VisibleBehaviour(() -> associationObjectObjectClass != null));
        tile.add(arrowTextLine);

        Label arrowTextLabel = new Label(ID_ARROW_TEXT_LABEL,
                createStringResource("SmartAssociationTilePanel.arrowTextLabel"));
        arrowTextLabel.setOutputMarkupId(true);
        arrowTextLine.add(arrowTextLabel);

        Label arrowTextValue = new Label(ID_ARROW_TEXT_VALUE,
                () -> associationObjectObjectClass != null ? associationObjectObjectClass : "");
        arrowTextValue.setOutputMarkupId(true);
        arrowTextLine.add(arrowTextValue);
    }

    private void initStatusModel() {
        statusModel = new LoadableModel<>(true) {
            @Override
            protected StatusInfo<?> load() {
                Task task = getPageBase().createSimpleTask("Load smart association status");
                OperationResult result = task.getResult();
                return getModelObject().getStatusInfo(getPageBase(), task, result);
            }
        };
    }

    private @NotNull WebMarkupContainer createTileContainer() {
        WebMarkupContainer tile = new WebMarkupContainer(ID_TILE_CONTENT);
        tile.setOutputMarkupId(true);
        return tile;
    }

    private void initCheckBox(@NotNull WebMarkupContainer container) {
        IModel<Boolean> selectedModel = new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                return getModelObject().getValue().isSelected();
            }

            @Override
            public void setObject(Boolean value) {
                getModelObject().getValue().setSelected(Boolean.TRUE.equals(value));
            }
        };

        AjaxCheckBox selectCheckbox = new AjaxCheckBox(ID_SELECT_CHECKBOX, selectedModel) {
            @Override
            protected void onUpdate(@NotNull AjaxRequestTarget target) {
                Component parent = SmartAssociationTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                Component componentToAdd = Objects.requireNonNullElse(parent, SmartAssociationTilePanel.this);
                target.add(componentToAdd);
            }
        };

        selectCheckbox.setOutputMarkupId(true);
        container.addOrReplace(selectCheckbox);
    }

    private void initLifecycleState(@NotNull WebMarkupContainer tile) {
        PrismPropertyPanel<?> lifecycleStatePanel = new PrismPropertyPanel<>(
                ID_LIFECYCLE_STATE,
                PrismPropertyWrapperModel.fromContainerValueWrapper(
                        () -> getModelObject().getValue(),
                        ShadowAssociationTypeDefinitionType.F_LIFECYCLE_STATE),
                null) {
            @Override
            protected void onInitialize() {
                super.onInitialize();

                Component valuesContainer = get("valuesContainer");
                if (valuesContainer != null) {
                    valuesContainer.add(AttributeModifier.replace("class", ""));
                }

                Component header = get("header");
                if (header != null) {
                    header.add(AttributeModifier.replace("class", ""));
                }
            }
        };

        lifecycleStatePanel.setOutputMarkupId(true);
        lifecycleStatePanel.add(new VisibleBehaviour(() -> !getModelObject().isSuggestion()));
        tile.add(lifecycleStatePanel);
    }

    private void initBasicLabels(@NotNull WebMarkupContainer tile) {
        tile.add(new Label(ID_NAME, () -> getModelObject().getName()));
    }

    private void initTag(@NotNull WebMarkupContainer tile) {
        IconWithLabel tag = new IconWithLabel(ID_TAG, () -> getModelObject().getTextTag()) {
            @Override
            protected String getIconCssClass() {
                return SmartAssociationTilePanel.this.getModelObject().getCssIconTag();
            }
        };

        tag.setOutputMarkupId(true);
        tag.add(AttributeModifier.replace("class", getModelObject().getCssTag()));
        tag.add(new VisibleBehaviour(() -> getModelObject().isSuggestion()));

        tile.add(tag);
    }

    private void initSubjectSection(@NotNull WebMarkupContainer tile) {
        tile.add(new Label(ID_SUBJECT_NAME, () -> getModelObject().getSubjectName()));

        tile.add(new Label(ID_SUBJECT_OBJECT_CLASS, () -> {
            var q = getModelObject().getSubjectType();
            return q != null ? q.getLocalPart() : "";
        }));
    }

    private void initObjectSection(@NotNull WebMarkupContainer tile) {
        tile.add(new Label(ID_OBJECT_NAME, () -> getModelObject().getObjectName()));

        tile.add(new Label(ID_OBJECT_OBJECT_CLASS, () -> {
            var q = getModelObject().getObjectType();
            return q != null ? q.getLocalPart() : "";
        }));
    }

    private void initSuggestionButtons(@NotNull WebMarkupContainer tile) {
        AjaxIconButton accept = new AjaxIconButton(ID_ACCEPT, () -> "fa fa-check",
                createStringResource("SmartAssociationTilePanel.accept")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performAcceptAction(target, SmartAssociationTilePanel.this.getModelObject().getValue());
            }
        };

        initSuggestionButtonCommon(accept);
        tile.add(accept);

        AjaxIconButton dismiss = new AjaxIconButton(ID_DISMISS, () -> "fa fa-times",
                createStringResource("SmartAssociationTilePanel.dismiss")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performDismissAction(target, SmartAssociationTilePanel.this.getModelObject().getValue());
            }
        };

        initSuggestionButtonCommon(dismiss);
        tile.add(dismiss);
    }

    private void initSuggestionButtonCommon(@NotNull AjaxIconButton button) {
        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        button.add(new VisibleBehaviour(() -> getModelObject().isSuggestion()));
    }

    private void initEditButton(@NotNull WebMarkupContainer tile) {
        AjaxIconButton edit = new AjaxIconButton(ID_EDIT, () -> "fa fa-edit",
                createStringResource("SmartAssociationTilePanel.edit")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performEditAction(target, SmartAssociationTilePanel.this.getModelObject().getValue());
            }
        };

        edit.setOutputMarkupId(true);
        edit.showTitleAsLabel(true);
        edit.add(new VisibleBehaviour(() -> !getModelObject().isSuggestion()));
        tile.add(edit);
    }

    private void initDetailsButton(@NotNull WebMarkupContainer tile) {
        AjaxIconButton details = new AjaxIconButton(
                ID_DETAILS,
                () -> isDetailedView ? "fa fa-chevron-down" : "fa fa-chevron-right",
                createStringResource("SmartAssociationTilePanel.details")) {

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                isDetailedView = !isDetailedView;
                //perform lazy loading of details panel
                if (isDetailedView) {
                    SmartAssociationTilePanel.this.get(ID_DETAILS_PANEL).replaceWith(
                            createDetailsPanel(ID_DETAILS_PANEL,
                                    SmartAssociationTilePanel.this.getModelObject().getValue(),
                                    () -> isDetailedView));
                }
                target.add(SmartAssociationTilePanel.this);
            }
        };

        details.setOutputMarkupId(true);
        details.showTitleAsLabel(true);
        tile.add(details);
    }

    public abstract @NotNull Component createDetailsPanel(String id, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value, IModel<Boolean> isDetailedViewModel);

    protected void customizeTileItemCss() {
        if (getModelObject().isSuggestion()) {
            add(AttributeModifier.append("class", "border border-system border-large-left"));
        }
    }

    protected abstract void performAcceptAction(@NotNull AjaxRequestTarget target, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value);

    protected abstract void performDismissAction(@NotNull AjaxRequestTarget target, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value);

    protected abstract void performEditAction(@NotNull AjaxRequestTarget target,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value);

    protected IModel<StatusInfo<?>> getStatusModel() {
        return statusModel;
    }
}
