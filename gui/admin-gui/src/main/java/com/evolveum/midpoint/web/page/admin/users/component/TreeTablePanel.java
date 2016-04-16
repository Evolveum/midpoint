/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeDialogPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Used as a main component of the Org tree page.
 * 
 * todo create function computeHeight() in midpoint.js, update height properly
 * when in "mobile" mode... [lazyman] todo implement midpoint theme for tree
 * [lazyman]
 *
 * @author lazyman
 */
public class TreeTablePanel extends AbstractTreeTablePanel {

	private static Map<Class, Class> objectDetailsMap;

	static {
		objectDetailsMap = new HashMap<>();
		objectDetailsMap.put(UserType.class, PageUser.class);
		objectDetailsMap.put(OrgType.class, PageOrgUnit.class);
		objectDetailsMap.put(RoleType.class, PageRole.class);
		objectDetailsMap.put(ServiceType.class, PageService.class);
		objectDetailsMap.put(ResourceType.class, PageResource.class);
		objectDetailsMap.put(TaskType.class, PageTaskEdit.class);
	}

	private PageBase parentPage;

	@Override
	public PageBase getPageBase() {
		return parentPage;
	}

	private static final String ID_MANAGER_SUMMARY = "managerSummary";
	private static final String ID_REMOVE_MANAGER = "removeManager";
	private static final String ID_EDIT_MANAGER = "editManager";
	
	private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

	public TreeTablePanel(String id, IModel<String> rootOid, PageBase parentPage) {
		super(id, rootOid);

		selected = new LoadableModel<OrgTreeDto>() {
			@Override
			protected OrgTreeDto load() {
				TabbedPanel currentTabbedPanel = null;
				MidPointAuthWebSession session = TreeTablePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				if (getTree().findParent(TabbedPanel.class) != null) {
					currentTabbedPanel = getTree().findParent(TabbedPanel.class);
					int tabId = currentTabbedPanel.getSelectedTab();
					if (storage.getUsers().getSelectedTabId() != -1
							&& tabId != storage.getUsers().getSelectedTabId()) {
						storage.getUsers().setSelectedItem(null);
					}
				}
				if (storage.getUsers().getSelectedItem() != null) {
					return storage.getUsers().getSelectedItem();
				} else {
					return getRootFromProvider();
				}
			}
		};

		this.parentPage = parentPage;
		setParent(parentPage);
		initLayout();
	}

