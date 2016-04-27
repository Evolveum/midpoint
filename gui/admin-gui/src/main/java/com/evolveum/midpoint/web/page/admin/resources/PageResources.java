/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.ResourcesStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/resources", action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL, label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL, description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_URL, label = "PageResources.auth.resources.label", description = "PageResources.auth.resources.description") })
public class PageResources extends PageAdminResources {

	private static final Trace LOGGER = TraceManager.getTrace(PageResources.class);
	private static final String DOT_CLASS = PageResources.class.getName() + ".";
	private static final String OPERATION_TEST_RESOURCE = DOT_CLASS + "testResource";
	private static final String OPERATION_SYNC_STATUS = DOT_CLASS + "syncStatus";
	private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
	private static final String OPERATION_DELETE_HOSTS = DOT_CLASS + "deleteHosts";
	private static final String OPERATION_CONNECTOR_DISCOVERY = DOT_CLASS + "connectorDiscovery";

	private static final String ID_DELETE_RESOURCES_POPUP = "deleteResourcesPopup";
	private static final String ID_DELETE_HOSTS_POPUP = "deleteHostsPopup";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TABLE = "table";
	private static final String ID_CONNECTOR_TABLE = "connectorTable";

	private IModel<Search> searchModel;
	private IModel<Search> chSearchModel;
	private ResourceType singleDelete;

	public PageResources() {
		this(true);
	}

	public PageResources(boolean clearSessionPaging) {
		this(clearSessionPaging, "");
	}

	public PageResources(String searchText) {
		this(true, searchText);
	}

