/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.mapping;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingVerticalPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

public class SmartMappingTilePanel<C extends PrismContainerValueWrapper<MappingType>>
        extends TemplateTilePanel<C, SmartMappingTileModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TILE_CONTENT = "tileContent";
    private static final String ID_GENERATING_TILE_FRAGMENT = "generatingTileFragment";
    private static final String ID_BASIC_TILE_FRAGMENT = "basicTileFragment";

    private static final String ID_CHECKBOX = "checkbox";
    private static final String ID_ACTIONS = "actions";

    private static final String ID_MAPPING_NAME = "mappingName";
    private static final String ID_MAPPING_ICON = "mappingIcon";
    private static final String ID_MAPPING_VIRTUAL_REF = "mappingVirtualRef";
    private static final String ID_MAPPING_EXPRESSION = "mappingExpression";
    private static final String ID_MAPPING_TARGET = "mappingTarget";
    private static final String ID_MAPPING_LIFECYCLE_STATE = "mappingLifecycleState";
    private static final String ID_GENERATION_PANEL = "generationPanel";

    LoadableModel<StatusInfo<?>> statusModel;

    public SmartMappingTilePanel(@NotNull String id, @NotNull IModel<SmartMappingTileModel<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.setOutputMarkupId(true);

        statusModel = new LoadableModel<>(true) {
            @Override
            protected StatusInfo<CorrelationSuggestionsType> load() {
                Task task = getPageBase().createSimpleTask("Load generation status");
                OperationResult result = task.getResult();
                return getModelObject().getStatusInfo(getPageBase(), task, result);
            }
        };

        StatusInfo<?> statusInfo = statusModel.getObject();
        if (statusInfo != null && statusInfo.getStatus() != null
                && (statusInfo.isExecuting() || statusInfo.getStatus() == OperationResultStatusType.FATAL_ERROR)) {
            add(createGeneratingFragment());
        } else {
            add(createBasicFragment());
        }

    }

    private @NotNull Fragment createBasicFragment() {
        Fragment fragment = new Fragment(ID_TILE_CONTENT, ID_BASIC_TILE_FRAGMENT, this);
        buildBasicPanel(fragment);
        return fragment;
    }

    private void buildBasicPanel(Fragment fragment) {
        initCheckBox(fragment);
        initActionButton(fragment);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .displayedInColumn(true)
                .isRemoveButtonVisible(false)
                .build();

        C value = getModelObject().getValue();

        WebMarkupContainer icon = new WebMarkupContainer(ID_MAPPING_ICON);
        icon.setOutputMarkupId(true);
        icon.add(new VisibleBehaviour(this::isSuggestion));
        icon.add(AttributeModifier.append("class", "fa-solid fa-wand-magic-sparkles text-purple"));
        fragment.add(icon);

        fragment.add(createPropertyPanel(ID_MAPPING_NAME, settings, value, MappingType.F_NAME));
        fragment.add(createPropertyPanel(ID_MAPPING_VIRTUAL_REF, settings, value, ResourceAttributeDefinitionType.F_REF));
        fragment.add(createPropertyPanel(ID_MAPPING_EXPRESSION, settings, value, MappingType.F_EXPRESSION));
        fragment.add(createPropertyPanel(ID_MAPPING_TARGET, settings, value, MappingType.F_TARGET));
        fragment.add(createPropertyPanel(ID_MAPPING_LIFECYCLE_STATE, settings, value, MappingType.F_LIFECYCLE_STATE)
                .add(new VisibleBehaviour(() -> !isSuggestion())));
    }

    private @NotNull Fragment createGeneratingFragment() {
        Fragment fragment = new Fragment(ID_TILE_CONTENT, ID_GENERATING_TILE_FRAGMENT, this);
        Component generatingPanelComponent = createGeneratingPanelComponent();
        fragment.add(generatingPanelComponent);
        return fragment;
    }

    protected Component createGeneratingPanelComponent() {
        return new SmartGeneratingVerticalPanel(ID_GENERATION_PANEL, () -> {
            Task task = getPageBase().createSimpleTask("Load generation statusInfo");
            OperationResult result = task.getResult();

            String token = statusModel.getObject().getToken();
            PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(
                    TaskType.class, token, getPageBase(), task, result);
            return new SmartGeneratingDto(statusModel, () -> taskTypePrismObject);
        }, false) {

            @Override
            protected void createButtons(@NotNull RepeatingView buttonsView) {
                initActionButton(buttonsView);
                initDiscardButton(buttonsView);
            }

            @Override
            protected IModel<String> getSubTitleModel() {
                return createStringResource(
                        "SmartGeneratingSuggestionStep.generating.mapping.suggestion.action.subText");
            }

            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                onFinishGeneration(target);
            }
        };
    }

    protected void onFinishGeneration(AjaxRequestTarget target) {
        // Override to implement finish generation behavior
    }

    protected AjaxIconButton initEditButton(String id) {
        AjaxIconButton editButton = new AjaxIconButton(id, Model.of(GuiStyleConstants.CLASS_EDIT_MENU_ITEM),
                createStringResource("Edit.configuration.button.title")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onEditButton(target);
            }
        };
        editButton.setOutputMarkupId(true);
        editButton.showTitleAsLabel(true);
        editButton.add(AttributeModifier.append("class", "flex-row-reverse"
                + " gap-2 btn btn-default btn-sm"));
        return editButton;
    }

    private @NotNull PrismPropertyPanel<Object> createPropertyPanel(
            @NotNull String id,
            @NotNull ItemPanelSettings settings,
            @NotNull C value,
            @NotNull ItemName field) {

        boolean isExpressionPanel = field.equivalent(MappingType.F_EXPRESSION);
        boolean isRefPanel = field.equivalent(ResourceAttributeDefinitionType.F_REF);
        boolean isNamePanel = field.equivalent(MappingType.F_NAME);
        return new PrismPropertyPanel<>(
                id,
                PrismPropertyWrapperModel.fromContainerValueWrapper(() -> value, field),
                settings) {

            @Override
            protected boolean isHeaderVisible() {
                if (isNamePanel) {
                    return !getModel().getObject().isReadOnly();
                }
                return true;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected ItemHeaderPanel createHeaderPanel() {
                PrismPropertyHeaderPanel<Object> components = new PrismPropertyHeaderPanel<>(ID_HEADER, getModel()) {

                    @Override
                    protected boolean isRequired() {
                        ItemMandatoryHandler handler = (getSettings() != null) ? getSettings().getMandatoryHandler() : null;
                        if (handler != null) {
                            return handler.isMandatory(getModelObject());
                        }

                        return super.isRequired();
                    }

                    @Override
                    public IModel<String> createLabelModel() {
                        if (isRefPanel) {
                            return createStringResource(
                                    "SmartMappingTilePanel.mapping.property.ref");
                        }
                        return super.createLabelModel();
                    }
                };

                components.setOutputMarkupId(true);
                components.add(AttributeModifier.remove("class"));
                return components;
            }

            @Override
            protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<Object>> item) {
                if (isExpressionPanel) {
                    if (value.getRealValue().getExpression() == null) {
                        value.getRealValue().setExpression(new ExpressionType());
                    }

                    ExpressionPanel expressionPanel = new ExpressionPanel(
                            ID_VALUE, () -> value.getRealValue().getExpression()) {
                        @Override
                        protected boolean isReadOnly() {
                            return true;
                        }
                    };
                    expressionPanel.setOutputMarkupId(true);
                    expressionPanel.setRenderBodyOnly(false);
                    item.add(expressionPanel);
                    return expressionPanel;
                }
                return super.createValuePanel(item);
            }

            @Override
            protected String getCssClassForValueContainer() {
                if (isExpressionPanel) {
                    return "d-flex align-items-center gap-2";
                }
                return "pl-2";
            }
        };
    }

    private void initCheckBox(@NotNull Fragment fragment) {
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

        AjaxCheckBox checkBox = new AjaxCheckBox(ID_CHECKBOX, selectedModel) {
            @Override
            protected void onUpdate(@NotNull AjaxRequestTarget target) {
                Component component = SmartMappingTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, SmartMappingTilePanel.this));

                component = SmartMappingTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, SmartMappingTilePanel.this));
            }
        };
        checkBox.setOutputMarkupId(true);
        fragment.add(checkBox);
    }

    private @NotNull RepeatingView initActionSuggestionButton(@NotNull String id) {
        RepeatingView buttonsView = new RepeatingView(id);
        initSuggestionActionButton(buttonsView);
        return buttonsView;
    }

    protected void initSuggestionActionButton(@NotNull RepeatingView buttonsView) {
        // Override to implement action button behavior
    }

    private void initActionButton(@NotNull Fragment fragment) {
        RepeatingView buttonContainer = new RepeatingView(ID_ACTIONS);

        if (isSuggestion()) {
            buttonContainer.add(initActionSuggestionButton(buttonContainer.newChildId()));
        } else {
            buttonContainer.add(initEditButton(buttonContainer.newChildId()));
        }

        DropdownButtonPanel buttonPanel = new DropdownButtonPanel(buttonContainer.newChildId(), new DropdownButtonDto(
                null, "fa-ellipsis-h ml-1", null, buildMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " px-1 py-0 ";
            }
        };
        buttonPanel.setOutputMarkupId(true);
        buttonPanel.add(AttributeModifier.replace(TITLE_CSS, createStringResource("RoleAnalysis.menu.moreOptions")));
        buttonPanel.add(new TooltipBehavior());
        buttonPanel.add(new VisibleBehaviour(() -> !isSuggestion()));
        buttonContainer.add(buttonPanel);
        fragment.add(buttonContainer);
    }

    private boolean isSuggestion() {
        return statusModel.getObject() != null;
    }

    protected List<InlineMenuItem> buildMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        List<InlineMenuItem> menuItems = createMenuItems();
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getVisibilityChecker() == null
                    || menuItem.getVisibilityChecker().isVisible(() -> getModelObject().getValue(), false)) {
                items.add(menuItem);
            }
        }
        return items;
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // Implement the logic to delete the correlation
                    }
                };
            }

        });
        return items;
    }

    @Override
    protected void initLayout() {
        // nothing to add
    }

    protected void onEditButton(AjaxRequestTarget target) {
        // Implement the logic to edit the correlation
    }
}

