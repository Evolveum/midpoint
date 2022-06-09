/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasicWizardPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private enum PersonOfInterest {

        MYSELF("fas fa-user-circle"),

        GROUP_OTHERS("fas fa-user-friends");

        // disabled for now
        // TEAM("fas fa-users");

        private String icon;

        PersonOfInterest(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    private enum SelectionState {

        TILES, USERS
    }

    private static final String ID_FRAGMENTS = "fragments";
    private static final String ID_TILE_FRAGMENT = "tileFragment";
    private static final String ID_SELECTION_FRAGMENT = "selectionFragment";
    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private IModel<List<Tile<PersonOfInterest>>> tiles;

    private IModel<SelectionState> selectionState = Model.of(SelectionState.TILES);

    public PersonOfInterestPanel(IModel<RequestAccess> model) {
        super(model);

        initModels();
        initLayout();
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("PersonOfInterestPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return () -> {
            String key = selectionState.getObject() == SelectionState.TILES ? "PersonOfInterestPanel.text" : "PersonOfInterestPanel.selection.text";
            return getString(key);
        };
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return () -> {
            String key = selectionState.getObject() == SelectionState.TILES ? "PersonOfInterestPanel.subtext" : "PersonOfInterestPanel.selection.subtext";
            return getString(key);
        };
    }

    private void initModels() {
        tiles = new LoadableModel<>(false) {
            @Override
            protected List<Tile<PersonOfInterest>> load() {
                List<Tile<PersonOfInterest>> list = new ArrayList<>();

                for (PersonOfInterest poi : PersonOfInterest.values()) {
                    Tile tile = new Tile(poi.getIcon(), getString(poi));
                    tile.setValue(poi);
                    list.add(tile);
                }

                return list;
            }
        };
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new EnableBehaviour(() -> tiles.getObject().stream().filter(t -> t.isSelected()).count() > 0);
    }

    private void initLayout() {
        setOutputMarkupId(true);

        add(new WebMarkupContainer(ID_FRAGMENTS));
    }

    private Fragment initTileFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENTS, ID_TILE_FRAGMENT, this);

        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        fragment.add(listContainer);
        ListView<Tile<PersonOfInterest>> list = new ListView<>(ID_LIST, tiles) {

            @Override
            protected void populateItem(ListItem<Tile<PersonOfInterest>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<PersonOfInterest> tile = item.getModelObject();
                        switch (tile.getValue()) {
                            case MYSELF:
                                myselfPerformed(target);
                                break;
                            case GROUP_OTHERS:
                                groupOthersPerformed(target);
                                break;
                        }
                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);

        return fragment;
    }

    private Fragment initSelectionFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENTS, ID_SELECTION_FRAGMENT, this);

        return fragment;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        Fragment fragment;
        switch (selectionState.getObject()) {
            case USERS:
                fragment = initSelectionFragment();
                break;
            case TILES:
            default:
                fragment = initTileFragment();
        }
        addOrReplace(fragment);
    }

    private void myselfPerformed(AjaxRequestTarget target) {
        ObjectReferenceType myself = new ObjectReferenceType()
                .oid(SecurityUtil.getPrincipalOidIfAuthenticated())
                .type(UserType.COMPLEX_TYPE);
        getModelObject().getPersonOfInterest().add(myself);

        getWizard().next();

        target.add(getWizard().getPanel());
    }

    private void groupOthersPerformed(AjaxRequestTarget target) {
        selectionState.setObject(SelectionState.USERS);
        target.add(this);
    }

    @Override
    protected void onBackPerformed(AjaxRequestTarget target) {
        if (selectionState.getObject() == SelectionState.TILES) {
            super.onBackPerformed(target);
            return;
        }

        selectionState.setObject(SelectionState.TILES);
        target.add(this);
    }


    @Override
    protected void onNextPerformed(AjaxRequestTarget target) {
        // todo save state

    }
}