	public PageResources(boolean clearSessionPaging, final String searchText) {
		searchModel = new LoadableModel<Search>(false) {

			@Override
			protected Search load() {
				ResourcesStorage storage = getSessionStorage().getResources();
				Search dto = storage.getSearch();

				if (dto == null) {
					dto = SearchFactory.createSearch(ResourceType.class, getPrismContext(), true);
				}

				return dto;
			}
		};

		chSearchModel = new LoadableModel<Search>(false) {

			@Override
			protected Search load() {
				return SearchFactory.createSearch(ConnectorHostType.class, getPrismContext(), true);
			}
		};

		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createNoFetch());
		options.add(SelectorOptions.create(ResourceType.F_CONNECTOR_REF,
				GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

		MainObjectListPanel<ResourceType> resourceListPanel = new MainObjectListPanel<ResourceType>(ID_TABLE,
				ResourceType.class, TableId.TABLE_RESOURCES, options, this) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return PageResources.this.createRowMenuItems();
			}

			@Override
			protected List<IColumn<SelectableBean<ResourceType>, String>> createColumns() {
				return PageResources.this.initResourceColumns();
			}

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, ResourceType object) {
				PageResources.this.resourceDetailsPerformed(target, object.getOid());

			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				setResponsePage(PageResourceWizard.class);

			}
		};
		resourceListPanel.setOutputMarkupId(true);
		resourceListPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES);
		mainForm.add(resourceListPanel);

		BoxedTablePanel connectorHosts = new BoxedTablePanel(ID_CONNECTOR_TABLE,
				new ObjectDataProvider(PageResources.this, ConnectorHostType.class),
				initConnectorHostsColumns(), UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS,
				(int) getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS)) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFormPanel(headerId, chSearchModel) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
						PageResources.this.searchHostPerformed(query, target);
					}
				};
			}
		};
		connectorHosts.setOutputMarkupId(true);
		mainForm.add(connectorHosts);

		add(new ConfirmationDialog(ID_DELETE_RESOURCES_POPUP,
				createStringResource("pageResources.dialog.title.confirmDelete"),
				createDeleteConfirmString("pageResources.message.deleteResourceConfirm",
						"pageResources.message.deleteResourcesConfirm", true)) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteResourceConfirmedPerformed(target);
			}
		});

		add(new ConfirmationDialog(ID_DELETE_HOSTS_POPUP,
				createStringResource("pageResources.dialog.title.confirmDelete"),
				createDeleteConfirmString("pageResources.message.deleteHostConfirm",
						"pageResources.message.deleteHostsConfirm", false)) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteHostConfirmedPerformed(target);
			}
		});
	}

	private List<InlineMenuItem> createRowMenuItems() {

		List<InlineMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new InlineMenuItem(createStringResource("PageResources.inlineMenuItem.test"),
				new ColumnMenuAction<SelectableBean<ResourceType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						SelectableBean<ResourceType> rowDto = getRowModel().getObject();
						testResourcePerformed(target, rowDto.getValue());
					}
				}));

		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
				new ColumnMenuAction<SelectableBean<ResourceType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						SelectableBean<ResourceType> rowDto = getRowModel().getObject();
						deleteResourcePerformed(target, rowDto.getValue());
					}
				}));

		menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.deleteSyncToken"),
				new ColumnMenuAction<SelectableBean<ResourceType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						SelectableBean<ResourceType> rowDto = getRowModel().getObject();
						deleteResourceSyncTokenPerformed(target, rowDto.getValue());
					}

				}));

		menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.editResource"),
				new ColumnMenuAction<SelectableBean<ResourceType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						SelectableBean<ResourceType> rowDto = getRowModel().getObject();
						editResourcePerformed(rowDto.getValue());
					}
				}));
		menuItems.add(new InlineMenuItem(createStringResource("pageResources.button.editAsXml"),
				new ColumnMenuAction<SelectableBean<ResourceType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						SelectableBean<ResourceType> rowDto = getRowModel().getObject();
						editAsXmlPerformed(rowDto.getValue());
					}
				}));

		return menuItems;
	}

	private List<IColumn<SelectableBean<ResourceType>, String>> initResourceColumns() {
		List<IColumn<SelectableBean<ResourceType>, String>> columns = new ArrayList<>();

		columns.add(new PropertyColumn(createStringResource("pageResources.connectorType"),
				SelectableBean.F_VALUE + ".connector.connectorType"));
		columns.add(new PropertyColumn(createStringResource("pageResources.version"),
				SelectableBean.F_VALUE + ".connector.connectorVersion"));

		InlineMenuHeaderColumn menu = new InlineMenuHeaderColumn(initInlineMenu());
		columns.add(menu);

		return columns;
	}

	private List<InlineMenuItem> initInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteResourcePerformed(target, null);
					}
				}));

		return headerMenuItems;
	}

	private List<IColumn<ConnectorHostType, String>> initConnectorHostsColumns() {
		List<IColumn<ConnectorHostType, String>> columns = new ArrayList<>();

		IColumn column = new CheckBoxHeaderColumn<ConnectorHostType>();
		columns.add(column);

		column = new LinkColumn<SelectableBean<ConnectorHostType>>(
				createStringResource("pageResources.connector.name"), "name", "value.name") {

			@Override
			public void onClick(AjaxRequestTarget target,
					IModel<SelectableBean<ConnectorHostType>> rowModel) {
				ConnectorHostType host = rowModel.getObject().getValue();
				// resourceDetailsPerformed(target, host.getOid());
			}
		};
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageResources.connector.hostname"),
				"value.hostname"));
		columns.add(new PropertyColumn(createStringResource("pageResources.connector.port"), "value.port"));
		columns.add(
				new PropertyColumn(createStringResource("pageResources.connector.timeout"), "value.timeout"));
		columns.add(new CheckBoxColumn(createStringResource("pageResources.connector.protectConnection"),
				"value.protectConnection"));

		InlineMenuHeaderColumn menu = new InlineMenuHeaderColumn(initInlineHostsMenu());
		columns.add(menu);

		return columns;
	}

	private List<InlineMenuItem> initInlineHostsMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteHostPerformed(target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("pageResources.button.discoveryRemote"),
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						discoveryRemotePerformed(target);
					}
				}));

		return headerMenuItems;
	}

	private void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, oid);
		setResponsePage(PageResource.class, parameters);
	}

	private void deleteHostPerformed(AjaxRequestTarget target) {
		List<SelectableBean<ConnectorHostType>> selected = WebComponentUtil
				.getSelectedData(getConnectorHostTable());
		if (selected.isEmpty()) {
			warn(getString("pageResources.message.noHostSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		ModalWindow dialog = (ModalWindow) get(ID_DELETE_HOSTS_POPUP);
		dialog.show(target);
	}

	private List<ResourceType> isAnyResourceSelected(AjaxRequestTarget target, ResourceType single) {
		List<ResourceType> selected = null;
		if (single != null) {
			selected = new ArrayList<>(1);
			selected.add(single);
			return selected;
		}
		selected = getResourceTable().getSelectedObjects();
		if (selected.size() < 1) {
			warn(createStringResource("pageResources.message.noResourceSelected"));
		}
		return selected;

	}

	private void deleteResourcePerformed(AjaxRequestTarget target, ResourceType single) {
		List<ResourceType> selected = isAnyResourceSelected(target, single);
		singleDelete = single;

		if (selected.isEmpty()) {
			return;
		}

		ModalWindow dialog = (ModalWindow) get(ID_DELETE_RESOURCES_POPUP);
		dialog.show(target);
	}

	private MainObjectListPanel<ResourceType> getResourceTable() {
		return (MainObjectListPanel<ResourceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private Table getConnectorHostTable() {
		return (Table) get(createComponentPath(ID_MAIN_FORM, ID_CONNECTOR_TABLE));
	}

	/**
	 * @param oneDeleteKey
	 *            message if deleting one item
	 * @param moreDeleteKey
	 *            message if deleting more items
	 * @param resources
	 *            if true selecting resources if false selecting from hosts
	 */
	private IModel<String> createDeleteConfirmString(final String oneDeleteKey, final String moreDeleteKey,
			final boolean resources) {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				List selected = new ArrayList();
				if (resources) {
					if (singleDelete != null) {
						selected.add(singleDelete);
					} else {
						selected = getResourceTable().getSelectedObjects();
					}
				} else {
					selected = WebComponentUtil.getSelectedData(getConnectorHostTable());
				}

				switch (selected.size()) {
					case 1:
						Object first = selected.get(0);
						String name = resources ? WebComponentUtil.getName(((ResourceType) first))
								: WebComponentUtil
										.getName(((SelectableBean<ConnectorHostType>) first).getValue());
						return createStringResource(oneDeleteKey, name).getString();
					default:
						return createStringResource(moreDeleteKey, selected.size()).getString();
				}
			}
		};
	}

	private void deleteHostConfirmedPerformed(AjaxRequestTarget target) {
		Table hostTable = getConnectorHostTable();
		List<SelectableBean<ConnectorHostType>> selected = WebComponentUtil.getSelectedData(hostTable);

		OperationResult result = new OperationResult(OPERATION_DELETE_HOSTS);
		for (SelectableBean<ConnectorHostType> selectable : selected) {
			try {
				Task task = createSimpleTask(OPERATION_DELETE_HOSTS);

				ObjectDelta<ConnectorHostType> delta = ObjectDelta.createDeleteDelta(ConnectorHostType.class,
						selectable.getValue().getOid(), getPrismContext());
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task,
						result);
			} catch (Exception ex) {
				result.recordPartialError("Couldn't delete host.", ex);
				LoggingUtils.logException(LOGGER, "Couldn't delete host", ex);
			}
		}

		result.recomputeStatus();
		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS,
					"The resource(s) have been successfully deleted.");
		}

		BaseSortableDataProvider provider = (BaseSortableDataProvider) hostTable.getDataTable()
				.getDataProvider();
		provider.clearCache();

		showResult(result);
		target.add(getFeedbackPanel(), (Component) hostTable);
	}

	private void deleteResourceConfirmedPerformed(AjaxRequestTarget target) {
		List<ResourceType> selected = new ArrayList<>();

		if (singleDelete != null) {
			selected.add(singleDelete);
		} else {
			selected = getResourceTable().getSelectedObjects();
		}

		OperationResult result = new OperationResult(OPERATION_DELETE_RESOURCES);
		for (ResourceType resource : selected) {
			try {
				Task task = createSimpleTask(OPERATION_DELETE_RESOURCES);

				ObjectDelta<ResourceType> delta = ObjectDelta.createDeleteDelta(ResourceType.class,
						resource.getOid(), getPrismContext());
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task,
						result);
			} catch (Exception ex) {
				result.recordPartialError("Couldn't delete resource.", ex);
				LoggingUtils.logException(LOGGER, "Couldn't delete resource", ex);
			}
		}

		result.recomputeStatus();
		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS,
					"The resource(s) have been successfully deleted.");
		}

		getResourceTable().clearCache();

		showResult(result);
		target.add(getFeedbackPanel(), (Component) getResourceTable());
	}

	private void discoveryRemotePerformed(AjaxRequestTarget target) {
		target.add(getFeedbackPanel());

		PageBase page = (PageBase) getPage();
		Task task = page.createSimpleTask(OPERATION_CONNECTOR_DISCOVERY);
		OperationResult result = task.getResult();
		List<SelectableBean<ConnectorHostType>> selected = WebComponentUtil
				.getSelectedData(getConnectorHostTable());
		if (selected.isEmpty()) {
			warn(getString("pageResources.message.noHostSelected"));
			return;
		}

		for (SelectableBean<ConnectorHostType> bean : selected) {
			ConnectorHostType host = bean.getValue();
			try {
				getModelService().discoverConnectors(host, task, result);
			} catch (Exception ex) {
				result.recordFatalError("Fail to discover connectors on host '" + host.getHostname() + ":"
						+ host.getPort() + "'", ex);
			}
		}

		result.recomputeStatus();
		showResult(result);
	}

	private void testResourcePerformed(AjaxRequestTarget target, ResourceType resourceType) {

		OperationResult result = new OperationResult(OPERATION_TEST_RESOURCE);

		// SelectableBean<ResourceType> dto = rowModel.getObject();
		// ResourceType resourceType = dto.getValue();
		if (StringUtils.isEmpty(resourceType.getOid())) {
			result.recordFatalError("Resource oid not defined in request");
		}

		Task task = createSimpleTask(OPERATION_TEST_RESOURCE);
		try {
			result = getModelService().testResource(resourceType.getOid(), task);
			// ResourceController.updateResourceState(resourceType.getState(),
			// result);

			// todo de-duplicate code (see the same operation in PageResource)
			// this provides some additional tests, namely a test for schema
			// handling section
			getModelService().getObject(ResourceType.class, resourceType.getOid(), null, task, result);
		} catch (Exception ex) {
			result.recordFatalError("Failed to test resource connection", ex);
		}

		// a bit of hack: result of TestConnection contains a result of
		// getObject as a subresult
		// so in case of TestConnection succeeding we recompute the result to
		// show any (potential) getObject problems
		if (result.isSuccess()) {
			result.recomputeStatus();
		}

		// if (!result.isSuccess()) {
		showResult(result);
		target.add(getFeedbackPanel());
		// }
		target.add(getResourceTable());
	}

	private void searchHostPerformed(ObjectQuery query, AjaxRequestTarget target) {
		target.add(getFeedbackPanel());

		Table panel = getConnectorHostTable();
		DataTable table = panel.getDataTable();
		ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
		provider.setQuery(query);
		provider.setOptions(SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));

		target.add((Component) panel);
	}

	private void deleteResourceSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType) {
		deleteSyncTokenPerformed(target, resourceType);
	}

	private void editResourcePerformed(ResourceType resourceType) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, resourceType.getOid());
		setResponsePage(new PageResourceWizard(parameters));
	}

	private void editAsXmlPerformed(ResourceType resourceType) {
		PageParameters parameters = new PageParameters();
		parameters.add(PageDebugView.PARAM_OBJECT_ID, resourceType.getOid());
		parameters.add(PageDebugView.PARAM_OBJECT_TYPE, ResourceType.class.getSimpleName());
		setResponsePage(PageDebugView.class, parameters);
	}
}
