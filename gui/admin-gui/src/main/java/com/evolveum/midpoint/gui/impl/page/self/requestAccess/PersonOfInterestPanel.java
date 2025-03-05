/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.commons.collections4.CollectionUtils;
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
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutocompleteConfigurationMixin;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasicWizardStepPanel<RequestAccess> implements AccessRequestMixin {

    @Serial private static final long serialVersionUID = 1L;

    public static final String STEP_ID = "poi";

    private static final Trace LOGGER = TraceManager.getTrace(TileType.class);

    private static final String DOT_CLASS = PersonOfInterestPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USERS = DOT_CLASS + "loadUsers";
    private static final String OPERATION_COMPILE_TARGET_SELECTION_COLLECTION = DOT_CLASS + "compileTargetSelectionCollection";
    private static final String OPERATION_EVALUATE_FILTER_EXPRESSION = DOT_CLASS + "evaluateFilterExpression";
    private static final String OPERATION_GET_ASSIGNMENT_OPERATION_OBJECT_FILTER = DOT_CLASS + "getAssignmentOperationObjectFilter";
    private static final String OPERATION_CALCULATE_AUTHORIZATION_INCONSISTENCY = DOT_CLASS + "calculateAuthorizationInconsistency";

    private static final int MULTISELECT_PAGE_SIZE = 10;

    private static final String DEFAULT_TILE_ICON = "fas fa-user-friends";

    private static final int AUTOCOMPLETE_MIN_CHARS = 2;

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

    private record PersonOfInterest(String groupIdentifier, TileType type) implements Serializable {

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

    private final PageBase page;

    private LoadableModel<List<Tile<PersonOfInterest>>> tiles;

    private IModel<SelectionState> selectionState;

    private IModel<Map<ObjectReferenceType, List<ObjectReferenceType>>> selectedGroupOfUsers;
    private IModel<Collection<ObjectReferenceType>> multiselectModel;

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

                RequestAccess access = getModelObject();

                TargetSelectionType selection = getTargetSelectionConfiguration();
                if (BooleanUtils.isNotFalse(selection.isAllowRequestForMyself())) {
                    list.add(createDefaultTile(TileType.MYSELF, access.isPoiMyself()));
                }

                if (BooleanUtils.isNotFalse(selection.isAllowRequestForOthers())) {
                    List<GroupSelectionType> selections = selection.getGroup();
                    if (selections.isEmpty()) {
                        boolean selected = BooleanUtils.isNotTrue(access.isPoiMyself()) && access.getPoiCount() != 0;
                        list.add(createDefaultTile(TileType.GROUP_OTHERS, selected));
                    } else {
                        for (GroupSelectionType gs : selections) {
                            list.add(createTile(gs));
                        }
                    }
                }

                if (access.getPoiCount() == 0) {
                    // if no POI was selected yet, we'll try to preselect one

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

        selectedGroupOfUsers = new LoadableModel<>() {

            @Override
            protected Map<ObjectReferenceType, List<ObjectReferenceType>> load() {
                RequestAccess access = getModelObject();

                Map<ObjectReferenceType, List<ObjectReferenceType>> map = new HashMap<>();
                map.putAll(access.getExistingPoiRoleMemberships());

                return map;
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
            label = LocalizationUtil.translatePolyString(display.getLabel());
        }

        Tile<PersonOfInterest> tile = new Tile<>(icon, label);
        tile.setValue(new PersonOfInterest(selection.getIdentifier(), TileType.GROUP_OTHERS));

        return tile;
    }

    private Tile<PersonOfInterest> createDefaultTile(TileType type, Boolean selected) {
        Tile<PersonOfInterest> tile = new Tile<>(type.getIcon(), getString(type));
        tile.setValue(new PersonOfInterest(null, type));
        tile.setSelected(BooleanUtils.isTrue(selected));

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
        return tiles.getObject().stream().filter(Tile::isSelected).findFirst().orElse(null);
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
                TilePanel<Tile<PersonOfInterest>, PersonOfInterest> tp = new TilePanel<>(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<PersonOfInterest> tile = item.getModelObject();
                        switch (tile.getValue().type) {
                            case MYSELF -> myselfPerformed(target, tile);
                            case GROUP_OTHERS -> groupOthersPerformed(target, tile);
                        }
                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);

        return fragment;
    }

    private AutocompleteSearchConfigurationType getAutocompleteConfiguration() {
        GroupSelectionType group = getSelectedGroupSelection();
        if (group == null) {
            return new AutocompleteSearchConfigurationType();
        }

        AutocompleteSearchConfigurationType config = group.getAutocompleteConfiguration();
        if (config != null) {
            return config;
        }

        config = new AutocompleteSearchConfigurationType();
        config.setAutocompleteMinChars(group.getAutocompleteMinChars());
        config.setDisplayExpression(group.getUserDisplayName());
        config.setSearchFilterTemplate(group.getSearchFilterTemplate());

        return config;
    }

    private Fragment initSelectionFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENTS, ID_SELECTION_FRAGMENT, this);

        multiselectModel = new LoadableModel<>() {

            @Override
            public Collection<ObjectReferenceType> load() {
                return new ArrayList<>(selectedGroupOfUsers.getObject().keySet());
            }

            @Override
            public void setObject(Collection<ObjectReferenceType> object) {
                super.setObject(object);
                updateSelectedGroupOfUsers(object);
            }
        };

        AutocompleteSearchConfigurationType config = getAutocompleteConfiguration();
        int minLength = config.getAutocompleteMinChars() != null ? config.getAutocompleteMinChars() : AUTOCOMPLETE_MIN_CHARS;
        if (minLength < 0) {
            minLength = AUTOCOMPLETE_MIN_CHARS;
        }

        Select2MultiChoicePanel<ObjectReferenceType> multiselect = new Select2MultiChoicePanel<>(ID_MULTISELECT, multiselectModel,
                new ObjectReferenceProvider(this), minLength){
            @Override
            protected IModel<String> getAriaLabelModel() {
                return PersonOfInterestPanel.this.createStringResource("PersonOfInterestPanel.selectFieldLabel");
            }
        };

        multiselect.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // model of multiselect was already updated, just "refresh" next button
                target.add(PersonOfInterestPanel.this.getNext());
            }
        });
        fragment.add(multiselect);

        AjaxLink<?> selectManually = new AjaxLink<>(ID_SELECT_MANUALLY) {

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

    /**
     * @param poiRefs this is always absolute state of autocomplete text field - all items that are currently in the field
     */
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
            submitDataPerformed();
        }
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !canSkipStep();
    }

    @Override
    protected void onBeforeRender() {
        Fragment fragment = selectionState.getObject() == SelectionState.USERS ? initSelectionFragment() : initTileFragment();
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

    private GroupSelectionType getSelectedGroupSelection() {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected == null) {
            return null;
        }

        String identifier = selected.getValue().groupIdentifier;
        if (identifier == null) {
            return null;
        }

        List<GroupSelectionType> selections = getTargetSelectionConfiguration().getGroup();
        return selections.stream().filter(gs -> identifier.equals(gs.getIdentifier())).findFirst().orElse(null);
    }

    private ObjectFilter createObjectFilterFromGroupSelection(String identifier) {
        ObjectFilter assignAuthorizationRestrictionFilter = getAssignmentOperationObjectFilter();

        if (identifier == null) {
            return assignAuthorizationRestrictionFilter;
        }

        GroupSelectionType selection = getSelectedGroupSelection();
        if (selection == null) {
            return assignAuthorizationRestrictionFilter;
        }

        CollectionRefSpecificationType collection = selection.getCollection();
        if (collection == null) {
            return assignAuthorizationRestrictionFilter;
        }

        Task task = getPageBase().createSimpleTask(OPERATION_COMPILE_TARGET_SELECTION_COLLECTION);
        OperationResult result = task.getResult();
        CompiledObjectCollectionView compiledObjectCollectionView;
        try {
            // now only UserType as a target is supported, so it is hardcoded here
            compiledObjectCollectionView = getPageBase().getModelInteractionService().compileObjectCollectionView(collection, UserType.class, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't compile object collection view {}", e);
            result.recordFatalError("Couldn't compile object collection view", e);
            return null;
        }

        if (compiledObjectCollectionView.getFilter() == null) {
            return assignAuthorizationRestrictionFilter;
        } else if (assignAuthorizationRestrictionFilter == null) {
            return compiledObjectCollectionView.getFilter();
        }
        return page.getPrismContext()
                .queryFactory()
                .createAnd(assignAuthorizationRestrictionFilter, compiledObjectCollectionView.getFilter());
    }

    private void selectManuallyPerformed(AjaxRequestTarget target) {
        ObjectQuery query = getPrismContext().queryFor(UserType.class).build();
        ObjectFilter filter = null;

        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected != null) {
            String identifier = selected.getValue().groupIdentifier;
            filter = WebComponentUtil.evaluateExpressionsInFilter(
                    createObjectFilterFromGroupSelection(identifier),
                    new OperationResult(OPERATION_EVALUATE_FILTER_EXPRESSION), page);
        }
        if (filter != null) {
            query.addFilter(filter);
        }

        //apply filter from the #assign authorization from object section
        filter = getAssignmentOperationObjectFilter();
        if (filter != null) {
            query.addFilter(filter);
        }

        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<>(page.getMainPopupBodyId(), UserType.class,
                List.of(UserType.COMPLEX_TYPE), true, page, filter) {

            @Serial private static final long serialVersionUID = 1L;

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
                .relation(ref.getRelation())        //we need relation as well, e.g. to analyze defaultAssignmentConstraints (#10425)
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
                    .targetName(getDefaultUserDisplayName(user.asPrismObject()));

            List<ObjectReferenceType> refs = user.getRoleMembershipRef().stream()
                    .map(this::cloneObjectReference)
                    .collect(Collectors.toList());

            userMemberships.put(poi, refs);
        }

        return userMemberships;
    }

    private void addUsersPerformed(AjaxRequestTarget target, List<UserType> users) {
        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = createPoiMembershipMap(users);
        selectedGroupOfUsers.getObject().putAll(userMemberships);

        page.hideMainPopup(target);
        target.add(getWizard().getPanel());
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        if (selectionState.getObject() == SelectionState.TILES) {
            boolean executeDefaultBehaviour = super.onBackPerformed(target);
            if (!executeDefaultBehaviour) {
                getPageBase().redirectBack();
            }
            return executeDefaultBehaviour;
        }

        selectionState.setObject(SelectionState.TILES);
        target.add(this);

        return false;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (shoppingCartDataToBeRecalculated() && shoppingCartInconsistencyExists()) {
            confirmShoppingCartDataRecalculation(target);
            return false;
        }

        submitAndRedirect(target);
        return false;
    }

    private void submitAndRedirect(AjaxRequestTarget target) {
        boolean submitted = submitDataPerformed();
        if (!submitted) {
            return;
        }

        getWizard().next();
        target.add(getWizard().getPanel());
    }

    private boolean submitDataPerformed() {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected == null) {
            return false;
        }

        PersonOfInterest poi = selected.getValue();

        RequestAccess access = getModelObject();
        if (poi.type() == TileType.MYSELF) {
            try {
                MidPointPrincipal principal = SecurityUtil.getPrincipal();

                ObjectReferenceType ref = new ObjectReferenceType()
                        .oid(principal.getOid())
                        .type(UserType.COMPLEX_TYPE)
                        .targetName(getDefaultUserDisplayName((PrismObject<UserType>) principal.getFocusPrismObject()));

                List<ObjectReferenceType> memberships = principal.getFocus().getRoleMembershipRef()
                        .stream().map(this::cloneObjectReference).collect(Collectors.toList());

                access.addPersonOfInterest(ref, memberships);
                access.setPoiMyself(true);
            } catch (SecurityViolationException ex) {
                LOGGER.debug("Couldn't get principal, shouldn't happen", ex);
            }
        } else {
            Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = selectedGroupOfUsers.getObject();
            access.addPersonOfInterest(new ArrayList<>(userMemberships.keySet()), userMemberships);
            access.setPoiGroupSelectionIdentifier(poi.groupIdentifier());
        }

        return true;
    }

    /**
     *  Check if new person of interest was added. If so, we need to recalculate shopping cart data
     *  (e.g. analyze if the requester has authorization to assign already existing shopping cart items
     *  to the newly added person of interest).
     * @return
     */
    private boolean shoppingCartDataToBeRecalculated() {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected == null) {
            return false;
        }

        RequestAccess access = getModelObject();
        if (access.getPoiCount() == 0) {
            return false;
        }

        PersonOfInterest poi = selected.getValue();
        if (poi.type() == TileType.MYSELF && access.isPoiMyself()) {
            return false;
        }

        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = selectedGroupOfUsers.getObject();

        if (!userMemberships.isEmpty() && poi.type() == TileType.MYSELF) {
            //this means that Person of interest was changed from group to myself,
            //so we need to recalculate shopping cart data
            return true;
        }

        List<ObjectReferenceType> newPersonOfInterestRefs = new ArrayList<>(userMemberships.keySet());
        return access.newPersonOfInterestExists(newPersonOfInterestRefs);
    }

    private <AR extends AbstractRoleType> boolean shoppingCartInconsistencyExists() {
        Map<ObjectReferenceType, List<ObjectReferenceType>> userMemberships = selectedGroupOfUsers.getObject();
        RequestAccess access = getModelObject();
        List<String> newPois = access.getNewPersonOfInterestOids(new ArrayList<>(userMemberships.keySet()));

        if (tileTypeWasChangedFromGroupToMyself()) {
            if (newPois == null) {
                newPois = new ArrayList<>();
            }
            newPois.add(page.getPrincipal().getOid());
        }

        if (CollectionUtils.isEmpty(newPois)) {
            return false;
        }

        OperationResult result = new OperationResult(OPERATION_CALCULATE_AUTHORIZATION_INCONSISTENCY);
        Task task = page.createSimpleTask(OPERATION_CALCULATE_AUTHORIZATION_INCONSISTENCY);
        Set<AssignmentType> existingShoppingCartItems = access.getTemplateAssignments();
        for (String newPoiOid : newPois) {
            for (AssignmentType assignment : existingShoppingCartItems) {
                Class<AR> targetType =
                        (Class<AR>) WebComponentUtil.qnameToClass(assignment.getTargetRef().getType());
                PrismObject<UserType> poiUser = WebModelServiceUtils.loadObject(UserType.class, newPoiOid, page, task, result);
                if (poiUser == null) {
                    continue;
                }
                String assignmentOid = assignment.getTargetRef().getOid();
                ObjectFilter assignableRolesFilter = access.getAssignableRolesFilter(poiUser, page, targetType);
                if (assignableRolesFilter == null) {
                    continue;
                }
                ObjectQuery query = page.getPrismContext().queryFor(targetType)
                        .id(assignmentOid)
                        .build();
                query.addFilter(assignableRolesFilter);
                List<PrismObject<AR>> targetObj = WebModelServiceUtils.searchObjects(targetType, query, result, page);
                if (targetObj.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void confirmShoppingCartDataRecalculation(AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(page.getMainPopupBodyId(), createStringResource("PersonOfInterestPanel.confirmShoppingCartDataRecalculation")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource("PersonOfInterestPanel.confirmPopup.shoppingCartCleanup");
            }

            @Override
            protected IModel<String> createNoLabel() {
                return createStringResource("PersonOfInterestPanel.confirmPopup.personOfInterestCleanup");
            }

            @Override
            protected String getNoButtonCssClass() {
                return "btn btn-primary";
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                page.getSessionStorage().getRequestAccess().clearCart();
                submitAndRedirect(target);
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                selectedGroupOfUsers.detach();
                multiselectModel.detach();
                if (tileTypeWasChangedFromGroupToMyself()) {
                    tiles.getObject().forEach(t -> t.setSelected(false));
                }
                target.add(PersonOfInterestPanel.this);
            }
        };
        page.showMainPopup(dialog, target);
    }

    private TargetSelectionType getTargetSelectionConfiguration() {
        AccessRequestType config = getAccessRequestConfiguration(page);
        TargetSelectionType result = null;
        if (config != null) {
            result = config.getTargetSelection();
        }

        return result != null ? result : new TargetSelectionType();
    }

    private ObjectFilter getAutocompleteFilter(String text) {
        return createAutocompleteFilter(
                text, getAutocompleteConfiguration().getSearchFilterTemplate(), this::createDefaultFilter, page);
    }

    private ObjectFilter createDefaultFilter(String text) {
        return getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).containsPoly(text).matchingNorm().buildFilter();
    }

    public static class ObjectReferenceProvider extends ChoiceProvider<ObjectReferenceType> implements AutocompleteConfigurationMixin {

        @Serial private static final long serialVersionUID = 1L;

        private final PersonOfInterestPanel panel;

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
            GroupSelectionType groupSelection = panel.getSelectedGroupSelection();
            ObjectFilter filter = groupSelection != null ? panel.createObjectFilterFromGroupSelection(groupSelection.getIdentifier()) : null;

            ObjectFilter autocompleteFilter = panel.getAutocompleteFilter(text);

            ObjectFilter full = autocompleteFilter;
            if (filter != null) {
                full = panel.getPrismContext().queryFactory().createAnd(filter, autocompleteFilter);
            }

            OperationResult result = new OperationResult(OPERATION_GET_ASSIGNMENT_OPERATION_OBJECT_FILTER);
            Task task = panel.page.createSimpleTask(OPERATION_GET_ASSIGNMENT_OPERATION_OBJECT_FILTER);
            //get filter from the @assign authorization from object section
            filter = WebComponentUtil.getAccessibleForAssignmentObjectsFilter(result, task, panel.page);
            result.close();
            if (!result.isSuccess() && !result.isHandledError()) {
                panel.page.showResult(result);
            }
            if (filter != null) {
                full = panel.getPrismContext().queryFactory().createAnd(full, filter);
            }

            ObjectQuery query = panel.getPrismContext()
                    .queryFor(UserType.class)
                    .filter(full)
                    .asc(UserType.F_NAME)
                    .maxSize(MULTISELECT_PAGE_SIZE).offset(page * MULTISELECT_PAGE_SIZE).build();

            task = panel.page.createSimpleTask(OPERATION_LOAD_USERS);
            result = task.getResult();

            try {
                List<PrismObject<UserType>> objects = WebModelServiceUtils.searchObjects(UserType.class, query, result, panel.page);

                response.addAll(objects.stream()
                        .map(o -> new ObjectReferenceType()
                                .oid(o.getOid())
                                .type(UserType.COMPLEX_TYPE)
                                .targetName(getDisplayName(o))).collect(Collectors.toList()));
            } catch (Exception ex) {
                LOGGER.debug("Couldn't search users for multiselect", ex);
            }
        }

        private String getDisplayName(PrismObject<UserType> o) {
            if (o == null) {
                return null;
            }

            String identifier = null;
            Tile<PersonOfInterest> selected = panel.getSelectedTile();
            if (selected != null) {
                identifier = selected.getValue().groupIdentifier;
            }

            if (identifier == null) {
                return panel.getDefaultUserDisplayName(o);
            }

            AutocompleteSearchConfigurationType config = panel.getAutocompleteConfiguration();
            String displayName = getDisplayNameFromExpression(
                    "User display name for group selection '" + identifier + "' expression",
                    config.getDisplayExpression(), obj -> panel.getDefaultUserDisplayName(obj), o, panel);

            return StringUtils.isNotEmpty(displayName) ? displayName : panel.getDefaultUserDisplayName(o);
        }

        @Override
        public Collection<ObjectReferenceType> toChoices(Collection<String> collection) {
            return collection.stream()
                    .map(oid -> new ObjectReferenceType()
                            .oid(oid)
                            .type(UserType.COMPLEX_TYPE)).collect(Collectors.toList());
        }
    }

    /**
     * Returns filter from the #assign authorization from object section
     * @return
     */
    private ObjectFilter getAssignmentOperationObjectFilter() {
        OperationResult result = new OperationResult(OPERATION_GET_ASSIGNMENT_OPERATION_OBJECT_FILTER);
        Task task = page.createSimpleTask(OPERATION_GET_ASSIGNMENT_OPERATION_OBJECT_FILTER);
        ObjectFilter filter = WebComponentUtil.getAccessibleForAssignmentObjectsFilter(result, task, page);
        result.close();
        if (!result.isSuccess() && !result.isHandledError()) {
            page.showResult(result);
        }
        return filter;
    }

    private boolean tileTypeWasChangedFromGroupToMyself() {
        Tile<PersonOfInterest> selected = getSelectedTile();
        if (selected == null) {
            return false;
        }
        PersonOfInterest poi = selected.getValue();
        return poi.type() == TileType.MYSELF && !getModelObject().isPoiMyself();
    }
}
