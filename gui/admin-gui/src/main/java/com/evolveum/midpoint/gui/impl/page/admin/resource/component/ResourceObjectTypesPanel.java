/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithBadgePanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

@PanelType(name = "resourceObjectTypes")
@PanelInstance(identifier = "resourceObjectTypes", applicableForType = ResourceType.class,
        childOf = SchemaHandlingPanel.class,
        display = @PanelDisplay(label = "PageResource.tab.objectTypes", icon = GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, order = 10))
public class ResourceObjectTypesPanel extends SchemaHandlingObjectsPanel<ResourceObjectTypeDefinitionType> {

    private static final String OP_DETERMINE_STATUSES =
            ResourceObjectTypesPanel.class.getName() + ".determineStatuses";

    public ResourceObjectTypesPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ItemPath getTypesContainerPath() {
        return ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "ResourceSchemaHandlingPanel.newObject";
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> columns = new ArrayList<>();
        LoadableDetachableModel<PrismContainerDefinition<ResourceObjectTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceObjectTypeDefinitionType> load() {
                ComplexTypeDefinition resourceDef =
                        PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                return resourceDef.findContainerDefinition(
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            }
        };

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ItemPath.create(ResourceObjectTypeDefinitionType.F_DELINEATION, ResourceObjectTypeDelineationType.F_OBJECT_CLASS),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                StatusInfo<ObjectTypesSuggestionType> statusInfo = suggestionByWrapper.get(rowModel.getObject());
                if (statusInfo != null) {
                    QName objectClassName = statusInfo.getObjectClassName();
                    if (objectClassName != null) {
                        Label label = new Label(componentId, objectClassName.getLocalPart());
                        label.setOutputMarkupId(true);
                        cellItem.add(label);
                        return;
                    }
                }
                super.populateItem(cellItem, componentId, rowModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_KIND,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_INTENT,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_DEFAULT,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new LifecycleStateColumn<>(defModel, getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                OperationResultStatusType status = statusFor(rowModel.getObject());
                if (status == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                    return;
                }
                var style = SmartIntegrationUtils.SuggestionUiStyle.from(status);
                Label statusLabel = new Label(componentId, createStringResource(
                        "ResourceObjectTypesPanel.suggestion." + status.value()));
                statusLabel.setOutputMarkupId(true);
                statusLabel.add(AttributeModifier.append("class", style.badgeClass));
                cellItem.add(statusLabel);
            }
        });

