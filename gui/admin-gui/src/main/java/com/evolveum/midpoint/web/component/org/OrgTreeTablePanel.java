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
package com.evolveum.midpoint.web.component.org;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractTreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgTreeProvider;
import com.evolveum.midpoint.web.page.admin.users.component.SelectableFolderContent;
import com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Used in assignment dialog when "assign org" is chosen.
 * 
 * Mostly copy&paste from TreeTablePanel. But we do not mind now. It will be
 * reworked anyway.
 * 
 * @author katkav
 */
public class OrgTreeTablePanel extends AbstractTreeTablePanel{
	
    private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

    public OrgTreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
        
        selected = new LoadableModel<OrgTreeDto>() {
            @Override
            protected OrgTreeDto load() {
                return getRootFromProvider();
            }
        };
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer treeHeader = new WebMarkupContainer(ID_TREE_HEADER);
        treeHeader.setOutputMarkupId(true);
        add(treeHeader);

        InlineMenu treeMenu = new InlineMenu(ID_TREE_MENU, new Model<>((Serializable) createTreeMenu()));
        treeHeader.add(treeMenu);

        ISortableTreeProvider provider = new OrgTreeProvider(this, getModel());
        List<IColumn<OrgTreeDto, String>> columns = new ArrayList<>();
        columns.add(new TreeColumn<OrgTreeDto, String>(createStringResource("TreeTablePanel.hierarchy")));

        WebMarkupContainer treeContainer = new WebMarkupContainer(ID_TREE_CONTAINER) {

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);

                //method computes height based on document.innerHeight() - screen height;
                response.render(OnDomReadyHeaderItem.forScript(computeTreeHeight()));
            }
        };
        add(treeContainer);

        TableTree<OrgTreeDto, String> tree = new TableTree<OrgTreeDto, String>(ID_TREE, columns, provider,
                Integer.MAX_VALUE, new TreeStateModel(provider)) {

            @Override
            protected Component newContentComponent(String id, IModel<OrgTreeDto> model) {
                return new SelectableFolderContent(id, this, model, selected) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        super.onClick(target);

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
        };
        tree.getTable().add(AttributeModifier.replace("class", "table table-striped table-condensed"));
        tree.add(new WindowsTheme());
//        tree.add(AttributeModifier.replace("class", "tree-midpoint"));
        treeContainer.add(tree);

        initTables();
        initSearch();
    }

    protected CharSequence computeTreeHeight() {
        return "updateHeight('" + getMarkupId()
                + "', ['#" + OrgTreeTablePanel.this.get(ID_FORM).getMarkupId() + "'], ['#"
                + OrgTreeTablePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])";
    }

    private void initTables() {
        Form form = new Form(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        //Child org. units container initialization
        final ObjectDataProvider childTableProvider = new ObjectDataProvider<OrgTableDto, OrgType>(this, OrgType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<OrgType> obj) {
                return OrgTableDto.createDto(obj);
            }

            @Override
            public ObjectQuery getQuery() {
                return createOrgChildQuery();
            }
        };
        childTableProvider.setOptions(WebModelUtils.createMinimalOptions());

        WebMarkupContainer childOrgUnitContainer = new WebMarkupContainer(ID_CONTAINER_CHILD_ORGS);
        childOrgUnitContainer.setOutputMarkupId(true);
        childOrgUnitContainer.setOutputMarkupPlaceholderTag(true);
        childOrgUnitContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return childTableProvider.size() != 0;
            }
        });
        form.add(childOrgUnitContainer);

        List<IColumn<OrgTableDto, String>> childTableColumns = createChildTableColumns();
        final TablePanel childTable = new TablePanel<>(ID_CHILD_TABLE, childTableProvider, childTableColumns,
                UserProfileStorage.TableId.TREE_TABLE_PANEL_CHILD, UserProfileStorage.DEFAULT_PAGING_SIZE);
        childTable.setOutputMarkupId(true);
        childTable.getNavigatorPanel().add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return childTableProvider.size() > childTable.getDataTable().getItemsPerPage();
            }
        });
        childOrgUnitContainer.add(childTable);
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
        
        return items;
    }

    private List<IColumn<OrgTableDto, String>> createChildTableColumns() {
        List<IColumn<OrgTableDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
        columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                ObjectTypeGuiDescriptor guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());

                String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

                return new Model<>(icon);
            }
        });

        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("ObjectType.name"), OrgTableDto.F_NAME, "name"));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.displayName"), OrgTableDto.F_DISPLAY_NAME));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.identifier"), OrgTableDto.F_IDENTIFIER));
      
        return columns;
    }

    /**
     * This method check selection in table.
     */
    public List<OrgTableDto> getSelectedOrgs(AjaxRequestTarget target) {
        List<OrgTableDto> objects = WebMiscUtil.getSelectedData(getOrgChildTable());
        if (objects.isEmpty()) {
            warn(getString("TreeTablePanel.message.nothingSelected"));
            target.add(getPageBase().getFeedbackPanel());
        }

        return objects;
    }
    
    public List<OrgTableDto> getSelectedOrgs() {
        List<OrgTableDto> objects = WebMiscUtil.getSelectedData(getOrgChildTable());
        if (objects.isEmpty()) {
//            warn(getString("TreeTablePanel.message.nothingSelected"));
//            target.add(getPageBase().getFeedbackPanel());
        }

        return objects;
    }
   
    private void selectTreeItemPerformed(AjaxRequestTarget target) {
        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        TablePanel orgTable = getOrgChildTable();
        orgTable.setCurrentPage(null);

        WebMarkupContainer orgChildContainer = getOrgChildContainer();
        if (target != null) {
            if (orgChildContainer != null) {
                target.add(orgChildContainer);
            }
            target.add(get(ID_SEARCH_FORM));
        }
    }
  
   
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enable) {
        List<OrgTableDto> objects = getSelectedOrgs(target);
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
            ObjectDelta delta = WebModelUtils.createActivationAdminStatusDelta(object.getType(), object.getOid(),
                    enable, page.getPrismContext());

            WebModelUtils.save(delta, subResult, page);
        }
        result.computeStatusComposite();

        page.showResult(result);
        target.add(page.getFeedbackPanel());

        refreshTable(target);
    }

    @Override
    protected void refreshTable(AjaxRequestTarget target) {
        ObjectDataProvider orgProvider = (ObjectDataProvider) getOrgChildTable().getDataTable().getDataProvider();
        orgProvider.clearCache();

        target.add(getOrgChildContainer());
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
    
    private static class TreeStateModel extends AbstractReadOnlyModel<Set<OrgTreeDto>> {

        private TreeStateSet<OrgTreeDto> set = new TreeStateSet<>();
        private ISortableTreeProvider provider;

        TreeStateModel(ISortableTreeProvider provider) {
            this.provider = provider;
        }

        @Override
        public Set<OrgTreeDto> getObject() {
            //just to have root expanded at all time
            if (set.isEmpty()) {
                Iterator<OrgTreeDto> iterator = provider.getRoots();
                if (iterator.hasNext()) {
                    set.add(iterator.next());
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
    
    private void addOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org){
        //TODO
    }

    private void removeOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org){
        //TODO
    }
}

