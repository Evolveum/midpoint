/**
 * Copyright (c) 2015-2016 Evolveum
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractTreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgTreeProvider;
import com.evolveum.midpoint.web.page.admin.users.component.SelectableFolderContent;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class OrgTreePanel extends AbstractTreeTablePanel {

	private boolean selectable;
    private String treeTitleKey = "";

	public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable) {
        this(id, rootOid, selectable, "");
    }

	public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, String treeTitleKey) {
		super(id, rootOid);

        this.treeTitleKey = treeTitleKey;
		this.selectable = selectable;
		selected = new LoadableModel<SelectableBean<OrgType>>() {
			@Override
			protected SelectableBean<OrgType> load() {
                TabbedPanel currentTabbedPanel = null;
				MidPointAuthWebSession session = OrgTreePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				if (getTree().findParent(PageOrgTree.class) != null) {
					currentTabbedPanel = getTree().findParent(PageOrgTree.class).getTabPanel().getTabbedPanel();
                    if (currentTabbedPanel != null) {
                        int tabId = currentTabbedPanel.getSelectedTab();
                        if (storage.getUsers().getSelectedTabId() != -1
                                && tabId != storage.getUsers().getSelectedTabId()) {
                            storage.getUsers().setSelectedItem(null);
                        }
                    }
				}
				if (storage.getUsers().getSelectedItem() != null) {
					return storage.getUsers().getSelectedItem();
				} else {
					return getRootFromProvider();
				}
			}
		};

		initLayout();
	}

	public SelectableBean<OrgType> getSelected() {
		return selected.getObject();
	}

	public void setSelected(SelectableBean<OrgType> org) {
		selected.setObject(org);
	}

	public List<OrgType> getSelectedOrgs() {
		return ((OrgTreeProvider) getTree().getProvider()).getSelectedObjects();
	}

	private static final long serialVersionUID = 1L;

	private void initLayout() {
		WebMarkupContainer treeHeader = new WebMarkupContainer(ID_TREE_HEADER);
		treeHeader.setOutputMarkupId(true);
		add(treeHeader);

        String title = StringUtils.isEmpty(treeTitleKey) ? "TreeTablePanel.hierarchy" : treeTitleKey;
        Label treeTitle = new Label(ID_TREE_TITLE, createStringResource(title));
        treeHeader.add(treeTitle);

		InlineMenu treeMenu = new InlineMenu(ID_TREE_MENU,
				new Model<>((Serializable) createTreeMenuInternal()));
		treeHeader.add(treeMenu);

		ISortableTreeProvider provider = new OrgTreeProvider(this, getModel()) {
			@Override
			protected List<InlineMenuItem> createInlineMenuItems() {
				return createTreeChildrenMenu();
			}
		};
		List<IColumn<SelectableBean<OrgType>, String>> columns = new ArrayList<>();

		if (selectable) {
			columns.add(new CheckBoxHeaderColumn<SelectableBean<OrgType>>());
		}

		columns.add(new TreeColumn<SelectableBean<OrgType>, String>(
				createStringResource("TreeTablePanel.hierarchy")));
		columns.add(new InlineMenuHeaderColumn(createTreeChildrenMenu()));

		WebMarkupContainer treeContainer = new WebMarkupContainer(ID_TREE_CONTAINER) {

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);

				// method computes height based on document.innerHeight() -
				// screen height;
				Component form = OrgTreePanel.this.getParent().get("memberPanel");
				if (form != null) {
					response.render(OnDomReadyHeaderItem.forScript(
							"updateHeight('" + getMarkupId() + "', ['#" + form.getMarkupId() + "'], ['#"
									+ OrgTreePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])"));
				}
			}
		};
		add(treeContainer);

		TableTree<SelectableBean<OrgType>, String> tree = new TableTree<SelectableBean<OrgType>, String>(
				ID_TREE, columns, provider, Integer.MAX_VALUE, new TreeStateModel(this, provider)) {

			@Override
			protected Component newContentComponent(String id, IModel<SelectableBean<OrgType>> model) {
				return new SelectableFolderContent(id, this, model, selected) {

					@Override
					protected void onClick(AjaxRequestTarget target) {
						super.onClick(target);

						MidPointAuthWebSession session = OrgTreePanel.this.getSession();
						SessionStorage storage = session.getSessionStorage();
						storage.getUsers().setSelectedItem(selected.getObject());

						selectTreeItemPerformed(selected.getObject(), target);
					}
				};
			}

			@Override
			protected Item<SelectableBean<OrgType>> newRowItem(String id, int index,
					final IModel<SelectableBean<OrgType>> model) {
				Item<SelectableBean<OrgType>> item = super.newRowItem(id, index, model);
				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						SelectableBean<OrgType> itemObject = model.getObject();
						if (itemObject != null && itemObject.equals(selected.getObject())) {
							return "success";
						}

						return null;
					}
				}));
				return item;
			}

			@Override
			public void collapse(SelectableBean<OrgType> collapsedItem) {
				super.collapse(collapsedItem);
				MidPointAuthWebSession session = OrgTreePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				Set<SelectableBean<OrgType>> items = storage.getUsers().getExpandedItems();
				if (items != null && items.contains(collapsedItem)) {
					items.remove(collapsedItem);
				}
				storage.getUsers().setExpandedItems((TreeStateSet) items);
				storage.getUsers().setCollapsedItem(collapsedItem);
			}

			@Override
			protected void onModelChanged() {
				super.onModelChanged();

				Set<SelectableBean<OrgType>> items = getModelObject();

				MidPointAuthWebSession session = OrgTreePanel.this.getSession();
				SessionStorage storage = session.getSessionStorage();
				storage.getUsers().setExpandedItems((TreeStateSet<SelectableBean<OrgType>>) items);
			}
		};
		tree.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
		tree.getTable().add(AttributeModifier.replace("class", "table table-striped table-condensed"));
		tree.add(new WindowsTheme());
		// tree.add(AttributeModifier.replace("class", "tree-midpoint"));
		treeContainer.add(tree);
	}

	private static class TreeStateModel extends AbstractReadOnlyModel<Set<SelectableBean<OrgType>>> {

		private TreeStateSet<SelectableBean<OrgType>> set = new TreeStateSet<SelectableBean<OrgType>>();
		private ISortableTreeProvider provider;
		private OrgTreePanel panel;

		TreeStateModel(OrgTreePanel panel, ISortableTreeProvider provider) {
			this.panel = panel;
			this.provider = provider;
		}

		@Override
		public Set<SelectableBean<OrgType>> getObject() {
			MidPointAuthWebSession session = panel.getSession();
			SessionStorage storage = session.getSessionStorage();
			Set<SelectableBean<OrgType>> dtos = storage.getUsers().getExpandedItems();
			SelectableBean<OrgType> collapsedItem = storage.getUsers().getCollapsedItem();
			Iterator<SelectableBean<OrgType>> iterator = provider.getRoots();

			if (collapsedItem != null) {
				if (set.contains(collapsedItem)) {
					set.remove(collapsedItem);
					storage.getUsers().setCollapsedItem(null);
				}
			}
			if (dtos != null && (dtos instanceof TreeStateSet)) {
				for (SelectableBean<OrgType> orgTreeDto : dtos) {
					if (!set.contains(orgTreeDto)) {
						set.add(orgTreeDto);
					}
				}
			}
			// just to have root expanded at all time
			if (iterator.hasNext()) {
				SelectableBean<OrgType> root = iterator.next();
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

	private List<InlineMenuItem> createTreeMenuInternal() {
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

		List<InlineMenuItem> additionalActions = createTreeMenu();
		if (additionalActions != null) {
			items.addAll(additionalActions);
		}
		return items;
	}

	protected List<InlineMenuItem> createTreeMenu() {
		return null;
	}

	protected List<InlineMenuItem> createTreeChildrenMenu() {
		return new ArrayList<>();
	}

	protected void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {

	}
	
	private void collapseAllPerformed(AjaxRequestTarget target) {
		TableTree<SelectableBean<OrgType>, String> tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.collapseAll();

		target.add(tree);
	}

	private void expandAllPerformed(AjaxRequestTarget target) {
		TableTree<SelectableBean<OrgType>, String> tree = getTree();
		TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
		model.expandAll();

		target.add(tree);
	}


}
