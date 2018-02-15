/* Copyright (c) 2010-2017 Evolveum
 *
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
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.component.dialog.*;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.TwoValueLinkPanel;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugButtonPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugConfDialogDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.session.ConfigurationStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/debugs", action = {
		@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL, label = "PageDebugList.auth.debugs.label", description = "PageDebugList.auth.debugs.description") })
public class PageDebugList extends PageAdminConfiguration {

	private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
	private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
	private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";

	private static final String OPERATION_LAXATIVE_DELETE = DOT_CLASS + "laxativeDelete";

	private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
	private static final String OPERATION_DELETE_SHADOWS = DOT_CLASS + "deleteShadows";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_ZIP_CHECK = "zipCheck";
	private static final String ID_TABLE = "table";
	private static final String ID_CHOICE_CONTAINER = "choiceContainer";
	private static final String ID_CHOICE = "choice";
	private static final String ID_EXPORT = "export";
	private static final String ID_EXPORT_ALL = "exportAll";
	private static final String ID_SEARCH_FORM = "searchForm";
	private static final String ID_RESOURCE = "resource";
	private static final String ID_TABLE_HEADER = "tableHeader";
	private static final String ID_SEARCH = "search";

	private static final Integer DELETE_LOG_INTERVAL = 50;

	// search form model;
	private IModel<DebugSearchDto> searchModel;
	// confirmation dialog model
	private IModel<DebugConfDialogDto> confDialogModel;
	private IModel<List<ObjectViewDto>> resourcesModel;

	public PageDebugList() {
		searchModel = new LoadableModel<DebugSearchDto>(false) {

			@Override
			protected DebugSearchDto load() {
				ConfigurationStorage storage = getSessionStorage().getConfiguration();

				DebugSearchDto dto = storage.getDebugSearchDto();
				if (dto == null) {
					dto = new DebugSearchDto();
					dto.setType(ObjectTypes.SYSTEM_CONFIGURATION);
					setupSearchDto(dto);
				}

				return dto;
			}
		};

		confDialogModel = new LoadableModel<DebugConfDialogDto>() {

			@Override
			protected DebugConfDialogDto load() {
				return new DebugConfDialogDto();
			}
		};

		resourcesModel = new LoadableModel<List<ObjectViewDto>>(false) {

			@Override
			protected List<ObjectViewDto> load() {
				return loadResources();
			}
		};

		initLayout();
	}

	private List<ObjectViewDto> loadResources() {
		List<ObjectViewDto> objects = new ArrayList<>();

		try {
			OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
			List<PrismObject<ResourceType>> list = WebModelServiceUtils.searchObjects(ResourceType.class,
					null, SelectorOptions.createCollection(GetOperationOptions.createRaw()), result, this,
					null);

			for (PrismObject obj : list) {
				ObjectViewDto dto = new ObjectViewDto(obj.getOid(), WebComponentUtil.getName(obj));
				objects.add(dto);
			}
		} catch (Exception ex) {
			// todo implement error handling
		}

		Collections.sort(objects, (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));

		return objects;
	}

	private void initLayout() {
		Form main = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		add(main);

		DebugSearchDto dto = searchModel.getObject();
		Class type = dto.getType().getClassDefinition();
		RepositoryObjectDataProvider provider = new RepositoryObjectDataProvider(this, type) {

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				ConfigurationStorage storage = getSessionStorage().getConfiguration();
				storage.setPaging(paging);
			}
		};
		DebugSearchDto search = searchModel.getObject();
		ObjectQuery query = search.getSearch().createObjectQuery(getPrismContext());
		provider.setQuery(createQuery(query));

		addOrReplaceTable(provider);

		PageDebugDownloadBehaviour ajaxDownloadBehavior = new PageDebugDownloadBehaviour();
		main.add(ajaxDownloadBehavior);
	}

	private void initDownload(AjaxRequestTarget target, Class<? extends ObjectType> type, ObjectQuery query) {
		List<PageDebugDownloadBehaviour> list = get(ID_MAIN_FORM)
				.getBehaviors(PageDebugDownloadBehaviour.class);
		PageDebugDownloadBehaviour downloadBehaviour = list.get(0);

		downloadBehaviour.setType(type);
		downloadBehaviour.setQuery(query);
		downloadBehaviour.setUseZip(hasToZip());
		downloadBehaviour.initiate(target);
	}

	private void addOrReplaceTable(RepositoryObjectDataProvider provider) {
		Form mainForm = (Form) get(ID_MAIN_FORM);

		BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, initColumns(provider.getType()),
				UserProfileStorage.TableId.CONF_DEBUG_LIST_PANEL,
				(int) getItemsPerPage(UserProfileStorage.TableId.CONF_DEBUG_LIST_PANEL)) {

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFragment(headerId, ID_TABLE_HEADER, PageDebugList.this, searchModel,
						resourcesModel);
			}

		};
		table.setOutputMarkupId(true);

		ConfigurationStorage storage = getSessionStorage().getConfiguration();
		table.setCurrentPage(storage.getPaging());

		mainForm.addOrReplace(table);
	}

	private List<IColumn> initColumns(final Class<? extends ObjectType> type) {
		List<IColumn> columns = new ArrayList<>();

		IColumn column = new CheckBoxHeaderColumn<ObjectType>();
		columns.add(column);

		column = new LinkColumn<DebugObjectItem>(createStringResource("pageDebugList.name"),
				DebugObjectItem.F_NAME, DebugObjectItem.F_NAME) {

			@Override
			public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
					final IModel<DebugObjectItem> rowModel) {

				TwoValueLinkPanel panel = new TwoValueLinkPanel(componentId,
						new AbstractReadOnlyModel<String>() {
							@Override
							public String getObject() {
								DebugObjectItem object = rowModel.getObject();
								if (object == null) {
									return null;
								}
								StringBuilder sb = new StringBuilder();
								sb.append(object.getName());
								if (object.getStatus() != null && object.getStatus() != OperationResultStatusType.SUCCESS
										&& object.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
									sb.append(" (");
									sb.append(object.getStatus());
									sb.append(")");
								}
								return sb.toString();
							}
						},
						new PropertyModel<String>(rowModel, DebugObjectItem.F_OID)) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						DebugObjectItem object = rowModel.getObject();
						objectEditPerformed(target, object.getOid(), type);
					}

					@Override
					public boolean isEnabled() {
						return rowModel.getObject().getOid() != null;
					}
				};

				cellItem.add(panel);
				cellItem.add(new AttributeModifier("class", "col-md-3"));

			}

		};

		columns.add(column);
		columns.add(new PropertyColumn(createStringResource("pageDebugList.description"),
				DebugObjectItem.F_DESCRIPTION));

		if (ShadowType.class.isAssignableFrom(type)) {
			columns.add(new PropertyColumn(createStringResource("pageDebugList.resourceName"),
					DebugObjectItem.F_RESOURCE_NAME));
			columns.add(new PropertyColumn(createStringResource("pageDebugList.resourceType"),
					DebugObjectItem.F_RESOURCE_TYPE));
		}

		column = new AbstractColumn<DebugObjectItem, String>(new Model(), null) {

			@Override
			public String getCssClass() {
				return "debug-list-buttons";
			}

			@Override
			public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
					IModel<DebugObjectItem> rowModel) {
				cellItem.add(new DebugButtonPanel<DebugObjectItem>(componentId, rowModel) {

					@Override
					public void deletePerformed(AjaxRequestTarget target, IModel<DebugObjectItem> model) {
						deleteSelected(target, model.getObject());
					}

					@Override
					public void exportPerformed(AjaxRequestTarget target, IModel<DebugObjectItem> model) {
						exportSelected(target, model.getObject());
					}
				});
			}
		};

		columns.add(column);

		column = new InlineMenuHeaderColumn<InlineMenuable>(initInlineMenu()) {

			@Override
			public void populateItem(Item<ICellPopulator<InlineMenuable>> cellItem, String componentId,
					IModel<InlineMenuable> rowModel) {
				// we don't need row inline menu
				cellItem.add(new Label(componentId));
			}
		};
		columns.add(column);

		return columns;
	}

	private List<InlineMenuItem> initInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportSelected"),
				true, new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						exportSelected(target, null);
					}
				}));

		headerMenuItems
				.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAllSelectedType"),
						true, new HeaderMenuAction(this) {

							@Override
							public void onSubmit(AjaxRequestTarget target, Form<?> form) {
								exportAllType(target);
							}
						}));

		headerMenuItems
				.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportShadowsOnResource"),
                        new Model(true), new AbstractReadOnlyModel<Boolean>() {

                    @Override
                    public Boolean getObject() {
                        DebugSearchDto dto = searchModel.getObject();
                        return ObjectTypes.SHADOW.equals(dto.getType());
                    }

                }, false, new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        exportAllShadowsOnResource(target);
                    }
                }));

		headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAll"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						exportAll(target);
					}
				}));

		headerMenuItems.add(new InlineMenuItem());

		headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteSelected"),
				true, new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						deleteSelected(target, null);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllType"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						deleteAllType(target);
					}
				}));

		headerMenuItems
				.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteShadowsOnResource"),
						new Model(true), new AbstractReadOnlyModel<Boolean>() {

							@Override
							public Boolean getObject() {
								DebugSearchDto dto = searchModel.getObject();
								return ObjectTypes.SHADOW.equals(dto.getType());
							}

						}, false, new HeaderMenuAction(this) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								deleteAllShadowsOnResource(target);
							}
						}));

		headerMenuItems.add(new InlineMenuItem());

		headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllIdentities"),
				true, new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						deleteAllIdentities(target);
					}
				}));

		return headerMenuItems;
	}

	private boolean hasToZip() {
		BoxedTablePanel table = (BoxedTablePanel) getListTable();
		SearchFragment header = (SearchFragment) table.getHeader();
		AjaxCheckBox zipCheck = header.getZipCheck();

		return zipCheck.getModelObject();
	}

	private Table getListTable() {
		return (Table) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	/**
	 * called when object type is changed, search panel will be refreshed
	 */
	private void listObjectsPerformed(AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();
		setupSearchDto(dto);

		Search search = dto.getSearch();
		ObjectQuery query = search.createObjectQuery(getPrismContext());

		listObjectsPerformed(query, target);
	}

	private void setupSearchDto(DebugSearchDto dto) {
		ObjectTypes type = dto.getType();
		Search search = SearchFactory.createSearch(type.getClassDefinition(), this);
		dto.setSearch(search);
	}

	private void listObjectsPerformed(ObjectQuery query, AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();
		ObjectTypes selected = dto.getType();

		RepositoryObjectDataProvider provider = getTableDataProvider();
		provider.setQuery(createQuery(query));

		if (selected != null) {
			provider.setType(selected.getClassDefinition());
			addOrReplaceTable(provider);
		}

		// save object type category to session storage, used by back button
		ConfigurationStorage storage = getSessionStorage().getConfiguration();
		storage.setDebugSearchDto(dto);

		Table table = getListTable();
		target.add((Component) table);
	}

	private ObjectQuery createQuery(ObjectQuery searchQuery) {
		DebugSearchDto dto = searchModel.getObject();

		List<ObjectFilter> filters = new ArrayList<>();
		if (ObjectTypes.SHADOW.equals(dto.getType()) && dto.getResource() != null) {
			String oid = dto.getResource().getOid();
			ObjectFilter objectFilter = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
					.item(ShadowType.F_RESOURCE_REF).ref(oid)
					.buildFilter();
			filters.add(objectFilter);
		}

		if (searchQuery != null && searchQuery.getFilter() != null) {
			filters.add(searchQuery.getFilter());
		}

		if (filters.isEmpty()) {
			return null;
		}

		ObjectFilter filter = filters.size() > 1 ? AndFilter.createAnd(filters) : filters.get(0);
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);

		return query;
	}

	private void objectEditPerformed(AjaxRequestTarget target, String oid, Class<? extends ObjectType> type) {
		PageParameters parameters = new PageParameters();
		parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
		parameters.add(PageDebugView.PARAM_OBJECT_TYPE, type.getSimpleName());
		navigateToNext(PageDebugView.class, parameters);
	}

	private RepositoryObjectDataProvider getTableDataProvider() {
		Table tablePanel = getListTable();
		DataTable table = tablePanel.getDataTable();
		return (RepositoryObjectDataProvider) table.getDataProvider();
	}

	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				DebugConfDialogDto dto = confDialogModel.getObject();

				switch (dto.getOperation()) {
					case DELETE_ALL_TYPE:
						String key = ObjectTypeGuiDescriptor.getDescriptor(dto.getType())
								.getLocalizationKey();
						String type = createStringResource(key).getString();
						return createStringResource("pageDebugList.message.deleteAllType", type).getString();
					case DELETE_SELECTED:
						List<DebugObjectItem> selectedList = dto.getObjects();

						if (selectedList.size() > 1) {
							return createStringResource("pageDebugList.message.deleteSelectedConfirm",
									selectedList.size()).getString();
						}

						DebugObjectItem selectedItem = selectedList.get(0);
						return createStringResource("pageDebugList.message.deleteObjectConfirm",
								selectedItem.getName()).getString();
					case DELETE_RESOURCE_SHADOWS:
						DebugSearchDto search = searchModel.getObject();
						return createStringResource("pageDebugList.messsage.deleteAllResourceShadows",
								search.getResource().getName()).getString();
				}

				return "";
			}
		};
	}

	private void deleteAllIdentitiesConfirmed(AjaxRequestTarget target, DeleteAllDto dto) {
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
		GetOperationOptions opt = GetOperationOptions.createRaw();
		options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

		OperationResult result = new OperationResult(OPERATION_LAXATIVE_DELETE);
		String taskOid = null;
		try {
			if (dto.getDeleteUsers()) {
				ObjectQuery query = createDeleteAllUsersQuery();
				taskOid = deleteObjectsAsync(UserType.COMPLEX_TYPE, query, true, "Delete all users", result);
			}
			if (dto.getDeleteOrgs()) {
				taskOid = deleteObjectsAsync(OrgType.COMPLEX_TYPE, null, true, "Delete all orgs", result);
			}
			if (dto.getDeleteAccountShadow()) {
				taskOid = deleteAllShadowsConfirmed(result, true);
			}
			if (dto.getDeleteNonAccountShadow()) {
				taskOid = deleteAllShadowsConfirmed(result, false);
			}
		} catch (Exception ex) {
			result.computeStatus(getString("pageDebugList.message.laxativeProblem"));
			LoggingUtils.logUnexpectedException(LOGGER, getString("pageDebugList.message.laxativeProblem"), ex);
		}

		if (taskOid != null) {
			PageParameters parameters = new PageParameters();
			parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
			navigateToNext(PageTaskEdit.class, parameters);
		} else {
			navigateToNext(PageTasks.class);
		}
		target.add(getFeedbackPanel());

		result.recomputeStatus();
		showResult(result);
	}

	private ObjectQuery createDeleteAllUsersQuery() {
		InOidFilter inOid = InOidFilter.createInOid(SystemObjectsType.USER_ADMINISTRATOR.value());
		NotFilter not = new NotFilter(inOid);

		return ObjectQuery.createObjectQuery(not);
	}

	private String deleteAllShadowsConfirmed(OperationResult result, boolean deleteAccountShadows)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

		ObjectFilter kindFilter = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
				.item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
				.buildFilter();

		String taskName;
		ObjectQuery query;
		if (deleteAccountShadows) {
			taskName = "Delete all account shadows";
			query = ObjectQuery.createObjectQuery(kindFilter);
		} else {
			taskName = "Delete all non-account shadows";
			query = ObjectQuery.createObjectQuery(NotFilter.createNot(kindFilter));
		}

		return deleteObjectsAsync(ShadowType.COMPLEX_TYPE, query, true, taskName, result);

	}

	private void exportSelected(AjaxRequestTarget target, DebugObjectItem item) {
		List<DebugObjectItem> selected = getSelectedData(target, item);
		if (selected.isEmpty()) {
			return;
		}

		List<String> oids = new ArrayList<>();
		for (DebugObjectItem dItem : selected) {
			oids.add(dItem.getOid());
		}

		ObjectFilter filter = InOidFilter.createInOid(oids);

		DebugSearchDto searchDto = searchModel.getObject();
		initDownload(target, searchDto.getType().getClassDefinition(), ObjectQuery.createObjectQuery(filter));
	}

	private void exportAllType(AjaxRequestTarget target) {
		DebugSearchDto searchDto = searchModel.getObject();
		initDownload(target, searchDto.getType().getClassDefinition(), null);
	}

	private void exportAll(AjaxRequestTarget target) {
		initDownload(target, ObjectType.class, null);
	}

	private void deleteAllType(AjaxRequestTarget target) {
		DebugSearchDto searchDto = searchModel.getObject();
		DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_ALL_TYPE, null,
				searchDto.getType().getClassDefinition());
		confDialogModel.setObject(dto);

		showMainPopup(getDeleteConfirmationPanel(), target);
	}

	private List<DebugObjectItem> getSelectedData(AjaxRequestTarget target, DebugObjectItem item) {
		List<DebugObjectItem> items;
		if (item != null) {
			items = new ArrayList<>();
			items.add(item);
			return items;
		}

		items = WebComponentUtil.getSelectedData(getListTable());
		if (items.isEmpty()) {
			warn(getString("pageDebugList.message.nothingSelected"));
			target.add(getFeedbackPanel());
		}

		return items;
	}

	private void deleteSelected(AjaxRequestTarget target, DebugObjectItem item) {
		List<DebugObjectItem> selected = getSelectedData(target, item);
		if (selected.isEmpty()) {
			return;
		}

		DebugSearchDto searchDto = searchModel.getObject();
		DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_SELECTED,
				selected, searchDto.getType().getClassDefinition());
		confDialogModel.setObject(dto);

		showMainPopup(getDeleteConfirmationPanel(), target);
	}

	private void deleteAllIdentities(AjaxRequestTarget target) {
        DeleteAllPanel dialog = new DeleteAllPanel(getMainPopupBodyId()){
            @Override
			public void yesPerformed(AjaxRequestTarget target) {
				hideMainPopup(target);
				deleteAllIdentitiesConfirmed(target, getModel().getObject());
			}
        };
        showMainPopup(dialog, target);
	}

	private void deleteAllTypeConfirmed(AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();

		LOGGER.debug("Deleting all of type {}", dto.getType());

		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
		String taskOid = null;
		try {
			ObjectQuery query = null;
			if (ObjectTypes.USER.equals(dto.getType())) {
				query = createDeleteAllUsersQuery();
			}

			QName type = dto.getType().getTypeQName();

			taskOid = deleteObjectsAsync(type, query, true, "Delete all of type " + type.getLocalPart(),
					result);

			info(getString("pageDebugList.messsage.deleteAllOfType", dto.getType()));
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't delete objects of type " + dto.getType(), ex);

			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete objects of type " + dto.getType(), ex);
		}

		showResult(result);
		if (taskOid != null) {
			PageParameters parameters = new PageParameters();
			parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
			navigateToNext(PageTaskEdit.class, parameters);
		} else {
			navigateToNext(PageTasks.class);
		}
		target.add(getFeedbackPanel());
	}

	private void deleteSelectedConfirmed(AjaxRequestTarget target, List<DebugObjectItem> items) {
		DebugConfDialogDto dto = confDialogModel.getObject();

		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
		for (DebugObjectItem bean : items) {
			WebModelServiceUtils.deleteObject(dto.getType(), bean.getOid(), ModelExecuteOptions.createRaw(),
					result, this);
		}
		result.computeStatusIfUnknown();

		RepositoryObjectDataProvider provider = getTableDataProvider();
		provider.clearCache();

		showResult(result);
		target.add((Component) getListTable());
		target.add(getFeedbackPanel());
	}

	private void deleteAllShadowsOnResource(AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();
		if (dto.getResource() == null) {
			error(getString("pageDebugList.message.resourceNotSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		LOGGER.debug("Displaying delete all shadows on resource {} confirmation dialog",
				dto.getResource().getName());

		DebugConfDialogDto dialogDto = new DebugConfDialogDto(
				DebugConfDialogDto.Operation.DELETE_RESOURCE_SHADOWS, null, null);
		confDialogModel.setObject(dialogDto);

		showMainPopup(getDeleteConfirmationPanel(), target);
	}

	private void exportAllShadowsOnResource(AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();
		if (dto.getResource() == null) {
			error(getString("pageDebugList.message.resourceNotSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		ObjectQuery objectQuery = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
				.item(ShadowType.F_RESOURCE_REF).ref(dto.getResource().getOid())
				.build();
		initDownload(target, dto.getType().getClassDefinition(), objectQuery);
	}

	private Popupable getDeleteConfirmationPanel() {
		return new ConfirmationPanel(getMainPopupBodyId(), createDeleteConfirmString()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				ModalWindow modalWindow = findParent(ModalWindow.class);
				if (modalWindow != null) {
					modalWindow.close(target);
					DebugConfDialogDto dto = confDialogModel.getObject();
					switch (dto.getOperation()) {
						case DELETE_ALL_TYPE:
							deleteAllTypeConfirmed(target);
							break;
						case DELETE_SELECTED:
							deleteSelectedConfirmed(target, dto.getObjects());
							break;
						case DELETE_RESOURCE_SHADOWS:
							deleteAllShadowsOnResourceConfirmed(target);
							break;
					}
				}
			}

			@Override
			public boolean getLabelEscapeModelStrings() {
				return false;
			}
		};
	}

	private void deleteAllShadowsOnResourceConfirmed(AjaxRequestTarget target) {
		DebugSearchDto dto = searchModel.getObject();
		String resourceOid = dto.getResource().getOid();

		LOGGER.debug("Deleting shadows on resource {}", resourceOid);

		OperationResult result = new OperationResult(OPERATION_DELETE_SHADOWS);
		String taskOid = null;
		try {
			ObjectQuery objectQuery = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
					.item(ShadowType.F_RESOURCE_REF).ref(dto.getResource().getOid())
					.build();

			QName type = ShadowType.COMPLEX_TYPE;

			taskOid = deleteObjectsAsync(type, objectQuery, true,
					"Delete shadows on " + dto.getResource().getName(), result);

			info(getString("pageDebugList.messsage.deleteAllShadowsStarted", dto.getResource().getName()));
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't delete shadows.", ex);

			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete shadows", ex);
		}

		showResult(result);
		if (taskOid != null) {
			PageParameters parameters = new PageParameters();
			parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
			navigateToNext(PageTaskEdit.class, parameters);
		} else {
			navigateToNext(PageTasks.class);
		}
		target.add(getFeedbackPanel());
	}

	private String deleteObjectsAsync(QName type, ObjectQuery objectQuery, boolean raw, String taskName,
			OperationResult result)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

		Task task = createSimpleTask(result.getOperation());
		task.setHandlerUri(ModelPublicConstants.DELETE_TASK_HANDLER_URI);

		if (objectQuery == null) {
			objectQuery = new ObjectQuery();
		}

		QueryType query = QueryJaxbConvertor.createQueryType(objectQuery, getPrismContext());

		PrismPropertyDefinition queryDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.COMPLEX_TYPE, getPrismContext());
		PrismProperty<QueryType> queryProp = queryDef.instantiate();
		queryProp.setRealValue(query);
		task.setExtensionProperty(queryProp);

		PrismPropertyDefinition typeDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, DOMUtil.XSD_QNAME, getPrismContext());
		PrismProperty<QName> typeProp = typeDef.instantiate();
		typeProp.setRealValue(type);
		task.setExtensionProperty(typeProp);

		PrismPropertyDefinition rawDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OPTION_RAW, DOMUtil.XSD_BOOLEAN, getPrismContext());
		PrismProperty<QName> rawProp = rawDef.instantiate();
		rawProp.setRealValue(raw);
		task.setExtensionProperty(rawProp);

		task.setName(taskName);
		task.savePendingModifications(result);

		TaskManager taskManager = getTaskManager();
		taskManager.switchToBackground(task, result);
		result.setBackgroundTaskOid(task.getOid());
		return task.getOid();
	}

	private static class SearchFragment extends Fragment {

		public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
				IModel<DebugSearchDto> model, IModel<List<ObjectViewDto>> resourcesModel) {
			super(id, markupId, markupProvider, model);

			initLayout(resourcesModel);
		}

		private void initLayout(IModel<List<ObjectViewDto>> resourcesModel) {
			final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
			add(searchForm);
			searchForm.setOutputMarkupId(true);

			final IModel<DebugSearchDto> model = (IModel) getDefaultModel();

			EnumChoiceRenderer<ObjectTypes> renderer = new EnumChoiceRenderer<ObjectTypes>() {

				protected String resourceKey(ObjectTypes object) {
					ObjectTypeGuiDescriptor descr = ObjectTypeGuiDescriptor.getDescriptor(object);
					String key = descr != null ? descr.getLocalizationKey()
							: ObjectTypeGuiDescriptor.ERROR_LOCALIZATION_KEY;
					return key;

				}
			};

			WebMarkupContainer choiceContainer = new WebMarkupContainer(ID_CHOICE_CONTAINER);
			choiceContainer.setOutputMarkupId(true);
			searchForm.add(choiceContainer);

			DropDownChoice choice = new DropDownChoice(ID_CHOICE,
					new PropertyModel(model, DebugSearchDto.F_TYPE), createChoiceModel(renderer), renderer);
			choice.add(getDropDownStyleAppender());
			choiceContainer.add(choice);
			choice.add(new OnChangeAjaxBehavior() {

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					PageDebugList page = (PageDebugList) getPage();
					page.listObjectsPerformed(target);
				}
			});

			DropDownChoice resource = new DropDownChoice(ID_RESOURCE,
					new PropertyModel(model, DebugSearchDto.F_RESOURCE), resourcesModel,
					createResourceRenderer());
			resource.add(getDropDownStyleAppender());
			resource.setNullValid(true);
			resource.add(new AjaxFormComponentUpdatingBehavior("blur") {

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					// nothing to do, it's here just to update model
				}
			});
			resource.add(new OnChangeAjaxBehavior() {

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					PageDebugList page = (PageDebugList) getPage();
					page.listObjectsPerformed(target);
				}
			});
			resource.add(new VisibleEnableBehaviour() {

				@Override
				public boolean isVisible() {
					DebugSearchDto dto = model.getObject();
					return ObjectTypes.SHADOW.equals(dto.getType());
				}
			});
			searchForm.add(resource);

			AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<>(false)) {

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
				}
			};
			add(zipCheck);

			SearchPanel search = new SearchPanel(ID_SEARCH,
					new PropertyModel<Search>(model, DebugSearchDto.F_SEARCH)) {

				@Override
				public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
					PageDebugList page = (PageDebugList) getPage();
					page.listObjectsPerformed(query, target);
				}
			};
			searchForm.add(search);
		}

		public AjaxCheckBox getZipCheck() {
			return (AjaxCheckBox) get(ID_ZIP_CHECK);
		}

		private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
			return new LoadableModel<List<ObjectTypes>>(false) {

				@Override
				protected List<ObjectTypes> load() {
					List<ObjectTypes> choices = new ArrayList<>();

					Collections.addAll(choices, ObjectTypes.values());
					choices.remove(ObjectTypes.OBJECT);

					choices.sort((o1, o2) -> {
						String str1 = (String) renderer.getDisplayValue(o1);
						String str2 = (String) renderer.getDisplayValue(o2);
						return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
					});

					return choices;
				}
			};
		}

		private IChoiceRenderer<ObjectViewDto> createResourceRenderer() {
			return new ChoiceableChoiceRenderer<ObjectViewDto>() {

				@Override
				public Object getDisplayValue(ObjectViewDto object) {
					if (object == null) {
						return getString("pageDebugList.resource");
					}
					return object.getName();
				}

			};
		}

		private AttributeAppender getDropDownStyleAppender(){
			return new AttributeAppender("style", new LoadableModel<String>() {
				private static final long serialVersionUID = 1L;
				@Override
				public String load(){
					PageDebugList page = (PageDebugList) getPage();
					return page.searchModel.getObject().getSearch().isFullTextSearchEnabled() ?
							"margin-top: -15px;" : "";
				}
			});
		}
	}
}
