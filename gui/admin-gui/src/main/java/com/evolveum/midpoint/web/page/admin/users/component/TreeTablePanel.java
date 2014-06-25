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
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.*;
import com.evolveum.midpoint.web.security.SecurityUtils;
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
    private static final String OPERATION_LOAD_PARENTS = DOT_CLASS + "loadParents";

    private static final String ID_TREE = "tree";
    private static final String ID_TREE_CONTAINER = "treeContainer";
    private static final String ID_TABLE = "table";
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
                ArrayList<String> oids = new ArrayList<String>();
                ObjectQuery query = new ObjectQuery();

                if(isMovingRoot() && getRootFromProvider() != null){
                    oids.add(getRootFromProvider().getOid());
                }

                //Selection from table, but only if we are not moving root
                if(!isMovingRoot()){
                    oids.remove(getRootFromProvider().getOid());

                    List<OrgTableDto> objects = WebMiscUtil.getSelectedData(getTable());
                    if(!objects.isEmpty()){
                        for(OrgTableDto dto: objects){
                            oids.add(dto.getOid());

                            //TODO - uncomment this, if parentOrgOid filtering is also required (see issue MID-1780)
                            //oids.addAll(getOrgParentOids(dto.getOid()));
                        }
                    }
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

        InlineMenu treeMenu = new InlineMenu(ID_TREE_MENU, new Model((Serializable) createTreeMenu()));
        treeHeader.add(treeMenu);

        ISortableTreeProvider provider = new OrgTreeProvider(this, getModel());
        List<IColumn<OrgTreeDto, String>> columns = new ArrayList<IColumn<OrgTreeDto, String>>();
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
                Item item = super.newRowItem(id, index, model);
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

        initTable();
        initSearch();
    }

    private List<String> getOrgParentOids(String orgOid){
        List<String> parentOids = new ArrayList<String>();
        OperationResult result = new OperationResult(OPERATION_LOAD_PARENTS);
        PrismObject<OrgType> org = WebModelUtils.loadObject(OrgType.class, orgOid, result, getPageBase());
        OrgType orgObject = org.asObjectable();

        if(orgObject.getParentOrg() != null && !orgObject.getParentOrg().isEmpty()){
            for(OrgType parentOrg: orgObject.getParentOrg()){
                parentOids.add(parentOrg.getOid());
            }
        }

        return parentOids;
    }

    private void initTable() {
        Form form = new Form(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        ObjectDataProvider tableProvider = new ObjectDataProvider<OrgTableDto, ObjectType>(this, ObjectType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<ObjectType> obj) {
                return OrgTableDto.createDto(obj);
            }

            @Override
            public ObjectQuery getQuery() {
                return createTableQuery();
            }
        };
        tableProvider.setOptions(WebModelUtils.createMinimalOptions());
        List<IColumn<OrgTableDto, String>> tableColumns = createTableColumns();
        TablePanel table = new TablePanel(ID_TABLE, tableProvider, tableColumns, UserProfileStorage.TableId.TREE_TABLE_PANEL);
        table.setOutputMarkupId(true);
        form.add(table);
    }

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
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

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
                                WebMiscUtil.getSelectedData(getTable()).size()).getString();
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

    private List<IColumn<OrgTableDto, String>> createTableColumns() {
        List<IColumn<OrgTableDto, String>> columns = new ArrayList<IColumn<OrgTableDto, String>>();

        columns.add(new CheckBoxHeaderColumn<OrgTableDto>());
        columns.add(new IconColumn<OrgTableDto>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<OrgTableDto> rowModel) {
                OrgTableDto dto = rowModel.getObject();

                ObjectTypeGuiDescriptor descr = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
                String icon = descr != null ? descr.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

                return new Model(icon);
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
                if (UserType.class.equals(dto.getType())) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
                    setResponsePage(PageUser.class, parameters);
                } else if (OrgType.class.equals(dto.getType())) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, dto.getOid());
                    setResponsePage(PageOrgUnit.class, parameters);
                }
            }
        });
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.displayName"), OrgTableDto.F_DISPLAY_NAME));
        //todo add relation
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.identifier"), OrgTableDto.F_IDENTIFIER));
        columns.add(new InlineMenuHeaderColumn(initInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addOrgUnit"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addOrgUnitPerformed(target);
                    }
                }));
        headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addUser"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addUserPerformed(target);
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

    private void addOrgUnitPerformed(AjaxRequestTarget target) {
        PrismObject object = addObjectPerformed(target, new OrgType());
        if (object == null) {
            return;
        }
        PageOrgUnit next = new PageOrgUnit(object);
        setResponsePage(next);
    }

    private void addUserPerformed(AjaxRequestTarget target) {
        PrismObject object = addObjectPerformed(target, new UserType());
        if (object == null) {
            return;
        }
        PageUser next = new PageUser(object);
        setResponsePage(next);
    }

    private PrismObject addObjectPerformed(AjaxRequestTarget target, ObjectType object) {
        PageBase page = getPageBase();
        try {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(selected.getObject().getOid());
            ref.setType(OrgType.COMPLEX_TYPE);
            object.getParentOrgRef().add(ref);

            PrismContext context = page.getPrismContext();
            context.adopt(object);

            return object.asPrismContainer();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create object with parent org. reference", ex);
            page.error("Couldn't create object with parent org. reference, reason: " + ex.getMessage());

            target.add(page.getFeedbackPanel());
        }

        return null;
    }

    /**
     * This method check selection in table.
     */
    private List<OrgTableDto> isAnythingSelected(AjaxRequestTarget target) {
        List<OrgTableDto> objects = WebMiscUtil.getSelectedData(getTable());
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
        }
        result.computeStatusComposite();

        page.showResult(result);
        target.add(page.getFeedbackPanel());

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

        ObjectDataProvider provider = (ObjectDataProvider) getTable().getDataTable().getDataProvider();
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

    private TableTree getTree() {
        return (TableTree) get(createComponentPath(ID_TREE_CONTAINER, ID_TREE));
    }

    private TablePanel getTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_TABLE));
    }

    private void selectTreeItemPerformed(AjaxRequestTarget target) {
        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        TablePanel table = getTable();
        table.setCurrentPage(null);

        target.add(table);
        target.add(get(ID_SEARCH_FORM));
    }

    private ObjectQuery createTableQuery() {
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

    private void collapseAllPerformed(AjaxRequestTarget target) {
        TableTree tree = getTree();
        TreeStateModel model = (TreeStateModel) tree.getDefaultModel();
        model.collapseAll();

        target.add(tree);
    }

    private void expandAllPerformed(AjaxRequestTarget target) {
        TableTree tree = getTree();
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
        ObjectDataProvider provider = (ObjectDataProvider) getTable().getDataTable().getDataProvider();
        provider.clearCache();

        target.add(getTable());
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
