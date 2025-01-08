/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Provides common inline menu functionality for focal objects: enable, disable, reconcile, delete.
 *
 * TODO deduplicate this functionality with the one in PageUsers
 */
public class FocusListInlineMenuHelper<F extends FocusType> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(FocusListInlineMenuHelper.class);

    // these are prefixed by parent page class name
    private static final String OPERATION_ENABLE_OBJECTS = "enableObjects";
    private static final String OPERATION_DISABLE_OBJECTS = "disableObjects";
    private static final String OPERATION_ENABLE_OBJECT = "enableObject";
    private static final String OPERATION_DISABLE_OBJECT = "disableObject";
    private static final String OPERATION_RECONCILE_OBJECTS = "reconcileObjects";
    private static final String OPERATION_RECONCILE_OBJECT = "reconcileObject";
    private static final String OPERATION_DELETE_OBJECTS = "deleteObjects";
    private static final String OPERATION_DELETE_OBJECT = "deleteObject";

    @NotNull private final Class<F> objectClass;
    @NotNull private final PageBase parentPage;
    @NotNull private final MainObjectListPanel<F> focusListComponent;

    public FocusListInlineMenuHelper(@NotNull Class<F> objectClass, @NotNull PageBase parentPage, @NotNull MainObjectListPanel<F> focusListComponent) {
        this.objectClass = objectClass;
        this.parentPage = parentPage;
        this.focusListComponent = focusListComponent;
    }

    public List<InlineMenuItem> createRowActions(Class<F> objectType) {
        List<InlineMenuItem> menu = new ArrayList<>();
        ButtonInlineMenuItem enableItem = new ButtonInlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.enable")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        updateActivationPerformed(target, true, getRowModel());
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return isShowConfirmationDialog((ColumnMenuAction<?>) getAction());
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(getEnableActionDefaultIcon(objectType));
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = parentPage.createStringResource("pageUsers.message.enableAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
        enableItem.setVisibilityChecker(FocusListInlineMenuHelper::isObjectDisabled);
        menu.add(enableItem);

        ButtonInlineMenuItem disableItem = new ButtonInlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.disable")) {

            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        updateActivationPerformed(target, false, getRowModel());
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return isShowConfirmationDialog((ColumnMenuAction<?>) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = parentPage.createStringResource("pageUsers.message.disableAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                CompositedIconBuilder builder = getDefaultCompositedIconBuilder(getEnableActionDefaultIcon(objectType));
                builder.appendLayerIcon(IconAndStylesUtil.createIconType(GuiStyleConstants.CLASS_BAN), IconCssStyle.BOTTOM_RIGHT_STYLE);
                return builder;
            }

        };
        disableItem.setVisibilityChecker(FocusListInlineMenuHelper::isObjectEnabled);
        menu.add(disableItem);

        menu.add(new ButtonInlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.reconcile")) {

            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        reconcilePerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return isShowConfirmationDialog((ColumnMenuAction<?>) getAction());
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = parentPage.createStringResource("pageUsers.message.reconcileAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }

        });

        menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.delete")) {

            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return isShowConfirmationDialog((ColumnMenuAction<?>) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = parentPage.createStringResource("pageUsers.message.deleteAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        });
        return menu;
    }

    private String getEnableActionDefaultIcon(Class<F> type) {
        String iconClass = "";
        if (type.equals(UserType.class)) {
            iconClass = GuiStyleConstants.CLASS_OBJECT_USER_ICON;
        } else if (type.equals(RoleType.class)) {
            iconClass = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
        } else if (type.equals(ServiceType.class)) {
            iconClass = GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
        } else if (type.equals(OrgType.class)) {
            iconClass = GuiStyleConstants.CLASS_OBJECT_ORG_ICON;
        }
        return iconClass;
    }

    public void deleteConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<F>> selectedObject) {
        List<SelectableBean<F>> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(OPERATION_DELETE_OBJECTS));
        for (SelectableBean<F> object : objects) {
            OperationResult subResult = result.createSubresult(getOperationName(OPERATION_DELETE_OBJECT));
            try {
                Task task = parentPage.createSimpleTask(getOperationName(OPERATION_DELETE_OBJECT));

                ObjectDelta<F> delta = parentPage.getPrismContext().deltaFactory().object().createDeleteDelta(objectClass, object.getValue().getOid()
                );
                WebModelServiceUtils.save(delta, subResult, task, parentPage);
                subResult.computeStatus();
            } catch (RuntimeException ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError(LocalizationUtil.translate("FocusListInlineMenuHelper.message.delete.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete object", ex);
            }
        }
        result.computeStatusComposite();
        focusListComponent.clearCache();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.refreshTable(target);
        focusListComponent.clearCache();
    }

    /**
     * This method updates object activation. If selectedObject parameter is not null,
     * than it updates only that objects, otherwise it checks table for selected
     * objects.
     */
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling, IModel<SelectableBean<F>> selectedObject) {
        List<SelectableBean<F>> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(enabling ? OPERATION_ENABLE_OBJECTS : OPERATION_DISABLE_OBJECTS));
        for (SelectableBean<F> object : objects) {
            String operationName = getOperationName(enabling ? OPERATION_ENABLE_OBJECT : OPERATION_DISABLE_OBJECT);
            OperationResult subResult = result.createSubresult(operationName);
            try {
                Task task = parentPage.createSimpleTask(operationName);

                ObjectDelta objectDelta = WebModelServiceUtils.createActivationAdminStatusDelta(
                        objectClass, object.getValue().getOid(), enabling, parentPage.getPrismContext());
                parentPage.getModelService().executeChanges(MiscUtil.createCollection(objectDelta), null, task, subResult);
                subResult.recordSuccess();
            } catch (CommonException | RuntimeException ex) {
                subResult.recomputeStatus();
                if (enabling) {
                    subResult.recordFatalError(LocalizationUtil.translate("FocusListInlineMenuHelper.message.enable.fatalError"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't enable object", ex);
                } else {
                    subResult.recordFatalError(LocalizationUtil.translate("FocusListInlineMenuHelper.message.disable.fatalError"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't disable object", ex);
                }
            }
        }
        result.recomputeStatus();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.clearCache();
        focusListComponent.refreshTable(target);
    }

    private void reconcilePerformed(AjaxRequestTarget target, IModel<SelectableBean<F>> selectedObject) {
        List<SelectableBean<F>> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(OPERATION_RECONCILE_OBJECTS));
        for (SelectableBean<F> object : objects) {
            OperationResult opResult = result.createSubresult(getOperationName(OPERATION_RECONCILE_OBJECT));
            F focus = object.getValue();
            try {
                Task task = parentPage.createSimpleTask(OPERATION_RECONCILE_OBJECT);
                ObjectDelta delta = parentPage.getPrismContext().deltaFactory().object()
                        .createEmptyModifyDelta(objectClass, focus.getOid()
                        );
                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                parentPage.getModelService().executeChanges(deltas, parentPage.executeOptions().reconcile(), task, opResult);
                opResult.computeStatusIfUnknown();
            } catch (CommonException | RuntimeException ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError(
                        parentPage.getString(
                                "FocusListInlineMenuHelper.message.reconcile.fatalError", WebComponentUtil.getName(focus)),
                        ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reconcile object " + focus + ".", ex);
            }
        }

        result.recomputeStatus();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.refreshTable(target);
        focusListComponent.clearCache();
    }

    private String getOperationName(String suffix) {
        return parentPage.getClass().getName() + "." + suffix;
    }

    /**
     * This method check selection in table. If selectedObject != null than it
     * returns only this object.
     *
     * @return list of selected objects
     */
    private List<SelectableBean<F>> getObjectsToActOn(AjaxRequestTarget target, IModel<SelectableBean<F>> selectedObject) {
        if (selectedObject != null) {
            return Collections.singletonList(selectedObject.getObject());
        } else {
            List<SelectableBean<F>> objects;
            objects = focusListComponent.isAnythingSelected(selectedObject);
            if (objects.isEmpty()) {
                parentPage.warn(parentPage.getString("FocusListInlineMenuHelper.message.nothingSelected"));
                target.add(parentPage.getFeedbackPanel());
            }
            return objects;
        }
    }

    public static boolean isObjectEnabled(IModel<?> rowModel, boolean isHeader) {
        if (rowModel == null || isHeader) {
            return true;
        }
        FocusType focusObject = ((IModel<SelectableBean<? extends FocusType>>) rowModel).getObject().getValue();
        if (focusObject == null) {
            return false;
        }

        //TODO is this correct?
        if (focusObject.getActivation() == null) {
            return true;
        }

        return ActivationStatusType.ENABLED == focusObject.getActivation().getEffectiveStatus();
    }

    public static boolean isObjectDisabled(IModel<?> rowModel, boolean isHeader) {
        if (rowModel == null || isHeader) {
            return true;
        }
        FocusType focusObject = ((IModel<SelectableBean<? extends FocusType>>) rowModel).getObject().getValue();
        if (focusObject == null) {
            return false;
        }

        if (focusObject.getActivation() == null) {
            return false;
        }

        return ActivationStatusType.DISABLED == focusObject.getActivation().getEffectiveStatus();
    }

    protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
        return false;
    }

    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
        return null;
    }
}
