/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web.page.admin.orgs;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.ITreeDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiFeature;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractTreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgTreeProvider;
import com.evolveum.midpoint.web.page.admin.users.component.SelectableFolderContent;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class OrgTreePanel extends AbstractTreeTablePanel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(OrgTreePanel.class);

	private boolean selectable;
    private String treeTitleKey = "";
//	SessionStorage storage;
	List<OrgType> preselecteOrgsList = new ArrayList<>();


	public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator) {
        this(id, rootOid, selectable, serviceLocator, "");
    }

	public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator, String treeTitleKey) {
		this(id, rootOid, selectable, serviceLocator, "", new ArrayList<>());
	}

	public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator, String treeTitleKey,
						List<OrgType> preselecteOrgsList) {
		super(id, rootOid);

//		MidPointAuthWebSession session = OrgTreePanel.this.getSession();
//		storage = session.getSessionStorage();

		this.treeTitleKey = treeTitleKey;
		this.selectable = selectable;
		if (preselecteOrgsList != null){
			this.preselecteOrgsList.addAll(preselecteOrgsList);
		}
		selected = new LoadableModel<TreeSelectableBean<OrgType>>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected TreeSelectableBean<OrgType> load() {
                TabbedPanel currentTabbedPanel = null;
                OrgTreeStateStorage storage = getOrgTreeStateStorage();
				if (getTree().findParent(PageOrgTree.class) != null) {
					currentTabbedPanel = getTree().findParent(PageOrgTree.class).getTabPanel().getTabbedPanel();
                    if (currentTabbedPanel != null) {
                        int tabId = currentTabbedPanel.getSelectedTab();
						int storedTabId = storage != null ? OrgTreePanel.this.getSelectedTabId(getOrgTreeStateStorage()) : -1;
                        if (storedTabId != -1
                                && tabId != storedTabId) {
                            OrgTreePanel.this.setSelectedItem(null, getOrgTreeStateStorage());
                        }
                    }
				}
				TreeSelectableBean<OrgType> bean;
				if (storage != null && OrgTreePanel.this.getSelectedItem(getOrgTreeStateStorage()) != null) {
					bean = OrgTreePanel.this.getSelectedItem(getOrgTreeStateStorage());
				} else {
					bean =  getRootFromProvider();
				}
				return bean;
			}
		};

		initLayout(serviceLocator);
	}

	public TreeSelectableBean<OrgType> getSelected() {
		return selected.getObject();
	}

	public void setSelected(TreeSelectableBean<OrgType> org) {
		selected.setObject(org);
	}

	public List<OrgType> getSelectedOrgs() {
		return ((OrgTreeProvider) getTree().getProvider()).getSelectedObjects();
	}

	private void initLayout(ModelServiceLocator serviceLocator) {
		WebMarkupContainer treeHeader = new WebMarkupContainer(ID_TREE_HEADER);
		treeHeader.setOutputMarkupId(true);
		add(treeHeader);

        String title = StringUtils.isEmpty(treeTitleKey) ? "TreeTablePanel.hierarchy" : treeTitleKey;
        Label treeTitle = new Label(ID_TREE_TITLE, createStringResource(title));
        treeHeader.add(treeTitle);

		InlineMenu treeMenu = new InlineMenu(ID_TREE_MENU,
				new Model((Serializable) createTreeMenuInternal(serviceLocator.getCompiledUserProfile())));
		treeMenu.setOutputMarkupId(true);
		treeMenu.setOutputMarkupPlaceholderTag(true);
		treeHeader.add(treeMenu);

		ISortableTreeProvider provider = new OrgTreeProvider(this, getModel(), preselecteOrgsList) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<InlineMenuItem> createInlineMenuItems(OrgType org) {
				return createTreeChildrenMenu(org);
			}

			@Override
			protected ObjectFilter getCustomFilter(){
				return OrgTreePanel.this.getCustomFilter();
			}
		};
		List<IColumn<TreeSelectableBean<OrgType>, String>> columns = new ArrayList<>();

		if (selectable) {
			columns.add(new CheckBoxHeaderColumn<TreeSelectableBean<OrgType>>() {
				private static final long serialVersionUID = 1L;

				@Override
				protected IModel<Boolean> getCheckBoxValueModel(IModel<TreeSelectableBean<OrgType>> rowModel) {
					return OrgTreePanel.this.getCheckBoxValueModel(rowModel);
				}

				@Override
				protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<TreeSelectableBean<OrgType>> rowModel, IModel<Boolean> selected) {
					super.onUpdateRow(target, table, rowModel, selected);
					rowModel.getObject().setSelected(selected.getObject());
					onOrgTreeCheckBoxSelectionPerformed(target, rowModel);
				}
			});
		}

		columns.add(new TreeColumn<>(
            createStringResource("TreeTablePanel.hierarchy")));
		columns.add(new InlineMenuHeaderColumn(createTreeChildrenMenu(null)));

		WebMarkupContainer treeContainer = new WebMarkupContainer(ID_TREE_CONTAINER) {
			private static final long serialVersionUID = 1L;

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);

				// method computes height based on document.innerHeight() -
				// screen height;
				Component form = OrgTreePanel.this.getParent().get("memberPanel");
				if (form != null) {
					//TODO fix
//					response.render(OnDomReadyHeaderItem.forScript(
//							"updateHeight('" + getMarkupId() + "', ['#" + form.getMarkupId() + "'], ['#"
//									+ OrgTreePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])"));
				}
			}
		};
		add(treeContainer);

		TreeStateModel treeStateMode = new TreeStateModel(this, provider, getOrgTreeStateStorage()) {
			private static final long serialVersionUID = 1L;

			@Override
			public Set<TreeSelectableBean<OrgType>> getExpandedItems(){
				return OrgTreePanel.this.getExpandedItems(getOrgTreeStateStorage());
			}
			@Override
			public TreeSelectableBean<OrgType> getCollapsedItem(){
				return OrgTreePanel.this.getCollapsedItem(getOrgTreeStateStorage());
			}
			@Override
			public void setCollapsedItem(TreeSelectableBean<OrgType> item){
				OrgTreePanel.this.setCollapsedItem(null, getOrgTreeStateStorage());
			}
		};

		MidpointNestedTree tree = new MidpointNestedTree(ID_TREE, provider) {


			@Override
			protected Component newContentComponent(String id, IModel<TreeSelectableBean<OrgType>> model) {
				return new SelectableFolderContent(id, this, model, selected) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void onClick(Optional<AjaxRequestTarget> target) {
						super.onClick(target);

						OrgTreePanel.this.setSelectedItem(selected.getObject(), getOrgTreeStateStorage());

						selectTreeItemPerformed(selected.getObject(), target.get());

						Component component = get("table");
						if (component != null) {
							target.get().add(component);
						}
					}
				};
			}
		};