	protected void initLayout() {
//		add(new ConfirmationDialog(ID_CONFIRM_DELETE_POPUP,
//				createStringResource("TreeTablePanel.dialog.title.confirmDelete"),
//				createDeleteConfirmString()) {
//
//			@Override
//			public void yesPerformed(AjaxRequestTarget target) {
//				close(target);
//
//				switch (getConfirmType()) {
//					case CONFIRM_DELETE:
//					case CONFIRM_DELETE_MANAGER:
//					case CONFIRM_DELETE_MEMBER:
//						deleteConfirmedPerformed(target);
//						break;
//					case CONFIRM_DELETE_ROOT:
//						deleteRootConfirmedPerformed(target);
//						break;
//				}
//
//			}
//		});

		add(new OrgUnitBrowser(ID_MOVE_POPUP) {

			@Override
			protected void createRootPerformed(AjaxRequestTarget target) {
				moveConfirmedPerformed(target, null, null, Operation.MOVE);
			}

			@Override
			protected void rowSelected(AjaxRequestTarget target, IModel<OrgTableDto> row,
					Operation operation) {
				moveConfirmedPerformed(target, selected.getObject(), row.getObject(), operation);
			}

			@Override
			public ObjectQuery createRootQuery() {
				ArrayList<String> oids = new ArrayList<>();
				ObjectQuery query = new ObjectQuery();

				if (isMovingRoot() && getRootFromProvider() != null) {
					oids.add(getRootFromProvider().getOid());
				}

				if (oids.isEmpty()) {
					return null;
				}

				ObjectFilter oidFilter = InOidFilter.createInOid(oids);
				query.setFilter(NotFilter.createNot(oidFilter));

				return query;
			}
		});

		add(new OrgUnitAddDeletePopup(ID_ADD_DELETE_POPUP) {

			@Override
			public void addPerformed(AjaxRequestTarget target, OrgType selected) {
				addOrgUnitToUserPerformed(target, selected);
			}

			@Override
			public void removePerformed(AjaxRequestTarget target, OrgType selected) {
				removeOrgUnitToUserPerformed(target, selected);
			}

			@Override
			public ObjectQuery getAddProviderQuery() {
				return null;
			}

			@Override
			public ObjectQuery getRemoveProviderQuery() {
				return null;
			}
		});

		WebMarkupContainer treeHeader = new WebMarkupContainer(ID_TREE_HEADER);
		treeHeader.setOutputMarkupId(true);
		add(treeHeader);

		InlineMenu treeMenu = new InlineMenu(ID_TREE_MENU, new Model<>((Serializable) createTreeMenu()));
		treeHeader.add(treeMenu);

		ISortableTreeProvider provider = new OrgTreeProvider(this, getModel()) {
			@Override
			protected List<InlineMenuItem> createInlineMenuItems() {
				// TODO Auto-generated method stub
				return createTreeChildrenMenu();
			}
		};
		List<IColumn<OrgTreeDto, String>> columns = new ArrayList<>();
		columns.add(new TreeColumn<OrgTreeDto, String>(createStringResource("TreeTablePanel.hierarchy")));
		columns.add(new InlineMenuHeaderColumn(createTreeChildrenMenu()));
		
		WebMarkupContainer treeContainer = new WebMarkupContainer(ID_TREE_CONTAINER) {

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);

				// method computes height based on document.innerHeight() -
				// screen height;
				response.render(OnDomReadyHeaderItem.forScript("updateHeight('" + getMarkupId() + "', ['#"
						+ TreeTablePanel.this.get(ID_FORM).getMarkupId() + "'], ['#"
						+ TreeTablePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])"));
			}
		};
		add(treeContainer);

		TableTree<OrgTreeDto, String> tree = new TableTree<OrgTreeDto, String>(ID_TREE, columns, provider,
				Integer.MAX_VALUE, new TreeStateModel(this, provider)) {

			@Override
			protected Component newContentComponent(String id, IModel<OrgTreeDto> model) {
				return new SelectableFolderContent(id, this, model, selected) {

					@Override
					protected void onClick(AjaxRequestTarget target) {
						super.onClick(target);

						MidPointAuthWebSession session = TreeTablePanel.this.getSession();
						SessionStorage storage = session.getSessionStorage();
						storage.getUsers().setSelectedItem(selected.getObject());

						selectTreeItemPerformed(target);
					}
				};
			}

			@Override
			protected Item<OrgTreeDto> newRowItem(String id, int index, final IModel<OrgTreeDto> model) {
				Item<OrgTreeDto> item = super.newRowItem(id, index, model);
				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						OrgTreeDto itemObject = model.getObject();
						if (itemObject != null && itemObject.equals(selected.getObject())) {
							return "success";
						}

						return null;
					}
				}));
				return item;
			}

			@Override
			public void collapse(OrgTreeDto collapsedItem) {
				super.collapse(collapsedItem);
				MidPointAuthWebSession session = TreeTablePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				Set<OrgTreeDto> items = storage.getUsers().getExpandedItems();
				if (items != null && items.contains(collapsedItem)) {
					items.remove(collapsedItem);
				}
				storage.getUsers().setExpandedItems((TreeStateSet) items);
				storage.getUsers().setCollapsedItem(collapsedItem);
			}

			@Override
			protected void onModelChanged() {
				super.onModelChanged();

				Set<OrgTreeDto> items = getModelObject();

				MidPointAuthWebSession session = TreeTablePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				storage.getUsers().setExpandedItems((TreeStateSet<OrgTreeDto>) items);
			}
		};
		tree.getTable().add(AttributeModifier.replace("class", "table table-striped table-condensed"));
		tree.add(new WindowsTheme());
		// tree.add(AttributeModifier.replace("class", "tree-midpoint"));
		treeContainer.add(tree);

		initSearch();
		initTables();

	}
	
	private void detailsPerformed(AjaxRequestTarget targer, ObjectType object) {
		Class responsePage = objectDetailsMap.get(object.getClass());
		if (responsePage == null) {
			error("Could not find proper response page");
			throw new RestartResponseException(getPageBase());
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
		setResponsePage(responsePage, parameters);
	}

	private void initTables() {
		Form form = new Form(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);

		WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
		memberContainer.setOutputMarkupId(true);
		memberContainer.setOutputMarkupPlaceholderTag(true);
		form.add(memberContainer);

		MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
				ID_MEMBER_TABLE, ObjectType.class, null, parentPage) {

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
				detailsPerformed(target, object);

			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				// TODO Auto-generated method stub

			}

			@Override
			protected List<IColumn<SelectableBean<ObjectType>, String>> createColumns() {
				return createMembersColumns();
			}

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				// TODO Auto-generated method stub
				return new ArrayList<>();
			}

			@Override
			protected ObjectQuery createContentQuery() {
				ObjectQuery q = super.createContentQuery();

				ObjectQuery members = createMemberQuery(null);

				List<ObjectFilter> filters = new ArrayList<>();

				if (q != null && q.getFilter() != null) {
					filters.add(q.getFilter());
				}

				if (members != null && members.getFilter() != null) {
					filters.add(members.getFilter());
				}

				if (filters.size() == 1) {
					return ObjectQuery.createObjectQuery(filters.iterator().next());
				}

				return ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
			}
		};
		childrenListPanel.setOutputMarkupId(true);
		memberContainer.add(childrenListPanel);
		// ObjectSummaryPanel<UserType> sumaryPanel = new ObjectSummaryPanel();

		WebMarkupContainer managerContainer = createManagerContainer();
		form.addOrReplace(managerContainer);
	}

	private WebMarkupContainer createManagerContainer() {
		WebMarkupContainer managerContainer = new WebMarkupContainer(ID_CONTAINER_MANAGER);
		managerContainer.setOutputMarkupId(true);
		managerContainer.setOutputMarkupPlaceholderTag(true);

		RepeatingView view = new RepeatingView(ID_MANAGER_TABLE);
		view.setOutputMarkupId(true);
		ObjectQuery managersQuery = createMemberQuery(SchemaConstants.ORG_MANAGER);

		OperationResult searchManagersResult = new OperationResult(OPERATION_SEARCH_MANAGERS);
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				FocusType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		List<PrismObject<FocusType>> managers = WebModelServiceUtils.searchObjects(FocusType.class,
				managersQuery, options, searchManagersResult, getPageBase());
		for (PrismObject<FocusType> manager : managers) {
			ObjectWrapper<FocusType> managerWrapper = ObjectWrapperUtil.createObjectWrapper(
					WebComponentUtil.getEffectiveName(manager, RoleType.F_DISPLAY_NAME), "", manager,
					ContainerStatus.MODIFYING, getPageBase());
			WebMarkupContainer managerMarkup = new WebMarkupContainer(view.newChildId());
			
			AjaxLink link = new AjaxLink(ID_EDIT_MANAGER) {
				@Override
				public void onClick(AjaxRequestTarget target) {
					FocusSummaryPanel<FocusType> summary = (FocusSummaryPanel<FocusType>) getParent().get(ID_MANAGER_SUMMARY);
					detailsPerformed(target, summary.getModelObject());
					
				}
			};
			if (manager.getCompileTimeClass().equals(UserType.class)) {
				managerMarkup.add(new UserSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(RoleType.class)) {
				managerMarkup.add(new RoleSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(OrgType.class)) {
				managerMarkup.add(new OrgSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(ServiceType.class)) {
				managerMarkup.add(new ServiceSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			}
			link.setOutputMarkupId(true);
			managerMarkup.setOutputMarkupId(true);
			managerMarkup.add(link);
			view.add(managerMarkup);
			
			AjaxButton removeManager = new AjaxButton(ID_REMOVE_MANAGER) {
				
				
				@Override
				public void onClick(AjaxRequestTarget target) {
					FocusSummaryPanel<FocusType> summary = (FocusSummaryPanel<FocusType>) getParent().get(ID_MANAGER_SUMMARY);
					removeManagerPerformed(summary.getModelObject(), target);
//					summary.setVisible(false);
//					get(ID_REMOVE_MANAGER).setVisible(false);
					getParent().setVisible(false);
					target.add(getParent());
					
				}
			};
			removeManager.setOutputMarkupId(true);
			managerMarkup.add(removeManager);
		}

		managerContainer.add(view);
		return managerContainer;
	}

	private List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
		List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<ObjectType>, String> column = new AbstractColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.fullName.displayName")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				if (object instanceof UserType) {
					cellItem.add(new Label(componentId,
							WebComponentUtil.getOrigStringFromPoly(((UserType) object).getFullName())));
				} else if (AbstractRoleType.class.isAssignableFrom(object.getClass())) {
					cellItem.add(new Label(componentId, WebComponentUtil
							.getOrigStringFromPoly(((AbstractRoleType) object).getDisplayName())));
				}

			}

		};

		column = new AbstractColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.identifier.description")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				if (object instanceof UserType) {
					cellItem.add(new Label(componentId, ((UserType) object).getEmailAddress()));
				} else if (AbstractRoleType.class.isAssignableFrom(object.getClass())) {
					cellItem.add(new Label(componentId, ((AbstractRoleType) object).getIdentifier()));
				} else {
					cellItem.add(new Label(componentId, object.getDescription()));
				}

			}

		};

		columns.add(new InlineMenuHeaderColumn(createMembersHeaderInlineMenu()));
		return columns;
	}

	private List<InlineMenuItem> createTreeMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.collapseAll"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						collapseAllPerformed(target);
					}
				});
		items.add(item);
		item = new InlineMenuItem(createStringResource("TreeTablePanel.expandAll"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						expandAllPerformed(target);
					}
				});
		items.add(item);
		items.add(new InlineMenuItem());
		item = new InlineMenuItem(createStringResource("TreeTablePanel.moveRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						moveRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.deleteRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.recomputeRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.editRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						editRootPerformed(null, target);
					}
				});
		items.add(item);

		return items;
	}
	
	private List<InlineMenuItem> createTreeChildrenMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		
		InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.move"),
				new ColumnMenuAction<OrgTreeDto>() {


			@Override
					public void onClick(AjaxRequestTarget target) {
						moveRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.delete"),
				new ColumnMenuAction<OrgTreeDto>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.recompute"),
				new ColumnMenuAction<OrgTreeDto>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.edit"),
				new ColumnMenuAction<OrgTreeDto>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						editRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);
		
		item = new InlineMenuItem(createStringResource("TreeTablePanel.createChild"),
				new ColumnMenuAction<OrgTreeDto>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						initObjectForAdd(ObjectTypeUtil.createObjectRef(getRowModel().getObject().getObject()),OrgType.COMPLEX_TYPE, null, target);
//						editRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		return items;
	}

	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				ConfirmationDialog dialog = (ConfirmationDialog) TreeTablePanel.this
						.get(ID_CONFIRM_DELETE_POPUP);
				switch (dialog.getConfirmType()) {
					case CONFIRM_DELETE:
						return createStringResource("TreeTablePanel.message.deleteObjectConfirm",
								WebComponentUtil.getSelectedData(getOrgChildTable()).size()).getString();
					case CONFIRM_DELETE_MANAGER:
						return createStringResource("TreeTablePanel.message.deleteObjectConfirm",
								WebComponentUtil.getSelectedData(getManagerTable()).size()).getString();
					case CONFIRM_DELETE_MEMBER:
						return createStringResource("TreeTablePanel.message.deleteObjectConfirm",
								getMemberTable().getSelectedObjects()).getString();
					case CONFIRM_DELETE_ROOT:
						OrgTreeDto dto = getRootFromProvider();

						return createStringResource("TreeTablePanel.message.deleteRootConfirm", dto.getName(),
								dto.getDisplayName()).getString();
				}
				return null;
			}
		};
	}

	// private List<IColumn<OrgTableDto, String>> createChildTableColumns() {
	// List<IColumn<OrgTableDto, String>> columns = new ArrayList<>();
	//
	// columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
	// columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {
	//
	// @Override
	// protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// ObjectTypeGuiDescriptor guiDescriptor =
	// ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
	//
	// String icon = guiDescriptor != null ? guiDescriptor.getIcon()
	// : ObjectTypeGuiDescriptor.ERROR_ICON;
	//
	// return new Model<>(icon);
	// }
	// });
	//
	// columns.add(new
	// LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"),
	// OrgTableDto.F_NAME,
	// "name") {
	//
	// @Override
	// public boolean isEnabled(IModel<OrgTableDto> rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// return UserType.class.equals(dto.getType()) ||
	// OrgType.class.equals(dto.getType());
	// }
	//
	// @Override
	// public void onClick(AjaxRequestTarget target, IModel<OrgTableDto>
	// rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// PageParameters parameters = new PageParameters();
	// parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
	// getSession().getSessionStorage().setPreviousPage(PageOrgTree.class);
	// setResponsePage(PageOrgUnit.class, parameters);
	// }
	// });
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("OrgType.displayName"),
	// OrgTableDto.F_DISPLAY_NAME));
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("OrgType.identifier"),
	// OrgTableDto.F_IDENTIFIER));
	// columns.add(new InlineMenuHeaderColumn(initOrgChildInlineMenu()));
	//
	// return columns;
	// }

	// private List<IColumn<OrgTableDto, String>> createUserTableColumns(boolean
	// isManagerTable) {
	// List<IColumn<OrgTableDto, String>> columns = new ArrayList<>();
	//
	// columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
	// columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {
	//
	// @Override
	// protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// OrgTreeDto selectedDto = selected.getObject();
	// String selectedOid = dto != null ? selectedDto.getOid() :
	// getModel().getObject();
	//
	// ObjectTypeGuiDescriptor guiDescriptor = null;
	// if (dto != null && dto.getRelation() == null) {
	// guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
	// } else {
	// if (dto != null) {
	// for (ObjectReferenceType parentOrgRef :
	// dto.getObject().getParentOrgRef()) {
	// if (parentOrgRef.getOid().equals(selectedOid)
	// && SchemaConstants.ORG_MANAGER.equals(parentOrgRef.getRelation())) {
	// guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getRelation());
	// String icon = guiDescriptor != null ? guiDescriptor.getIcon()
	// : ObjectTypeGuiDescriptor.ERROR_ICON;
	// return new Model<>(icon);
	// }
	// }
	//
	// guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
	// }
	// }
	//
	// String icon = guiDescriptor != null ? guiDescriptor.getIcon()
	// : ObjectTypeGuiDescriptor.ERROR_ICON;
	//
	// return new Model<>(icon);
	// }
	// });
	//
	// columns.add(new
	// LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"),
	// OrgTableDto.F_NAME,
	// "name") {
	//
	// @Override
	// public boolean isEnabled(IModel<OrgTableDto> rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// return UserType.class.equals(dto.getType()) ||
	// OrgType.class.equals(dto.getType());
	// }
	//
	// @Override
	// public void onClick(AjaxRequestTarget target, IModel<OrgTableDto>
	// rowModel) {
	// OrgTableDto dto = rowModel.getObject();
	// PageParameters parameters = new PageParameters();
	// parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
	// getSession().getSessionStorage().setPreviousPage(PageOrgTree.class);
	// setResponsePage(new PageUser(parameters, (PageBase) target.getPage()));
	// }
	// });
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("UserType.givenName"),
	// UserType.F_GIVEN_NAME.getLocalPart(), OrgTableDto.F_OBJECT +
	// ".givenName"));
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("UserType.familyName"),
	// UserType.F_FAMILY_NAME.getLocalPart(), OrgTableDto.F_OBJECT +
	// ".familyName"));
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("UserType.fullName"),
	// UserType.F_FULL_NAME.getLocalPart(), OrgTableDto.F_OBJECT +
	// ".fullName"));
	// columns.add(new PropertyColumn<OrgTableDto,
	// String>(createStringResource("UserType.emailAddress"),
	// null, OrgTableDto.F_OBJECT + ".emailAddress"));
	// columns.add(new InlineMenuHeaderColumn(
	// isManagerTable ? initOrgManagerInlineMenu() :
	// initOrgMemberInlineMenu()));
	//
	// return columns;
	// }

	private List<InlineMenuItem> createMembersHeaderInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createMember"),
				false, new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						createFocusMemberPerformed(null, target);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createManager"),
				false, new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						createFocusMemberPerformed(SchemaConstants.ORG_MANAGER, target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem());

		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addMembers"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						addMemberPerformed(null, target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addManagers"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						addMemberPerformed(SchemaConstants.ORG_MANAGER, target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem());
		
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.removeMembersSelected"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						 removeMembersPerformed(null, target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.removeMembersAll"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						removAllMembersPerformed(null, target);
					}
				}));
		
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersSelected"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeMembersPerformed(target);
					}
				}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersAll"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeAllMembersPerformed(target);
					}
				}));

		return headerMenuItems;
	}

	private void createFocusMemberPerformed(final QName relation, AjaxRequestTarget target) {

		ChooseFocusTypeDialogPanel chooseTypePopupContent = new ChooseFocusTypeDialogPanel(
				getPageBase().getMainPopupBodyId()) {

			protected void okPerformed(QName type, AjaxRequestTarget target) {
				initObjectForAdd(null, type, relation, target);

			};
		};

		getPageBase().showMainPopup(chooseTypePopupContent, new Model<String>("Choose type"), target, 300,
				200);

	}

	private void addMemberPerformed(final QName relation, AjaxRequestTarget target) {

		List<QName> types = new ArrayList<>(ObjectTypes.values().length);
		for (ObjectTypes t : ObjectTypes.values()) {
			types.add(t.getTypeQName());
		}
		FocusBrowserPanel<ObjectType> browser = new FocusBrowserPanel(getPageBase().getMainPopupBodyId(),
				UserType.class, types, true, getPageBase()) {

			@Override
			protected void addPerformed(AjaxRequestTarget target, QName type, List selected) {
				TreeTablePanel.this.addMembers(type, relation, selected, target);

			}
		};
		browser.setOutputMarkupId(true);

		getPageBase().showMainPopup(browser, new Model<String>("Select members"), target, 900, 700);

	}

	private boolean isFocus(QName type) {
		return FocusType.COMPLEX_TYPE.equals(type) || UserType.COMPLEX_TYPE.equals(type)
				|| RoleType.COMPLEX_TYPE.equals(type) || OrgType.COMPLEX_TYPE.equals(type)
				|| ServiceType.COMPLEX_TYPE.equals(type);
	}

	private AssignmentType createAssignmentToModify(QName type, QName relation) throws SchemaException {

		AssignmentType assignmentToModify = new AssignmentType();

		assignmentToModify.setTargetRef(createReference(relation));

		getPageBase().getPrismContext().adopt(assignmentToModify);

		return assignmentToModify;
	}

	private ObjectReferenceType createReference(QName relation) {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(selected.getObject().getObject());
		ref.setRelation(relation);
		return ref;
	}

	private Class qnameToClass(QName type) {
		return getPageBase().getPrismContext().getSchemaRegistry().determineCompileTimeClass(type);
	}

	private void addMembers(QName type, QName relation, List selected, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Add members");
		Task operationalTask = getPageBase().createSimpleTask("Add members");

		try {
			ObjectDelta delta = null;
			Class classType = qnameToClass(type);
			if (isFocus(type)) {

				delta = ObjectDelta.createModificationAddContainer(classType, "fakeOid",
						FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
						createAssignmentToModify(type, relation));
			} else {
				delta = ObjectDelta.createModificationAddReference(classType, "fakeOid",
						ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
						createReference(relation).asReferenceValue());
			}
			TaskType task = WebComponentUtil.createSingleRecurenceTask("Add member(s)", type,
					createQueryForAdd(selected), delta, TaskCategory.EXECUTE_CHANGES, getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {
			parentResult.recordFatalError("Failed to add members " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove members", e);
			getPageBase().showResult(parentResult);
		}

		target.add(getPageBase().getFeedbackPanel());

	}

	private void removeMembersPerformed(QName relation, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Remove members");
		Task operationalTask = getPageBase().createSimpleTask("Remove members");
		try {
//			Class classType = qnameToClass(type);

			ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(FocusType.class, "fakeOid",
					FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
					createAssignmentToModify(FocusType.COMPLEX_TYPE, relation));

			TaskType task = WebComponentUtil.createSingleRecurenceTask("Remove focus member(s)",
					FocusType.COMPLEX_TYPE, createQueryForFocusRemove(), delta, TaskCategory.EXECUTE_CHANGES,
					getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());

			delta = ObjectDelta.createModificationDeleteReference(ObjectType.class, "fakeOid",
					ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
					createReference(relation).asReferenceValue());

			task = WebComponentUtil.createSingleRecurenceTask("Remove non-focus member(s)",
					ObjectType.COMPLEX_TYPE, createQueryForNonFocusRemove(), delta,
					TaskCategory.EXECUTE_CHANGES, getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {

			parentResult.recordFatalError("Failed to remove members " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove members", e);
			getPageBase().showResult(parentResult);
		}
		target.add(getPageBase().getFeedbackPanel());
	}
	
	private void removeManagerPerformed(FocusType manager, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Remove manager");
		Task task = getPageBase().createSimpleTask("Remove manager");
		try {
//			Class classType = qnameToClass(type);

			ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(manager.asPrismObject().getCompileTimeClass(), manager.getOid(),
					FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
					createAssignmentToModify(manager.asPrismObject().getDefinition().getTypeName(), SchemaConstants.ORG_MANAGER));

			getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, parentResult);
			parentResult.computeStatus();
		} catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {

			parentResult.recordFatalError("Failed to remove manager " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove manager", e);
			getPageBase().showResult(parentResult);
		}
		target.add(getPageBase().getFeedbackPanel());
	}

	private void removAllMembersPerformed(QName relation, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Remove members");
		Task operationalTask = getPageBase().createSimpleTask("Remove members");
		try {
//			Class classType = qnameToClass(type);

			ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(FocusType.class, "fakeOid",
					FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
					createAssignmentToModify(FocusType.COMPLEX_TYPE, relation));

			TaskType task = WebComponentUtil.createSingleRecurenceTask("Remove focus member(s)",
					FocusType.COMPLEX_TYPE, createQueryForAllRemove(FocusType.COMPLEX_TYPE), delta,
					TaskCategory.EXECUTE_CHANGES, getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());

			delta = ObjectDelta.createModificationDeleteReference(ObjectType.class, "fakeOid",
					ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
					createReference(relation).asReferenceValue());

			task = WebComponentUtil.createSingleRecurenceTask("Remove non-focus member(s)",
					ObjectType.COMPLEX_TYPE, createQueryForAllRemove(null), delta,
					TaskCategory.EXECUTE_CHANGES, getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {

			parentResult.recordFatalError("Failed to remove members " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove members", e);
			getPageBase().showResult(parentResult);
		}
		target.add(getPageBase().getFeedbackPanel());
	}

	private void recomputeMembersPerformed(AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask("Recompute selected members");
		OperationResult parentResult = operationalTask.getResult();

		try {
			TaskType task = WebComponentUtil.createSingleRecurenceTask("Recompute member(s)",
					ObjectType.COMPLEX_TYPE, createQueryForRecompute(), null, TaskCategory.RECOMPUTATION,
					getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {
			parentResult.recordFatalError("Failed to remove members " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove members", e);
			target.add(getPageBase().getFeedbackPanel());
		}

		target.add(getPageBase().getFeedbackPanel());
	}

	private void recomputeAllMembersPerformed(AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask("Recompute all members");
		OperationResult parentResult = operationalTask.getResult();

		try {
			TaskType task = WebComponentUtil.createSingleRecurenceTask("Recompute member(s)",
					ObjectType.COMPLEX_TYPE, createQueryForRecomputeAll(), null, TaskCategory.RECOMPUTATION,
					getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {
			parentResult.recordFatalError("Failed to remove members " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove members", e);
			target.add(getPageBase().getFeedbackPanel());
		}

		target.add(getPageBase().getFeedbackPanel());
	}

	private ObjectQuery createQueryForAdd(List selected) {
		List<String> oids = new ArrayList<>();
		for (Object selectable : selected) {
			if (selectable instanceof ObjectType) {
				oids.add(((ObjectType) selectable).getOid());
			}

		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	private ObjectQuery createQueryForFocusRemove() {

		List<ObjectType> objects = getMemberTable().getSelectedObjects();
		List<String> oids = new ArrayList<>();
		for (ObjectType object : objects) {
			if (FocusType.class.isAssignableFrom(object.getClass())) {
				oids.add(object.getOid());
			}
		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	private ObjectQuery createQueryForNonFocusRemove() {

		List<ObjectType> objects = getMemberTable().getSelectedObjects();
		List<String> oids = new ArrayList<>();
		for (ObjectType object : objects) {
			if (!FocusType.class.isAssignableFrom(object.getClass())) {
				oids.add(object.getOid());
			}
		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	private ObjectQuery createQueryForAllRemove(QName type) {
		OrgType org = (OrgType) selected.getObject().getObject();
		if (type == null) {

			PrismReferenceDefinition def = org.asPrismObject().getDefinition()
					.findReferenceDefinition(UserType.F_PARENT_ORG_REF);
			ObjectFilter orgFilter = RefFilter.createReferenceEqual(new ItemPath(ObjectType.F_PARENT_ORG_REF),
					def, ObjectTypeUtil.createObjectRef(org).asReferenceValue());
			TypeFilter typeFilter = TypeFilter.createType(FocusType.COMPLEX_TYPE, null);
			return ObjectQuery
					.createObjectQuery(AndFilter.createAnd(NotFilter.createNot(typeFilter), orgFilter));

		}

		if (FocusType.COMPLEX_TYPE.equals(type)) {
			PrismReferenceDefinition def = org.asPrismObject().getDefinition()
					.findReferenceDefinition(new ItemPath(OrgType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF));
			ObjectFilter orgASsignmentFilter = RefFilter.createReferenceEqual(
					new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), def,
					ObjectTypeUtil.createObjectRef(org).asReferenceValue());
			return ObjectQuery
					.createObjectQuery(TypeFilter.createType(FocusType.COMPLEX_TYPE, orgASsignmentFilter));
		}
		
		return null;

	}

	private ObjectQuery createQueryForRecompute() {

		List<ObjectType> objects = getMemberTable().getSelectedObjects();
		List<String> oids = new ArrayList<>();
		for (ObjectType object : objects) {
			oids.add(object.getOid());

		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	private ObjectQuery createQueryForRecomputeAll() {
		OrgType org = (OrgType) selected.getObject().getObject();
		PrismReferenceDefinition def = org.asPrismObject().getDefinition()
				.findReferenceDefinition(UserType.F_PARENT_ORG_REF);
		ObjectFilter orgFilter = RefFilter.createReferenceEqual(new ItemPath(ObjectType.F_PARENT_ORG_REF),
				def, ObjectTypeUtil.createObjectRef(org).asReferenceValue());

		return ObjectQuery.createObjectQuery(orgFilter);

	}

	private void initObjectForAdd(ObjectReferenceType parentOrgRef, QName type, QName relation, AjaxRequestTarget target) {
		TreeTablePanel.this.getPageBase().hideMainPopup(target);
		PrismContext prismContext = TreeTablePanel.this.getPageBase().getPrismContext();
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		PrismObject obj = def.instantiate();
		if (parentOrgRef == null){
		ObjectType org = selected.getObject().getObject();
		 parentOrgRef = ObjectTypeUtil.createObjectRef(org);
		parentOrgRef.setRelation(relation);
		}
		ObjectType objType = (ObjectType) obj.asObjectable();
		objType.getParentOrgRef().add(parentOrgRef);

		if (FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(parentOrgRef);
			((FocusType) objType).getAssignment().add(assignment);
		}

		Class newObjectPageClass = objectDetailsMap.get(obj.getCompileTimeClass());

		Constructor constructor = null;
		try {
			constructor = newObjectPageClass.getConstructor(PrismObject.class);

		} catch (NoSuchMethodException | SecurityException e) {
			throw new SystemException("Unable to locate constructor (PrismObject) in " + newObjectPageClass
					+ ": " + e.getMessage(), e);
		}

		PageBase page;
		try {
			page = (PageBase) constructor.newInstance(obj);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new SystemException("Error instantiating " + newObjectPageClass + ": " + e.getMessage(), e);
		}

		setResponsePage(page);

	}



	private void addOrgUnitPerformed(AjaxRequestTarget target) {
		PrismObject<OrgType> object = addChildOrgUnitPerformed(target, new OrgType());
		if (object == null) {
			return;
		}
		PageOrgUnit next = new PageOrgUnit(object);
		setResponsePage(next);
	}

	private void addUserPerformed(AjaxRequestTarget target, boolean isUserManager) {
		PrismObject object = addUserPerformed(target, new UserType(), isUserManager);
		if (object == null) {
			return;
		}
		PageUser next = new PageUser(object);
		setResponsePage(next);
	}

	private PrismObject<OrgType> addChildOrgUnitPerformed(AjaxRequestTarget target, OrgType org) {
		PageBase page = getPageBase();
		try {
			ObjectReferenceType ref = WebComponentUtil.createObjectRef(selected.getObject().getOid(),
					selected.getObject().getName(), OrgType.COMPLEX_TYPE);
			org.getParentOrgRef().add(ref);
			AssignmentType newOrgAssignment = new AssignmentType();
			newOrgAssignment.setTargetRef(ref);
			org.getAssignment().add(newOrgAssignment);

			PrismContext context = page.getPrismContext();
			context.adopt(org);

			return org.asPrismContainer();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't create child org. unit with parent org. reference",
					ex);
			page.error(
					"Couldn't create child org. unit with parent org. reference, reason: " + ex.getMessage());

			target.add(page.getFeedbackPanel());
		}

		return null;
	}

	private PrismObject<UserType> addUserPerformed(AjaxRequestTarget target, UserType user,
			boolean isUserManager) {
		PageBase page = getPageBase();
		try {
			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(selected.getObject().getOid());
			ref.setType(OrgType.COMPLEX_TYPE);

			if (isUserManager) {
				ref.setRelation(SchemaConstants.ORG_MANAGER);
			}

			user.getParentOrgRef().add(ref.clone());

			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(ref);

			user.getAssignment().add(assignment);

			PrismContext context = page.getPrismContext();
			context.adopt(user);

			return user.asPrismContainer();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't create user with parent org. reference", ex);
			page.error("Couldn't create user with parent org. reference, reason: " + ex.getMessage());

			target.add(page.getFeedbackPanel());
		}

		return null;
	}

	/**
	 * This method check selection in table.
	 */
	private List<OrgTableDto> isAnythingSelected(TablePanel table, AjaxRequestTarget target) {
		List<OrgTableDto> objects = WebComponentUtil.getSelectedData(table);
		if (objects.isEmpty()) {
			warn(getString("TreeTablePanel.message.nothingSelected"));
			target.add(getPageBase().getFeedbackPanel());
		}

		return objects;
	}

	private void deletePerformed(TablePanel table, AjaxRequestTarget target) {
		List<OrgTableDto> objects = isAnythingSelected(table, target);
		if (objects.isEmpty()) {
			return;
		}

		ConfirmationDialog dialog = (ConfirmationDialog) get(ID_CONFIRM_DELETE_POPUP);
		if (table.getId().equals(ID_CHILD_TABLE)) {
			dialog.setConfirmType(CONFIRM_DELETE);
		} else if (table.getId().equals(ID_MANAGER_TABLE)) {
			dialog.setConfirmType(CONFIRM_DELETE_MANAGER);
		} else if (table.getId().equals(ID_MEMBER_TABLE)) {
			dialog.setConfirmType(CONFIRM_DELETE_MEMBER);
		}
		dialog.show(target);
	}

	private void deleteConfirmedPerformed(AjaxRequestTarget target) {

		List<ObjectType> objects = getMemberTable().getSelectedObjects();
		if (objects.isEmpty()) {
			warn("nothing selected");
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		PageBase page = getPageBase();
		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
		for (ObjectType object : objects) {
			OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
			WebModelServiceUtils.deleteObject(object.getClass(), object.getOid(), subResult, page);
			subResult.computeStatusIfUnknown();

			MidPointAuthWebSession session = getSession();
			SessionStorage storage = session.getSessionStorage();
			storage.getUsers().setExpandedItems(null);
		}
		result.computeStatusComposite();

		page.showResult(result);
		target.add(page.getFeedbackPanel());
		target.add(getTree());

		refreshTable(target);
	}

	private void movePerformed(TablePanel table, AjaxRequestTarget target,
			OrgUnitBrowser.Operation operation) {
		movePerformed(table, target, operation, null, false);
	}

	private void movePerformed(TablePanel table, AjaxRequestTarget target, OrgUnitBrowser.Operation operation,
			OrgTableDto selected, boolean movingRoot) {
		List<OrgTableDto> objects;
		if (selected == null) {
			objects = isAnythingSelected(table, target);
			if (objects.isEmpty()) {
				return;
			}
		} else {
			objects = new ArrayList<>();
			objects.add(selected);
		}

		OrgUnitBrowser dialog = (OrgUnitBrowser) get(ID_MOVE_POPUP);
		dialog.setMovingRoot(movingRoot);
		dialog.setOperation(operation);
		dialog.setSelectedObjects(objects);
		dialog.show(target);
	}

	private ObjectDelta createMoveDelta(PrismObject<OrgType> orgUnit, OrgTreeDto oldParent,
			OrgTableDto newParent, OrgUnitBrowser.Operation operation) {
		ObjectDelta delta = orgUnit.createDelta(ChangeType.MODIFY);
		PrismReferenceDefinition refDef = orgUnit.getDefinition()
				.findReferenceDefinition(OrgType.F_PARENT_ORG_REF);
		ReferenceDelta refDelta = delta.createReferenceModification(OrgType.F_PARENT_ORG_REF, refDef);
		PrismReferenceValue value;
		switch (operation) {
			case ADD:
				LOGGER.debug("Adding new parent {}", new Object[] { newParent });
				// adding parentRef newParent to orgUnit
				value = createPrismRefValue(newParent);
				refDelta.addValuesToAdd(value);
				break;
			case REMOVE:
				LOGGER.debug("Removing new parent {}", new Object[] { newParent });
				// removing parentRef newParent from orgUnit
				value = createPrismRefValue(newParent);
				refDelta.addValuesToDelete(value);
				break;
			case MOVE:
				if (oldParent == null && newParent == null) {
					LOGGER.debug("Moving to root");
					// moving orgUnit to root, removing all parentRefs
					PrismReference ref = orgUnit.findReference(OrgType.F_PARENT_ORG_REF);
					if (ref != null) {
						for (PrismReferenceValue val : ref.getValues()) {
							refDelta.addValuesToDelete(val.clone());
						}
					}
				} else {
					LOGGER.debug("Adding new parent {}, removing old parent {}",
							new Object[] { newParent, oldParent });
					// moving from old to new, removing oldParent adding
					// newParent refs
					value = createPrismRefValue(newParent);
					refDelta.addValuesToAdd(value);

					value = createPrismRefValue(oldParent);
					refDelta.addValuesToDelete(value);
				}
				break;
		}

		return delta;
	}

	private void moveConfirmedPerformed(AjaxRequestTarget target, OrgTreeDto oldParent, OrgTableDto newParent,
			OrgUnitBrowser.Operation operation) {
		OrgUnitBrowser dialog = (OrgUnitBrowser) get(ID_MOVE_POPUP);
		List<OrgTableDto> objects = dialog.getSelected();

		PageBase page = getPageBase();
		ModelService model = page.getModelService();
		Task task = getPageBase().createSimpleTask(OPERATION_MOVE_OBJECTS);
		OperationResult result = task.getResult();
		for (OrgTableDto object : objects) {
			OperationResult subResult = result.createSubresult(OPERATION_MOVE_OBJECT);

			PrismObject<OrgType> orgUnit = WebModelServiceUtils.loadObject(OrgType.class, object.getOid(),
					WebModelServiceUtils.createOptionsForParentOrgRefs(), getPageBase(), task, subResult);
			try {
				ObjectDelta delta = createMoveDelta(orgUnit, oldParent, newParent, operation);

				model.executeChanges(WebComponentUtil.createDeltaCollection(delta), null,
						page.createSimpleTask(OPERATION_MOVE_OBJECT), subResult);
			} catch (Exception ex) {
				subResult.recordFatalError(
						"Couldn't move object " + object.getName() + " to " + newParent.getName() + ".", ex);
				LoggingUtils.logException(LOGGER, "Couldn't move object {} to {}", ex, object.getName());
			} finally {
				subResult.computeStatusIfUnknown();
			}
		}
		result.computeStatusComposite();

		ObjectDataProvider provider = (ObjectDataProvider) getOrgChildTable().getDataTable()
				.getDataProvider();
		provider.clearCache();

		page.showResult(result);
		dialog.close(target);

		refreshTabbedPanel(target);
	}

	private MainObjectListPanel<ObjectType> getMemberTable() {
		return (MainObjectListPanel<ObjectType>) get(
				createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
	}

	private TablePanel getManagerTable() {
		return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_MANAGER, ID_MANAGER_TABLE));
	}

	private void selectTreeItemPerformed(AjaxRequestTarget target) {
		BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(
				createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
		basicSearch.getModel().setObject(null);

		getMemberTable().refreshTable(target);
		;

		Form mainForm = (Form) get(ID_FORM);
		mainForm.addOrReplace(createManagerContainer());
		target.add(mainForm);
		target.add(get(ID_SEARCH_FORM));
	}

	private ObjectQuery createMemberQuery(QName relation) {
		ObjectQuery query = null;
		OrgTreeDto dto = selected.getObject();
		String oid = dto != null ? dto.getOid() : getModel().getObject();

		BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(
				createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
		String object = basicSearch.getModelObject();

		SubstringFilter substring;
		PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
		String normalizedString = normalizer.normalize(object);

		List<ObjectFilter> filters = new ArrayList<>();
		if (StringUtils.isNotBlank(normalizedString)) {
			substring = SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class,
					getPageBase().getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedString);
			filters.add(substring);
		}

		DropDownChoice<String> searchScopeChoice = (DropDownChoice) get(
				createComponentPath(ID_SEARCH_FORM, ID_SEARCH_SCOPE));
		String scope = searchScopeChoice.getModelObject();

		try {
			OrgFilter org;
			if (SEARCH_SCOPE_ONE.equals(scope)) {
				filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL));
			} else {
				filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.SUBTREE));
			}
			PrismReferenceValue referenceFilter = new PrismReferenceValue();
			referenceFilter.setOid(oid);
			referenceFilter.setRelation(relation);
			RefFilter referenceOidFilter = RefFilter.createReferenceEqual(
					new ItemPath(FocusType.F_PARENT_ORG_REF), UserType.class, getPageBase().getPrismContext(),
					referenceFilter);
			filters.add(referenceOidFilter);

			query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
			}

		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. managers.", e);
		}

		DropDownChoice<ObjectTypes> searchByTypeChoice = (DropDownChoice) get(
				createComponentPath(ID_SEARCH_FORM, ID_SEARCH_BY_TYPE));
		ObjectTypes typeModel = searchByTypeChoice.getModelObject();

		if (typeModel.equals(ObjectTypes.OBJECT)) {
			return query;
		}

		return ObjectQuery
				.createObjectQuery(TypeFilter.createType(typeModel.getTypeQName(), query.getFilter()));

		// return query;
	}

	private void collapseAllPerformed(AjaxRequestTarget target) {
		TableTree<OrgTreeDto, String> tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.collapseAll();

		target.add(tree);
	}

	private void expandAllPerformed(AjaxRequestTarget target) {
		TableTree<OrgTreeDto, String> tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.expandAll();

		target.add(tree);
	}

	private void moveRootPerformed(OrgTreeDto root, AjaxRequestTarget target) {
		if (root == null){
			root = getRootFromProvider();
		}
		OrgTableDto dto = new OrgTableDto(root.getOid(), root.getType());
		movePerformed(getOrgChildTable(), target, OrgUnitBrowser.Operation.MOVE, dto, true);
	}

	private void updateActivationPerformed(TablePanel table, AjaxRequestTarget target, boolean enable) {
		List<OrgTableDto> objects = isAnythingSelected(table, target);
		if (objects.isEmpty()) {
			return;
		}

		PageBase page = getPageBase();
		OperationResult result = new OperationResult(OPERATION_UPDATE_OBJECTS);
		for (OrgTableDto object : objects) {
			if (!(FocusType.class.isAssignableFrom(object.getType()))) {
				continue;
			}

			OperationResult subResult = result.createSubresult(OPERATION_UPDATE_OBJECT);
			ObjectDelta delta = WebModelServiceUtils.createActivationAdminStatusDelta(object.getType(),
					object.getOid(), enable, page.getPrismContext());

			WebModelServiceUtils.save(delta, subResult, page);
		}
		result.computeStatusComposite();

		page.showResult(result);
		target.add(page.getFeedbackPanel());

		refreshTable(target);
	}

	@Override
	protected void refreshTable(AjaxRequestTarget target) {
		getMemberTable().clearCache();
		getMemberTable().refreshTable(target);
	}

	private void recomputeRootPerformed(OrgTreeDto root, AjaxRequestTarget target) {
		if (root == null){
		 root = getRootFromProvider();
		}
		
		recomputePerformed(root, target);
	}

//	private void recomputePerformed(AjaxRequestTarget target,
//			OrgUnitBrowser.Operation operation) {
//		recomputePerformed(target);
//	}

	private void recomputePerformed(OrgTreeDto orgToRecompute, AjaxRequestTarget target) {
//		List<OrgTableDto> objects;
//		if (orgDto == null) {
//			objects = isAnythingSelected(table, target);
//			if (objects.isEmpty()) {
//				return;
//			}
//		} else {
//			objects = new ArrayList<>();
//			objects.add(orgDto);
//		}

		Task task = getPageBase().createSimpleTask(OPERATION_RECOMPUTE);
		OperationResult result = new OperationResult(OPERATION_RECOMPUTE);

		try {
//			for (OrgTableDto org : objects) {

//				PrismObject<TaskType> recomputeTask = prepareRecomputeTask(org);
//
//				ObjectDelta taskDelta = ObjectDelta.createAddDelta(recomputeTask);
//
//				if (LOGGER.isTraceEnabled()) {
//					LOGGER.trace(taskDelta.debugDump());
//				}

				ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class, orgToRecompute.getOid(),
						getPageBase().getPrismContext());
				ModelExecuteOptions options = new ModelExecuteOptions();
				options.setReconcile(true);
				getPageBase().getModelService().executeChanges(
						WebComponentUtil.createDeltaCollection(emptyDelta), options, task, result);
//			}

			result.recordSuccess();
		} catch (Exception e) {
			result.recordFatalError(getString("TreeTablePanel.message.recomputeError"), e);
			LoggingUtils.logException(LOGGER, getString("TreeTablePanel.message.recomputeError"), e);
		}

		getPageBase().showResult(result);
		target.add(getPageBase().getFeedbackPanel());
		refreshTabbedPanel(target);
	}

	private PrismObject<TaskType> prepareRecomputeTask(OrgTableDto org) throws SchemaException {
		PrismPropertyDefinition propertyDef = getPageBase().getPrismContext().getSchemaRegistry()
				.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);

		ObjectFilter refFilter = RefFilter.createReferenceEqual(UserType.F_PARENT_ORG_REF, UserType.class,
				getPageBase().getPrismContext(), org.getOid());

		SearchFilterType filterType = QueryConvertor.createSearchFilterType(refFilter,
				getPageBase().getPrismContext());
		QueryType queryType = new QueryType();
		queryType.setFilter(filterType);

		PrismProperty<QueryType> property = propertyDef.instantiate();
		property.setRealValue(queryType);

		TaskType taskType = new TaskType();

		taskType.setName(WebComponentUtil.createPolyFromOrigString(
				createStringResource("TreeTablePanel.recomputeTask", org.getName()).getString()));
		taskType.setBinding(TaskBindingType.LOOSE);
		taskType.setExecutionStatus(TaskExecutionStatusType.RUNNABLE);
		taskType.setRecurrence(TaskRecurrenceType.SINGLE);

		MidPointPrincipal owner = SecurityUtils.getPrincipalUser();

		ObjectReferenceType ownerRef = new ObjectReferenceType();
		ownerRef.setOid(owner.getOid());
		ownerRef.setType(owner.getUser().COMPLEX_TYPE);
		taskType.setOwnerRef(ownerRef);

		ExtensionType extensionType = new ExtensionType();
		taskType.setExtension(extensionType);

		getPageBase().getPrismContext().adopt(taskType);

		extensionType.asPrismContainerValue().add(property);

		taskType.setHandlerUri(
				"http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/recompute/handler-3");

		return taskType.asPrismObject();
	}

	private void deleteRootPerformed(final OrgTreeDto orgToDelete, AjaxRequestTarget target) {
//		if (selected.getObject() == null) {
//			warn(getString("TreeTablePanel.message.nothingSelected"));
//			target.add(getPageBase().getFeedbackPanel());
//			return;
//		}
		
		ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId()) {
			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				deleteRootConfirmedPerformed(orgToDelete, target);
			}
		};
		
//		ConfirmationDialog dialog = (ConfirmationDialog) get(ID_CONFIRM_DELETE_POPUP);
//		dialog.setConfirmType(CONFIRM_DELETE_ROOT);
//		dialog.show(target);
		confirmationPanel.setOutputMarkupId(true);
		getPageBase().showMainPopup(confirmationPanel, new Model<String>("Delete org?"), target, 150, 100);
	}

	private void deleteRootConfirmedPerformed(OrgTreeDto orgToDelete, AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

		PageBase page = getPageBase();

		if (orgToDelete == null){
			orgToDelete = getRootFromProvider();
		}
		WebModelServiceUtils.deleteObject(OrgType.class, orgToDelete.getOid(), result, page);

		result.computeStatusIfUnknown();
		page.showResult(result);

		refreshTabbedPanel(target);
	}

	private static class TreeStateModel extends AbstractReadOnlyModel<Set<OrgTreeDto>> {

		private TreeStateSet<OrgTreeDto> set = new TreeStateSet<OrgTreeDto>();
		private ISortableTreeProvider provider;
		private TreeTablePanel panel;

		TreeStateModel(TreeTablePanel panel, ISortableTreeProvider provider) {
			this.panel = panel;
			this.provider = provider;
		}

		@Override
		public Set<OrgTreeDto> getObject() {
			MidPointAuthWebSession session = panel.getSession();
			SessionStorage storage = session.getSessionStorage();
			Set<OrgTreeDto> dtos = storage.getUsers().getExpandedItems();
			OrgTreeDto collapsedItem = storage.getUsers().getCollapsedItem();
			Iterator<OrgTreeDto> iterator = provider.getRoots();

			if (collapsedItem != null) {
				if (set.contains(collapsedItem)) {
					set.remove(collapsedItem);
					storage.getUsers().setCollapsedItem(null);
				}
			}
			if (dtos != null && (dtos instanceof TreeStateSet)) {
				for (OrgTreeDto orgTreeDto : dtos) {
					if (!set.contains(orgTreeDto)) {
						set.add(orgTreeDto);
					}
				}
			}
			// just to have root expanded at all time
			if (iterator.hasNext()) {
				OrgTreeDto root = iterator.next();
				if (set.isEmpty() || !set.contains(root)) {
					set.add(root);
				}
			}
			return set;
		}

		public void expandAll() {
			set.expandAll();
		}

		public void collapseAll() {
			set.collapseAll();
		}
	}

	private void editRootPerformed(OrgTreeDto root, AjaxRequestTarget target) {
		if (root == null){
		 root = getRootFromProvider();
		}
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, root.getOid());
		setResponsePage(PageOrgUnit.class, parameters);
	}

	private void addToHierarchyPerformed(AjaxRequestTarget target) {
		showAddDeletePopup(target, OrgUnitAddDeletePopup.ActionState.ADD);
	}

	private void removeFromHierarchyPerformed(AjaxRequestTarget target) {
		showAddDeletePopup(target, OrgUnitAddDeletePopup.ActionState.DELETE);
	}

	private void showAddDeletePopup(AjaxRequestTarget target, OrgUnitAddDeletePopup.ActionState state) {
		OrgUnitAddDeletePopup dialog = (OrgUnitAddDeletePopup) get(ID_ADD_DELETE_POPUP);
		dialog.setState(state, target);

		dialog.show(target);
	}

	private void addOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org) {
		// TODO
	}

	private void removeOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org) {
		// TODO
	}
}
