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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
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
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
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
import java.time.Duration;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectTypeSuggestionWrappers;
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
                StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(rowModel.getObject());
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

        inlineMenuItems.add(createSuggestionOperationInlineMenu());
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
        StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = getStatusInfo(object);

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
                    return GuiStyleConstants.ICON_FA_SPINNER + " fa-spin  text-info";
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
        StatusInfo<ObjectTypesSuggestionType> info = getStatusInfo(wrapper);
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
        addAjaxTimeBehaviorIfRequested(model.getObject(), item);
    }

    private void addAjaxTimeBehaviorIfRequested(
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> value,
            Item<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> item) {
        StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(value);
        if (statusInfo != null && statusInfo.getStatus() != null) {
            item.add(AttributeModifier.append("class", SuggestionUiStyle.from(statusInfo).rowClass));

            boolean executing = statusInfo.isExecuting() && !statusInfo.isSuspended();
            if (executing) {
                AbstractAjaxTimerBehavior timer = new AbstractAjaxTimerBehavior(Duration.ofSeconds(3)) {
                    @Override
                    protected void onTimer(@NotNull AjaxRequestTarget target) {
                        target.add(item);
                        StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(value);
                        if (statusInfo == null || !statusInfo.isExecuting() || statusInfo.isSuspended()) {
                            stop(target);
                            refreshForm(target);
                            target.add(item);
                        }
                    }
                };
                item.add(timer);
            }
        }
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createProvider() {
        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        final Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionsIndex = new HashMap<>();

        LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> containerModel =
                new LoadableModel<>() {
                    @Override
                    protected @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> load() {
                        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> out = new ArrayList<>();

                        suggestionsIndex.clear();
                        if (Boolean.TRUE.equals(getSwitchSuggestionModel().getObject())) {
                            final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
                            final OperationResult result = task.getResult();

                            final String resourceOid = getObjectDetailsModels().getObjectType().getOid();

                            SmartIntegrationStatusInfoUtils.@NotNull ObjectTypeSuggestionProviderResult suggestions = loadObjectTypeSuggestionWrappers(
                                    getPageBase(), resourceOid, task, result);
                            out.addAll(suggestions.wrappers());
                            suggestionsIndex.putAll(suggestions.suggestionByWrapper());
                        }

                        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resource = resourceDefWrapper
                                .getObject().getValues();
                        if (resource != null) {
                            out.addAll(resource);
                        }
                        return out;
                    }
                };

        String resourceOid = getObjectWrapperObject().getOid();
        return new StatusAwareDataProvider<>(this, resourceOid, Model.of(), containerModel, suggestionsIndex::get);
    }

    @SuppressWarnings("unchecked")
    private @Nullable StatusAwareDataProvider<ResourceObjectTypeDefinitionType> getStatusAwareProvider() {
        var dp = getTable().getDataProvider();
        return (dp instanceof StatusAwareDataProvider<?> sap)
                ? (StatusAwareDataProvider<ResourceObjectTypeDefinitionType>) sap
                : null;
    }

    @SuppressWarnings("unchecked")
    protected @Nullable StatusInfo<ObjectTypesSuggestionType> getStatusInfo(PrismContainerValueWrapper<?> value) {
        var provider = getStatusAwareProvider();
        return provider != null ? (StatusInfo<ObjectTypesSuggestionType>) provider.getSuggestionInfo((PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>) value) : null;
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

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<ObjectTypesSuggestionType> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.FATAL_ERROR;
                    }
                    return false;
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
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(valueWrapper);
                            if (statusInfo == null) {
                                return;
                            }

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

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<ObjectTypesSuggestionType> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.SUCCESS;
                    }
                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {

                            PrismContainerValue<ResourceObjectTypeDefinitionType> prismContainerValue =
                                    (PrismContainerValue<ResourceObjectTypeDefinitionType>)
                                            PrismValueCollectionsUtil.cloneCollectionComplex(
                                            CloneStrategy.REUSE,
                                            Collections.singletonList(valueWrapper.getOldValue()))
                                    .iterator().next();

                            WebPrismUtil.cleanupEmptyContainerValue(prismContainerValue);
                            IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createContainerModel();
                            prismContainerValue.setParent(containerModel.getObject().getItem());

                            try {
                                containerModel.getObject().getItem().add(prismContainerValue);
                            } catch (SchemaException e) {
                                throw new RuntimeException("Couldn't add object type suggestion to the container: "
                                        + e.getMessage(), e);
                            }

                            onNewValue(prismContainerValue, containerModel, target, false);
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


    protected ButtonInlineMenuItem createSuggestionOperationInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.suspend.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getLabel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                IModel<?> rowModel = action.getRowModel();
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                    StatusInfo<ObjectTypesSuggestionType> suggestionStatus = getStatusInfo(wrapper);
                    if (suggestionStatus != null && suggestionStatus.isSuspended()) {
                        return createStringResource("ResourceObjectTypesPanel.resume.generating.inlineMenu");
                    }
                }
                return super.getLabel();
            }

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

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<ObjectTypesSuggestionType> suggestionStatus = getStatusInfo(wrapper);
                        if (suggestionStatus == null) {
                            return false;
                        }
                        OperationResultStatusType status = suggestionStatus.getStatus();
                        return !suggestionStatus.isComplete() && status != OperationResultStatusType.FATAL_ERROR;
                    }

                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().getPageTask();
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(wrapper);
                            if (statusInfo != null) {
                                if (statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
                                    resumeSuggestionTask(getPageBase(), statusInfo, task, result);
                                } else {
                                    suspendSuggestionTask(getPageBase(), statusInfo, task, result);
                                }
                                refreshForm(target);
                            }
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

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<ObjectTypesSuggestionType> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null;
                    }
                    return false;
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
                            StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(valueWrapper);
                            if (statusInfo == null) {
                                return;
                            }
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
        return Objects.requireNonNull(getStatusAwareProvider()).size() == 0;
    }

    @Override
    protected IModel<Boolean> getSwitchSuggestionModel() {
        return super.getSwitchSuggestionModel();
    }

    @Override
    protected boolean isToggleSuggestionVisible() {
        final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        final OperationResult result = task.getResult();

        final String resourceOid = getObjectDetailsModels().getObjectType().getOid();

        SmartIntegrationStatusInfoUtils.@NotNull ObjectTypeSuggestionProviderResult suggestions = loadObjectTypeSuggestionWrappers(
                getPageBase(), resourceOid, task, result);

        return !suggestions.wrappers().isEmpty();
    }
}