//		TableTree<TreeSelectableBean<OrgType>, String> tree = new TableTree<TreeSelectableBean<OrgType>, String>(
//				ID_TREE, columns, provider, 20, treeStateMode) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected Component newContentComponent(String id, IModel<TreeSelectableBean<OrgType>> model) {
//				return new SelectableFolderContent(id, this, model, selected) {
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					protected void onClick(Optional<AjaxRequestTarget> target) {
//						super.onClick(target);
//
//						OrgTreePanel.this.setSelectedItem(selected.getObject(), getOrgTreeStateStorage());
//
//						selectTreeItemPerformed(selected.getObject(), target.get());
//
//						Component component = get("table");
//						if (component != null) {
//							target.get().add(component);
//						}
//					}
//				};
//			}
//
//			@Override
//			protected Item<TreeSelectableBean<OrgType>> newRowItem(String id, int index,
//					final IModel<TreeSelectableBean<OrgType>> model) {
//				Item<TreeSelectableBean<OrgType>> item = super.newRowItem(id, index, model);
//				item.add(AttributeModifier.append("class", new IModel<String>() {
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public String getObject() {
//						TreeSelectableBean<OrgType> itemObject = model.getObject();
//						if (itemObject != null && itemObject.equals(selected.getObject())) {
//							return "success";
//						}
//
//						return null;
//					}
//				}));
//				return item;
//			}
//
//			@Override
//			public void collapse(TreeSelectableBean<OrgType> collapsedItem) {
//				super.collapse(collapsedItem);
//
//				Set<TreeSelectableBean<OrgType>> items = OrgTreePanel.this.getExpandedItems(getOrgTreeStateStorage());
//				if (items != null && items.contains(collapsedItem)) {
//					items.remove(collapsedItem);
//				}
//				OrgTreePanel.this.setExpandedItems((TreeStateSet) items, getOrgTreeStateStorage());
//				OrgTreePanel.this.setCollapsedItem(collapsedItem, getOrgTreeStateStorage());
//			}
//
//			@Override
//			protected void onModelChanged() {
//				super.onModelChanged();
//
//				TreeStateSet<TreeSelectableBean<OrgType>> items = (TreeStateSet) getModelObject();
//				boolean isInverse = getOrgTreeStateStorage() != null ? getOrgTreeStateStorage().isInverse() : items.isInverse();
//				if (!isInverse) {
//					OrgTreePanel.this.setExpandedItems(items, getOrgTreeStateStorage());
//				}
//			}
//
//			@Override
//			protected ITreeDataProvider<TreeSelectableBean<OrgType>> newDataProvider(ITreeProvider<TreeSelectableBean<OrgType>> provider) {
//				return new TreeDataProvider<TreeSelectableBean<OrgType>>(provider)
//				{
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					protected boolean iterateChildren(TreeSelectableBean<OrgType> object)
//					{
//						return getState(object) == State.EXPANDED;
//					}
//
//					@Override
//					public Iterator<? extends TreeSelectableBean<OrgType>> iterator(long first, long count) {
//						if (!(provider instanceof OrgTreeProvider)) {
//							return super.iterator(first, count);
//						}
//
//						OrgTreeProvider orgTreeProvider = (OrgTreeProvider) provider;
////						orgTreeProvider.setOffset(first);
////						orgTreeProvider.setCount(count);
//						selected.getObject().setOffset(first);
//						selected.getObject().setCount(count);
//						orgTreeProvider.setSelected(selected.getObject().getValue());
//
//
//						return super.iterator(0, count);
//
//					}
//
//					@Override
//					public long size() {
//						if (!(provider instanceof OrgTreeProvider)) {
//							return super.size();
//						}
//
//						OrgTreeProvider orgTreeProvider = (OrgTreeProvider) provider;
//						orgTreeProvider.setSelected(selected.getObject().getValue());
//						return orgTreeProvider.size() + 1;
//
//					}
//				};
//			}
//
//			@Override
//			protected DataTable<TreeSelectableBean<OrgType>, String> newDataTable(String id, List<? extends IColumn<TreeSelectableBean<OrgType>, String>> iColumns, IDataProvider<TreeSelectableBean<OrgType>> dataProvider, long rowsPerPage) {
//				DataTable dataTable = super.newDataTable(id, iColumns, dataProvider, rowsPerPage);
//
//
//				NavigationToolbar navigation = new NavigationToolbar(dataTable);
//				dataTable.addBottomToolbar(navigation);
//				return dataTable;
//			}
//		};
		tree.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
//		tree.getTable().add(AttributeModifier.replace("class", "table table-striped table-condensed"));
		tree.add(new WindowsTheme());
		// tree.add(AttributeModifier.replace("class", "tree-midpoint"));
		treeContainer.add(tree);
	}

	private static class TreeStateModel implements IModel<Set<TreeSelectableBean<OrgType>>> {
		private static final long serialVersionUID = 1L;

		private TreeStateSet<TreeSelectableBean<OrgType>> set = new TreeStateSet<>();
		private ISortableTreeProvider provider;
		private OrgTreePanel panel;
		private OrgTreeStateStorage storage;

		TreeStateModel(OrgTreePanel panel, ISortableTreeProvider provider, OrgTreeStateStorage storage) {
			this.panel = panel;
			this.provider = provider;
			this.storage = storage;
			set.setInverse(storage != null ? storage.isInverse() : false);
		}

		@Override
		public Set<TreeSelectableBean<OrgType>> getObject() {
			Set<TreeSelectableBean<OrgType>> dtos = TreeStateModel.this.getExpandedItems();
			TreeSelectableBean<OrgType> collapsedItem = TreeStateModel.this.getCollapsedItem();

			if (collapsedItem != null) {
				if (set.contains(collapsedItem)) {
					set.remove(collapsedItem);
					TreeStateModel.this.setCollapsedItem(null);
				}
			}
			if (dtos != null && (dtos instanceof TreeStateSet)) {
				for (TreeSelectableBean<OrgType> orgTreeDto : dtos) {
					if (!set.contains(orgTreeDto)) {
						set.add(orgTreeDto);
					}
				}
			}
			// just to have root expanded at all time
			Iterator<TreeSelectableBean<OrgType>> iterator = provider.getRoots();
			if (iterator.hasNext()) {
				TreeSelectableBean<OrgType> root = iterator.next();
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
			if (getExpandedItems() != null) {
				getExpandedItems().clear();
			}
			set.collapseAll();
		}

		public Set<TreeSelectableBean<OrgType>> getExpandedItems(){
			return storage != null ? storage.getExpandedItems() : null;
		}

		public TreeSelectableBean<OrgType> getCollapsedItem(){
			return storage != null ? storage.getCollapsedItem() : null;
		}

		public void setCollapsedItem(TreeSelectableBean<OrgType> item){
			if (storage != null){
				storage.setCollapsedItem(item);
			}
		}
	}

	protected ObjectFilter getCustomFilter(){
		return null;
	}

	private List<InlineMenuItem> createTreeMenuInternal(CompiledUserProfile adminGuiConfig) {
		List<InlineMenuItem> items = new ArrayList<>();

		if (adminGuiConfig.isFeatureVisible(GuiFeature.ORGTREE_COLLAPSE_ALL.getUri())) {
			InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.collapseAll")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							collapseAllPerformed(target);
						}
					};
				}
			};
			items.add(item);
		}
		if (adminGuiConfig.isFeatureVisible(GuiFeature.ORGTREE_EXPAND_ALL.getUri())) {
			InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.expandAll")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							expandAllPerformed(target);
						}
					};
				}
			};
			items.add(item);
		}

		List<InlineMenuItem> additionalActions = createTreeMenu();
		if (additionalActions != null) {
			items.addAll(additionalActions);
		}
		return items;
	}

	protected List<InlineMenuItem> createTreeMenu() {
		return null;
	}

	protected List<InlineMenuItem> createTreeChildrenMenu(OrgType org) {
		return new ArrayList<>();
	}

	protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected, AjaxRequestTarget target) {

	}

	private void collapseAllPerformed(AjaxRequestTarget target) {
		MidpointNestedTree tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.collapseAll();
		if (getOrgTreeStateStorage() != null){
			getOrgTreeStateStorage().setInverse(false);
		}

		target.add(tree);
	}

	private void expandAllPerformed(AjaxRequestTarget target) {
		MidpointNestedTree tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.expandAll();

		if (getOrgTreeStateStorage() != null){
			getOrgTreeStateStorage().setInverse(true);
		}

		target.add(tree);
	}

	public Set<TreeSelectableBean<OrgType>> getExpandedItems(OrgTreeStateStorage storage){
		return storage != null ? storage.getExpandedItems() : null;
	}

	public void setExpandedItems(TreeStateSet items, OrgTreeStateStorage storage){
		if (storage != null){
			storage.setExpandedItems(items);
		}
	}

	public TreeSelectableBean<OrgType> getCollapsedItem(OrgTreeStateStorage storage){
		return storage != null ? storage.getCollapsedItem() : null;
	}

	public void setCollapsedItem(TreeSelectableBean<OrgType> item, OrgTreeStateStorage storage){
		if (storage != null){
			storage.setCollapsedItem(item);
		}
	}

	public void setSelectedItem(TreeSelectableBean<OrgType> item, OrgTreeStateStorage storage){
		if (storage != null){
			storage.setSelectedItem(item);
		}
	}

	public TreeSelectableBean<OrgType> getSelectedItem(OrgTreeStateStorage storage){
		return storage != null ? storage.getSelectedItem() : null;
	}

	public OrgTreeStateStorage getOrgTreeStateStorage(){
		MidPointAuthWebSession session = OrgTreePanel.this.getSession();
		SessionStorage storage = session.getSessionStorage();
		return storage.getOrgStructurePanelStorage();
	}

	public int getSelectedTabId(OrgTreeStateStorage storage){
		return storage != null ? storage.getSelectedTabId() : -1;
	}

	protected IModel<Boolean> getCheckBoxValueModel(IModel<TreeSelectableBean<OrgType>> rowModel){
		return Model.of(rowModel.getObject().isSelected());
	}

	protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel){}
}