        return columns;
    }

    @Override
    protected Class<ResourceObjectTypeDefinitionType> getSchemaHandlingObjectsType() {
        return ResourceObjectTypeDefinitionType.class;
    }

    @Override
    protected void onNewValue(
            PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target, boolean isDuplicate) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showObjectTypeWizard(value, target, newWrapperModel.getObject().getPath());
    }

    @Override
    protected void onSuggestValue(PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showSuggestObjectTypeWizard(target, createContainerModel().getObject().getPath());
    }

    @Override
    protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
        if (valueModel != null) {
            getObjectDetailsModels().getPageResource().showResourceObjectTypePreviewWizard(
                    target,
                    valueModel.getObject().getPath());
        }
    }

    @Override
    protected boolean allowNoValuePanel() {
        return true;
    }

    private final Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>,
            StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper = new HashMap<>();

    /** Creates value wrappers for each suggested object type. */
    protected List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> loadSuggestionWrappers() {
        final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        final OperationResult result = task.getResult();

        final ResourceType resource = getObjectDetailsModels().getObjectType();
        final List<StatusInfo<ObjectTypesSuggestionType>> suggestions = loadObjectTypeSuggestions(
                getPageBase(), resource.getOid(), task, result);
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        final List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> out = new ArrayList<>();
        suggestions.stream().filter(Objects::nonNull).forEach(si -> {
            ObjectTypesSuggestionType suggestion = si.getResult();
            if (si.getStatus() == OperationResultStatusType.NOT_APPLICABLE) {
                return;
            }

            if (si.getResult() == null) {
                ObjectTypesSuggestionType tmp = new ObjectTypesSuggestionType();
                tmp.getObjectType().add(new ResourceObjectTypeDefinitionType());
                suggestion = tmp;
            }

            try {
                @SuppressWarnings("unchecked")
                PrismContainer<ResourceObjectTypeDefinitionType> container =
                        suggestion.asPrismContainerValue().findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);

                PrismContainerWrapper<ResourceObjectTypeDefinitionType> wrapper = getPageBase().createItemWrapper(
                        container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                wrapper.getValues().forEach(value -> {
                    suggestionByWrapper.put(value, si);
                    out.add(value);
                });
            } catch (SchemaException e) {
                throw new IllegalStateException("Failed to wrap object type suggestions", e);
            }
        });

        return out;
    }

    protected <C extends Containerable> boolean isSuggestionRow(@Nullable IModel<PrismContainerValueWrapper<C>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return false;
        }

        return statusFor(rowModel.getObject()) != null;
    }

    @Override
    protected @Nullable String customInlineMenuItemCssClass(
            @Nullable IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
        return isSuggestionRow(rowModel)
                ? "position-relative btn btn-link btn-sm d-flex"
                : super.customInlineMenuItemCssClass(rowModel);
    }

    @Override
    protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
        for (InlineMenuItem menuItem : inlineMenuItems) {
            menuItem.setVisibilityChecker((InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper) {
                    return statusFor((PrismContainerValueWrapper<?>) rowModel.getObject()) == null;
                }
                return true;
            });
        }

        inlineMenuItems.add(createSuggestionStopInlineMenu());
        inlineMenuItems.add(createSuggestionDetailsInlineMenu());
        inlineMenuItems.add(createSuggestionReviewInlineMenu());
        inlineMenuItems.add(createDeleteSuggestionInlineMenu());
    }

    @Override
    protected Component onNameColumnPopulateItem(
            Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
            String componentId,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = rowModel.getObject();
        StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionByWrapper.get(object);

        ResourceObjectTypeDefinitionType realValue = object.getRealValue();

        if (suggestionTypeStatusInfo != null) {
            OperationResultStatusType status = suggestionTypeStatusInfo.getStatus();

            LoadableModel<String> displayNameModel = new LoadableModel<>() {
                @Override
                protected String load() {
                    if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
                        return createStringResource("ResourceObjectTypesPanel.suggestion.inProgress").getString();
                    }
                    return realValue != null ? realValue.getDisplayName() : " - ";
                }
            };

            LabelWithBadgePanel labelWithBadgePanel = new LabelWithBadgePanel(
                    componentId, getAiBadgeModel(), displayNameModel) {
                @Override
                protected boolean isIconVisible() {
                    return status.equals(OperationResultStatusType.IN_PROGRESS);
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getIconCss() {
                    return GuiStyleConstants.ICON_FA_SPINNER + " text-info";
                }

                @Contract(pure = true)
                @Override
                protected @Nullable String getLabelCss() {
                    return status.equals(OperationResultStatusType.IN_PROGRESS)
                            ? " text-info"
                            : null;
                }

                @Override
                protected boolean isBadgeVisible() {
                    return status.equals(OperationResultStatusType.SUCCESS);
                }
            };
            labelWithBadgePanel.setOutputMarkupId(true);
            cellItem.add(labelWithBadgePanel);
            return labelWithBadgePanel;
        }

        return super.onNameColumnPopulateItem(cellItem, componentId, rowModel);
    }

    private @Nullable OperationResultStatusType statusFor(
            PrismContainerValueWrapper<?> wrapper) {
        StatusInfo<ObjectTypesSuggestionType> info = suggestionByWrapper.get(wrapper);
        return info != null ? info.getStatus() : null;
    }

    @Override
    protected void customizeNewRowItem(
            Item<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> item,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        OperationResultStatusType status = statusFor(model.getObject());
        if (status == null) {
            super.customizeNewRowItem(item, model);
            return;
        }
        item.add(AttributeModifier.append("class", SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createProvider() {
        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> containerModel =
                new LoadableModel<>() {
                    @Override
                    protected @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> load() {
                        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> out = new ArrayList<>();

                        if (Boolean.TRUE.equals(getSwitchSuggestionModel().getObject())) {
                            List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> suggestions = loadSuggestionWrappers();
                            if (suggestions != null) {
                                out.addAll(suggestions);
                            }
                        }

                        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resource = resourceDefWrapper
                                .getObject().getValues();
                        if (resource != null) {
                            out.addAll(resource);
                        }
                        return out;
                    }
                };

        return new MultivalueContainerListDataProvider<>(ResourceObjectTypesPanel.this, Model.of(), containerModel);
    }

    protected ButtonInlineMenuItem createSuggestionDetailsInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.details.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    @SuppressWarnings("SuspiciousMethodCalls")
                    StatusInfo<ObjectTypesSuggestionType> suggestionStatus = suggestionByWrapper.get(rowModel.getObject());
                    return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.FATAL_ERROR;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = suggestionByWrapper.get(valueWrapper);

                            HelpInfoPanel helpInfoPanel = new HelpInfoPanel(
                                    getPageBase().getMainPopupBodyId(),
                                    statusInfo::getLocalizedMessage) {
                                @Override
                                public StringResourceModel getTitle() {
                                    return createStringResource("ResourceObjectTypesPanel.suggestion.details.title");
                                }

                                @Override
                                protected @NotNull Label initLabel(IModel<String> messageModel) {
                                    Label label = super.initLabel(messageModel);
                                    label.add(AttributeModifier.append("class", "alert alert-danger"));
                                    return label;
                                }

                                @Override
                                public @NotNull Component getFooter() {
                                    Component footer = super.getFooter();
                                    footer.add(new VisibleBehaviour(() -> false));
                                    return footer;
                                }
                            };

                            target.add(getPageBase().getMainPopup());

                            getPageBase().showMainPopup(
                                    helpInfoPanel, target);
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createSuggestionReviewInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.review.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    @SuppressWarnings("SuspiciousMethodCalls")
                    StatusInfo<ObjectTypesSuggestionType> suggestionStatus = suggestionByWrapper.get(rowModel.getObject());
                    return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.SUCCESS;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            ResourceObjectTypeDefinitionType realValue = (ResourceObjectTypeDefinitionType) valueWrapper.getRealValue();
                            @SuppressWarnings("unchecked") PrismContainerValue<ResourceObjectTypeDefinitionType> newValue =
                                    (PrismContainerValue<ResourceObjectTypeDefinitionType>) realValue.asPrismContainerValue();
                            //why this not work? upper statement (newValue)

                            PrismContainerValue<ResourceObjectTypeDefinitionType> tmpNewValue =
                                    createNewResourceObjectTypePrismContainerValue(createContainerModel(), realValue);

                            onNewValue(
                                    tmpNewValue, createContainerModel(),
                                    target, false);
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createSuggestionStopInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.stop.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    @SuppressWarnings("SuspiciousMethodCalls")
                    StatusInfo<ObjectTypesSuggestionType> suggestionStatus = suggestionByWrapper.get(rowModel.getObject());
                    return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.IN_PROGRESS;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = suggestionByWrapper.get(valueWrapper);
                            SmartIntegrationUtils.suspendSuggestionTask(
                                    getPageBase(), statusInfo, task, result);

                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createDeleteSuggestionInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    @SuppressWarnings("SuspiciousMethodCalls")
                    StatusInfo<ObjectTypesSuggestionType> suggestionStatus = suggestionByWrapper.get(rowModel.getObject());
                    return suggestionStatus != null;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = suggestionByWrapper.get(valueWrapper);
                            //noinspection unchecked
                            SmartIntegrationUtils.removeObjectTypeSuggestion(
                                    getPageBase(),
                                    statusInfo,
                                    (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>) valueWrapper,
                                    task,
                                    result);
                            target.add(getPageBase().getFeedbackPanel());
                            refreshForm(target);
                        }
                    }
                };
            }
        };
    }

    @Override
    protected boolean hasNoValues() {
        return createProvider().size() == 0;
    }

    @Override
    protected IModel<Boolean> getSwitchSuggestionModel() {
        return super.getSwitchSuggestionModel();
    }

    @Override
    protected boolean isToggleSuggestionVisible() {
        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> suggestions = loadSuggestionWrappers();
        return suggestions != null && !suggestions.isEmpty();
    }
}
