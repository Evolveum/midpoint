/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends WizardStepPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

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

    public RoleCatalogPanel(IModel<RequestAccess> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

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

    private void initLayout() {
        setOutputMarkupId(true);

        IModel<Search> searchModel = new LoadableModel<>(false) {
            @Override
            protected Search load() {
                return SearchFactory.createSearch(RoleType.class, getPageBase());
            }
        };

        ObjectDataProvider provider = new ObjectDataProvider(this, searchModel);

        List<IColumn> columns = createColumns();
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
                        // todo improve
                        CatalogTile t = new CatalogTile("fas fa-building", WebComponentUtil.getName(object.getValue()));
                        t.setLogo("fas fa-utensils fa-2x");
                        t.setDescription(object.getValue().getDescription());
                        t.setValue(object);

                        return t;
                    }

                    @Override
                    protected Component createTile(String id, IModel<CatalogTile<SelectableBean<ObjectType>>> model) {
                        return new CatalogTilePanel(id, model) {

                            @Override
                            protected void onAdd(AjaxRequestTarget target) {
                                SelectableBean<ObjectType> bean = model.getObject().getValue();
                                addItemsPerformed(target, Arrays.asList(bean.getValue()));
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
                        };
                    }

                    @Override
                    protected IModel<Search> createSearchModel() {
                        return searchModel;
                    }
                };
        add(tilesTable);

        ViewTogglePanel viewToggle = new ViewTogglePanel(ID_VIEW_TOGGLE, tilesTable.getViewToggleModel()) {

            @Override
            protected void onTogglePerformed(AjaxRequestTarget target, ViewToggle newState) {
                super.onTogglePerformed(target, newState);
                target.add(RoleCatalogPanel.this);
            }
        };
        add(viewToggle);

        IModel<List<ListGroupMenuItem>> model = new LoadableModel<>(false) {
            @Override
            protected List<ListGroupMenuItem> load() {
                return loadRoleCatalogMenu();
            }
        };
        ListGroupMenuPanel menu = new ListGroupMenuPanel(ID_MENU, model);
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
        CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
        if (profile == null) {
            return null;
        }

        AccessRequestType accessRequest = profile.getAccessRequest();
        if (accessRequest == null) {
            return null;
        }

        return accessRequest.getRoleCatalog();
    }

    private List<ListGroupMenuItem> loadRoleCatalogMenu() {
        RoleCatalogType roleCatalog = getRoleCatalogConfiguration();
        if (roleCatalog == null) {
            return new ArrayList<>();
        }

        ObjectReferenceType ref = roleCatalog.getRoleCatalogRef();
        if (ref != null) {
            return loadMenuItems(ref);
        }

        // todo custom menu tree definition, not via org. tree hierarchy

        return loadMenuItems(ref);
    }

    private List<ListGroupMenuItem> loadMenuItems(ObjectReferenceType ref) {
        if (ref == null) {
            return new ArrayList<>();
        }

        QName type = ref.getType() != null ? ref.getType() : OrgType.COMPLEX_TYPE;
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        ObjectQuery query = getPrismContext()
                .queryFor(ot.getClassDefinition())
                .isInScopeOf(ref.getOid(), OrgFilter.Scope.ONE_LEVEL)
                .asc(ObjectType.F_NAME)
                .build();

        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ROLE_CATALOG_MENU);
        OperationResult result = task.getResult();

        List<ListGroupMenuItem> list = new ArrayList<>();
        try {
            List<PrismObject<ObjectType>> objects = WebModelServiceUtils.searchObjects(ot.getClassDefinition(), query, result, getPageBase());
            for (PrismObject o : objects) {
                String name = WebComponentUtil.getDisplayNameOrName(o, true);
                ListGroupMenuItem menu = new ListGroupMenuItem(name);
                list.add(menu);
            }
        } catch (Exception ex) {
            LOGGER.debug("Couldn't load menu using role catalog reference to org. structure, reason: " + ex.getMessage(), ex);
        }

        return list;
    }

    private List<IColumn> createColumns() {
        List<IColumn> columns = new ArrayList<>();
        columns.add(new CheckBoxHeaderColumn());
        columns.add(new IconColumn(null) {
            @Override
            protected DisplayType getIconDisplayType(IModel rowModel) {
                return new DisplayType();
            }
        });
        columns.add(new PropertyColumn(createStringResource("ObjectType.name"), "value.name"));
        columns.add(new PropertyColumn(createStringResource("ObjectType.description"), "value.description"));

        columns.add(new AbstractColumn(null) {

            @Override
            public void populateItem(Item item, String id, IModel rowModel) {
                item.add(new AjaxLink<>(id, createStringResource("RoleCatalogPanel.details")) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        itemDetailsPerformed(target, );
                    }
                });
            }
        });

        return columns;
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, ObjectType object) {

    }

    private void addAllItemsPerformed(AjaxRequestTarget target) {

    }

    private void addItemsPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
        RequestAccess requestAccess = getModelObject();
        for (ObjectType object : selected) {
            AssignmentType a = new AssignmentType()
                    .targetRef(object.getOid(), ObjectTypes.getObjectType(object.getClass()).getTypeQName());
            requestAccess.getShoppingCartAssignments().add(a);
        }

        getPageBase().reloadShoppingCartIcon(target);
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
                .cssClass("bg-success m-3")
                .title(getString("RoleCatalogPanel.itemAdded"))
                .icon("fas fa-cart-shopping")
                .autohide(true)
                .delay(10_000)
                .body(msg).show(target);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleEnableBehaviour(() -> !getModelObject().getShoppingCartAssignments().isEmpty());
    }
}
