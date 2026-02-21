/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithBadgePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.handleSuggestionSuspendResumeOperation;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.showSuggestionInfoPanelPopup;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.getAiBadgeModel;

/**
 * Utility methods for handling {@link StatusInfo} in tables.
 */
public class StatusInfoTableUtil {

    private static final String CLASS_DOT = StatusInfoTableUtil.class.getName() + ".";
    private static final String OP_SUSPEND_SUGGESTION = CLASS_DOT + "suspendSuggestion";

    private StatusInfoTableUtil() {
        // utility class
    }

    /**
     * Sets a visibility checker for an inline menu item based on whether a mapping suggestion
     * is present and optionally on its status.
     */
    public static <C extends Containerable, T> void setVisibilityBySuggestion(
            @NotNull InlineMenuItem item,
            boolean showWhenPresent,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfoFn) {
        item.setVisibilityChecker(bySuggestion(showWhenPresent, getStatusInfoFn));
    }

    /**
     * Returns a {@link InlineMenuItem.VisibilityChecker} that controls visibility
     * based on mapping suggestion presence and its status.
     */
    @Contract(pure = true)
    public static <C extends Containerable, T> InlineMenuItem.@NotNull VisibilityChecker bySuggestion(
            boolean showWhenPresent,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfoFn) {

        return (rowModel, isHeader) -> {
            if (rowModel == null || rowModel.getObject() == null) {
                return true;
            }

            @SuppressWarnings("unchecked")
            PrismContainerValueWrapper<C> wrapper = (PrismContainerValueWrapper<C>) rowModel.getObject();
            StatusInfo<T> statusInfo = getStatusInfoFn.apply(wrapper);

            boolean present = statusInfo != null;

            if (present && statusInfo.getStatus() != OperationResultStatusType.SUCCESS) {
                return false;
            }

            return present == showWhenPresent;
        };
    }

