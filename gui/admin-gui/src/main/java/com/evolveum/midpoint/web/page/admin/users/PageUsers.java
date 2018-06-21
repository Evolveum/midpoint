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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin/users", matchUrlForSecurity = "/admin/users")
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
						label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
						description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_URL,
						label = "PageUsers.auth.users.label",
						description = "PageUsers.auth.users.description")
		})
public class PageUsers extends PageAdminUsers {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);

	private static final String DOT_CLASS = PageUsers.class.getName() + ".";

	private static final String OPERATION_DELETE_USERS = DOT_CLASS + "deleteUsers";
	private static final String OPERATION_DELETE_USER = DOT_CLASS + "deleteUser";
	private static final String OPERATION_DISABLE_USERS = DOT_CLASS + "disableUsers";
	private static final String OPERATION_DISABLE_USER = DOT_CLASS + "disableUser";
	private static final String OPERATION_ENABLE_USERS = DOT_CLASS + "enableUsers";
	private static final String OPERATION_ENABLE_USER = DOT_CLASS + "enableUser";
	private static final String OPERATION_RECONCILE_USERS = DOT_CLASS + "reconcileUsers";
	private static final String OPERATION_RECONCILE_USER = DOT_CLASS + "reconcileUser";
	private static final String OPERATION_UNLOCK_USERS = DOT_CLASS + "unlockUsers";
	private static final String OPERATION_UNLOCK_USER = DOT_CLASS + "unlockUser";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TABLE = "table";

	private UserType singleDelete;
    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

	public PageUsers() {
		this(true, null, null);
	}

	public PageUsers(boolean clearPagingInSession) {
		this(clearPagingInSession, null, null);
	}

	public PageUsers(boolean clearPagingInSession, final UsersDto.SearchType type, final String text) {

		executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {

			@Override
			protected ExecuteChangeOptionsDto load() {
				return ExecuteChangeOptionsDto.createFromSystemConfiguration();
			}
		};

        if (StringUtils.isNotEmpty(text)){
            initSearch(text);
        }
        initLayout();
	}

	public PageUsers(UsersDto.SearchType type, String text) {
		this(true, type, text);
	}

    private void initSearch(String text){
        PageStorage storage = getSessionStorage().getPageStorageMap().get(SessionStorage.KEY_USERS);
        if (storage == null) {
            storage = getSessionStorage().initPageStorage(SessionStorage.KEY_USERS);
        }
        Search search = SearchFactory.createSearch(UserType.class, this);
		if (SearchBoxModeType.FULLTEXT.equals(search.getSearchType())){
			search.setFullText(text);
		} else if (search.getItems() != null && search.getItems().size() > 0){
            SearchItem searchItem = search.getItems().get(0);
            searchItem.getValues().add(new SearchValue<>(text));
        }
        storage.setSearch(search);
        getSessionStorage().getPageStorageMap().put(SessionStorage.KEY_USERS, storage);

    }

	private void initLayout() {
		Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		add(mainForm);

		initTable(mainForm);
	}

	private void initTable(Form mainForm) {
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
//		options.add(SelectorOptions.create(UserType.F_LINK_REF,
//				GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
//		options.add(SelectorOptions.create(UserType.F_ASSIGNMENT,
//				GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
		MainObjectListPanel<UserType> userListPanel = new MainObjectListPanel<UserType>(ID_TABLE,
				UserType.class, TableId.TABLE_USERS, options, this) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<IColumn<SelectableBean<UserType>, String>> createColumns() {
				return PageUsers.this.initColumns();
			}

			@Override
			protected PrismObject<UserType> getNewObjectListObject(){
				return (new UserType()).asPrismObject();
			}

			@Override
			protected IColumn<SelectableBean<UserType>, String> createActionsColumn() {
				return new InlineMenuButtonColumn<SelectableBean<UserType>>(createRowActions(false), 3, PageUsers.this){
					@Override
					protected int getHeaderNumberOfButtons() {
						return 2;
					}

					@Override
					protected List<InlineMenuItem> getHeaderMenuItems() {
						return createRowActions(true);
					}
				};
			}

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return createRowActions(false);
			}

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, UserType object) {
				userDetailsPerformed(target, object.getOid());
			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				navigateToNext(PageUser.class);
			}
		};

		userListPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES);
		userListPanel.setOutputMarkupId(true);
		mainForm.add(userListPanel);
	}

	private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
		List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<UserType>, String> column = new PropertyColumn(
				createStringResource("UserType.givenName"), UserType.F_GIVEN_NAME.getLocalPart(),
				SelectableBean.F_VALUE + ".givenName");
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.familyName"),
				UserType.F_FAMILY_NAME.getLocalPart(), SelectableBean.F_VALUE + ".familyName");
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.fullName"),
				UserType.F_FULL_NAME.getLocalPart(), SelectableBean.F_VALUE + ".fullName");
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.emailAddress"), null,
				SelectableBean.F_VALUE + ".emailAddress");
		columns.add(column);

		column = new AbstractExportableColumn<SelectableBean<UserType>, String>(
				createStringResource("pageUsers.accounts")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
					String componentId, IModel<SelectableBean<UserType>> model) {
				cellItem.add(new Label(componentId,
						model.getObject().getValue() != null ?
								model.getObject().getValue().getLinkRef().size() : null));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
				return Model.of(rowModel.getObject().getValue() != null ?
						Integer.toString(rowModel.getObject().getValue().getLinkRef().size()) : "");
			}


		};

		columns.add(column);

		return columns;
	}

	private List<InlineMenuItem> createRowActions(boolean isHeader) {
    	int id = isHeader ?
				InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.HEADER_ENABLE.getMenuItemId() :
				InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.ENABLE.getMenuItemId();

		List<InlineMenuItem> menu = new ArrayList<>();
		menu.add(new InlineMenuItem(
				createStringResource("pageUsers.menu.enable"),
            new Model<>(false),
            new Model<>(false),
				false,
				new ColumnMenuAction<SelectableBean<UserType>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							updateActivationPerformed(target, true, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
							updateActivationPerformed(target, true, rowDto.getValue());
						}
					}
				},
				id,
				GuiStyleConstants.CLASS_OBJECT_USER_ICON,
				null) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel() {
				String actionName = createStringResource("pageUsers.message.enableAction").getString();
				return PageUsers.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});

		menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.disable"),
				isHeader ? new Model<>(true) : new Model<>(false),
				isHeader ? new Model<>(true) : new Model<>(false),
				false,
				new ColumnMenuAction<SelectableBean<UserType>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							updateActivationPerformed(target, false, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
							updateActivationPerformed(target, false, rowDto.getValue());
						}
                    }
                }, isHeader ? InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.HEADER_DISABLE.getMenuItemId()
                : InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.DISABLE.getMenuItemId(),
				GuiStyleConstants.CLASS_OBJECT_USER_ICON,
				null) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = createStringResource("pageUsers.message.disableAction").getString();
				return PageUsers.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}

		});

		menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.reconcile"),
            new Model<>(false), new Model<>(false), false,
				new ColumnMenuAction<SelectableBean<UserType>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							reconcilePerformed(target, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
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
				return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = createStringResource("pageUsers.message.reconcileAction").getString();
				return PageUsers.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});

		menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.unlock"), false,
				new ColumnMenuAction<SelectableBean<UserType>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							unlockPerformed(target, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
							unlockPerformed(target, rowDto.getValue());
						}
					}
				}, InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.UNLOCK.getMenuItemId()){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = createStringResource("pageUsers.message.unlockAction").getString();
				return PageUsers.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});

		menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.delete"), false,
				new ColumnMenuAction<SelectableBean<UserType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							deleteConfirmedPerformed(target, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
							deleteConfirmedPerformed(target, rowDto.getValue());
						}
					}
				}, InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.DELETE.getMenuItemId()){

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowConfirmationDialog() {
				return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
			}

			@Override
			public IModel<String> getConfirmationMessageModel(){
				String actionName = createStringResource("pageUsers.message.deleteAction").getString();
				return PageUsers.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			}
		});

		menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.merge"),
				isHeader ? new Model<>(false) : new Model<>(true),
				isHeader ? new Model<>(false) : new Model<>(true),
				false,
				new ColumnMenuAction<SelectableBean<UserType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (getRowModel() == null){
							mergePerformed(target, null);
						} else {
							SelectableBean<UserType> rowDto = getRowModel().getObject();
							mergePerformed(target, rowDto.getValue());
						}
					}
				}, InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.MERGE.getMenuItemId(), "", ""));
		return menu;
	}

	private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, oid);
		navigateToNext(PageUser.class, parameters);
	}

	private MainObjectListPanel<UserType> getTable() {
		return (MainObjectListPanel<UserType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private void deleteConfirmedPerformed(AjaxRequestTarget target, UserType userToDelete) {
		List<UserType> users = isAnythingSelected(target, userToDelete);

		if (users.isEmpty()) {
			return;
		}

		OperationResult result = new OperationResult(OPERATION_DELETE_USERS);
		for (UserType user : users) {
			OperationResult subResult = result.createSubresult(OPERATION_DELETE_USER);
			try {
				Task task = createSimpleTask(OPERATION_DELETE_USER);

				ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.DELETE, getPrismContext());
				delta.setOid(user.getOid());

				ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
				ModelExecuteOptions options = executeOptions.createOptions();
				LOGGER.debug("Using options {}.", new Object[] { executeOptions });
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), options, task,
						subResult);
				subResult.computeStatus();
			} catch (Exception ex) {
				subResult.recomputeStatus();
				subResult.recordFatalError("Couldn't delete user.", ex);
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete user", ex);
			}
		}
		result.computeStatusComposite();
		getTable().clearCache();

		showResult(result);
		target.add(getFeedbackPanel());
		getTable().refreshTable(UserType.class, target);
		getTable().clearCache();
	}

    private void mergePerformed(AjaxRequestTarget target, final UserType selectedUser) {
        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(UserType.COMPLEX_TYPE);
        ObjectFilter filter = InOidFilter.createInOid(selectedUser.getOid());
        ObjectFilter notFilter = NotFilter.createNot(filter);
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                getMainPopupBodyId(), UserType.class,
                supportedTypes, false, PageUsers.this, notFilter) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                hideMainPopup(target);
                mergeConfirmedPerformed(selectedUser, user, target);
            }

        };
        panel.setOutputMarkupId(true);
        showMainPopup(panel, target);
    }

    private void mergeConfirmedPerformed(UserType mergeObject, UserType mergeWithObject, AjaxRequestTarget target) {
        setResponsePage(new PageMergeObjects(mergeObject, mergeWithObject, UserType.class));
    }

    private void unlockPerformed(AjaxRequestTarget target, UserType selectedUser) {
		List<UserType> users = isAnythingSelected(target, selectedUser);
		if (users.isEmpty()) {
			return;
		}
		OperationResult result = new OperationResult(OPERATION_UNLOCK_USERS);
		for (UserType user : users) {
			OperationResult opResult = result.createSubresult(getString(OPERATION_UNLOCK_USER, user));
			try {
				Task task = createSimpleTask(OPERATION_UNLOCK_USER + user);
				// TODO skip the operation if the user has no password
				// credentials specified (otherwise this would create
				// almost-empty password container)
				ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(
						UserType.class, user.getOid(), new ItemPath(UserType.F_ACTIVATION,
                                ActivationType.F_LOCKOUT_STATUS),
						getPrismContext(), LockoutStatusType.NORMAL);
				Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil
						.createDeltaCollection(delta);
				getModelService().executeChanges(deltas, null, task, opResult);
				opResult.computeStatusIfUnknown();
			} catch (Exception ex) {
				opResult.recomputeStatus();
				opResult.recordFatalError("Couldn't unlock user " + user + ".", ex);
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't unlock user " + user + ".", ex);
			}
		}

		result.recomputeStatus();

		showResult(result);
		target.add(getFeedbackPanel());
		getTable().refreshTable(UserType.class, target);
		getTable().clearCache();
	}

	private void reconcilePerformed(AjaxRequestTarget target, UserType selectedUser) {
		List<UserType> users = isAnythingSelected(target, selectedUser);
		if (users.isEmpty()) {
			return;
		}

		OperationResult result = new OperationResult(OPERATION_RECONCILE_USERS);
		for (UserType user : users) {
			OperationResult opResult = result.createSubresult(getString(OPERATION_RECONCILE_USER, user));
			try {
				Task task = createSimpleTask(OPERATION_RECONCILE_USER + user);
				ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, user.getOid(),
						getPrismContext());
				Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil
						.createDeltaCollection(delta);
				getModelService().executeChanges(deltas, ModelExecuteOptions.createReconcile(), task,
						opResult);
				opResult.computeStatusIfUnknown();
			} catch (Exception ex) {
				opResult.recomputeStatus();
				opResult.recordFatalError("Couldn't reconcile user " + user + ".", ex);
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reconcile user " + user + ".", ex);
			}
		}

		result.recomputeStatus();

		showResult(result);
		target.add(getFeedbackPanel());
		getTable().refreshTable(UserType.class, target);
		getTable().clearCache();
	}

	/**
	 * This method check selection in table. If selectedUser != null than it
	 * returns only this user.
	 */
	private List<UserType> isAnythingSelected(AjaxRequestTarget target, UserType selectedUser) {
		List<UserType> users;
		if (selectedUser != null) {
			users = new ArrayList<>();
			users.add(selectedUser);
		} else {
			users = getTable().getSelectedObjects();
			if (users.isEmpty()) {
				warn(getString("pageUsers.message.nothingSelected"));
				target.add(getFeedbackPanel());
			}
		}

		return users;
	}

	/**
	 * This method updates user activation. If userOid parameter is not null,
	 * than it updates only that user, otherwise it checks table for selected
	 * users.
	 */
	private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling,
			UserType selectedUser) {
		List<UserType> users = isAnythingSelected(target, selectedUser);
		if (users.isEmpty()) {
			return;
		}

		String operation = enabling ? OPERATION_ENABLE_USERS : OPERATION_DISABLE_USERS;
		OperationResult result = new OperationResult(operation);
		for (UserType user : users) {
			operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
			OperationResult subResult = result.createSubresult(operation);
			try {
				Task task = createSimpleTask(operation);

				ObjectDelta objectDelta = WebModelServiceUtils.createActivationAdminStatusDelta(
						UserType.class, user.getOid(), enabling, getPrismContext());

				ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
				ModelExecuteOptions options = executeOptions.createOptions();
				LOGGER.debug("Using options {}.", new Object[] { executeOptions });
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(objectDelta), options,
						task, subResult);
				subResult.recordSuccess();
			} catch (Exception ex) {
				subResult.recomputeStatus();
				if (enabling) {
					subResult.recordFatalError("Couldn't enable user.", ex);
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't enable user", ex);
				} else {
					subResult.recordFatalError("Couldn't disable user.", ex);
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't disable user", ex);
				}
			}
		}
		result.recomputeStatus();

		showResult(result);
		target.add(getFeedbackPanel());
		getTable().clearCache();
		getTable().refreshTable(UserType.class, target);
	}

    private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
		if (action.getRowModel() == null) {
			return createStringResource("pageUsers.message.confirmationMessageForMultipleObject",
					actionName, getTable().getSelectedObjectsCount() );
		} else {
			return createStringResource("pageUsers.message.confirmationMessageForSingleObject",
					actionName, ((ObjectType)((SelectableBean)action.getRowModel().getObject()).getValue()).getName());
		}

	}

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
		return action.getRowModel() != null ||
				getTable().getSelectedObjectsCount() > 0;
	}
}
