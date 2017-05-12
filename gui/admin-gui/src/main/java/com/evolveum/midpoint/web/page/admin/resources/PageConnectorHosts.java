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

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.search.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
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
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/connectorHosts", action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_CONNECTOR_HOSTS_ALL, label = PageAdminResources.AUTH_CONNECTOR_HOSTS_ALL_LABEL, description = PageAdminResources.AUTH_CONNECTOR_HOSTS_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_HOSTS_ALL_URL, label = "PageResources.auth.connectorHosts.label", description = "PageResources.auth.connectorHosts.description") })
public class PageConnectorHosts extends PageAdminResources {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageConnectorHosts.class);
	private static final String DOT_CLASS = PageConnectorHosts.class.getName() + ".";
	private static final String OPERATION_DELETE_HOSTS = DOT_CLASS + "deleteHosts";
	private static final String OPERATION_CONNECTOR_DISCOVERY = DOT_CLASS + "connectorDiscovery";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_CONNECTOR_TABLE = "connectorTable";

	private IModel<Search> searchModel;

	public PageConnectorHosts() {
		this(true);
	}

	public PageConnectorHosts(boolean clearSessionPaging) {
		this(clearSessionPaging, "");
	}

	public PageConnectorHosts(String searchText) {
		this(true, searchText);
	}

	public PageConnectorHosts(boolean clearSessionPaging, String searchText) {

		searchModel = new LoadableModel<Search>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Search load() {
				return SearchFactory.createSearch(ConnectorHostType.class, PageConnectorHosts.this);
			}
		};

		initLayout();
	}

	private void initLayout() {
		Form<?> mainForm = new Form<>(ID_MAIN_FORM);
		add(mainForm);

		BoxedTablePanel<ConnectorHostType> connectorHosts = new BoxedTablePanel<ConnectorHostType>(
				ID_CONNECTOR_TABLE,
				new ObjectDataProvider<ConnectorHostType, ConnectorHostType>(PageConnectorHosts.this,
						ConnectorHostType.class),
				initConnectorHostsColumns(), UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS,
				(int) getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFormPanel(headerId, searchModel) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
						PageConnectorHosts.this.searchHostPerformed(query, target);
					}
				};
			}
		};
		connectorHosts.setOutputMarkupId(true);
		mainForm.add(connectorHosts);
	}

	private List<IColumn<ConnectorHostType, String>> initConnectorHostsColumns() {
		List<IColumn<ConnectorHostType, String>> columns = new ArrayList<>();

		IColumn column = new CheckBoxHeaderColumn<ConnectorHostType>();
		columns.add(column);

		column = new LinkColumn<SelectableBean<ConnectorHostType>>(
				createStringResource("pageResources.connector.name"), "name", "value.name") {
			private static final long serialVersionUID = 1L;

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
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteHostPerformed(target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("pageResources.button.discoveryRemote"),
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						discoveryRemotePerformed(target);
					}
				}));

		return headerMenuItems;
	}

	private void deleteHostPerformed(AjaxRequestTarget target) {
		List<SelectableBean<ConnectorHostType>> selected = WebComponentUtil
				.getSelectedData(getConnectorHostTable());
		if (selected.isEmpty()) {
			warn(getString("pageResources.message.noHostSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		ConfirmationPanel dialog = new ConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
				createDeleteConfirmString("pageResources.message.deleteHostConfirm",
						"pageResources.message.deleteHostsConfirm", false)) {
			private static final long serialVersionUID = 1L;
			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				((PageBase) getPage()).hideMainPopup(target);
				deleteHostConfirmedPerformed(target);
			}
		};
		((PageBase) getPage()).showMainPopup(dialog, target);

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
			private static final long serialVersionUID = 1L;
			@Override
			public String getObject() {
				List<SelectableBean<ConnectorHostType>> selected = WebComponentUtil.getSelectedData(getConnectorHostTable());

				switch (selected.size()) {
					case 1:
						Object first = selected.get(0);
						String name = WebComponentUtil
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

				if (selectable.getValue() != null) {
					ObjectDelta<ConnectorHostType> delta = ObjectDelta.createDeleteDelta(ConnectorHostType.class,
							selectable.getValue().getOid(), getPrismContext());
					getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task,
							result);
				}
			} catch (Exception ex) {
				result.recordPartialError("Couldn't delete host.", ex);
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete host", ex);
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
			if (bean.getValue() != null) {
				ConnectorHostType host = bean.getValue();
				try {
					getModelService().discoverConnectors(host, task, result);
				} catch (Exception ex) {
					result.recordFatalError("Fail to discover connectors on host '" + host.getHostname() + ":"
							+ host.getPort() + "'", ex);
				}
			}
		}

		result.recomputeStatus();
		showResult(result);
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
		navigateToNext(new PageResourceWizard(parameters));
	}

	private void editAsXmlPerformed(ResourceType resourceType) {
		PageParameters parameters = new PageParameters();
		parameters.add(PageDebugView.PARAM_OBJECT_ID, resourceType.getOid());
		parameters.add(PageDebugView.PARAM_OBJECT_TYPE, ResourceType.class.getSimpleName());
		navigateToNext(PageDebugView.class, parameters);
	}
}
