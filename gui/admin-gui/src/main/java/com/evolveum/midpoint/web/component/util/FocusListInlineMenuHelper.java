/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Provides common inline menu functionality for focal objects: enable, disable, reconcile, delete.
 *
 * TODO deduplicate this functionality with the one in PageUsers
 *
 * @author mederly
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
    @NotNull private final PageAdminObjectList<F> focusListComponent;
    private F singleDelete;

    public FocusListInlineMenuHelper(@NotNull Class<F> objectClass, @NotNull PageBase parentPage, @NotNull PageAdminObjectList<F> focusListComponent) {
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
                return new ColumnMenuAction<SelectableBeanImpl<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            updateActivationPerformed(target, true, null);
                        } else {
                            SelectableBeanImpl<F> rowDto = getRowModel().getObject();
                            updateActivationPerformed(target, true, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(getEnableActionDefaultIcon(objectType));
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = parentPage.createStringResource("pageUsers.message.enableAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        };
        enableItem.setVisibilityChecker(FocusListInlineMenuHelper::isObjectDisabled);
        menu.add(enableItem);

        ButtonInlineMenuItem disableItem = new ButtonInlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.disable")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            updateActivationPerformed(target, false, null);
                        } else {
                            SelectableBeanImpl<F> rowDto = getRowModel().getObject();
                            updateActivationPerformed(target, false, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = parentPage.createStringResource("pageUsers.message.disableAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                CompositedIconBuilder builder = getDefaultCompositedIconBuilder(getEnableActionDefaultIcon(objectType));
                builder.appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_BAN), IconCssStyle.BOTTOM_RIGHT_STYLE);
                return builder;            }

        };
        disableItem.setVisibilityChecker(FocusListInlineMenuHelper::isObjectEnabled);
        menu.add(disableItem);

        menu.add(new ButtonInlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.reconcile")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            reconcilePerformed(target, null);
                        } else {
                            SelectableBeanImpl<F> rowDto = getRowModel().getObject();
                            reconcilePerformed(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = parentPage.createStringResource("pageUsers.message.reconcileAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });

        menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteConfirmedPerformed(target, null);
                        } else {
                            SelectableBeanImpl<F> rowDto = getRowModel().getObject();
                            deleteConfirmedPerformed(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = parentPage.createStringResource("pageUsers.message.deleteAction").getString();
                return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        return menu;
    }

    private String getEnableActionDefaultIcon(Class<F> type){
        String iconClass = "";
        if (type.equals(RoleType.class)) {
            iconClass = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
        } else if (type.equals(ServiceType.class)){
            iconClass = GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
        }
        return iconClass;
    }

    public void deleteConfirmedPerformed(AjaxRequestTarget target, F selectedObject) {
        List<F> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(OPERATION_DELETE_OBJECTS));
        for (F object : objects) {
            OperationResult subResult = result.createSubresult(getOperationName(OPERATION_DELETE_OBJECT));
            try {
                Task task = parentPage.createSimpleTask(getOperationName(OPERATION_DELETE_OBJECT));

                ObjectDelta<F> delta = parentPage.getPrismContext().deltaFactory().object().createDeleteDelta(objectClass, object.getOid()
                );
                WebModelServiceUtils.save(delta, subResult, task, parentPage);
                subResult.computeStatus();
            } catch (RuntimeException ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError("FocusListInlineMenuHelper.message.delete.fatalError", ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete object", ex);
            }
        }
        result.computeStatusComposite();
        focusListComponent.getObjectListPanel().clearCache();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.getObjectListPanel().refreshTable(objectClass, target);
        focusListComponent.getObjectListPanel().clearCache();
    }

    /**
     * This method updates object activation. If selectedObject parameter is not null,
     * than it updates only that objects, otherwise it checks table for selected
     * objects.
     */
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling, F selectedObject) {
        List<F> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(enabling ? OPERATION_ENABLE_OBJECTS : OPERATION_DISABLE_OBJECTS));
        for (F object : objects) {
            String operationName = getOperationName(enabling ? OPERATION_ENABLE_OBJECT : OPERATION_DISABLE_OBJECT);
            OperationResult subResult = result.createSubresult(operationName);
            try {
                Task task = parentPage.createSimpleTask(operationName);

                ObjectDelta objectDelta = WebModelServiceUtils.createActivationAdminStatusDelta(
                        objectClass, object.getOid(), enabling, parentPage.getPrismContext());
                parentPage.getModelService().executeChanges(MiscUtil.createCollection(objectDelta), null, task, subResult);
                subResult.recordSuccess();
            } catch (CommonException|RuntimeException ex) {
                subResult.recomputeStatus();
                if (enabling) {
                    subResult.recordFatalError("FocusListInlineMenuHelper.message.enable.fatalError", ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't enable object", ex);
                } else {
                    subResult.recordFatalError("FocusListInlineMenuHelper.message.disable.fatalError", ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't disable object", ex);
                }
            }
        }
        result.recomputeStatus();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.getObjectListPanel().clearCache();
        focusListComponent.getObjectListPanel().refreshTable(objectClass, target);
    }

    private void reconcilePerformed(AjaxRequestTarget target, F selectedObject) {
        List<F> objects = getObjectsToActOn(target, selectedObject);
        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(getOperationName(OPERATION_RECONCILE_OBJECTS));
        for (F object : objects) {
            OperationResult opResult = result.createSubresult(getOperationName(OPERATION_RECONCILE_OBJECT));
            try {
                Task task = parentPage.createSimpleTask(OPERATION_RECONCILE_OBJECT);
                ObjectDelta delta = parentPage.getPrismContext().deltaFactory().object()
                        .createEmptyModifyDelta(objectClass, object.getOid()
                        );
                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                parentPage.getModelService().executeChanges(deltas, parentPage.executeOptions().reconcile(), task, opResult);
                opResult.computeStatusIfUnknown();
            } catch (CommonException|RuntimeException ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError(
                        parentPage.createStringResource("FocusListInlineMenuHelper.message.reconcile.fatalError", object).getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reconcile object " + object + ".", ex);
            }
        }

        result.recomputeStatus();

        parentPage.showResult(result);
        target.add(parentPage.getFeedbackPanel());
        focusListComponent.getObjectListPanel().refreshTable(objectClass, target);
        focusListComponent.getObjectListPanel().clearCache();
    }


    private String getOperationName(String suffix) {
        return parentPage.getClass().getName() + "." + suffix;
    }

    /**
     * This method check selection in table. If selectedObject != null than it
     * returns only this object.
     */
    private List<F> getObjectsToActOn(AjaxRequestTarget target, F selectedObject) {
        if (selectedObject != null) {
            return Collections.singletonList(selectedObject);
        } else {
            List<F> objects;
            objects = focusListComponent.getObjectListPanel().getSelectedObjects();
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
    protected boolean isShowConfirmationDialog(ColumnMenuAction action){
        return false;
    }

    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
        return null;
    }
}
