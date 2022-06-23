/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasicWizardPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PersonOfInterest.class);

    private enum PersonOfInterest {

        MYSELF("fas fa-user-circle"),

        GROUP_OTHERS("fas fa-user-friends");

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

    private static final String ID_SELECT_MANUALLY = "selectManually";

    private IModel<List<Tile<PersonOfInterest>>> tiles;

    private IModel<SelectionState> selectionState = Model.of(SelectionState.TILES);

    private IModel<List<ObjectReferenceType>> selectedGroupOfUsers = Model.ofList(new ArrayList<>());

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

                TargetSelectionType targetSelection = getTargetSelectionConfiguration();
                if (targetSelection == null) {
                    for (PersonOfInterest poi : PersonOfInterest.values()) {
                        Tile tile = new Tile(poi.getIcon(), getString(poi));
                        tile.setValue(poi);
                        list.add(tile);
                    }

                    return list;
                }

                List<GroupSelectionType> selections = targetSelection.getGroup();
                for (GroupSelectionType selection : selections) {
                    // todo create tile for group selection
                }

                return list;
            }
        };
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleBehaviour(() -> {
            Tile<PersonOfInterest> myself = getTileBy(PersonOfInterest.MYSELF);
            Tile<PersonOfInterest> others = getTileBy(PersonOfInterest.GROUP_OTHERS);

            return myself.isSelected() || (others.isSelected() && selectedGroupOfUsers.getObject().size() > 0);
        });
    }

    private Tile<PersonOfInterest> getTileBy(PersonOfInterest type) {
        return tiles.getObject().stream().filter(t -> t.getValue() == type).findFirst().orElseGet(null);
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
                                myselfPerformed(target, tile);
                                break;
                            case GROUP_OTHERS:
                                groupOthersPerformed(target, tile);
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

        AjaxLink selectManually = new AjaxLink<>(ID_SELECT_MANUALLY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectManuallyPerformed(target);
            }
        };
        fragment.add(selectManually);

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

    private void myselfPerformed(AjaxRequestTarget target, Tile<PersonOfInterest> myself) {
        boolean wasSelected = myself.isSelected();

        tiles.getObject().forEach(t -> t.setSelected(false));
        myself.setSelected(!wasSelected);

        target.add(this);
    }

    private void groupOthersPerformed(AjaxRequestTarget target, Tile<PersonOfInterest> groupOthers) {
        tiles.getObject().forEach(t -> t.setSelected(false));

        if (!groupOthers.isSelected()) {
            selectionState.setObject(SelectionState.USERS);
        }

        groupOthers.toggle();

        target.add(this);
    }

    private void selectManuallyPerformed(AjaxRequestTarget target) {
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<>(
                getPageBase().getMainPopupBodyId(), UserType.class,
                List.of(UserType.COMPLEX_TYPE), true, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                addUsersPerformed(target, List.of(user));
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, QName type, List<UserType> selected) {
                addUsersPerformed(target, selected);
            }
        };
        getPageBase().showMainPopup(panel, target);
    }

    private void addUsersPerformed(AjaxRequestTarget target, List<UserType> users) {
        List<ObjectReferenceType> refs = new ArrayList<>();
        for (UserType user : users) {
            refs.add(new ObjectReferenceType()
                    .oid(user.getOid())
                    .type(UserType.COMPLEX_TYPE));
        }

        selectedGroupOfUsers.setObject(refs);

        getPageBase().hideMainPopup(target);
        target.add(getWizard().getPanel());
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        if (selectionState.getObject() == SelectionState.TILES) {
            return super.onBackPerformed(target);
        }

        selectionState.setObject(SelectionState.TILES);
        target.add(this);

        return false;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        Tile<PersonOfInterest> myself = getTileBy(PersonOfInterest.MYSELF);
        if (myself.isSelected()) {
            try {
                MidPointPrincipal principal = SecurityUtil.getPrincipal();

                ObjectReferenceType ref = new ObjectReferenceType()
                        .oid(principal.getOid())
                        .type(UserType.COMPLEX_TYPE)
                        .targetName(principal.getName());
                getModelObject().addPersonOfInterest(ref);
            } catch (SecurityViolationException ex) {
                LOGGER.debug("Couldn't get principal, shouldn't happen", ex);
            }
        } else {
            getModelObject().addPersonOfInterest(selectedGroupOfUsers.getObject());
        }

        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    private TargetSelectionType getTargetSelectionConfiguration() {
        CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
        if (profile == null) {
            return null;
        }

        AccessRequestType accessRequest = profile.getAccessRequest();
        if (accessRequest == null) {
            return null;
        }

        return accessRequest.getTargetSelection();
    }
}
