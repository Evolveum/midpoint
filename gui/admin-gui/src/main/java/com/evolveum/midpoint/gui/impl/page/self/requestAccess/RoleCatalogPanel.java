/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.tile.CatalogTile;
import com.evolveum.midpoint.gui.impl.component.tile.CatalogTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends WizardStepPanel<RequestAccess> implements AccessRequestStep {

    private static final long serialVersionUID = 1L;

    public static final String STEP_ID = "catalog";

    private static final Trace LOGGER = TraceManager.getTrace(RoleCatalogPanel.class);

    private static final String DOT_CLASS = RoleCatalogPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_CATALOG_MENU = DOT_CLASS + "loadRoleCatalogMenu";

    private static final ViewToggle DEFAULT_VIEW = ViewToggle.TILE;

    private static final String ID_VIEW_TOGGLE = "viewToggle";
    private static final String ID_MENU = "menu";
    private static final String ID_TILES = "tilesTable";
    private static final String ID_TABLE_FOOTER_FRAGMENT = "tableFooterFragment";
    private static final String ID_ADD_SELECTED = "addSelected";
    private static final String ID_ADD_ALL = "addAll";

    private PageBase page;

    private IModel<Search> searchModel;

    private IModel<ListGroupMenu<RoleCatalogQueryItem>> menuModel;

    public RoleCatalogPanel(IModel<RequestAccess> model, PageBase page) {
        super(model);

        this.page = page;
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return () -> {
            String text;

            int count = getModelObject().getPersonOfInterest().size();
            if (isRequestingForMyself()) {
                text = count > 1 ? getString("RoleCatalogPanel.badgeMyselfAndOthers", count - 1) : getString("RoleCatalogPanel.badgeMyself");
            } else {
                text = getString("RoleCatalogPanel.badgeOthers", count);
            }

            return List.of(new Badge("badge badge-info", text));
        };
    }

    private boolean isRequestingForMyself() {
        String principalOid = SecurityUtil.getPrincipalOidIfAuthenticated();
        RequestAccess request = getModelObject();
        return request.getPersonOfInterest().stream().filter(o -> Objects.equals(principalOid, o.getOid())).count() > 0;
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("RoleCatalogPanel.title");
    }

    private ObjectQuery createQueryFromOrgRef(ObjectReferenceType ref, boolean scopeOne) {
        return getPrismContext()
                .queryFor(OrgType.class)
                .isInScopeOf(ref.getOid(), scopeOne ? OrgFilter.Scope.ONE_LEVEL : OrgFilter.Scope.SUBTREE)
                .asc(ObjectType.F_NAME)
                .build();
    }

    private ObjectQuery createQueryFromCollectionRef(ObjectReferenceType collectionRef) {
        PrismObject<ObjectCollectionType> collection = WebModelServiceUtils.loadObject(collectionRef, page);
        ObjectCollectionType objectCollection = collection.asObjectable();

        try {
            QName type = objectCollection.getType();
            if (type == null) {
                type = RoleType.COMPLEX_TYPE;
            }
            ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

            ObjectFilter filter = page.getQueryConverter().createObjectFilter(ot.getClassDefinition(), objectCollection.getFilter());
            return getPrismContext()
                    .queryFor(ot.getClassDefinition())
                    .filter(filter)
                    .asc(ObjectType.F_NAME)
                    .build();
        } catch (Exception ex) {
            LOGGER.debug("Couldn't create search filter", ex);
            page.error("Couldn't create search filter, reason: " + ex.getMessage());
        }

        return null;
    }

    private ObjectQuery createQueryFromCollectionUri(String collectionUri) {
        AssignmentViewType view = AssignmentViewType.getViewByUri(collectionUri);
        if (view == null) {
            return null;
        }

        return getPrismContext()
                .queryFor(view.getType())
                .asc(ObjectType.F_NAME)
                .build();
    }

    private void initModels() {
        searchModel = new LoadableModel<>(false) {
            @Override
            protected Search load() {
                return SearchFactory.createSearch(RoleType.class, page);
            }
        };

        menuModel = new LoadableModel<>(false) {
            @Override
            protected ListGroupMenu<RoleCatalogQueryItem> load() {
                ListGroupMenu<RoleCatalogQueryItem> menu = loadRoleCatalogMenu();
                menu.activateFirstAvailableItem();

                return menu;
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        ObjectDataProvider provider = new ObjectDataProvider(this, searchModel) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                ListGroupMenu menu = menuModel.getObject();
                ListGroupMenuItem<RoleCatalogQueryItem> active = menu.getActiveMenu();
                RoleCatalogQueryItem item = active != null ? active.getValue() : null;

                if (item == null) {
                    return null;
                }

                if (item.orgRef() != null) {
                    return createQueryFromOrgRef(item.orgRef(), item.scopeOne());
                }

                RoleCollectionViewType collection = item.collection();
                if (collection == null) {
                    return null;
                }

                if (collection.getCollectionRef() != null) {
                    return createQueryFromCollectionRef(collection.getCollectionRef());
                } else if (collection.getCollectionUri() != null) {
                    return createQueryFromCollectionUri(collection.getCollectionUri());
                }

                return null;
            }
        };
        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();
        provider.setOptions(options);

        List<IColumn<SelectableBean<ObjectType>, String>> columns = createColumns();
        TileTablePanel<CatalogTile<SelectableBean<ObjectType>>, SelectableBean<ObjectType>> tilesTable =
                new TileTablePanel<>(ID_TILES, provider, columns, createViewToggleModel()) {

                    @Override
                    protected WebMarkupContainer createTableButtonToolbar(String id) {
                        Fragment fragment = new Fragment(id, ID_TABLE_FOOTER_FRAGMENT, RoleCatalogPanel.this);
                        fragment.add(new AjaxLink<>(ID_ADD_SELECTED) {

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                List<ObjectType> selected = provider.getSelectedData();
                                addItemsPerformed(target, selected);
                            }
                        });

                        fragment.add(new AjaxLink<>(ID_ADD_ALL) {

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                addAllItemsPerformed(target);
                            }
                        });

                        return fragment;
                    }

                    @Override
                    protected CatalogTile createTileObject(SelectableBean<ObjectType> object) {
                        CatalogTile t = new CatalogTile(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED, WebComponentUtil.getName(object.getValue()));
                        t.setDescription(object.getValue().getDescription());
                        t.setValue(object);

                        return t;
                    }

                    @Override
                    protected Component createTile(String id, IModel<CatalogTile<SelectableBean<ObjectType>>> model) {
                        return new CatalogTilePanel<>(id, model) {

                            @Override
                            protected void onAdd(AjaxRequestTarget target) {
                                SelectableBean<ObjectType> bean = model.getObject().getValue();
                                addItemsPerformed(target, Arrays.asList(bean.getValue()));

                                target.add(this);
                            }

                            @Override
                            protected void onDetails(AjaxRequestTarget target) {
                                SelectableBean<ObjectType> bean = model.getObject().getValue();
                                itemDetailsPerformed(target, bean.getValue());
                            }

                            @Override
                            protected void onClick(AjaxRequestTarget target) {
                                // no selection to be done
                            }

                            @Override
                            protected Component createAddButton(String id) {
                                Component details = super.createAddButton(id);
                                WebComponentUtil.addDisabledClassBehavior(details);

                                details.add(new EnableBehaviour(() -> {
                                    ObjectType object = model.getObject().getValue().getValue();

                                    RequestAccess access = RoleCatalogPanel.this.getModelObject();
                                    return access.getSelectedAssignments().stream()
                                            .filter(a -> Objects.equals(object.getOid(), a.getTargetRef().getOid()))
                                            .count() == 0;
                                }));
                                return details;
                            }

                            @Override
                            protected IModel<IResource> createPreferredImage(IModel<CatalogTile<SelectableBean<ObjectType>>> model) {
                                return createImage(() -> model.getObject().getValue().getValue());
                            }

                            @Override
                            protected DisplayType createDisplayType(IModel<CatalogTile<SelectableBean<ObjectType>>> model) {
                                return new DisplayType()
                                        .icon(new IconType()
                                                .cssClass(StringUtils.joinWith(" ", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "fa-2x")));
                            }
                        };
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-12 col-md-6 col-lg-4 col-xxl-2";
                    }

                    @Override
                    protected IModel<Search> createSearchModel() {
                        return searchModel;
                    }
                };
        add(tilesTable);

        IModel<List<Toggle<ViewToggle>>> items = new LoadableModel<>(false) {

            @Override
            protected List<Toggle<ViewToggle>> load() {
                ViewToggle toggle = tilesTable.getViewToggleModel().getObject();
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle asList = new Toggle("fa-solid fa-table-list", null);
                asList.setActive(ViewToggle.TABLE == toggle);
                asList.setValue(ViewToggle.TABLE);
                list.add(asList);

                Toggle asTile = new Toggle("fa-solid fa-table-cells", null);
                asTile.setActive(ViewToggle.TILE == toggle);
                asTile.setValue(ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };

        TogglePanel<ViewToggle> viewToggle = new TogglePanel<>(ID_VIEW_TOGGLE, items) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                super.itemSelected(target, item);

                tilesTable.getViewToggleModel().setObject(item.getObject().getValue());
                target.add(RoleCatalogPanel.this);
            }
        };
        add(viewToggle);

        ListGroupMenuPanel menu = new ListGroupMenuPanel(ID_MENU, menuModel) {

            @Override
            protected void onMenuClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                super.onMenuClickPerformed(target, item);

                if (!item.isActive()) {
                    // we've clicked on menu that has submenus
                    return;
                }

                target.add(tilesTable);
            }
        };
        add(menu);
    }

    // todo use configuration getAllowedViews from RoleCatalogType

    private IModel<ViewToggle> createViewToggleModel() {
        return new LoadableModel<>(false) {

            @Override
            protected ViewToggle load() {
                RoleCatalogType config = getRoleCatalogConfiguration();
                if (config == null) {
                    return DEFAULT_VIEW;
                }

                RoleCatalogViewType view = config.getDefaultView();
                if (view == null) {
                    return DEFAULT_VIEW;
                }

                switch (view) {
                    case TABLE:
                        return ViewToggle.TABLE;
                    case TILE:
                    default:
                        return ViewToggle.TILE;
                }
            }
        };
    }

    private RoleCatalogType getRoleCatalogConfiguration() {
        AccessRequestType config = getAccessRequestConfiguration(page);
        return config != null ? config.getRoleCatalog() : null;
    }

    private ListGroupMenu<RoleCatalogQueryItem> loadRoleCatalogMenu() {
        RoleCatalogType roleCatalog = getRoleCatalogConfiguration();
        if (roleCatalog == null) {
            return new ListGroupMenu<>();
        }

        ListGroupMenu<RoleCatalogQueryItem> menu = new ListGroupMenu<>();

        ObjectReferenceType ref = roleCatalog.getRoleCatalogRef();
        if (ref != null) {
            List<ListGroupMenuItem<RoleCatalogQueryItem>> items = loadMenuFromOrgTree(ref);
            if (items != null) {
                menu.getItems().addAll(items);
            }
        }

        List<RoleCollectionViewType> collections = roleCatalog.getCollection();
        List<ListGroupMenuItem<RoleCatalogQueryItem>> items = createMenuFromRoleCollections(collections);
        if (items != null) {
            menu.getItems().addAll(items);
        }

        return menu;
    }

    private List<ListGroupMenuItem<RoleCatalogQueryItem>> createMenuFromRoleCollections(List<RoleCollectionViewType> collections) {
        List<ListGroupMenuItem<RoleCatalogQueryItem>> items = new ArrayList<>();
        for (RoleCollectionViewType collection : collections) {
            try {
                String name = null;
                RoleCatalogQueryItem rcq = new RoleCatalogQueryItem();
                rcq.collection(collection);

                ObjectReferenceType collectionRef = collection.getCollectionRef();
                if (collectionRef != null) {
                    PrismObject<ObjectCollectionType> objectCollection = WebModelServiceUtils.loadObject(collectionRef, page);

                    name = WebComponentUtil.getDisplayNameOrName(objectCollection, true);
                }

                String collectionUri = collection.getCollectionUri();
                if (StringUtils.isNotEmpty(collectionUri)) {
                    AssignmentViewType view = AssignmentViewType.getViewByUri(collectionUri);
                    if (view != null) {
                        name = getString(view);
                    }
                }

                if (name == null) {
                    continue;
                }

                ListGroupMenuItem<RoleCatalogQueryItem> item = new ListGroupMenuItem<>(name);
                item.setValue(rcq);

                items.add(item);
            } catch (Exception ex) {
                LOGGER.debug("Couldn't load object collection as role catalog menu item", ex);
            }
        }

        return items;
    }

    private List<ListGroupMenuItem<RoleCatalogQueryItem>> loadMenuFromOrgTree(ObjectReferenceType ref) {
        return loadMenuFromOrgTree(ref, 1, 3);
    }

    private List<ListGroupMenuItem<RoleCatalogQueryItem>> loadMenuFromOrgTree(ObjectReferenceType ref, int currentLevel, int maxLevel) {
        if (ref == null) {
            return new ArrayList<>();
        }

        if (currentLevel > maxLevel) {
            return new ArrayList<>();
        }

        QName type = ref.getType() != null ? ref.getType() : OrgType.COMPLEX_TYPE;
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        ObjectQuery query = getPrismContext()
                .queryFor(ot.getClassDefinition())
                .isInScopeOf(ref.getOid(), OrgFilter.Scope.ONE_LEVEL)
                .asc(ObjectType.F_NAME)
                .build();

        Task task = page.createSimpleTask(OPERATION_LOAD_ROLE_CATALOG_MENU);
        OperationResult result = task.getResult();

        List<ListGroupMenuItem<RoleCatalogQueryItem>> list = new ArrayList<>();
        try {
            List<PrismObject<ObjectType>> objects = WebModelServiceUtils.searchObjects(ot.getClassDefinition(), query, result, page);
            for (PrismObject o : objects) {
                String name = WebComponentUtil.getDisplayNameOrName(o, true);
                ListGroupMenuItem<RoleCatalogQueryItem> menu = new ListGroupMenuItem<>(name);
                menu.setValue(new RoleCatalogQueryItem()
                        .orgRef(new ObjectReferenceType().oid(o.getOid()).type(o.getDefinition().getTypeName()))
                        .scopeOne(currentLevel < maxLevel));

                menu.setItemsModel(new LoadableModel<>(false) {
                    @Override
                    protected List<ListGroupMenuItem<RoleCatalogQueryItem>> load() {
                        ObjectReferenceType parentRef = new ObjectReferenceType()
                                .oid(o.getOid())
                                .targetName(o.getName().getOrig())
                                .type(o.getDefinition().getTypeName());

                        return loadMenuFromOrgTree(parentRef, currentLevel + 1, maxLevel);
                    }
                });
                list.add(menu);
            }
        } catch (Exception ex) {
            LOGGER.debug("Couldn't load menu using role catalog reference to org. structure, reason: " + ex.getMessage(), ex);
        }

        return list;
    }

    private IModel<IResource> createImage(IModel<ObjectType> model) {
        return new LoadableModel<>(false) {
            @Override
            protected IResource load() {
                ObjectType object = model.getObject();

                return WebComponentUtil.createJpegPhotoResource((FocusType) object);
            }
        };
    }

    private List<IColumn<SelectableBean<ObjectType>, String>> createColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn());
        columns.add(new RoundedIconColumn<>(null) {

            @Override
            protected IModel<IResource> createPreferredImage(IModel<SelectableBean<ObjectType>> model) {
                return RoleCatalogPanel.this.createImage(() -> model.getObject().getValue());
            }
        });
        columns.add(new PropertyColumn(createStringResource("ObjectType.name"), "value.name"));
        columns.add(new PropertyColumn(createStringResource("ObjectType.description"), "value.description"));

        columns.add(new AbstractColumn<>(null) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> item, String id, IModel<SelectableBean<ObjectType>> model) {
                item.add(new AjaxLinkPanel(id, createStringResource("RoleCatalogPanel.details")) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        itemDetailsPerformed(target, model.getObject().getValue());
                    }
                });
            }
        });

        return columns;
    }

    private ContainerPanelConfigurationType createDefaultContainerPanelConfiguration() {
        ContainerPanelConfigurationType c = new ContainerPanelConfigurationType();
        c.identifier("sample-panel");
        c.type(RoleType.COMPLEX_TYPE);
        c.panelType("formPanel");
        VirtualContainersSpecificationType vcs =
                c.beginContainer()
                        .beginDisplay()
                        .label("RoleCatalogPanel.details")
                        .end();
        vcs.identifier("sample-container");
        vcs.beginItem().path(new ItemPathType(ItemPath.create(RoleType.F_DESCRIPTION))).end();

        return c;
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
        ContainerPanelConfigurationType config;

        ListGroupMenuItem<RoleCatalogQueryItem> selectedMenu = menuModel.getObject().getActiveMenu();

        RoleCatalogQueryItem item = selectedMenu != null ? selectedMenu.getValue() : null;
        if (item == null || item.collection() == null) {
            config = createDefaultContainerPanelConfiguration();
        } else {
            RoleCollectionViewType view = item.collection();
            config = view.getDetails() != null ? view.getDetails() : createDefaultContainerPanelConfiguration();
        }

        List<ContainerPanelConfigurationType> finalConfig = new ArrayList<>();
        if (!config.getPanel().isEmpty()) {
            finalConfig.addAll(config.getPanel());
        } else {
            finalConfig.add(config);
        }

        CatalogItemDetailsPanel panel = new CatalogItemDetailsPanel(() -> finalConfig, Model.of(object)) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, IModel<ObjectType> model) {
                addItemsPerformed(target, List.of(model.getObject()));

                page.getMainPopup().close(target);
            }

            @Override
            protected void closePerformed(AjaxRequestTarget target, IModel<ObjectType> model) {
                page.getMainPopup().close(target);
            }
        };

        page.showMainPopup(panel, target);
    }

    private void addAllItemsPerformed(AjaxRequestTarget target) {

    }

    private AssignmentType createNewAssignment(ObjectType object, QName relation) {
        AssignmentType a = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType()
                .targetName(object.getName())
                .type(ObjectTypes.getObjectType(object.getClass()).getTypeQName())
                .oid(object.getOid())
                .relation(relation);
        a.targetRef(targetRef);

        return a;
    }

    private void addItemsPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
        RequestAccess requestAccess = getModelObject();
        QName relation = requestAccess.getRelation();

        List<AssignmentType> newAssignments = selected.stream().map(o -> createNewAssignment(o, relation)).collect(Collectors.toList());
        requestAccess.addAssignments(newAssignments);

        getPageBase().reloadShoppingCartIcon(target);
        target.add(getWizard().getHeader());
        target.add(get(ID_TILES));

        String msg;
        if (selected.size() > 1) {
            msg = getString("RoleCatalogPanel.multipleAdded", selected.size());
        } else {
            String name = WebComponentUtil.getName(selected.get(0));
            msg = getString("RoleCatalogPanel.singleAdded",
                    Strings.escapeMarkup(name, false, true));
        }

        new Toast()
                .success()
                .title(getString("RoleCatalogPanel.itemAdded"))
                .icon("fas fa-cart-shopping")
                .autohide(true)
                .delay(5_000)
                .body(msg).show(target);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleEnableBehaviour(() -> !getModelObject().getShoppingCartAssignments().isEmpty());
    }
}
