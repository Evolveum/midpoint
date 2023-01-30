/*
 * Copyright (c) 2015-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import com.evolveum.midpoint.gui.api.GuiFeature;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.gui.impl.page.admin.org.component.AbstractTreeTablePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.OrgTreeProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.*;

public class OrgTreePanel extends AbstractTreeTablePanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OrgTreePanel.class);

    private boolean selectable;
    private String treeTitleKey = "";
    private List<OrgTreeFolderContent> contentPannels = new ArrayList<OrgTreeFolderContent>();


    public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator) {
        this(id, rootOid, selectable, serviceLocator, "");
    }

    public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator, String treeTitleKey) {
        this(id, rootOid, selectable, serviceLocator, treeTitleKey, new ArrayList<>());
    }

    public OrgTreePanel(String id, IModel<String> rootOid, boolean selectable, ModelServiceLocator serviceLocator, String treeTitleKey,
                        List<OrgType> preselecteOrgsList) {
        super(id, rootOid);
        this.treeTitleKey = treeTitleKey;
        this.selectable = selectable;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

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

        initLayout();
    }

    public TreeSelectableBean<OrgType> getSelected() {
        return selected.getObject();
    }

    public IModel<TreeSelectableBean<OrgType>> getSelectedOrgModel() {
        return selected;
    }

    public void setSelected(TreeSelectableBean<OrgType> org) {
        selected.setObject(org);
    }

    private void initLayout() {
        WebMarkupContainer treeHeader = new WebMarkupContainer(ID_TREE_HEADER);
        treeHeader.setOutputMarkupId(true);
        add(treeHeader);

        String title = StringUtils.isEmpty(treeTitleKey) ? "TreeTablePanel.hierarchy" : treeTitleKey;
        Label treeTitle = new Label(ID_TREE_TITLE, createStringResource(title));
        treeTitle.setRenderBodyOnly(true);
        treeHeader.add(treeTitle);

        List<InlineMenuItem> actions = createTreeMenu();
        DropdownButtonDto model = new DropdownButtonDto(null, "fa fa-cog", null, actions);
        DropdownButtonPanel treeMenu = new DropdownButtonPanel(ID_TREE_MENU, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn-default";
            }

            @Override
            protected boolean visibleCaret() {
                return false;
            }
        };
        treeMenu.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return !actions.isEmpty();
            }
        });
        treeMenu.setOutputMarkupId(true);
        treeMenu.setOutputMarkupPlaceholderTag(true);
        treeHeader.add(treeMenu);

        ISortableTreeProvider provider = new OrgTreeProvider(this, getModel()) {

            @Override
            protected ObjectFilter getCustomFilter(){
                return OrgTreePanel.this.getCustomFilter();
            }
        };

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
//                    response.render(OnDomReadyHeaderItem.forScript(
//                            "updateHeight('" + getMarkupId() + "', ['#" + form.getMarkupId() + "'], ['#"
//                                    + OrgTreePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])"));
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
            public Set<TreeSelectableBean<OrgType>> getCollapsedItems(){
                return OrgTreePanel.this.getCollapsedItems(getOrgTreeStateStorage());
            }
            @Override
            public void setCollapsedItems(TreeStateSet items){
                OrgTreePanel.this.setCollapsedItems(null, getOrgTreeStateStorage());
            }
        };

        contentPannels = new ArrayList<OrgTreeFolderContent>();

        MidpointNestedTree tree = new MidpointNestedTree(ID_TREE, provider, treeStateMode) {


            @Override
            protected Component newContentComponent(String id, IModel<TreeSelectableBean<OrgType>> model) {
                OrgTreeFolderContent contentPannel = new OrgTreeFolderContent(id, model, selectable, selected, this, getOrgTreeStateStorage()) {

                    @Override
                    protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected, AjaxRequestTarget target) {
                        OrgTreePanel.this.selectTreeItemPerformed(selected, target);
                    }

                    @Override
                    protected IModel<Boolean> getCheckboxModel(IModel<TreeSelectableBean<OrgType>> org) {
                        return OrgTreePanel.this.getCheckBoxValueModel(org);
                    }

                    @Override
                    protected void onUpdateCheckbox(AjaxRequestTarget target) {
                        selected = getModel();
                        onOrgTreeCheckBoxSelectionPerformed(target, selected);
                    }

                    @Override
                    protected IModel<List<InlineMenuItem>> createInlineMenuItemsModel() {
                        return new ReadOnlyModel<>(() -> createTreeChildrenMenuInternal(model.getObject(), getPageBase().getCompiledGuiProfile()));
                    }
                };
                contentPannels.add(contentPannel);
                return contentPannel;
            }

            @Override
            public void collapse(TreeSelectableBean<OrgType> collapsedItem) {
                super.collapse(collapsedItem);

                Set<TreeSelectableBean<OrgType>> items = OrgTreePanel.this.getExpandedItems(getOrgTreeStateStorage());
                if (items != null && items.contains(collapsedItem)) {
                    items.remove(collapsedItem);
                }
                OrgTreePanel.this.setExpandedItems((TreeStateSet) items, getOrgTreeStateStorage());
                OrgTreePanel.this.addCollapsedItem(collapsedItem, getOrgTreeStateStorage());
            }

            @Override
            public void expand(TreeSelectableBean<OrgType> expandedItem) {
                super.expand(expandedItem);

                Set<TreeSelectableBean<OrgType>> items = OrgTreePanel.this.getCollapsedItems(getOrgTreeStateStorage());
                if (items != null && items.contains(expandedItem)) {
                    items.remove(expandedItem);
                }
                OrgTreePanel.this.setCollapsedItems((TreeStateSet) items, getOrgTreeStateStorage());
                OrgTreePanel.this.addExpandedItem(expandedItem, getOrgTreeStateStorage());
            }
        };
        tree.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
        tree.setOutputMarkupId(true);
        tree.add(new WindowsTheme());
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
        }

        @Override
        public Set<TreeSelectableBean<OrgType>> getObject() {
            Set<TreeSelectableBean<OrgType>> dtos = TreeStateModel.this.getExpandedItems();
            Set<TreeSelectableBean<OrgType>> collapsedItems = TreeStateModel.this.getCollapsedItems();

            // just to have root expanded at all time
            Iterator<TreeSelectableBean<OrgType>> iterator = provider.getRoots();
            if (iterator.hasNext()) {
                TreeSelectableBean<OrgType> root = iterator.next();
                if (set.isEmpty() || !set.contains(root)) {
                    set.add(root);
                }
            }

            if (collapsedItems != null) {
                for (TreeSelectableBean<OrgType> collapsedItem : collapsedItems) {
                    if (set.contains(collapsedItem)) {
                        set.remove(collapsedItem);
                    }
                }
            }
            if (dtos != null && (dtos instanceof TreeStateSet)) {
                for (TreeSelectableBean<OrgType> orgTreeDto : dtos) {
                    if (!set.contains(orgTreeDto)) {
                        set.add(orgTreeDto);
                    }
                }
            }
            return set;
        }

        public Set<TreeSelectableBean<OrgType>> getExpandedItems(){
            return storage != null ? storage.getExpandedItems() : null;
        }

        public Set<TreeSelectableBean<OrgType>> getCollapsedItems(){
            return storage != null ? storage.getCollapsedItems() : null;
        }

        public void setCollapsedItems(TreeStateSet<TreeSelectableBean<OrgType>> items){
            if (storage != null){
                storage.setCollapsedItems(items);
            }
        }
    }

    protected ObjectFilter getCustomFilter(){
        return null;
    }

    protected List<InlineMenuItem> createTreeMenu() {
        return new ArrayList<>();
    }

    private List<InlineMenuItem> createTreeChildrenMenuInternal(TreeSelectableBean<OrgType> org, CompiledGuiProfile adminGuiConfig) {
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
                            collapseAllPerformed(org, target);
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
                            expandAllPerformed(org, target);
                        }
                    };
                }
            };
            items.add(item);
        }

        List<InlineMenuItem> additionalActions = createTreeChildrenMenu(org);
        if (additionalActions != null) {
            items.addAll(additionalActions);
        }
        return items;
    }

    protected List<InlineMenuItem> createTreeChildrenMenu(TreeSelectableBean<OrgType> org) {
        return null;
    }

    protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected, AjaxRequestTarget target) {

    }

    private void expandAllPerformed(TreeSelectableBean<OrgType> org, AjaxRequestTarget target) {
        TreeStateSet expandItems = new TreeStateSet<>();
        if (getExpandedItems(getOrgTreeStateStorage()) != null) {
            expandItems.addAll(getExpandedItems(getOrgTreeStateStorage()));
        }
        if (!expandItems.contains(org)) {
            expandItems.add(org);
        }
        TreeStateSet collapsedItems = new TreeStateSet<>();
        if (getCollapsedItems(getOrgTreeStateStorage()) != null) {
            collapsedItems.addAll(getCollapsedItems(getOrgTreeStateStorage()));
        }
        if (!collapsedItems.contains(org)) {
            collapsedItems.remove(org);
        }
        Iterator<? extends TreeSelectableBean<OrgType>> childIterator = getTree().getProvider().getChildren(org);
        while (childIterator.hasNext()) {
            TreeSelectableBean<OrgType> child = childIterator.next();
            if (!expandItems.contains(child)) {
                expandItems.add(child);
            }
            if (collapsedItems.contains(child)) {
                collapsedItems.remove(child);
            }
        }
        setCollapsedItems(collapsedItems, getOrgTreeStateStorage());
        setExpandedItems(expandItems, getOrgTreeStateStorage());
        target.add(getTree());
    }

    private void collapseAllPerformed(TreeSelectableBean<OrgType> org, AjaxRequestTarget target) {
        TreeStateSet expandItems = new TreeStateSet<>();
        if (getExpandedItems(getOrgTreeStateStorage()) != null) {
            expandItems.addAll(getExpandedItems(getOrgTreeStateStorage()));
        }
        TreeStateSet collapsedItems = new TreeStateSet<>();
        if (getCollapsedItems(getOrgTreeStateStorage()) != null) {
            collapsedItems.addAll(getCollapsedItems(getOrgTreeStateStorage()));
        }
        Iterator<? extends TreeSelectableBean<OrgType>> childIterator = getTree().getProvider().getChildren(org);
        while (childIterator.hasNext()) {
            TreeSelectableBean<OrgType> child = childIterator.next();
            if (expandItems.contains(child)) {
                expandItems.remove(child);
            }
            if (!collapsedItems.contains(child)) {
                collapsedItems.add(child);
            }
        }
        setCollapsedItems(collapsedItems, getOrgTreeStateStorage());
        setExpandedItems(expandItems, getOrgTreeStateStorage());
        target.add(getTree());
    }

    public Set<TreeSelectableBean<OrgType>> getExpandedItems(OrgTreeStateStorage storage){
        return storage != null ? storage.getExpandedItems() : null;
    }

    public void setExpandedItems(TreeStateSet items, OrgTreeStateStorage storage){
        if (storage != null){
            storage.setExpandedItems(items);
        }
    }

    public void addExpandedItem(TreeSelectableBean<OrgType> item, OrgTreeStateStorage storage){
        if (storage == null) {
            return;
        }

        Set<TreeSelectableBean<OrgType>> expandedItems = storage.getExpandedItems();
        if (expandedItems == null) {
            TreeStateSet<TreeSelectableBean<OrgType>> expanded = new TreeStateSet<>();
            expanded.add(item);
            storage.setExpandedItems(expanded);
        } else {
            expandedItems.add(item);
        }
    }

    public Set<TreeSelectableBean<OrgType>> getCollapsedItems(OrgTreeStateStorage storage){
        return storage != null ? storage.getCollapsedItems() : null;
    }

    public void setCollapsedItems(TreeStateSet<TreeSelectableBean<OrgType>> items, OrgTreeStateStorage storage){
        if (storage != null){
            storage.setCollapsedItems(items);
        }
    }

    public void addCollapsedItem(TreeSelectableBean<OrgType> item, OrgTreeStateStorage storage){
        if (storage == null ) {
            return;
        }
        Set<TreeSelectableBean<OrgType>> collapsedItems = storage.getCollapsedItems();
        if (collapsedItems == null) {
            TreeStateSet<TreeSelectableBean<OrgType>> collapsed = new TreeStateSet<>();
            collapsed.add(item);
            storage.setCollapsedItems(collapsed);
        } else {
            collapsedItems.add(item);
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
        return new PropertyModel<>(rowModel, TreeSelectableBean.F_SELECTED);
    }

    public void refreshContentPannels() {
        if (contentPannels != null) {
            for (OrgTreeFolderContent contentPannel : contentPannels) {
                contentPannel.onInitialize();
            }
        }
    }

    protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel){}
}
