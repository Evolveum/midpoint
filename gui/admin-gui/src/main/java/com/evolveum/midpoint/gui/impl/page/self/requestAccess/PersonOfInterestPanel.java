/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasicWizardStepPanel<RequestAccess> implements AccessRequestStep {

    private static final long serialVersionUID = 1L;

    public static final String STEP_ID = "poi";

    private static final Trace LOGGER = TraceManager.getTrace(TileType.class);

    private static final String DOT_CLASS = RelationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USERS = DOT_CLASS + "loadUsers";

    private static final int MULTISELECT_PAGE_SIZE = 10;

    private static final String DEFAULT_TILE_ICON = "fas fa-user-friends";

    private enum TileType {

        MYSELF("fas fa-user-circle"),

        GROUP_OTHERS(DEFAULT_TILE_ICON);

        private final String icon;

        TileType(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    private static class PersonOfInterest implements Serializable {

        private String groupIdentifier;

        private TileType type;

        public PersonOfInterest(String groupIdentifier, TileType type) {
            this.groupIdentifier = groupIdentifier;
            this.type = type;
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
    private static final String ID_MULTISELECT = "multiselect";
    private static final String ID_USER_SELECTION_LABEL = "userSelectionLabel";

    private PageBase page;

    private LoadableModel<List<Tile<PersonOfInterest>>> tiles;

    private IModel<SelectionState> selectionState;

    private IModel<Map<ObjectReferenceType, List<ObjectReferenceType>>> selectedGroupOfUsers = Model.ofMap(new HashMap<>());

    public PersonOfInterestPanel(IModel<RequestAccess> model, PageBase page) {
        super(model);

        this.page = page;

        initModels();
        initLayout();
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PersonOfInterestPanel.title");
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

                TargetSelectionType selection = getTargetSelectionConfiguration();
                if (BooleanUtils.isNotFalse(selection.isAllowRequestForMyself())) {
                    list.add(createDefaultTile(TileType.MYSELF));
                }

                if (BooleanUtils.isNotFalse(selection.isAllowRequestForOthers())) {
                    List<GroupSelectionType> selections = selection.getGroup();
                    if (selections.isEmpty()) {
                        list.add(createDefaultTile(TileType.GROUP_OTHERS));
                    } else {
                        for (GroupSelectionType gs : selections) {
                            list.add(createTile(gs));
                        }
                    }
                }

                if (list.size() == 1) {
                    Tile<PersonOfInterest> tile = list.get(0);
                    tile.setSelected(true);
                } else if (list.size() > 1 && StringUtils.isNotEmpty(selection.getDefaultSelection())) {
                    String identifier = selection.getDefaultSelection();
                    if (RequestAccess.DEFAULT_MYSELF_IDENTIFIER.equals(identifier)) {
                        list.stream()
                                .filter(t -> TileType.MYSELF == t.getValue().type)
                                .findFirst()
                                .ifPresent(t -> t.setSelected(true));
                    } else {
                        list.stream()
                                .filter(t -> identifier.equals(t.getValue().groupIdentifier))
                                .findFirst()
                                .ifPresent(t -> t.setSelected(true));
                    }
                }

                return list;
            }
        };

        selectionState = new LoadableModel<>(false) {

            @Override
            protected SelectionState load() {
                Tile<PersonOfInterest> selected = getSelectedTile();
                if (selected != null && selected.getValue().type == TileType.GROUP_OTHERS) {
                    return SelectionState.USERS;
                }

                return SelectionState.TILES;
            }
        };
    }

    private Tile<PersonOfInterest> createTile(GroupSelectionType selection) {
        DisplayType display = selection.getDisplay();
        if (display == null) {
            display = new DisplayType();
        }

        String icon = DEFAULT_TILE_ICON;

        IconType iconType = display.getIcon();
        if (iconType != null && iconType.getCssClass() != null) {
            icon = iconType.getCssClass();
        }

        String label = getString(TileType.GROUP_OTHERS);
        if (display.getLabel() != null) {
            label = WebComponentUtil.getTranslatedPolyString(display.getLabel());
        }

        Tile tile = new Tile(icon, label);
        tile.setValue(new PersonOfInterest(selection.getIdentifier(), TileType.GROUP_OTHERS));

        return tile;
    }

    private Tile<PersonOfInterest> createDefaultTile(TileType type) {
        Tile tile = new Tile(type.getIcon(), getString(type));
        tile.setValue(new PersonOfInterest(null, type));

        return tile;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleBehaviour(() -> {
            Tile<PersonOfInterest> selected = getSelectedTile();
            if (selected == null) {
                return false;
            }

            TileType type = selected.getValue().type;

            return type == TileType.MYSELF || (type == TileType.GROUP_OTHERS && selectedGroupOfUsers.getObject().size() > 0);
        });
    }

    private Tile<PersonOfInterest> getSelectedTile() {
        return tiles.getObject().stream().filter(t -> t.isSelected()).findFirst().orElse(null);
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
                TilePanel<PersonOfInterest> tp = new TilePanel<>(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<PersonOfInterest> tile = item.getModelObject();
                        switch (tile.getValue().type) {
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

        IModel<Collection<ObjectReferenceType>> multiselectModel = new IModel<>() {

            @Override
            public Collection<ObjectReferenceType> getObject() {
                return new ArrayList<>(selectedGroupOfUsers.getObject().keySet());
            }

            @Override
            public void setObject(Collection<ObjectReferenceType> object) {
                updateSelectedGroupOfUsers(object);
            }
        };

        Select2MultiChoice<ObjectReferenceType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT, multiselectModel,
                new ObjectReferenceProvider(this));
        multiselect.getSettings()
                .setMinimumInputLength(2);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<ObjectReferenceType> refs = multiselect.getModel().getObject();
                updateSelectedGroupOfUsers(refs);

                target.add(PersonOfInterestPanel.this.getNext());
            }
        });
        fragment.add(multiselect);

        AjaxLink selectManually = new AjaxLink<>(ID_SELECT_MANUALLY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectManuallyPerformed(target);
            }
        };
        fragment.add(selectManually);

        Label userSelectionLabel = new Label(ID_USER_SELECTION_LABEL, () -> {
            Tile<PersonOfInterest> selected = getSelectedTile();
            String name = selected != null ? selected.getTitle() : null;

            return getString("PersonOfInterestPanel.userSelection", name);
        });
        userSelectionLabel.setRenderBodyOnly(true);
        fragment.add(userSelectionLabel);

        return fragment;
    }

    private void updateSelectedGroupOfUsers(Collection<ObjectReferenceType> poiRefs) {
        if (poiRefs == null) {
            selectedGroupOfUsers.setObject(createPoiMembershipMap(null));
            return;
        }

        List<UserType> users = new ArrayList<>();
        for (ObjectReferenceType poiRef : poiRefs) {
            PrismObject<UserType> user = WebModelServiceUtils.loadObject(poiRef, page);
            if (user != null) {
                users.add(user.asObjectable());
            }
        }

        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = createPoiMembershipMap(users);
        selectedGroupOfUsers.setObject(userMemberships);
    }

    private boolean canSkipStep() {
        List<Tile<PersonOfInterest>> list = tiles.getObject();
        if (list.size() != 1) {
            return false;
        }

        Tile<PersonOfInterest> tile = list.get(0);
        return tile.isSelected() && tile.getValue().type == TileType.MYSELF;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        tiles.reset();
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);

        if (canSkipStep()) {
            // no user input needed, we'll populate model with data
            submitData();
        }
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !canSkipStep();
    }

    @Override
    protected void onBeforeRender() {
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

        super.onBeforeRender();
    }

    private void myselfPerformed(AjaxRequestTarget target, Tile<PersonOfInterest> myself) {
        boolean wasSelected = myself.isSelected();

        tiles.getObject().forEach(t -> t.setSelected(false));
        myself.setSelected(!wasSelected);

        target.add(this);
    }

    private void groupOthersPerformed(AjaxRequestTarget target, Tile<PersonOfInterest> groupOthers) {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected != null && selected.getValue().type == TileType.GROUP_OTHERS && selected != groupOthers) {
            // we've selected different group of users as it was previously selected, so we clear our map of selected users
            selectedGroupOfUsers.setObject(createPoiMembershipMap(null));
        }

        tiles.getObject().forEach(t -> t.setSelected(false));

        if (!groupOthers.isSelected()) {
            selectionState.setObject(SelectionState.USERS);
        }

        groupOthers.toggle();

        target.add(this);
    }

    private ObjectFilter createObjectFilterFromGroupSelection(String identifier) {
        if (identifier == null) {
            return null;
        }

        TargetSelectionType targetSelection = getTargetSelectionConfiguration();

        List<GroupSelectionType> selections = getTargetSelectionConfiguration().getGroup();
        GroupSelectionType selection = selections.stream().filter(gs -> identifier.equals(gs.getIdentifier())).findFirst().orElse(null);
        if (selection == null) {
            return null;
        }

        CollectionRefSpecificationType collection = selection.getCollection();
        if (collection == null) {
            return null;
        }

        SearchFilterType search;
        if (collection.getCollectionRef() != null) {
            com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType collectionRef = collection.getCollectionRef();
            PrismObject obj = WebModelServiceUtils.loadObject(collectionRef, page);
            if (obj == null) {
                return null;
            }

            ObjectCollectionType objectCollection = (ObjectCollectionType) obj.asObjectable();
            search = objectCollection.getFilter();
        } else {
            search = collection.getFilter();
        }

        if (search == null) {
            return null;
        }

        try {
            return page.getQueryConverter().createObjectFilter(UserType.class, search);
        } catch (Exception ex) {
            LOGGER.debug("Couldn't create search filter", ex);
            page.error("Couldn't create search filter, reason: " + ex.getMessage());
        }

        return null;
    }

    private void selectManuallyPerformed(AjaxRequestTarget target) {
        ObjectFilter filter = null;

        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected != null) {
            String identifier = selected.getValue().groupIdentifier;
            filter = createObjectFilterFromGroupSelection(identifier);
        }

        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<>(page.getMainPopupBodyId(), UserType.class,
                List.of(UserType.COMPLEX_TYPE), true, page, filter) {

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
        page.showMainPopup(panel, target);
    }

    /**
     * Currently the only nice way to create clean object reference from ObjectReferenceType which already contains
     * definitions and other prism stuff - dehydrate it to be stored in session without storing MBs of data
     */
    private ObjectReferenceType cloneObjectReference(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }

        return new ObjectReferenceType()
                .oid(ref.getOid())
                .type(ref.getType())
                .targetName(ref.getTargetName());
    }

    private Map<ObjectReferenceType, List<ObjectReferenceType>> createPoiMembershipMap(List<UserType> users) {
        if (users == null) {
            return new HashMap<>();
        }

        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = new HashMap<>();

        for (UserType user : users) {
            ObjectReferenceType poi = new ObjectReferenceType()
                    .oid(user.getOid())
                    .type(UserType.COMPLEX_TYPE)
                    .targetName(WebComponentUtil.getDisplayNameOrName(user.asPrismObject()));

            List<ObjectReferenceType> refs = user.getRoleMembershipRef().stream()
                    .map(o -> cloneObjectReference(o))
                    .collect(Collectors.toList());

            userMemberships.put(poi, refs);
        }

        return userMemberships;
    }

    private void addUsersPerformed(AjaxRequestTarget target, List<UserType> users) {
        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = createPoiMembershipMap(users);
        selectedGroupOfUsers.setObject(userMemberships);

        page.hideMainPopup(target);
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
        boolean submitted = submitData();
        if (!submitted) {
            return false;
        }

        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    private boolean submitData() {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected == null) {
            return false;
        }

        TileType type = selected.getValue().type;
        if (type == TileType.MYSELF) {
            try {
                MidPointPrincipal principal = SecurityUtil.getPrincipal();

                ObjectReferenceType ref = new ObjectReferenceType()
                        .oid(principal.getOid())
                        .type(UserType.COMPLEX_TYPE)
                        .targetName(principal.getName());

                List<ObjectReferenceType> memberships = principal.getFocus().getRoleMembershipRef()
                        .stream().map(o -> cloneObjectReference(o)).collect(Collectors.toList());

                getModelObject().addPersonOfInterest(ref, memberships);
            } catch (SecurityViolationException ex) {
                LOGGER.debug("Couldn't get principal, shouldn't happen", ex);
            }
        } else {
            Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = selectedGroupOfUsers.getObject();
            getModelObject().addPersonOfInterest(new ArrayList<>(userMemberships.keySet()), userMemberships);
        }

        return true;
    }

    private TargetSelectionType getTargetSelectionConfiguration() {
        AccessRequestType config = getAccessRequestConfiguration(page);
        TargetSelectionType result = null;
        if (config != null) {
            result = config.getTargetSelection();
        }

        return result != null ? result : new TargetSelectionType();
    }

    public static class ObjectReferenceProvider extends ChoiceProvider<ObjectReferenceType> {

        private static final long serialVersionUID = 1L;

        private PersonOfInterestPanel panel;

        public ObjectReferenceProvider(PersonOfInterestPanel panel) {
            this.panel = panel;
        }

        @Override
        public String getDisplayValue(ObjectReferenceType ref) {
            return WebComponentUtil.getDisplayNameOrName(ref);
        }

        @Override
        public String getIdValue(ObjectReferenceType ref) {
            return ref != null ? ref.getOid() : null;
        }

        @Override
        public void query(String text, int page, Response<ObjectReferenceType> response) {
            ObjectFilter filter = null;

            Tile<PersonOfInterest> selected = panel.getSelectedTile();
            if (selected != null) {
                String identifier = selected.getValue().groupIdentifier;
                filter = panel.createObjectFilterFromGroupSelection(identifier);
            }

            ObjectFilter substring = panel.getPrismContext().queryFor(UserType.class)
                    .item(UserType.F_NAME).containsPoly(text).matchingNorm().buildFilter();

            ObjectFilter full = substring;
            if (filter != null) {
                full = panel.getPrismContext().queryFactory().createAnd(filter, substring);
            }

            ObjectQuery query = panel.getPrismContext()
                    .queryFor(UserType.class)
                    .filter(full)
                    .asc(UserType.F_NAME)
                    .maxSize(MULTISELECT_PAGE_SIZE).offset(page * MULTISELECT_PAGE_SIZE).build();

            Task task = panel.page.createSimpleTask(OPERATION_LOAD_USERS);
            OperationResult result = task.getResult();

            try {
                List<PrismObject<UserType>> objects = WebModelServiceUtils.searchObjects(UserType.class, query, result, panel.page);

                response.addAll(objects.stream()
                        .map(o -> new ObjectReferenceType()
                                .oid(o.getOid())
                                .type(UserType.COMPLEX_TYPE)
                                .targetName(WebComponentUtil.getDisplayNameOrName(o))).collect(Collectors.toList()));
            } catch (Exception ex) {
                LOGGER.debug("Couldn't search users for multiselect", ex);
            }
        }

        @Override
        public Collection<ObjectReferenceType> toChoices(Collection<String> collection) {
            return collection.stream()
                    .map(oid -> new ObjectReferenceType()
                            .oid(oid)
                            .type(UserType.COMPLEX_TYPE)).collect(Collectors.toList());
        }
    }
}
