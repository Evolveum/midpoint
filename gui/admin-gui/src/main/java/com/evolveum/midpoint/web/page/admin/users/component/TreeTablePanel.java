/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.*;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * todo create function computeHeight() in midpoint.js, update height properly when in "mobile" mode... [lazyman]
 * todo implement midpoint theme for tree [lazyman]
 *
 * @author lazyman
 */
public class TreeTablePanel extends SimplePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

    private static final int CONFIRM_DELETE = 0;
    private static final int CONFIRM_DELETE_ROOT = 1;

    private static final String DOT_CLASS = TreeTablePanel.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_MOVE_OBJECTS = DOT_CLASS + "moveObjects";
    private static final String OPERATION_MOVE_OBJECT = DOT_CLASS + "moveObject";
    private static final String OPERATION_UPDATE_OBJECTS = DOT_CLASS + "updateObjects";
    private static final String OPERATION_UPDATE_OBJECT = DOT_CLASS + "updateObject";
    private static final String OPERATION_RECOMPUTE = DOT_CLASS + "recompute";

    private static final String ID_TREE = "tree";
    private static final String ID_TREE_CONTAINER = "treeContainer";
    private static final String ID_CONTAINER_CHILD_ORGS = "childOrgContainer";
    private static final String ID_CONTAINER_MANAGER = "managerContainer";
    private static final String ID_CONTAINER_MEMBER = "memberContainer";
    private static final String ID_CHILD_TABLE = "childUnitTable";
    private static final String ID_MANAGER_TABLE = "managerTable";
    private static final String ID_MEMBER_TABLE = "memberTable";
    private static final String ID_FORM = "form";
    private static final String ID_CONFIRM_DELETE_POPUP = "confirmDeletePopup";
    private static final String ID_MOVE_POPUP = "movePopup";
    private static final String ID_ADD_DELETE_POPUP = "addDeletePopup";
    private static final String ID_TREE_MENU = "treeMenu";
    private static final String ID_TREE_HEADER = "treeHeader";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";

    private IModel<OrgTreeDto> selected = new LoadableModel<OrgTreeDto>() {

        @Override
        protected OrgTreeDto load() {
            TabbedPanel currentTabbedPanel = null;
            MidPointAuthWebSession session = TreeTablePanel.this.getSession();
            SessionStorage storage = session.getSessionStorage();
            if (getTree().findParent(TabbedPanel.class) != null) {
                currentTabbedPanel = getTree().findParent(TabbedPanel.class);
                int tabId = currentTabbedPanel.getSelectedTab();
                if (storage.getUsers().getSelectedTabId() != -1 && tabId != storage.getUsers().getSelectedTabId()){
                    storage.getUsers().setSelectedItem(null);
                }
            }
            if (storage.getUsers().getSelectedItem() != null){
                return storage.getUsers().getSelectedItem();
            } else {
                return getRootFromProvider();
            }
        }
    };

    public TreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
    }

    @Override
    protected void initLayout() {
        add(new ConfirmationDialog(ID_CONFIRM_DELETE_POPUP,
                createStringResource("TreeTablePanel.dialog.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);

                switch (getConfirmType()) {
                    case CONFIRM_DELETE:
                        deleteConfirmedPerformed(target);
                        break;
                    case CONFIRM_DELETE_ROOT:
                        deleteRootConfirmedPerformed(target);
                        break;
                }

            }
        });

        add(new OrgUnitBrowser(ID_MOVE_POPUP) {

            @Override
            protected void createRootPerformed(AjaxRequestTarget target) {
                moveConfirmedPerformed(target, null, null, Operation.MOVE);
            }

            @Override
            protected void rowSelected(AjaxRequestTarget target, IModel<OrgTableDto> row, Operation operation) {
                moveConfirmedPerformed(target, selected.getObject(), row.getObject(), operation);
            }

            @Override
            public ObjectQuery createRootQuery(){
                ArrayList<String> oids = new ArrayList<>();
                ObjectQuery query = new ObjectQuery();

                if(isMovingRoot() && getRootFromProvider() != null){
                    oids.add(getRootFromProvider().getOid());
                }

                if(oids.isEmpty()){
                    return null;
                }

                ObjectFilter oidFilter = InOidFilter.createInOid(oids);
                query.setFilter(NotFilter.createNot(oidFilter));

                return query;
            }
        });

        add(new OrgUnitAddDeletePopup(ID_ADD_DELETE_POPUP){

            @Override
            public void addPerformed(AjaxRequestTarget target, OrgType selected){
                addOrgUnitToUserPerformed(target, selected);
            }

            @Override
            public void removePerformed(AjaxRequestTarget target, OrgType selected){
                removeOrgUnitToUserPerformed(target, selected);
            }

            @Override
            public ObjectQuery getAddProviderQuery(){
                return null;
            }

            @Override
            public ObjectQuery getRemoveProviderQuery(){
                return null;
            }
        });

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
                response.render(OnDomReadyHeaderItem.forScript("updateHeight('" + getMarkupId()
                        + "', ['#" + TreeTablePanel.this.get(ID_FORM).getMarkupId() + "'], ['#"
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
            public void collapse(OrgTreeDto collapsedItem){
                super.collapse(collapsedItem);
                MidPointAuthWebSession session = TreeTablePanel.this.getSession();
                SessionStorage storage = session.getSessionStorage();
                Set<OrgTreeDto> items  = storage.getUsers().getExpandedItems();
                if (items != null && items.contains(collapsedItem)){
                    items.remove(collapsedItem);
                }
                storage.getUsers().setExpandedItems((TreeStateSet)items);
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
//        tree.add(AttributeModifier.replace("class", "tree-midpoint"));
        treeContainer.add(tree);

        initTables();
        initSearch();
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
        form.add(childOrgUnitContainer);

        List<IColumn<OrgTableDto, String>> childTableColumns = createChildTableColumns();

        MidPointAuthWebSession session = getSession();
        SessionStorage storage = session.getSessionStorage();
        int pageSize = storage.getUserProfile().getPagingSize(UserProfileStorage.TableId.TREE_TABLE_PANEL_CHILD);

        final TablePanel childTable = new TablePanel<>(ID_CHILD_TABLE, childTableProvider, childTableColumns,
                UserProfileStorage.TableId.TREE_TABLE_PANEL_CHILD, pageSize);
        childTable.setOutputMarkupId(true);
        childTable.getNavigatorPanel().add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return childTableProvider.size() > childTable.getDataTable().getItemsPerPage();
            }
        });
        childOrgUnitContainer.add(childTable);

        //Manager container initialization
        final ObjectDataProvider managerTableProvider = new ObjectDataProvider<OrgTableDto, UserType>(this, UserType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<UserType> obj) {
                return OrgTableDto.createDto(obj);
            }

            @Override
            public ObjectQuery getQuery() {
                return createManagerTableQuery();
            }
        };
        managerTableProvider.setOptions(WebModelUtils.createMinimalOptions());

        WebMarkupContainer managerContainer = new WebMarkupContainer(ID_CONTAINER_MANAGER);
        managerContainer.setOutputMarkupId(true);
        managerContainer.setOutputMarkupPlaceholderTag(true);
        form.add(managerContainer);

        List<IColumn<OrgTableDto, String>> managerTableColumns = createUserTableColumns(true);
        final TablePanel managerTablePanel = new TablePanel<>(ID_MANAGER_TABLE, managerTableProvider, managerTableColumns,
                UserProfileStorage.TableId.TREE_TABLE_PANEL_MANAGER, UserProfileStorage.DEFAULT_PAGING_SIZE);
        managerTablePanel.setOutputMarkupId(true);
        managerTablePanel.getNavigatorPanel().add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return managerTableProvider.size() > managerTablePanel.getDataTable().getItemsPerPage();
            }
        });
        managerContainer.add(managerTablePanel);

        //Member container initialization
        final ObjectDataProvider memberTableProvider = new ObjectDataProvider<OrgTableDto, UserType>(this, UserType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<UserType> obj) {
                return OrgTableDto.createDto(obj);
            }

            @Override
            public ObjectQuery getQuery() {
                return createMemberQuery();
            }
        };
        memberTableProvider.setOptions(WebModelUtils.createMinimalOptions());

        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        List<IColumn<OrgTableDto, String>> memberTableColumns = createUserTableColumns(false);
        final TablePanel memberTablePanel = new TablePanel<>(ID_MEMBER_TABLE, memberTableProvider, memberTableColumns,
                UserProfileStorage.TableId.TREE_TABLE_PANEL_MEMBER, UserProfileStorage.DEFAULT_PAGING_SIZE);
        memberTablePanel.setOutputMarkupId(true);
        memberTablePanel.getNavigatorPanel().add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return memberTableProvider.size() > memberTablePanel.getDataTable().getItemsPerPage();
            }
        });
        memberContainer.add(memberTablePanel);
    }

    /**
     * TODO - test search
     * */
    private void initSearch() {
        Form form = new Form(ID_SEARCH_FORM);
        form.setOutputMarkupId(true);
        add(form);

        BasicSearchPanel basicSearch = new BasicSearchPanel(ID_BASIC_SEARCH, new Model()) {

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                clearTableSearchPerformed(target);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                tableSearchPerformed(target);
            }
        };
        form.add(basicSearch);
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
        item = new InlineMenuItem(createStringResource("TreeTablePanel.moveRoot"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                moveRootPerformed(target);
            }
        });
        items.add(item);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.deleteRoot"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteRootPerformed(target);
            }
        });
        items.add(item);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.recomputeRoot"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                recomputeRootPerformed(target, OrgUnitBrowser.Operation.RECOMPUTE);
            }
        });
        items.add(item);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.editRoot"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                editRootPerformed(target);
            }
        });
        items.add(item);

        return items;
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ConfirmationDialog dialog = (ConfirmationDialog) TreeTablePanel.this.get(ID_CONFIRM_DELETE_POPUP);
                switch (dialog.getConfirmType()) {
                    case CONFIRM_DELETE:
                        return createStringResource("TreeTablePanel.message.deleteObjectConfirm",
                                WebMiscUtil.getSelectedData(getOrgChildTable()).size()).getString();
                    case CONFIRM_DELETE_ROOT:
                        OrgTreeDto dto = getRootFromProvider();

                        return createStringResource("TreeTablePanel.message.deleteRootConfirm",
                                dto.getName(), dto.getDisplayName()).getString();
                }
                return null;
            }
        };
    }

    private OrgTreeDto getRootFromProvider() {
        TableTree<OrgTreeDto, String> tree = getTree();
        ITreeProvider<OrgTreeDto> provider = tree.getProvider();
        Iterator<? extends OrgTreeDto> iterator = provider.getRoots();

        return iterator.hasNext() ? iterator.next() : null;
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

        columns.add(new LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"), OrgTableDto.F_NAME, "name") {

            @Override
            public boolean isEnabled(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                return UserType.class.equals(dto.getType()) || OrgType.class.equals(dto.getType());
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
                setResponsePage(PageOrgUnit.class, parameters);
            }
        });
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.displayName"), OrgTableDto.F_DISPLAY_NAME));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.identifier"), OrgTableDto.F_IDENTIFIER));
        columns.add(new InlineMenuHeaderColumn(initOrgChildInlineMenu()));

        return columns;
    }

    private List<IColumn<OrgTableDto, String>> createUserTableColumns(boolean isManagerTable) {
        List<IColumn<OrgTableDto, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
        columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                OrgTreeDto selectedDto = selected.getObject();
                String selectedOid = dto != null ? selectedDto.getOid() : getModel().getObject();

                ObjectTypeGuiDescriptor guiDescriptor = null;
                if(dto != null && dto.getRelation() == null) {
                    guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
                } else {
                    if(dto != null){
                        for(ObjectReferenceType parentOrgRef: dto.getObject().getParentOrgRef()){
                            if(parentOrgRef.getOid().equals(selectedOid) && SchemaConstants.ORG_MANAGER.equals(parentOrgRef.getRelation())){
                                guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getRelation());
                                String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                                return new Model<>(icon);
                            }
                        }

                        guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
                    }
                }

                String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

                return new Model<>(icon);
            }
        });

        columns.add(new LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"), OrgTableDto.F_NAME, "name") {

            @Override
            public boolean isEnabled(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                return UserType.class.equals(dto.getType()) || OrgType.class.equals(dto.getType());
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
                setResponsePage(new PageUser(parameters, (PageTemplate) target.getPage()));
            }
        });
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("UserType.givenName"),
                UserType.F_GIVEN_NAME.getLocalPart(), OrgTableDto.F_OBJECT + ".givenName"));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("UserType.familyName"),
                UserType.F_FAMILY_NAME.getLocalPart(), OrgTableDto.F_OBJECT + ".familyName"));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("UserType.fullName"),
                UserType.F_FULL_NAME.getLocalPart(), OrgTableDto.F_OBJECT + ".fullName"));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("UserType.emailAddress"),
                null, OrgTableDto.F_OBJECT + ".emailAddress"));
        columns.add(new InlineMenuHeaderColumn(isManagerTable ? initOrgManagerInlineMenu() : initOrgMemberInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> initOrgChildInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addOrgUnit"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addOrgUnitPerformed(target);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem());

        //TODO - disabled until issue MID-1809 is resolved. Uncomment when finished
//        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addToHierarchy"), true,
//                new HeaderMenuAction(this) {
//
//                    @Override
//                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                        addToHierarchyPerformed(target);
//                    }
//                }));
//        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.removeFromHierarchy"), true,
//                new HeaderMenuAction(this) {
//
//                    @Override
//                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                        removeFromHierarchyPerformed(target);
//                    }
//                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.enable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, true);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.disable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, false);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.move"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        movePerformed(target, OrgUnitBrowser.Operation.MOVE);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel,menu.recompute"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        recomputePerformed(target, OrgUnitBrowser.Operation.RECOMPUTE);
                    }
                }));

        return headerMenuItems;
    }

    private List<InlineMenuItem> initOrgMemberInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addMember"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addUserPerformed(target, false);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.enable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, true);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.disable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, false);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel,menu.recompute"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        recomputePerformed(target, OrgUnitBrowser.Operation.RECOMPUTE);
                    }
                }));

        return headerMenuItems;
    }

    private List<InlineMenuItem> initOrgManagerInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addManager"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addUserPerformed(target, true);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.enable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, true);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.disable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, false);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel,menu.recompute"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        recomputePerformed(target, OrgUnitBrowser.Operation.RECOMPUTE);
                    }
                }));

        return headerMenuItems;
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
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(selected.getObject().getOid());
            ref.setType(OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);

            PrismContext context = page.getPrismContext();
            context.adopt(org);

            return org.asPrismContainer();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create child org. unit with parent org. reference", ex);
            page.error("Couldn't create child org. unit with parent org. reference, reason: " + ex.getMessage());

            target.add(page.getFeedbackPanel());
        }

        return null;
    }

    private PrismObject<UserType> addUserPerformed(AjaxRequestTarget target, UserType user, boolean isUserManager) {
        PageBase page = getPageBase();
        try {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(selected.getObject().getOid());
            ref.setType(OrgType.COMPLEX_TYPE);

            if(isUserManager){
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
    private List<OrgTableDto> isAnythingSelected(AjaxRequestTarget target) {
        List<OrgTableDto> objects = WebMiscUtil.getSelectedData(getOrgChildTable());
        if (objects.isEmpty()) {
            warn(getString("TreeTablePanel.message.nothingSelected"));
            target.add(getPageBase().getFeedbackPanel());
        }

        return objects;
    }

    private void deletePerformed(AjaxRequestTarget target) {
        List<OrgTableDto> objects = isAnythingSelected(target);
        if (objects.isEmpty()) {
            return;
        }

        ConfirmationDialog dialog = (ConfirmationDialog) get(ID_CONFIRM_DELETE_POPUP);
        dialog.setConfirmType(CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<OrgTableDto> objects = isAnythingSelected(target);
        if (objects.isEmpty()) {
            return;
        }

        PageBase page = getPageBase();
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        for (OrgTableDto object : objects) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            WebModelUtils.deleteObject(object.getType(), object.getOid(), subResult, page);
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

    private void movePerformed(AjaxRequestTarget target, OrgUnitBrowser.Operation operation) {
        movePerformed(target, operation, null, false);
    }

    private void movePerformed(AjaxRequestTarget target, OrgUnitBrowser.Operation operation, OrgTableDto selected, boolean movingRoot) {
        List<OrgTableDto> objects;
        if (selected == null) {
            objects = isAnythingSelected(target);
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

    private PrismReferenceValue createPrismRefValue(OrgDto dto) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(dto.getOid());
        value.setRelation(dto.getRelation());
        value.setTargetType(ObjectTypes.getObjectType(dto.getType()).getTypeQName());
        return value;
    }

    private ObjectDelta createMoveDelta(PrismObject<OrgType> orgUnit, OrgTreeDto oldParent, OrgTableDto newParent,
                                        OrgUnitBrowser.Operation operation) {
        ObjectDelta delta = orgUnit.createDelta(ChangeType.MODIFY);
        PrismReferenceDefinition refDef = orgUnit.getDefinition().findReferenceDefinition(OrgType.F_PARENT_ORG_REF);
        ReferenceDelta refDelta = delta.createReferenceModification(OrgType.F_PARENT_ORG_REF, refDef);
        PrismReferenceValue value;
        switch (operation) {
            case ADD:
                LOGGER.debug("Adding new parent {}", new Object[]{newParent});
                //adding parentRef newParent to orgUnit
                value = createPrismRefValue(newParent);
                refDelta.addValuesToAdd(value);
                break;
            case REMOVE:
                LOGGER.debug("Removing new parent {}", new Object[]{newParent});
                //removing parentRef newParent from orgUnit
                value = createPrismRefValue(newParent);
                refDelta.addValuesToDelete(value);
                break;
            case MOVE:
                if (oldParent == null && newParent == null) {
                    LOGGER.debug("Moving to root");
                    //moving orgUnit to root, removing all parentRefs
                    PrismReference ref = orgUnit.findReference(OrgType.F_PARENT_ORG_REF);
                    if (ref != null) {
                        for (PrismReferenceValue val : ref.getValues()) {
                            refDelta.addValuesToDelete(val.clone());
                        }
                    }
                } else {
                    LOGGER.debug("Adding new parent {}, removing old parent {}", new Object[]{newParent, oldParent});
                    //moving from old to new, removing oldParent adding newParent refs
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
        OperationResult result = new OperationResult(OPERATION_MOVE_OBJECTS);
        for (OrgTableDto object : objects) {
            OperationResult subResult = result.createSubresult(OPERATION_MOVE_OBJECT);

            PrismObject<OrgType> orgUnit = WebModelUtils.loadObject(OrgType.class, object.getOid(),
                    WebModelUtils.createOptionsForParentOrgRefs(), subResult, getPageBase());
            try {
                ObjectDelta delta = createMoveDelta(orgUnit, oldParent, newParent, operation);

                model.executeChanges(WebMiscUtil.createDeltaCollection(delta), null,
                        page.createSimpleTask(OPERATION_MOVE_OBJECT), subResult);
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't move object " + null + " to " + null + ".", ex);
                LoggingUtils.logException(LOGGER, "Couldn't move object {} to {}", ex, object.getName());
            } finally {
                subResult.computeStatusIfUnknown();
            }
        }
        result.computeStatusComposite();

        ObjectDataProvider provider = (ObjectDataProvider) getOrgChildTable().getDataTable().getDataProvider();
        provider.clearCache();

        page.showResult(result);
        dialog.close(target);

        refreshTabbedPanel(target);
    }

    private void refreshTabbedPanel(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        TabbedPanel tabbedPanel = findParent(TabbedPanel.class);
        IModel<List<ITab>> tabs = tabbedPanel.getTabs();

        if (tabs instanceof LoadableModel) {
            ((LoadableModel) tabs).reset();
        }

        tabbedPanel.setSelectedTab(0);

        target.add(tabbedPanel);
        target.add(page.getFeedbackPanel());
    }

    private TableTree<OrgTreeDto, String> getTree() {
        return (TableTree<OrgTreeDto, String>) get(createComponentPath(ID_TREE_CONTAINER, ID_TREE));
    }

    private WebMarkupContainer getOrgChildContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS));
    }

    private WebMarkupContainer getMemberContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER));
    }

    private WebMarkupContainer getManagerContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_MANAGER));
    }

    private TablePanel getOrgChildTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS, ID_CHILD_TABLE));
    }

    private TablePanel getMemberTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    private TablePanel getManagerTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_MANAGER, ID_MANAGER_TABLE));
    }

    private void selectTreeItemPerformed(AjaxRequestTarget target) {
        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        TablePanel orgTable = getOrgChildTable();
        orgTable.setCurrentPage(null);

        TablePanel memberTable = getMemberTable();
        memberTable.setCurrentPage(null);

        TablePanel managerTable = getManagerTable();
        managerTable.setCurrentPage(null);

        target.add(getOrgChildContainer(), getMemberContainer(), getManagerContainer());
        target.add(get(ID_SEARCH_FORM));
    }

    private ObjectQuery createOrgChildQuery() {
        OrgTreeDto dto = selected.getObject();
        String oid = dto != null ? dto.getOid() : getModel().getObject();

        OrgFilter org = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        String object = basicSearch.getModelObject();

        if (StringUtils.isEmpty(object)) {
            return ObjectQuery.createObjectQuery(org);
        }

        PageBase page = getPageBase();
        PrismContext context = page.getPrismContext();

        PolyStringNormalizer normalizer = context.getDefaultPolyStringNormalizer();
        String normalizedString = normalizer.normalize(object);
        if (StringUtils.isEmpty(normalizedString)) {
            return ObjectQuery.createObjectQuery(org);
        }

        SubstringFilter substring =  SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class, context,
                PolyStringNormMatchingRule.NAME, normalizedString);

        AndFilter and = AndFilter.createAnd(org, substring);

        return ObjectQuery.createObjectQuery(and);
    }

    private ObjectQuery createManagerTableQuery(){
        ObjectQuery query = null;
        OrgTreeDto dto = selected.getObject();
        String oid = dto != null ? dto.getOid() : getModel().getObject();

        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        String object = basicSearch.getModelObject();

        SubstringFilter substring;
        PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
        String normalizedString = normalizer.normalize(object);

        if (StringUtils.isEmpty(normalizedString)) {
            substring = null;
        } else {
            substring =  SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class, getPageBase().getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalizedString);
        }

        try {
            OrgFilter org = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

            PrismReferenceValue referenceValue = new PrismReferenceValue();
            referenceValue.setOid(oid);
            referenceValue.setRelation(SchemaConstants.ORG_MANAGER);
            RefFilter relationFilter = RefFilter.createReferenceEqual(new ItemPath(FocusType.F_PARENT_ORG_REF),
                    UserType.class, getPageBase().getPrismContext(), referenceValue);

            if(substring != null){
                query = ObjectQuery.createObjectQuery(AndFilter.createAnd(org, relationFilter, substring));
            } else {
                query = ObjectQuery.createObjectQuery(AndFilter.createAnd(org, relationFilter));
            }


        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. managers.", e);
        }

        return query;
    }

    private ObjectQuery createMemberQuery(){
        ObjectQuery query = null;
        OrgTreeDto dto = selected.getObject();
        String oid = dto != null ? dto.getOid() : getModel().getObject();

        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        String object = basicSearch.getModelObject();

        SubstringFilter substring;
        PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
        String normalizedString = normalizer.normalize(object);

        if (StringUtils.isEmpty(normalizedString)) {
            substring = null;
        } else {
            substring =  SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class, getPageBase().getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalizedString);
        }

        try {
            OrgFilter org = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

            PrismReferenceValue referenceFilter = new PrismReferenceValue();
            referenceFilter.setOid(oid);
            referenceFilter.setRelation(null);
            RefFilter referenceOidFilter = RefFilter.createReferenceEqual(new ItemPath(FocusType.F_PARENT_ORG_REF),
                    UserType.class, getPageBase().getPrismContext(), referenceFilter);

            if(substring != null){
                query = ObjectQuery.createObjectQuery(AndFilter.createAnd(org, referenceOidFilter , substring));
            } else {
                query = ObjectQuery.createObjectQuery(AndFilter.createAnd(org, referenceOidFilter));
            }

            if(LOGGER.isTraceEnabled()){
                LOGGER.info(query.debugDump());
            }

        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. managers.", e);
        }

        return query;
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

    private void moveRootPerformed(AjaxRequestTarget target) {
        OrgTreeDto root = getRootFromProvider();
        OrgTableDto dto = new OrgTableDto(root.getOid(), root.getType());
        movePerformed(target, OrgUnitBrowser.Operation.MOVE, dto, true);
    }

    private void updateActivationPerformed(AjaxRequestTarget target, boolean enable) {
        List<OrgTableDto> objects = isAnythingSelected(target);
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

    private void refreshTable(AjaxRequestTarget target) {
        ObjectDataProvider orgProvider = (ObjectDataProvider) getOrgChildTable().getDataTable().getDataProvider();
        orgProvider.clearCache();

        ObjectDataProvider memberProvider = (ObjectDataProvider) getMemberTable().getDataTable().getDataProvider();
        memberProvider.clearCache();

        ObjectDataProvider managerProvider = (ObjectDataProvider) getManagerTable().getDataTable().getDataProvider();
        managerProvider.clearCache();

        target.add(getOrgChildContainer(), getMemberContainer(), getManagerContainer());
    }

    private void recomputeRootPerformed(AjaxRequestTarget target, OrgUnitBrowser.Operation operation){
        OrgTreeDto root = getRootFromProvider();
        OrgTableDto dto = new OrgTableDto(root.getOid(), root.getType());
        recomputePerformed(target, operation, dto);
    }

    private void recomputePerformed(AjaxRequestTarget target, OrgUnitBrowser.Operation operation){
        recomputePerformed(target, operation, null);
    }

    private void recomputePerformed(AjaxRequestTarget target, OrgUnitBrowser.Operation operation, OrgTableDto orgDto){
        List<OrgTableDto> objects;
        if (orgDto == null) {
            objects = isAnythingSelected(target);
            if (objects.isEmpty()) {
                return;
            }
        } else {
            objects = new ArrayList<>();
            objects.add(orgDto);
        }

        Task task = getPageBase().createSimpleTask(OPERATION_RECOMPUTE);
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE);

        try {
            for(OrgTableDto org: objects){

                PrismObject<TaskType> recomputeTask = prepareRecomputeTask(org);

                ObjectDelta taskDelta = ObjectDelta.createAddDelta(recomputeTask);

                if(LOGGER.isTraceEnabled()){
                    LOGGER.trace(taskDelta.debugDump());
                }

                ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class,
                        org.getOid(), getPageBase().getPrismContext());
                ModelExecuteOptions options = new ModelExecuteOptions();
                options.setReconcile(true);
                getPageBase().getModelService().executeChanges(WebMiscUtil.createDeltaCollection(emptyDelta, taskDelta), options, task, result);
            }

            result.recordSuccess();
        } catch (Exception e){
            result.recordFatalError(getString("TreeTablePanel.message.recomputeError"), e);
            LoggingUtils.logException(LOGGER, getString("TreeTablePanel.message.recomputeError"), e);
        }

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        refreshTabbedPanel(target);
    }

    private PrismObject<TaskType> prepareRecomputeTask(OrgTableDto org) throws SchemaException{
        PrismPropertyDefinition propertyDef = getPageBase().getPrismContext().getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);

        ObjectFilter refFilter = RefFilter.createReferenceEqual(UserType.F_PARENT_ORG_REF,
                UserType.class, getPageBase().getPrismContext(), org.getOid());

        SearchFilterType filterType = QueryConvertor.createSearchFilterType(refFilter, getPageBase().getPrismContext());
        QueryType queryType = new QueryType();
        queryType.setFilter(filterType);

        PrismProperty<QueryType> property = propertyDef.instantiate();
        property.setRealValue(queryType);

        TaskType taskType = new TaskType();

        taskType.setName(WebMiscUtil.createPolyFromOrigString(createStringResource("TreeTablePanel.recomputeTask", org.getName()).getString()));
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

        taskType.setHandlerUri("http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/recompute/handler-3");

        return taskType.asPrismObject();
    }

    private void deleteRootPerformed(AjaxRequestTarget target) {
        if (selected.getObject() == null) {
            warn(getString("TreeTablePanel.message.nothingSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ConfirmationDialog dialog = (ConfirmationDialog) get(ID_CONFIRM_DELETE_POPUP);
        dialog.setConfirmType(CONFIRM_DELETE_ROOT);
        dialog.show(target);
    }

    private void deleteRootConfirmedPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

        PageBase page = getPageBase();

        OrgTreeDto dto = getRootFromProvider();
        WebModelUtils.deleteObject(OrgType.class, dto.getOid(), result, page);

        result.computeStatusIfUnknown();
        page.showResultInSession(result);

        refreshTabbedPanel(target);
    }

    private void clearTableSearchPerformed(AjaxRequestTarget target) {
        BasicSearchPanel basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        refreshTable(target);
    }

    private void tableSearchPerformed(AjaxRequestTarget target) {
        refreshTable(target);
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

            if (collapsedItem != null){
                if (set.contains(collapsedItem)){
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
            //just to have root expanded at all time
            if (iterator.hasNext()){
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

    private void editRootPerformed(AjaxRequestTarget target){
        OrgTreeDto root = getRootFromProvider();
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, root.getOid());
        setResponsePage(PageOrgUnit.class, parameters);
    }

    private void addToHierarchyPerformed(AjaxRequestTarget target){
        showAddDeletePopup(target, OrgUnitAddDeletePopup.ActionState.ADD);
    }

    private void removeFromHierarchyPerformed(AjaxRequestTarget target){
        showAddDeletePopup(target, OrgUnitAddDeletePopup.ActionState.DELETE);
    }

    private void showAddDeletePopup(AjaxRequestTarget target, OrgUnitAddDeletePopup.ActionState state){
        OrgUnitAddDeletePopup dialog = (OrgUnitAddDeletePopup) get(ID_ADD_DELETE_POPUP);
        dialog.setState(state, target);

        dialog.show(target);
    }

    private void addOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org){
        //TODO
    }

    private void removeOrgUnitToUserPerformed(AjaxRequestTarget target, OrgType org){
        //TODO
    }
}
