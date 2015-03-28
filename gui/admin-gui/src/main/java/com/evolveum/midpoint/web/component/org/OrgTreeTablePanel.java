package com.evolveum.midpoint.web.component.org;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.component.OrgTreeProvider;
import com.evolveum.midpoint.web.page.admin.users.component.SelectableFolderContent;
import com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OrgTreeTablePanel extends SimplePanel{
	
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
            return getRootFromProvider();
        }
    };

    public OrgTreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
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
                response.render(OnDomReadyHeaderItem.forScript("updateHeight('" + getMarkupId()
                        + "', ['#" + OrgTreeTablePanel.this.get(ID_FORM).getMarkupId() + "'], ['#"
                        + OrgTreeTablePanel.this.get(ID_TREE_HEADER).getMarkupId() + "'])"));
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
        
        return items;
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

       private PrismReferenceValue createPrismRefValue(OrgDto dto) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(dto.getOid());
        value.setRelation(dto.getRelation());
        value.setTargetType(ObjectTypes.getObjectType(dto.getType()).getTypeQName());
        return value;
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

  
    private TablePanel getOrgChildTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS, ID_CHILD_TABLE));
    }

    private void selectTreeItemPerformed(AjaxRequestTarget target) {
        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        TablePanel orgTable = getOrgChildTable();
        orgTable.setCurrentPage(null);

        target.add(getOrgChildContainer());
        target.add(get(ID_SEARCH_FORM));
    }

    private ObjectQuery createOrgChildQuery() {
        OrgTreeDto dto = selected.getObject();
        String oid = (String) (dto != null ? dto.getOid() : getModel().getObject());

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

    private void refreshTable(AjaxRequestTarget target) {
        ObjectDataProvider orgProvider = (ObjectDataProvider) getOrgChildTable().getDataTable().getDataProvider();
        orgProvider.clearCache();

        target.add(getOrgChildContainer());
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