    /**
     * Applies a suggestion-based CSS style to a tile component.
     */
    public static <C extends Containerable, T> void applySuggestionCss(
            @NotNull Component tile,
            @NotNull PrismContainerValueWrapper<C> value,
            @NotNull String baseCss,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfoFn) {

        StatusInfo<T> statusInfo = getStatusInfoFn.apply(value);
        if (statusInfo != null && statusInfo.getStatus() != null) {
            String styleClass = SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).tileClass;
            tile.add(AttributeModifier.replace("class", baseCss + " " + styleClass));
        }
    }

    /**
     * Creates an inline menu item that shows details about mapping suggestions when clicked.
     * The item is only visible when there is a fatal error in the suggestion status.
     */
    public static <C extends Containerable, T> @NotNull ButtonInlineMenuItem createSuggestionDetailsInlineMenu(
            @NotNull PageBase pageBase,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfoFn) {
        return new ButtonInlineMenuItem(
                pageBase.createStringResource("SuggestionDetailsInlineMenu.details.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @SuppressWarnings("unchecked")
            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<T> suggestionStatus = getStatusInfoFn.apply((PrismContainerValueWrapper<C>) wrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.FATAL_ERROR;
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
                            StatusInfo<T> statusInfo = getStatusInfoFn.apply((PrismContainerValueWrapper<C>) valueWrapper);
                            showSuggestionInfoPanelPopup(pageBase, target, statusInfo);
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

    /**
     * Creates an inline menu item that allows suspending or resuming the generation of mapping suggestions.
     * The item is only visible when the suggestion generation is in progress or suspended.
     */
    public static <C extends Containerable, T> @NotNull ButtonInlineMenuItem createSuggestionOperationInlineMenu(
            @NotNull PageBase pageBase,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfoFn,
            @NotNull SerializableConsumer<AjaxRequestTarget> refreshFn) {
        return new ButtonInlineMenuItem(
                pageBase.createStringResource("SuggestionOperationInlineMenu.suspend.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public IModel<String> getLabel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                IModel<?> rowModel = action.getRowModel();
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                    StatusInfo<T> s = getStatusInfoFn.apply((PrismContainerValueWrapper<C>) wrapper);
                    if (s != null) {
                        if (s.isExecuting() && !s.isSuspended()) {
                            return pageBase.createStringResource("SuggestionOperationInlineMenu.suspend.generating.inlineMenu");
                        }
                        if (s.isSuspended()) {
                            return pageBase.createStringResource("SuggestionOperationInlineMenu.resume.generating.inlineMenu");
                        }
                    }
                }
                return super.getLabel();
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<T> suggestionStatus = getStatusInfoFn.apply((PrismContainerValueWrapper) wrapper);
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

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = pageBase.createSimpleTask(OP_SUSPEND_SUGGESTION);
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<T> statusInfo = getStatusInfoFn.apply((PrismContainerValueWrapper<C>) valueWrapper);
                            if (statusInfo != null) {
                                handleSuggestionSuspendResumeOperation(pageBase, statusInfo, task, result);
                                refreshFn.accept(target);
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

    /**
     * Creates an inline menu item that allows reviewing mapping suggestions when clicked.
     * The item is only visible when there is a successful suggestion available.
     */
    public static <C extends Containerable, T> @NotNull ButtonInlineMenuItem createSuggestionReviewInlineMenu(
            @NotNull PageBase pageBase,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<T>> getStatusInfo,
            @NotNull SerializableTriConsumer<AjaxRequestTarget, PrismContainerValueWrapper<C>, StatusInfo<T>> performOnReview) {
        return new ButtonInlineMenuItem(
                pageBase.createStringResource("SuggestionReviewInlineMenu.review.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @SuppressWarnings("unchecked")
            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<T> suggestionStatus = getStatusInfo.apply((PrismContainerValueWrapper<C>) valueWrapper);
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
                        if (!(rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper)) {
                            pageBase.warn("Couldn't get value from the row model.");
                            target.add(pageBase.getFeedbackPanel().getParent());
                            return;
                        }

                        StatusInfo<T> suggestionStatus = getStatusInfo.apply((PrismContainerValueWrapper<C>) valueWrapper);
                        performOnReview.accept(target, (PrismContainerValueWrapper<C>) valueWrapper, suggestionStatus);
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    /**
     * Adds AJAX timer behavior and CSS class based on suggestion execution status.
     */
    public static <C extends Containerable, T> void applySuggestionAutoRefresh(
            @NotNull PrismContainerValueWrapper<C> value,
            @NotNull Item<PrismContainerValueWrapper<C>> item,
            @NotNull Duration ajaxUpdateDuration,
            @NotNull SerializableFunction<PrismContainerValueWrapper<C>, @Nullable StatusInfo<T>> getStatusInfo,
            @NotNull SerializableConsumer<AjaxRequestTarget> refreshFn) {

        StatusInfo<T> statusInfo = getStatusInfo.apply(value);
        if (statusInfo == null || statusInfo.getStatus() == null) {
            return;
        }

        item.add(AttributeModifier.append("class",
                SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).rowClass));

        boolean executing = statusInfo.isExecuting() && !statusInfo.isSuspended()
                && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR;

        if (executing) {
            AbstractAjaxTimerBehavior timer = new AbstractAjaxTimerBehavior(ajaxUpdateDuration) {
                @Override
                protected void onTimer(@NotNull AjaxRequestTarget target) {
                    target.add(item);

                    StatusInfo<T> latest = getStatusInfo.apply(value);
                    if (latest == null
                            || !latest.isExecuting()
                            || statusInfo.isSuspended()
                            || latest.getStatus() == OperationResultStatusType.FATAL_ERROR) {
                        stop(target);
                        refreshFn.accept(target);
                        target.add(item);
                    }
                }
            };
            item.add(timer);
        }
    }

    /**
     * Builds a label with badge panel for displaying suggestion names and statuses.
     */
    public static @NotNull LabelWithBadgePanel buildSuggestionNameLabel(
            @NotNull String componentId,
            @NotNull StatusInfo<?> suggestionTypeStatusInfo,
            @NotNull IModel<String> displayNameModel,
            @NotNull OperationResultStatusType status) {
        LabelWithBadgePanel labelWithBadgePanel = new LabelWithBadgePanel(
                componentId, getAiBadgeModel(), displayNameModel) {
            @Override
            protected boolean isIconVisible() {
                return suggestionTypeStatusInfo.isExecuting();
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCss() {
                return GuiStyleConstants.ICON_FA_SPINNER + " fa-spin text-info";
            }

            @Contract(pure = true)
            @Override
            protected @Nullable String getLabelCss() {
                return switch (status) {
                    case IN_PROGRESS -> " text-info";
                    case FATAL_ERROR -> " text-danger";
                    default -> null;
                };
            }

            @Override
            protected boolean isBadgeVisible() {
                return status.equals(OperationResultStatusType.SUCCESS);
            }
        };
        labelWithBadgePanel.setOutputMarkupId(true);
        return labelWithBadgePanel;
    }

    /**
     * Creates a toggle checkbox panel for showing/hiding suggestions.
     */
    public static @NotNull ToggleCheckBoxPanel createToggleSuggestionVisibilityButton(
            @NotNull PageBase pageBase, @NotNull String idButton,
            @NotNull IModel<Boolean> switchSuggestionModel,
            @NotNull SerializableConsumer<AjaxRequestTarget> refreshFn,
            @Nullable Component componentToUpdate) {
        ToggleCheckBoxPanel togglePanel = new ToggleCheckBoxPanel(idButton, switchSuggestionModel) {

            @Override
            public @NotNull Component getTitleComponent(@NotNull String id) {
                return new IconWithLabel(id, () -> switchSuggestionModel.getObject()
                        ? pageBase.getString("ToggleCheckBoxPanel.suggestion.enabled")
                        : pageBase.getString("ToggleCheckBoxPanel.suggestion.disabled")) {
                    @Override
                    protected String getIconCssClass() {
                        return GuiStyleConstants.CLASS_MAGIC_WAND + " text-purple ml-2";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return "d-flex m-0 font-weight-normal text-body";
                    }
                };
            }

            @Override
            protected @NotNull Map<String, Object> createSwitchOptions() {
                Map<String, Object> options = new LinkedHashMap<>();
                options.put("onColor", "purple");
                options.put("size", "mini");
                return options;
            }

            @Override
            protected void onToggle(@NotNull AjaxRequestTarget target) {
                refreshFn.accept(target);
                if (componentToUpdate != null) {
                    target.add(componentToUpdate);
                }
            }

            @Override
            public @NotNull String getContainerCssClass() {
                return "d-flex flex-row-reverse align-items-center gap-1";
            }
        };

        togglePanel.setOutputMarkupId(true);
        return togglePanel;
    }

    /**
     * Creates an actions column with inline menu buttons with link style and ellipsis action button.
     */
    public static <O extends Serializable> @Nullable IColumn<O, String> createLinkStyleActionsColumn(
            @NotNull PageBase pageBase,
            @NotNull List<InlineMenuItem> allItems) {
        return !allItems.isEmpty() ? new InlineMenuButtonColumn<>(allItems, pageBase) {
            @Override
            public String getCssClass() {
                return "inline-menu-column col";
            }

            @Override
            protected String getDropDownButtonIcon() {
                return "fa fa-ellipsis-h";
            }

            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-link btn-sm";
            }

            @Override
            protected String getInlineMenuItemCssClass(IModel<O> rowModel) {
                return "btn btn-link btn-sm text-nowrap";
            }

            @Override
            protected String getAdditionalMultiButtonPanelCssClass() {
                return "justify-content-end";
            }
        } : null;
    }

    @FunctionalInterface
    public interface SerializableTriConsumer<A, B, C> extends Serializable {
        void accept(A a, B b, C c);
    }

    public static StringResourceModel createConfirmationTitle(PageBase pageBase, int selectedCount, boolean empty, boolean isAccept) {
        if (isAccept) {
            return acceptConfirmationTitle(pageBase, selectedCount, empty);
        } else {
            return dismissConfirmationTitle(pageBase, selectedCount, empty);
        }
    }

    private static StringResourceModel acceptConfirmationTitle(PageBase pageBase, int selectedCount, boolean empty) {
        if (selectedCount == 0 && empty) {
            return pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.acceptConfirmation.title.noItems");
        }

        return selectedCount == 0
                ? pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.acceptConfirmation.title.empty")
                : pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.acceptConfirmation.title", selectedCount);
    }

    private static StringResourceModel dismissConfirmationTitle(PageBase pageBase, int selectedCount, boolean empty) {
        if (selectedCount == 0 && empty) {
            return pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.dismissConfirmation.title.noItems");
        }

        return selectedCount == 0
                ? pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.dismissConfirmation.title.empty")
                : pageBase.createStringResource("MultiSelectContainerActionTileTablePanel.dismissConfirmation.title", selectedCount);
    }

}
