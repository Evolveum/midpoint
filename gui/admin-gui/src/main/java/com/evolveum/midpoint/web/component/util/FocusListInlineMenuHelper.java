/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

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
	@NotNull private final FocusListComponent<F> focusListComponent;
	private F singleDelete;

	public FocusListInlineMenuHelper(@NotNull Class<F> objectClass, @NotNull PageBase parentPage, @NotNull FocusListComponent<F> focusListComponent) {
		this.objectClass = objectClass;
		this.parentPage = parentPage;
		this.focusListComponent = focusListComponent;
	}

	public List<InlineMenuItem> createRowActions(boolean isHeader) {
		List<InlineMenuItem> menu = new ArrayList<>();
		menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.enable"),
            new Model<>(false), new Model<>(false), false,
				new ColumnMenuAction<SelectableBean<F>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							updateActivationPerformed(target, true, null);
						} else {
							SelectableBean<F> rowDto = getRowModel().getObject();
							updateActivationPerformed(target, true, rowDto.getValue());
						}
					}
				}, isHeader ? InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.HEADER_ENABLE.getMenuItemId()
				: InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.ENABLE.getMenuItemId(),
				GuiStyleConstants.CLASS_OBJECT_USER_ICON,
				DoubleButtonColumn.BUTTON_COLOR_CLASS.SUCCESS.toString()){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return FocusListInlineMenuHelper.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = parentPage.createStringResource("pageUsers.message.enableAction").getString();
				return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}

		});

		menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.disable"),
				isHeader ? new Model<>(true) : new Model<>(false),
				isHeader ? new Model<>(true) : new Model<>(false),
				false,
				new ColumnMenuAction<SelectableBean<F>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							updateActivationPerformed(target, false, null);
						} else {
							SelectableBean<F> rowDto = getRowModel().getObject();
							updateActivationPerformed(target, false, rowDto.getValue());
						}
					}
				}, isHeader ? InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.HEADER_DISABLE.getMenuItemId()
				: InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.DISABLE.getMenuItemId(),
				GuiStyleConstants.CLASS_OBJECT_USER_ICON,
				DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return FocusListInlineMenuHelper.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = parentPage.createStringResource("pageUsers.message.disableAction").getString();
				return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});
		menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.reconcile"),
            new Model<>(false), new Model<>(false), false,
				new ColumnMenuAction<SelectableBean<F>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							reconcilePerformed(target, null);
						} else {
							SelectableBean<F> rowDto = getRowModel().getObject();
							reconcilePerformed(target, rowDto.getValue());
						}
					}
				}, isHeader ? InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.HEADER_RECONCILE.getMenuItemId()
				: InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.RECONCILE.getMenuItemId(),
				GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM,
				DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return FocusListInlineMenuHelper.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = parentPage.createStringResource("pageUsers.message.reconcileAction").getString();
				return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});



		menu.add(new InlineMenuItem(parentPage.createStringResource("FocusListInlineMenuHelper.menu.delete"),
				new ColumnMenuAction<SelectableBean<F>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							deleteConfirmedPerformed(target, null);
						} else {
							SelectableBean<F> rowDto = getRowModel().getObject();
							deleteConfirmedPerformed(target, rowDto.getValue());
						}
					}
				}){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return FocusListInlineMenuHelper.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = parentPage.createStringResource("pageUsers.message.deleteAction").getString();
				return FocusListInlineMenuHelper.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}

		});
		return menu;
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

				ObjectDelta<F> delta = ObjectDelta.createDeleteDelta(objectClass, object.getOid(), parentPage.getPrismContext());
				WebModelServiceUtils.save(delta, subResult, task, parentPage);
				subResult.computeStatus();
			} catch (RuntimeException ex) {
				subResult.recomputeStatus();
				subResult.recordFatalError("Couldn't delete object.", ex);
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
				parentPage.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(objectDelta), null, task, subResult);
				subResult.recordSuccess();
			} catch (CommonException|RuntimeException ex) {
				subResult.recomputeStatus();
				if (enabling) {
					subResult.recordFatalError("Couldn't enable object.", ex);
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't enable object", ex);
				} else {
					subResult.recordFatalError("Couldn't disable object.", ex);
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
				ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(objectClass, object.getOid(), parentPage.getPrismContext());
				Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
				parentPage.getModelService().executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, opResult);
				opResult.computeStatusIfUnknown();
			} catch (CommonException|RuntimeException ex) {
				opResult.recomputeStatus();
				opResult.recordFatalError("Couldn't reconcile object " + object + ".", ex);
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

	protected boolean isShowConfirmationDialog(ColumnMenuAction action){
		return false;
	}

	protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
		return null;
	}
}
