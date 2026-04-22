/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AbstractMappingsTable.createChangeNameColumnAction;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResult;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel.getSimulationResultReference;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.bySuggestion;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.ChangeLifecycleSelectedMappingsPopup;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.MappingDataDto;

import com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.GroupedMappingDataProvider;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.PreviewMappingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.FocusStatisticsActions;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.ObjectTypeStatisticsActions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationActionFlow;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationParams;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Encapsulates user actions for {@link SmartMappingTable}.
 *
 * <p>Handles operations such as:
 * <ul>
 *   <li>accept/discard suggestions</li>
 *   <li>delete, duplicate, and edit mappings</li>
 *   <li>simulation, preview, and statistics</li>
 * </ul>
 *
 * <p>Delegates state changes and refresh logic back to the table.
 */
record SmartMappingActions<P extends Containerable>(SmartMappingTable<P> table) implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SmartMappingActions.class);
    private static final String CLASS_DOT = SmartMappingTable.class.getName() + ".";
    private static final String OP_DELETE_MAPPING = CLASS_DOT + "deleteMapping";

    @NotNull Component createLegend(String id) {
        Fragment fragment = new Fragment(id, "legendFragment", table);

        WebMarkupContainer legend = new WebMarkupContainer("legend");
        legend.setOutputMarkupPlaceholderTag(true);
        fragment.add(legend);

        legend.add(new Label("legendTitle",
                table.createStringResource("MappingSuggestionGroupColumnTilePanel.legendTitle")));

        legend.add(new IconWithLabel("aiLegend",
                table.createStringResource("MappingSuggestionGroupColumnTilePanel.legendAi")) {
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-circle text-purple mr-1";
            }
        });

        legend.add(new IconWithLabel("systemLegend",
                table.createStringResource("MappingSuggestionGroupColumnTilePanel.legendSystem")) {
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-circle text-primary mr-1";
            }
        });

        fragment.add(new VisibleBehaviour(() ->
                table.isSuggestionSwitchSupported()
                        && Boolean.TRUE.equals(table.getSuggestionToggleModel().getObject())));

        return fragment;
    }

    @NotNull Component createMappingTypeDropdownButton(String idButton) {
        DropDownChoicePanel<MappingUsedFor> dropdown = WebComponentUtil.createEnumPanel(
                idButton,
                WebComponentUtil.createReadonlyModelFromEnum(MappingUsedFor.class),
                table.getMappingUsedForModel(),
                table,
                true,
                table.getString("InboundAttributeMappingsTable.allMappings"));

        dropdown.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px;"));
        dropdown.getBaseFormComponent().add(AttributeModifier.replace("class", "form-control"));
        dropdown.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                table.getMappingUsedForModel().setObject(dropdown.getModel().getObject());
                table.getTable().refreshAndDetach(target);
            }
        });

        dropdown.add(new VisibleBehaviour(() -> table.isInbound() && !table.getTable().displayNoValuePanel()));
        return dropdown;
    }

    String resolveAdditionalTileCss(PrismContainerValueWrapper<MappingType> rowValue) {
        StatusInfo<?> statusInfo = table.getStatusInfo(rowValue);
        if (statusInfo != null && statusInfo.getStatus() != null) {
            SuggestionUiStyle uiStyle =
                    SuggestionUiStyle.from(statusInfo, rowValue);
            return "border-large-left " + uiStyle.tileClass;
        }
        return "";
    }

    void performAcceptFromGroup(
            @NotNull PrismContainerValueWrapper<MappingType> selected,
            @NotNull AjaxRequestTarget target) {
        if (isForceDiscardMappingEnabled()) {
            acceptGroupedSuggestion(selected, target);
        } else {
            showAcceptConfirmation(selected, target);
        }
    }

    void acceptGroupedSuggestion(
            @NotNull PrismContainerValueWrapper<MappingType> selected,
            @NotNull AjaxRequestTarget target) {

        PrismContainerValueWrapper<MappingType> accepted =
                table.acceptSuggestionItemPerformed(() -> selected, target);
        table.getAcceptedSuggestionsCache().add(accepted);

        GroupedMappingDataProvider provider = table.getProvider();
        MappingDataDto groupedDto = provider != null ? provider.findGroupedDto(selected) : null;
        if (groupedDto != null) {
            groupedDto.getColumnsValues().forEach(table::deleteItemPerform);
        }

        table.refreshAndDetach(target);
    }

    private boolean isForceDiscardMappingEnabled() {
        return table.getPageBase()
                .getSessionStorage()
                .getSuggestionsStorage()
                .isForceDiscardMappingEnabled();
    }

    void deleteItemPerform(@NotNull PrismContainerValueWrapper<MappingType> value) {
        Task task = table.getPageBase().createSimpleTask(OP_DELETE_MAPPING);
        @Nullable StatusInfo<?> status = table.getStatusInfo(value);

        if (status != null) {
            PrismContainerValueWrapper<AttributeMappingsSuggestionType> parent =
                    value.getParentContainerValue(AttributeMappingsSuggestionType.class);
            if (parent == null || parent.getRealValue() == null) {
                return;
            }

            PrismValue oldValue = parent.getOldValue();
            removeMappingTypeSuggestionNew(
                    table.getPageBase(),
                    status,
                    oldValue.getRealValue(),
                    task,
                    task.getResult());
            return;
        }

        table.getAcceptedSuggestionsCache().remove(value);
        resolveDeletedItem(value);
    }

    void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        try {
            if (value.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper<ResourceAttributeDefinitionType> container =
                        table.getValueModel().getObject()
                                .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

                for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> valueR : container.getValues()) {
                    PrismContainerWrapper<MappingType> mappingContainer =
                            valueR.findContainer(table.getMappingDirectionType().getContainerName());
                    mappingContainer.getValues().removeIf(v -> v.equals(value));
                }
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
        } catch (SchemaException e) {
            table.getPageBase().error("Couldn't delete mapping: " + e.getMessage());
        } finally {
            value.setSelected(false);
        }
    }

    @Nullable ItemPathType getRefPath(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
        try {
            PrismPropertyWrapper<ItemPathType> refProperty =
                    mappingWrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);
            return refProperty != null && refProperty.getValue() != null
                    ? refProperty.getValue().getRealValue()
                    : null;
        } catch (SchemaException e) {
            table.getPageBase().error("Couldn't get ref attribute path: " + e.getMessage());
            return null;
        }
    }

    @NotNull InlineMenuItem.VisibilityChecker defaultMenuVisibilityChecker() {
        return bySuggestion(false, table::getStatusInfo);
    }

    @NotNull InlineMenuItem createAcceptSuggestionInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingTable.accept.suggestion"))
                .icon("fa fa-check")
                .additionalCssClass("text-success")
                .headerMenuItem(true)
                .visibilityChecker((rowModel, isHeader) ->
                        isHeader && Boolean.TRUE.equals(table.getSuggestionToggleModel().getObject()))
                .action(createAcceptSuggestionBulkAction())
                .buildInlineMenu();
    }

    @NotNull InlineMenuItem createDiscardSuggestionInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingTable.discard.suggestion"))
                .icon("fa fa-times")
                .additionalCssClass("text-danger")
                .headerMenuItem(true)
                .visibilityChecker((rowModel, isHeader) ->
                        isHeader && Boolean.TRUE.equals(table.getSuggestionToggleModel().getObject()))
                .action(createDiscardSuggestionBulkAction())
                .buildInlineMenu();
    }

    @NotNull InlineMenuItem createChangeLifecycleButtonInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-sync")
                .label(table.createStringResource("AttributeMappingsTable.button.changeLifecycle"))
                .action(new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<PrismContainerValueWrapper<MappingType>> selectedMappings =
                                getSelectedMappings();

                        if (selectedMappings.isEmpty()) {
                            table.getPageBase().warn(table.createStringResource(
                                    "MultivalueContainerListPanel.message.noItemsSelected").getString());
                            target.add(table.getPageBase().getFeedbackPanel());
                            return;
                        }

                        ChangeLifecycleSelectedMappingsPopup popup =
                                new ChangeLifecycleSelectedMappingsPopup(
                                        table.getPageBase().getMainPopupBodyId(),
                                        Model.ofList(selectedMappings)) {
                                    @Override
                                    protected void applyChanges(AjaxRequestTarget target) {
                                        super.applyChanges(target);
                                        table.refreshAndDetach(target);
                                    }
                                };

                        table.getPageBase().showMainPopup(popup, target);
                    }
                })
                .visibilityChecker((rowModel, isHeader) -> isHeader)
                .buildInlineMenu();
    }

    @NotNull InlineMenuItem createChangeMappingNameInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-sync")
                .label(table.createStringResource("AttributeMappingsTable.button.changeMappingName"))
                .action(createChangeNameColumnAction(table.getPageBase(), table::refreshAndDetach))
                .headerMenuItem(false)
                .visibilityChecker(changeNameVisibilityChecker())
                .buildInlineMenu();
    }

    private InlineMenuItem.VisibilityChecker changeNameVisibilityChecker() {
        return (rowModel, isHeader) -> {
            InlineMenuItem.VisibilityChecker base = bySuggestion(false, table::getStatusInfo);
            if (!base.isVisible(rowModel, isHeader) || rowModel == null || rowModel.getObject() == null) {
                return false;
            }

            try {
                @SuppressWarnings("unchecked")
                PrismPropertyWrapper<String> property =
                        ((PrismContainerValueWrapper<MappingType>) rowModel.getObject()).findProperty(MappingType.F_NAME);
                return property != null && property.isReadOnly();
            } catch (SchemaException e) {
                return false;
            }
        };
    }

    @NotNull InlineMenuItem createDuplicateInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-copy")
                .label(table.createStringResource("DuplicationProcessHelper.menu.duplicate"))
                .action(DuplicationProcessHelper.createDuplicateColumnAction(
                        table.getPageBase(),
                        this::createDuplicateValuePerform))
                .headerMenuItem(false)
                .visibilityChecker(bySuggestion(false, table::getStatusInfo))
                .buildInlineMenu();
    }

    void createDuplicateValuePerform(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        table.createNewValue(value, target);
        table.refreshAndDetach(target);
    }

    @NotNull InlineMenuItem createResourceAttributeStatisticsMenu(ResourceObjectTypeDefinitionType objectTypeDefinitionType) {
        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingTable.objectTypeStatistics.resourceAttribute"))
                .icon("fa fa-bar-chart")
                .action(new ColumnMenuAction<>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        ItemPathType ref = null;
                        if (getRowModel() != null) {
                            PrismContainerValueWrapper<MappingType> valueWrapper =
                                    (PrismContainerValueWrapper<MappingType>) getRowModel().getObject();
                            ref = getRefPath(valueWrapper);
                        }

                        ResourceObjectTypeIdentification id = ResourceObjectTypeIdentification.of(objectTypeDefinitionType);
                        ObjectTypeStatisticsActions.handleClick(
                                target,
                                table.getPageBase(),
                                table.getPageBase().getSmartIntegrationService(),
                                table.getResourceOid(),
                                id,
                                ref,
                                false);
                    }
                })
                .headerMenuItem(true)
                .buildInlineMenu();
    }

    @NotNull InlineMenuItem createFocusAttributeStatisticsMenu(ResourceObjectTypeDefinitionType objectTypeDef) {
        boolean isOutbound = table.isOutbound();

        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingTable.objectTypeStatistics.focusAttribute.outbound." + isOutbound))
                .icon("fa fa-line-chart")
                .action(new ColumnMenuAction<>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        QName focusTypeName = objectTypeDef.getFocus().getType();
                        if (focusTypeName == null) {
                            table.getPageBase().warn("Focus type is not specified for the object type. Cannot show statistics.");
                            target.add(table.getPageBase().getFeedbackPanel());
                            return;
                        }

                        String resourceOid = table.getResourceOid();
                        ShadowKindType kind = objectTypeDef.getKind();
                        String intent = objectTypeDef.getIntent();
                        if (kind == null || intent == null) {
                            table.getPageBase().warn("Resource, kind, and intent must be specified for focus statistics.");
                            target.add(table.getPageBase().getFeedbackPanel());
                            return;
                        }

                        ItemPathType targetPath = extractTargetPath();

                        FocusStatisticsActions.handleClick(
                                target,
                                table.getPageBase(),
                                table.getPageBase().getSmartIntegrationService(),
                                focusTypeName,
                                resourceOid,
                                kind,
                                intent,
                                targetPath,
                                false);
                    }

                    private @Nullable ItemPathType extractTargetPath() {
                        ItemPathType targetPath = null;
                        if (getRowModel() != null) {
                            @SuppressWarnings("unchecked") PrismContainerValueWrapper<MappingType> valueWrapper =
                                    (PrismContainerValueWrapper<MappingType>) getRowModel().getObject();
                            MappingType mapping = valueWrapper.getRealValue();

                            if (table.isInbound()) {
                                if (mapping != null && mapping.getTarget() != null && mapping.getTarget().getPath() != null) {
                                    targetPath = mapping.getTarget().getPath();
                                }
                            } else {
                                if (mapping != null && mapping.getSource() != null && !mapping.getSource().isEmpty()
                                        && mapping.getSource().get(0) != null
                                        && mapping.getSource().get(0).getPath() != null) {
                                    targetPath = mapping.getSource().get(0).getPath();
                                }
                            }
                        }
                        return targetPath;
                    }
                })
                .headerMenuItem(true)
                .buildInlineMenu();
    }

    boolean isSuggestionInlineMenuVisible() {
        return Boolean.TRUE.equals(table.getSuggestionToggleModel().getObject());
    }

    @NotNull InlineMenuItem createAcceptItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(table.createStringResource("SmartMappingTable.apply"))
                .icon("fa fa-check mr-2")
                .action(createAcceptSuggestionColumnAction())
                .additionalCssClass("btn-link text-primary rounded border-primary mr-2")
                .buildButtonMenu();
    }

    @NotNull InlineMenuItem createDiscardItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(table.createStringResource("SmartMappingTable.dismiss"))
                .icon("fa fa-times mr-2")
                .action(createDiscardColumnAction())
                .additionalCssClass("btn-link text-danger")
                .buildButtonMenu();
    }

    private InlineMenuItemBuilder createSuggestActionMenuBuilder() {
        return InlineMenuItemBuilder.create()
                .labelVisible(true)
                .menuLinkVisible(false)
                .headerMenuItem(false)
                .visible(Model.of(isSuggestionInlineMenuVisible()))
                .visibilityChecker(bySuggestion(true, table::getStatusInfo));
    }

    @NotNull ColumnMenuAction<Serializable> createDiscardColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }

                if (getRowModel().getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                    //noinspection unchecked
                    deleteItemPerform((PrismContainerValueWrapper<MappingType>) valueWrapper);
                }

                table.refreshAndDetach(target);
            }
        };
    }

    @NotNull ColumnMenuAction<Serializable> createAcceptSuggestionColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }

                if (getRowModel().getObject() instanceof MappingDataDto dto) {
                    for (PrismContainerValueWrapper<MappingType> value : dto.getMappings()) {
                        if (table.getStatusInfo(value) != null) {
                            PrismContainerValueWrapper<MappingType> newValue =
                                    table.acceptSuggestionItemPerformed(() -> value, target);
                            table.getAcceptedSuggestionsCache().add(newValue);
                        }
                    }
                } else if (getRowModel().getObject() instanceof PrismContainerValueWrapper<?> raw) {
                    @SuppressWarnings("unchecked")
                    PrismContainerValueWrapper<MappingType> value = (PrismContainerValueWrapper<MappingType>) raw;
                    if (table.getStatusInfo(value) != null) {
                        PrismContainerValueWrapper<MappingType> newValue =
                                table.acceptSuggestionItemPerformed(() -> value, target);
                        table.getAcceptedSuggestionsCache().add(newValue);
                    }
                }

                table.refreshAndDetach(target);
            }
        };
    }

    @NotNull ColumnMenuAction<Serializable> createDiscardSuggestionBulkAction() {
        return createBulkSuggestionAction(false);
    }

    @NotNull ColumnMenuAction<Serializable> createAcceptSuggestionBulkAction() {
        return createBulkSuggestionAction(true);
    }

    private @NotNull ColumnMenuAction<Serializable> createBulkSuggestionAction(boolean accept) {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<PrismContainerValueWrapper<MappingType>> selectedSuggestions = getAllSelectedItemsWithStatus();
                List<PrismContainerValueWrapper<MappingType>> allSuggestions = getAllItemsWithStatus();

                int selectedCount = selectedSuggestions.size();
                int allCount = allSuggestions.size();

                StringResourceModel title = accept
                        ? acceptSuggestionTitle(selectedCount, allCount)
                        : discardConfirmationTitle(selectedCount, allCount);

                ConfirmationPanel dialog = new ConfirmationPanel(
                        table.getPageBase().getMainPopupBodyId(),
                        title) {

                    @Override
                    protected IModel<String> createNoLabel() {
                        return selectedCount == 0
                                ? table.createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.cancel")
                                : super.createNoLabel();
                    }

                    @Override
                    protected boolean isYesButtonVisible() {
                        return selectedCount > 0 || allCount > 0;
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        List<PrismContainerValueWrapper<MappingType>> effective =
                                selectedSuggestions.isEmpty() ? allSuggestions : selectedSuggestions;

                        if (accept) {
                            effective.forEach(v -> table.acceptSuggestionItemPerformed(() -> v, target));
                        } else {
                            effective.forEach(table::deleteItemPerform);
                        }

                        table.getPageBase().hideMainPopup(target);
                        table.refreshAndDetach(target);
                    }
                };

                table.getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getSelectedMappings() {
        List<PrismContainerValueWrapper<MappingType>> selectedValues = new ArrayList<>();
        List<MappingDataDto> selectedItems = table.getTable().getSelectedContainerItems();

        for (MappingDataDto item : selectedItems) {
            PrismContainerValueWrapper<MappingType> primary = item.getPrimaryMapping();
            if (primary != null) {
                selectedValues.add(primary);
            }
        }
        return selectedValues;
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getAllSelectedItemsWithStatus() {
        List<PrismContainerValueWrapper<MappingType>> result = new ArrayList<>();
        for (MappingDataDto item : table.getTable().getSelectedContainerItems()) {
            for (PrismContainerValueWrapper<MappingType> mapping : item.getMappings()) {
                if (table.getStatusInfo(mapping) != null) {
                    result.add(mapping);
                }
            }
        }
        return result;
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getAllItemsWithStatus() {
        List<PrismContainerValueWrapper<MappingType>> result = new ArrayList<>();
        for (MappingDataDto item : table.getTable().getAllItems()) {
            for (PrismContainerValueWrapper<MappingType> mapping : item.getMappings()) {
                if (table.getStatusInfo(mapping) != null) {
                    result.add(mapping);
                }
            }
        }
        return result;
    }

    private StringResourceModel discardConfirmationTitle(int selectedCount, int allCount) {
        if (selectedCount == 0 && allCount == 0) {
            return table.createStringResource("ColumnTileTable.discard.title.noItems");
        }
        return selectedCount == 0
                ? table.createStringResource("ColumnTileTable.discard.title.empty", allCount)
                : table.createStringResource("ColumnTileTable.discard.title", selectedCount);
    }

    private StringResourceModel acceptSuggestionTitle(int selectedCount, int allCount) {
        if (selectedCount == 0 && allCount == 0) {
            return table.createStringResource("ColumnTileTable.accept.title.noItems");
        }
        return selectedCount == 0
                ? table.createStringResource("ColumnTileTable.accept.title.empty", allCount)
                : table.createStringResource("ColumnTileTable.accept.title", selectedCount);
    }

    void showAcceptConfirmation(
            @NotNull PrismContainerValueWrapper<MappingType> selected,
            @NotNull AjaxRequestTarget target) {

        IModel<Boolean> rememberChoiceModel = Model.of(false);

        List<ConfirmationOption<Describable>> options = List.of(
                new ConfirmationOption<>(rememberChoiceModel, new RememberDiscardSelection(), null)
        );

        GroupedMappingDataProvider provider = table.getProvider();
        MappingDataDto groupedDto = provider != null ? provider.findGroupedDto(selected) : null;
        if (groupedDto == null) {
            return;
        }

        String targetName = String.valueOf(groupedDto.getKeyValue());

        ConfirmationWithOptionsDto<Describable> dto = ConfirmationWithOptionsDto.builder()
                .confirmationTitle(table.createStringResource("MappingSuggestionGroupColumnTilePanel.accept.confirmation.title"))
                .confirmationSubtitle(table.createStringResource(
                        "MappingSuggestionGroupColumnTilePanel.accept.confirmation.message",
                        targetName))
                .confirmationOptionsTitle(null)
                .confirmationInfoMessage(null)
                .confirmationButtonLabel(table.createStringResource("MappingSuggestionGroupColumnTilePanel.acceptSelected"))
                .titleIconCssClass("text-danger")
                .externalLinkUrl(null)
                .confirmationOptions(options)
                .build();

        ConfirmationWithOptionsPanel<Describable> dialog =
                new ConfirmationWithOptionsPanel<>(table.getPageBase().getMainPopupBodyId(), () -> dto) {
                    @Override
                    public void confirmationPerformed(
                            AjaxRequestTarget target,
                            IModel<List<ConfirmationOption<Describable>>> confirmedOptions) {

                        if (Boolean.TRUE.equals(rememberChoiceModel.getObject())) {
                            table.getPageBase()
                                    .getSessionStorage()
                                    .getSuggestionsStorage()
                                    .setForceDiscardMappingEnabled(true);
                        }

                        PrismContainerValueWrapper<MappingType> accepted =
                                table.acceptSuggestionItemPerformed(() -> selected, target);
                        table.getAcceptedSuggestionsCache().add(accepted);
                        groupedDto.getColumnsValues().forEach(table::deleteItemPerform);
                        table.refreshAndDetach(target);
                    }
                };

        table.getPageBase().replaceMainPopup(dialog, target);
    }

    @NotNull InlineMenuItem createSimulationInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingPanel.simulate"))
                .icon("fa fa-flask")
                .headerMenuItem(false)
                .visibilityChecker((rowModel, isHeader) -> !isHeader && table.isInbound())
                .action(new ColumnMenuAction<PrismContainerValueWrapper<MappingType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }

                        InlineMappingDefinitionType mappingToSimulate = new InlineMappingDefinitionType();
                        ItemPathType refPath = getRefPath(getRowModel().getObject());
                        if (refPath == null) {
                            return;
                        }
                        mappingToSimulate.setRef(refPath);

                        MappingType mappingRealValue = getRowModel().getObject().getRealValue();
                        //noinspection unchecked
                        WebPrismUtil.cleanupEmptyContainerValue(
                                mappingRealValue.asPrismContainerValue());

                        if (mappingRealValue instanceof InboundMappingType inbound) {
                            mappingToSimulate.getInbound().add(inbound.clone());
                        } else if (mappingRealValue instanceof OutboundMappingType outbound) {
                            mappingToSimulate.getOutbound().add(outbound.clone());
                        }

                        SimulationParams<?> params = new SimulationParams<>(
                                table.getPageBase(),
                                table.getResourceType(),
                                table.findResourceObjectTypeDefinition().getRealValue(),
                                ResourceTaskFlavors.MAPPING_PREVIEW_ACTIVITY,
                                mappingToSimulate.clone(),
                                ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW
                        );

                        SimulationActionFlow<?> flow = getSimulationActionFlow(params);
                        flow.start(target);
                    }
                })
                .buildInlineMenu();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @NotNull SimulationActionFlow<?> getSimulationActionFlow(SimulationParams<?> params) {
        SimulationActionFlow<?> flow = new SimulationActionFlow(params) {
            @Override
            public void onShowResultProcess(
                    AjaxRequestTarget target,
                    TaskType task,
                    PageBase pageBase) {

                ObjectReferenceType simulationResultReference = getSimulationResultReference(task);
                if (simulationResultReference == null || simulationResultReference.getOid() == null) {
                    LOGGER.error("Simulation result reference or OID is null for task {}", task.getName());
                    return;
                }

                SimulationResultType result =
                        loadSimulationResult(pageBase, simulationResultReference.getOid());
                table.buildSimulationResultPanel(target, Model.of(result));
            }
        };
        flow.enableSampling();
        flow.showProgressPopup();
        return flow;
    }

    @NotNull InlineMenuItem createPreviewInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(table.createStringResource("SmartMappingPanel.preview.mapping"))
                .icon("fa fa-search")
                .headerMenuItem(false)
                .action(new ColumnMenuAction<PrismContainerValueWrapper<MappingType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }

                        PreviewMappingPanel popup = buildPreviewMappingPanelPopup(getRowModel());
                        table.getPageBase().showMainPopup(popup, target);
                    }
                })
                .visibilityChecker(bySuggestion(true, table::getStatusInfo))
                .buildInlineMenu();
    }

    @NotNull PreviewMappingPanel buildPreviewMappingPanelPopup(
            IModel<PrismContainerValueWrapper<MappingType>> mappingWrapper) {

        return new PreviewMappingPanel(
                table.getPageBase().getMainPopupBodyId(),
                mappingWrapper,
                table.isInbound()) {

            @Override
            public void customizeFooterButtons(@NotNull RepeatingView repeater) {
                super.customizeFooterButtons(repeater);

                StatusInfo<?> statusInfo = table.getStatusInfo(mappingWrapper.getObject());
                if (statusInfo != null) {
                    AjaxIconButton discard = buildDiscardButton(repeater, mappingWrapper);
                    discard.add(AttributeModifier.replace("class", "btn-link text-danger ml-auto"));
                    repeater.add(discard);

                    AjaxIconButton accept = buildAcceptInGroupButton(repeater, mappingWrapper);
                    accept.add(AttributeModifier.replace("class", "btn btn-primary"));
                    repeater.add(accept);
                }
            }
        };
    }

    private @NotNull AjaxIconButton buildAcceptInGroupButton(
            @NotNull RepeatingView repeater,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {

        AjaxIconButton button = new AjaxIconButton(
                repeater.newChildId(),
                Model.of("fa fa-check mr-2"),
                table.createStringResource("SmartMappingTable.apply.suggestion")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                GroupedMappingDataProvider provider = table.getProvider();
                MappingDataDto groupedDto =
                        provider != null ? provider.findGroupedDto(rowModel.getObject()) : null;

                if (groupedDto != null) {
                    if (isForceDiscardMappingEnabled()) {
                        PrismContainerValueWrapper<MappingType> accepted =
                                table.acceptSuggestionItemPerformed(rowModel, target);
                        table.getAcceptedSuggestionsCache().add(accepted);
                        groupedDto.getColumnsValues().forEach(table::deleteItemPerform);
                        table.refreshAndDetach(target);
                        table.getPageBase().hideMainPopup(target);
                    } else {
                        showAcceptConfirmation(rowModel.getObject(), target);
                    }
                    return;
                }

                PrismContainerValueWrapper<MappingType> accepted =
                        table.acceptSuggestionItemPerformed(rowModel, target);
                table.getAcceptedSuggestionsCache().add(accepted);
                table.refreshAndDetach(target);
                table.getPageBase().hideMainPopup(target);
            }
        };

        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        return button;
    }

    private @NotNull AjaxIconButton buildDiscardButton(
            @NotNull RepeatingView repeater,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {

        AjaxIconButton button = new AjaxIconButton(
                repeater.newChildId(),
                Model.of("fa fa-times mr-2"),
                table.createStringResource("SmartMappingTable.dismiss")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteItemPerform(rowModel.getObject());
                table.refreshAndDetach(target);
                table.getPageBase().hideMainPopup(target);
            }
        };

        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        return button;
    }

    @NotNull InlineMenuItem createDeleteItemMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-trash text-danger")
                .additionalCssClass("text-danger")
                .label(table.createStringResource("pageAdminFocus.button.delete"))
                .action(table.getTable().createDeleteColumnAction())
                .visibilityChecker(defaultMenuVisibilityChecker())
                .buildInlineMenu();
    }

    @NotNull InlineMenuItem createEditInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-edit")
                .label(table.createStringResource("PageBase.button.edit"))
                .action(createEditColumnAction())
                .headerMenuItem(false)
                .visibilityChecker(defaultMenuVisibilityChecker())
                .buildInlineMenu();
    }

    private @NotNull ColumnMenuAction<PrismContainerValueWrapper<MappingType>> createEditColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null || getRowModel().getObject() == null) {
                    return;
                }

                PrismContainerValueWrapper<MappingType> columnValue = getRowModel().getObject();
                MappingDataDto rowObject = findRowObject(columnValue);
                if (rowObject != null) {
                    table.getTable().editItemPerformed(target, Model.of(rowObject));
                }
            }
        };
    }

    private @Nullable MappingDataDto findRowObject(
            @NotNull PrismContainerValueWrapper<MappingType> columnValue) {
        for (MappingDataDto row : table.getTable().getCurrentPageItems()) {
            if (Objects.equals(row.getColumnValue(), columnValue)) {
                return row;
            }
        }
        return null;
    }

    @NotNull InlineMenuItem createSuggestionOperationInlineMenu() {
        return StatusInfoTableUtil.createSuggestionOperationInlineMenu(
                table.getPageBase(),
                table::getStatusInfo,
                table::refreshAndDetach);
    }

    @NotNull InlineMenuItem createSuggestionDetailsInlineMenu() {
        return StatusInfoTableUtil.createSuggestionDetailsInlineMenu(
                table.getPageBase(),
                table::getStatusInfo);
    }

    private class RememberDiscardSelection implements Describable {
        @Override
        public IModel<String> title() {
            return table.createStringResource(
                    "MappingSuggestionGroupColumnTilePanel.accept.confirmation.option.hide.title");
        }

        @Override
        public IModel<String> description() {
            return Model.of("");
        }
    }
}
