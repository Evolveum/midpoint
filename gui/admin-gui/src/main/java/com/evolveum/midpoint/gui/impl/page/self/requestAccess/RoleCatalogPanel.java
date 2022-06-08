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
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends WizardStepPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

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
        return Model.ofList(List.of(
                new Badge("badge badge-info", "Requesting for 4 users"),
                new Badge("badge badge-danger", "1 fatal conflict"),
                new Badge("badge badge-danger", "fa fa-exclamation-triangle", "1 fatal conflict")));
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

//        List<CatalogTile> list = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            CatalogTile t = new CatalogTile("fas fa-building", "Canteen");
//            t.setLogo("fas fa-utensils fa-2x");
//            t.setDescription("Grants you access to canteen services, coffee bar and vending machines");
//            list.add(t);
//        }

        ObjectDataProvider provider = new ObjectDataProvider(this, searchModel);

        List<IColumn> columns = createColumns();
        TileTablePanel<CatalogTile<SelectableBean<ObjectType>>, SelectableBean<ObjectType>> tilesTable = new TileTablePanel<>(ID_TILES, provider, columns) {

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
        DetailsMenuPanel menu = new DetailsMenuPanel(ID_MENU);
        add(menu);
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
        return new VisibleEnableBehaviour(() -> !getPageBase().getSessionStorage().getRequestAccess().getShoppingCartAssignments().isEmpty());
    }
}
