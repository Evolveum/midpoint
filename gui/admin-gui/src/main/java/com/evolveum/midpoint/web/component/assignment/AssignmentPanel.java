/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConstructionValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AssignmentPanel extends BasePanel<PrismContainerWrapper<AssignmentType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
    private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";

    private static final String DOT_CLASS = AssignmentPanel.class.getName() + ".";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ = DOT_CLASS + "loadAssignmentsTargetRefObject";
    protected static final String OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS = DOT_CLASS + "loadAssignmentTargetRelations";
    protected static final String OPERATION_LOAD_ASSIGNMENT_HOLDER_SPECIFICATION = DOT_CLASS + "loadAssignmentHolderSpecification";

    protected int assignmentsRequestsLimit = -1;

    public AssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        assignmentsRequestsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
        initLayout();
    }

    private void initLayout() {

        MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentObjectRelation> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentObjectRelation>(ID_ASSIGNMENTS, getModel() != null ? getModel() : Model.of(), getTableId(),
                        getAssignmentsTabStorage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void initPaging() {
                        initCustomPaging();
                    }

                    @Override
                    protected boolean enableActionNewObject() {
                        return isNewObjectButtonVisible(getFocusObject());
                    }

                    @Override
                    protected void cancelItemDetailsPerformed(AjaxRequestTarget target) {
                        AssignmentPanel.this.cancelAssignmentDetailsPerformed(target);
                    }

                    @Override
                    protected ObjectQuery createQuery() {
                        return createObjectQuery();
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> createColumns() {
                        if (AssignmentPanel.this.getModelObject() == null) {
                            return new ArrayList<>();
                        }
                        return initBasicColumns();
                    }

                    @Override
                    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation) {
                        newAssignmentClickPerformed(target, assignmentTargetRelation);
                    }

                    @Override
                    protected List<MultiFunctinalButtonDto> createNewButtonDescription() {
                        return newButtonDescription();
                    }

                    @Override
                    protected boolean getNewObjectGenericButtonVisibility() {
                        AssignmentCandidatesSpecification spec = loadAssignmentHolderSpecification();
                        return spec == null || spec.isSupportGenericAssignment();
                    }

                    @Override
                    protected DisplayType getNewObjectButtonDisplayType() {
                        return WebComponentUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green",
                                AssignmentPanel.this.createStringResource(isInducement() ?
                                        "AssignmentPanel.newInducementTitle" : "AssignmentPanel.newAssignmentTitle", "", "").getString());
                    }

                    @Override
                    protected boolean isNewObjectButtonEnabled() {
                        return !isAssignmentsLimitReached();
                    }

                    @Override
                    protected void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<AssignmentType>> toDeleteList) {
                        int countAddedAssignments = 0;
                        for (PrismContainerValueWrapper<AssignmentType> assignment : toDeleteList) {
                            if (ValueStatus.ADDED.equals(assignment.getStatus())) {
                                countAddedAssignments++;
                            }
                        }
                        boolean isLimitReached = isAssignmentsLimitReached(toDeleteList.size() - countAddedAssignments, true);
                        if (isLimitReached) {
                            warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }
                        super.deleteItemPerformed(target, toDeleteList);
                    }

                    @Override
                    protected List<PrismContainerValueWrapper<AssignmentType>> postSearch(
                            List<PrismContainerValueWrapper<AssignmentType>> assignments) {
                        return customPostSearch(assignments);
                    }

                    @Override
                    protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel(
                            ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
                        return createMultivalueContainerDetailsPanel(item);
                    }

                    @Override
                    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
                        return getCustomSearchPanel(contentAreaId);
                    }

                    @Override
                    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
                        return createSearchableItems(containerDef);
                    }

                    @Override
                    protected WebMarkupContainer initButtonToolbar(String id) {
                        WebMarkupContainer buttonToolbar = initCustomButtonToolbar(id);
                        if (buttonToolbar == null) {
                            return super.initButtonToolbar(id);
                        }
                        return buttonToolbar;
                    }

                };
        multivalueContainerListPanel.add(new VisibleBehaviour(() -> getModel() != null && getModelObject() != null));
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);
    }

    private List<MultiFunctinalButtonDto> newButtonDescription() {
        List<MultiFunctinalButtonDto> buttonDtoList = new ArrayList<>();
        if (AssignmentPanel.this.getModelObject() == null) {
            return null;
        }
        if (isInducement()) {
            return null;
        }

        List<AssignmentObjectRelation> relations = getAssignmentObjectRelationList();
        if (relations == null) {
            return null;
        }

        relations.forEach(relation -> {
            MultiFunctinalButtonDto buttonDto = new MultiFunctinalButtonDto();
            buttonDto.setAssignmentObjectRelation(relation);

            DisplayType additionalButtonDisplayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(AssignmentPanel.this.getPageBase(), relation,
                    isInducement() ? "AssignmentPanel.newInducementTitle" : "AssignmentPanel.newAssignmentTitle");
            buttonDto.setAdditionalButtonDisplayType(additionalButtonDisplayType);

            CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(AssignmentPanel.this.getPageBase(), relation,
                    additionalButtonDisplayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green"));
            CompositedIcon icon = null;
            if (builder != null) {
                icon = builder.build();
            }
            buttonDto.setCompositedIcon(icon);
            buttonDtoList.add(buttonDto);
        });
        return buttonDtoList;
    }

    private List<AssignmentObjectRelation> getAssignmentObjectRelationList() {
        if (AssignmentPanel.this.getModelObject() == null) {
            return null;
        }
        if (isInducement()) {
            return null;
        } else {
            List<AssignmentObjectRelation> assignmentRelationsList =
                    WebComponentUtil.divideAssignmentRelationsByAllValues(loadAssignmentTargetRelationsList());
            if (assignmentRelationsList == null || assignmentRelationsList.isEmpty()) {
                return assignmentRelationsList;
            }
            QName assignmentType = getAssignmentType();
            if (assignmentType == null) {
                return assignmentRelationsList;
            }
            List<AssignmentObjectRelation> assignmentRelationsListFilteredByType =
                    new ArrayList<>();
            assignmentRelationsList.forEach(assignmentRelation -> {
                QName objectType = assignmentRelation.getObjectTypes() != null
                        && !assignmentRelation.getObjectTypes().isEmpty()
                        ? assignmentRelation.getObjectTypes().get(0) : null;
                if (QNameUtil.match(assignmentType, objectType)) {
                    assignmentRelationsListFilteredByType.add(assignmentRelation);
                }
            });
            return assignmentRelationsListFilteredByType;
        }
    }

    protected Fragment initCustomButtonToolbar(String contentAreaId) {
        Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, this);

        MultiCompositedButtonPanel newObjectIcon = getMultivalueContainerListPanel().getNewItemButton(ID_NEW_ITEM_BUTTON);
        searchContainer.add(newObjectIcon);

        return searchContainer;
    }

    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        if (getAssignmentType() == null) {
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs);
            SearchFactory.addSearchRefDef(containerDef,
                    ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
                            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
        }
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;

    }

    protected QName getAssignmentType() {
        return null;
    }

    protected void initCustomPaging() {
        if (getModel() == null || getModelObject() == null) {
            return;
        }
        getAssignmentsTabStorage().setPaging(getPrismContext().queryFactory().createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
    }

    protected ObjectTabStorage getAssignmentsTabStorage() {
        if (getModel() == null || getModelObject() == null) {
            return null;
        }
        if (isInducement()) {
            return getParentPage().getSessionStorage().getInducementsTabStorage();
        } else {
            return getParentPage().getSessionStorage().getAssignmentsTabStorage();
        }
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
        return assignments;
    }

    protected <AH extends AssignmentHolderType> boolean isNewObjectButtonVisible(PrismObject<AH> focusObject) {
        try {
            return getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
                    AuthorizationPhaseType.REQUEST, focusObject,
                    null, null, null);
        } catch (Exception ex) {
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
        }
    }

    protected ObjectQuery createObjectQuery() {
        Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.DELEGATION);

        //do not show archetype assignments
        ObjectReferenceType archetypeRef = new ObjectReferenceType();
        archetypeRef.setType(ArchetypeType.COMPLEX_TYPE);
        archetypeRef.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
        RefFilter archetypeFilter = (RefFilter) getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(archetypeRef.asReferenceValue())
                .buildFilter();
        archetypeFilter.setOidNullAsAny(true);
        archetypeFilter.setRelationNullAsAny(true);

        ObjectQuery query = getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .not()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(delegationRelations.toArray(new QName[0]))
                .build();
        query.addFilter(getPrismContext().queryFactory().createNot(archetypeFilter));
        return query;
    }

    protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
    }

    private <AH extends AssignmentHolderType> List<AssignmentObjectRelation> loadAssignmentTargetRelationsList() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS);
        List<AssignmentObjectRelation> assignmentTargetRelations = new ArrayList<>();
        PrismObject<AH> obj = getMultivalueContainerListPanel().getFocusObject();
        try {
            AssignmentCandidatesSpecification spec = getPageBase().getModelInteractionService()
                    .determineAssignmentTargetSpecification(obj, result);
            assignmentTargetRelations = spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return assignmentTargetRelations;
    }

    private AssignmentCandidatesSpecification loadAssignmentHolderSpecification() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_HOLDER_SPECIFICATION);
        PrismObject obj = getMultivalueContainerListPanel().getFocusObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment holder specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        columns.add(new IconColumn<PrismContainerValueWrapper<AssignmentType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentType assignment = rowModel.getObject().getRealValue();
                LOGGER.trace("Create icon for AssignmentType: " + assignment);
                if (assignment != null && assignment.getTargetRef() != null && StringUtils.isNotEmpty(assignment.getTargetRef().getOid())) {
                    List<ObjectType> targetObjectList = WebComponentUtil.loadReferencedObjectList(Collections.singletonList(assignment.getTargetRef()), OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ,
                            AssignmentPanel.this.getPageBase());
                    if (CollectionUtils.isNotEmpty(targetObjectList) && targetObjectList.size() == 1) {
                        ObjectType targetObject = targetObjectList.get(0);
                        DisplayType displayType = WebComponentUtil.getArchetypePolicyDisplayType(targetObject, AssignmentPanel.this.getPageBase());
                        if (displayType != null) {
                            String disabledStyle;
                            if (targetObject instanceof FocusType) {
                                disabledStyle = WebComponentUtil.getIconEnabledDisabled(((FocusType) targetObject).asPrismObject());
                                if (displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass()) &&
                                        disabledStyle != null) {
                                    displayType.getIcon().setCssClass(displayType.getIcon().getCssClass() + " " + disabledStyle);
                                    displayType.getIcon().setColor("");
                                }
                            }
                            return displayType;
                        }
                    }
                }
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
                        AssignmentsUtil.getTargetType(rowModel.getObject().getRealValue())));
            }

        });

        columns.add(new AjaxLinkColumn<PrismContainerValueWrapper<AssignmentType>>(createStringResource("PolicyRulesPanel.nameColumn")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                LOGGER.trace("Create name for AssignmentType: " + rowModel.getObject().getRealValue());
                String name = AssignmentsUtil.getName(rowModel.getObject(), getParentPage());
                LOGGER.trace("Name for AssignmentType: " + name);
                if (StringUtils.isBlank(name)) {
                    return createStringResource("AssignmentPanel.noName");
                }
                return Model.of(name);
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return rowModel.getObject().getRealValue().getFocusMappings() == null;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentPanel.this.assignmentDetailsPerformed(target);
                getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        });

        columns.add(new PrismContainerWrapperColumn<>(getModel(), AssignmentType.F_ACTIVATION, getPageBase()));

        if (getAssignmentType() == null) {
            columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("AssignmentPanel.moreData")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                        IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                    AssignmentType assignmentType = rowModel != null && rowModel.getObject() != null ?
                            rowModel.getObject().getRealValue() : null;
                    cellItem.add(new Label(componentId, AssignmentsUtil.getAssignmentSpecificInfoLabel(assignmentType, AssignmentPanel.this.getPageBase())));
                }
            });
        }

        columns.addAll(initColumns());
        List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
        columns.add(new InlineMenuButtonColumn<PrismContainerValueWrapper<AssignmentType>>(menuActionsList, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isButtonMenuItemEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                if (rowModel != null
                        && ValueStatus.ADDED.equals(rowModel.getObject().getStatus())) {
                    return true;
                }
                return !isAssignmentsLimitReached();
            }
        });
        return columns;
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return new ArrayList<>();
    }

    protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
    }

    protected void newAssignmentClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation) {
        AssignmentPopup popupPanel = new AssignmentPopup(getPageBase().getMainPopupBodyId()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                super.addPerformed(target, newAssignmentsList);
                addSelectedAssignmentsPerformed(target, newAssignmentsList);
            }

            @Override
            protected List<ObjectTypes> getAvailableObjectTypesList() {
                if (assignmentTargetRelation == null || CollectionUtils.isEmpty(assignmentTargetRelation.getObjectTypes())) {
                    return getObjectTypesList();
                } else {
                    return mergeNewAssignmentTargetTypeLists(assignmentTargetRelation.getObjectTypes(), getObjectTypesList());
                }
            }

            @Override
            protected QName getPredefinedRelation() {
                if (assignmentTargetRelation == null) {
                    return AssignmentPanel.this.getPredefinedRelation();
                }
                return !CollectionUtils.isEmpty(assignmentTargetRelation.getRelations()) ? assignmentTargetRelation.getRelations().get(0) : null;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList() {
                return assignmentTargetRelation != null ? assignmentTargetRelation.getArchetypeRefs() : null;
            }

            @Override
            protected ObjectFilter getSubtypeFilter() {
                return AssignmentPanel.this.getSubtypeFilter();
            }

            @Override
            protected boolean isEntitlementAssignment() {
                return AssignmentPanel.this.isEntitlementAssignment();
            }

            @Override
            protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                return AssignmentPanel.this.getModelObject();
            }

            @Override
            protected boolean isOrgTreeTabVisible() {
                return assignmentTargetRelation == null;
            }
        };
        popupPanel.setOutputMarkupId(true);
        popupPanel.setOutputMarkupPlaceholderTag(true);
        getPageBase().showMainPopup(popupPanel, target);
    }

    //this is here just becasue we want to override default behaviour for GDPR assignment panel
    protected QName getPredefinedRelation() {
        return null;
    }

    protected List<ObjectTypes> getObjectTypesList() {
        if (getAssignmentType() == null) {
            return WebComponentUtil.createAssignableTypesList();
        } else {
            return Collections.singletonList(ObjectTypes.getObjectTypeFromTypeQName(getAssignmentType()));
        }
    }

    protected boolean isEntitlementAssignment() {
        return false;
    }

    protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
        if (CollectionUtils.isEmpty(newAssignmentsList)) {
            warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList.size(), true);
        if (isAssignmentsLimitReached) {
            warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        newAssignmentsList.forEach(assignment -> {

            PrismContainerValue<AssignmentType> newAssignment = getModelObject().getItem().createNewValue();
            AssignmentType assignmentType = newAssignment.asContainerable();

            if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
                assignmentType.setConstruction(assignment.getConstruction());
            } else {
                assignmentType.setTargetRef(assignment.getTargetRef());
            }
            getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModelObject(),
                    target);
            getMultivalueContainerListPanel().refreshTable(target);
            getMultivalueContainerListPanel().reloadSavePreviewButtons(target);

        });

    }

    protected WebMarkupContainer getCustomSearchPanel(String contentAreaId) {
        return new WebMarkupContainer(contentAreaId);
    }

    private MultivalueContainerDetailsPanel<AssignmentType> createMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
        if (isAssignmentsLimitReached()) {
            item.getModelObject().setReadOnly(true, true);
        } else if (item.getModelObject().isReadOnly()) {
            item.getModelObject().setReadOnly(false, true);
        }

        return new MultivalueContainerDetailsPanel<AssignmentType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                return AssignmentPanel.this.getContainerVisibility(itemWrapper);
            }

            @Override
            protected void addBasicContainerValuePanel(String idPanel) {
                add(getBasicContainerPanel(idPanel, item.getModel()));
            }

            @Override
            protected boolean getBasicTabEditability(ItemWrapper<?, ?> itemWrapper) {
                return getContainerReadability(itemWrapper);
            }

            @Override
            protected DisplayNamePanel<AssignmentType> createDisplayNamePanel(String displayNamePanelId) {
                IModel<AssignmentType> displayNameModel = getDisplayModel(item.getModelObject().getRealValue());
                return new DisplayNamePanel<AssignmentType>(displayNamePanelId, displayNameModel) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getRelation() {
                        return getRelationForDisplayNamePanel(item.getModelObject());
                    }

                    @Override
                    protected IModel<String> getKindIntentLabelModel() {
                        return getKindIntentLabelModelForDisplayNamePanel(item.getModelObject());
                    }

                };
            }

        };
    }

    protected Panel getBasicContainerPanel(String idPanel, IModel<PrismContainerValueWrapper<AssignmentType>> model) {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(this::getContainerVisibility)
                .editabilityHandler(this::getContainerReadability)
                .build();
        return getPageBase().initContainerValuePanel(idPanel, model, settings);
    }

    protected boolean getContainerReadability(ItemWrapper<?, ?> wrapper) {
        return true;
    }

    protected ItemVisibility getContainerVisibility(ItemWrapper<?, ?> wrapper) {
        if (QNameUtil.match(ActivationType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.AUTO;
        }
        if (QNameUtil.match(MetadataType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.AUTO;
        }

        if (ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).equivalent(wrapper.getPath().namedSegmentsOnly())) {
            return ItemVisibility.HIDDEN;
        }

        if (ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TARGET_REF).equivalent(wrapper.getPath().namedSegmentsOnly())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(OrderConstraintsType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentSelectorType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(OtherPrivilegesLimitationType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(PolicyExceptionType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentRelationType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(ExtensionType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return ItemVisibility.HIDDEN;
        }

        ItemPath assignmentConditionPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONDITION);
        ItemPath inducementConditionPath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONDITION);

        ItemPath wrapperPath = wrapper.getPath().namedSegmentsOnly();
        if (assignmentConditionPath.isSubPath(wrapperPath) || inducementConditionPath.isSubPath(wrapperPath)) {
            ItemPath assignmentConditionExpressionPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);
            ItemPath inducementConditionExpressionPath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);
            if (wrapperPath.equivalent(assignmentConditionExpressionPath) || wrapperPath.equivalent(inducementConditionExpressionPath)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP).equivalent(wrapper.getPath().namedSegmentsOnly())) {
            return ItemVisibility.HIDDEN;
        }

        if (ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).equivalent(wrapper.getPath().namedSegmentsOnly())) {
            return ItemVisibility.HIDDEN;
        }

        return getTypedContainerVisibility(wrapper);
    }

    protected ItemVisibility getTypedContainerVisibility(ItemWrapper<?, ?> wrapper) {
        return ItemVisibility.AUTO;
    }

    private QName getRelationForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
        AssignmentType assignment = modelObject.getRealValue();
        if (assignment.getTargetRef() != null) {
            return assignment.getTargetRef().getRelation();
        } else {
            return null;
        }
    }

    private IModel<String> getKindIntentLabelModelForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
        AssignmentType assignment = modelObject.getRealValue();
        if (assignment.getConstruction() != null) {
            PrismContainerValueWrapper<ConstructionType> constructionValue = null;
            try {
                PrismContainerWrapper<ConstructionType> construction = modelObject.findContainer(AssignmentType.F_CONSTRUCTION);
                if (construction == null) {
                    return null;
                }
                constructionValue = construction.getValue();
            } catch (SchemaException e) {
                LOGGER.error("Unexpected problem during construction wrapper lookup, {}", e.getMessage(), e);
            }
            ShadowKindType kind;
            String intent;
            if (constructionValue instanceof ConstructionValueWrapper) {
                kind = ((ConstructionValueWrapper) constructionValue).getKind();
                intent = ((ConstructionValueWrapper) constructionValue).getIntent();
            } else {
                kind = assignment.getConstruction().getKind();
                intent = assignment.getConstruction().getIntent();
            }

            return createStringResource("DisplayNamePanel.kindIntentLabel", kind, intent);
        }
        return Model.of();
    }

    private List<ObjectTypes> mergeNewAssignmentTargetTypeLists(List<QName> allowedByAssignmentTargetSpecification, List<ObjectTypes> availableTypesList) {
        if (CollectionUtils.isEmpty(allowedByAssignmentTargetSpecification)) {
            return availableTypesList;
        }
        if (CollectionUtils.isEmpty(availableTypesList)) {
            return availableTypesList;
        }
        List<ObjectTypes> mergedList = new ArrayList<>();
        allowedByAssignmentTargetSpecification.forEach(qnameValue -> {
            ObjectTypes objectTypes = ObjectTypes.getObjectTypeFromTypeQName(qnameValue);
            for (ObjectTypes availableObjectTypes : availableTypesList) {
                if (availableObjectTypes.getClassDefinition().equals(objectTypes.getClassDefinition())) {
                    mergedList.add(objectTypes);
                    break;
                }
            }
        });
        return mergedList;
    }

    @SuppressWarnings("unchecked")
    private <C extends Containerable> IModel<C> getDisplayModel(AssignmentType assignment) {
        return (IModel<C>) () -> {
            if (assignment.getTargetRef() != null && assignment.getTargetRef().getOid() != null) {
                Task task = getPageBase().createSimpleTask("Load target");
                OperationResult result = task.getResult();
                PrismObject<ObjectType> targetObject = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result);
                return targetObject != null ? (C) targetObject.asObjectable() : null;
            }
            if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
                if (assignment.getConstruction().getResourceRef().getOid() != null) {
                    Task task = getPageBase().createSimpleTask("Load resource");
                    OperationResult result = task.getResult();
                    PrismObject<?> object = WebModelServiceUtils.loadObject(assignment.getConstruction().getResourceRef(), getPageBase(), task, result);
                    if (object != null) {
                        return (C) object.asObjectable();
                    }
                } else {
                    return (C) assignment.getConstruction();
                }
            } else if (assignment.getPersonaConstruction() != null) {
                return (C) assignment.getPersonaConstruction();
            } else if (assignment.getPolicyRule() != null) {
                return (C) assignment.getPolicyRule();
            }
            return null;
        };
    }

    private <AH extends AssignmentHolderType> List<InlineMenuItem> getAssignmentMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        PrismObject<AH> obj = getMultivalueContainerListPanel().getFocusObject();
        try {
            boolean isUnassignAuthorized = getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
                    AuthorizationPhaseType.REQUEST, obj,
                    null, null, null);
            if (isUnassignAuthorized) {
                menuItems.add(new ButtonInlineMenuItem(getAssignmentsLimitReachedUnassignTitleModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIconBuilder getIconCompositedBuilder() {
                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return getMultivalueContainerListPanel().createDeleteColumnAction();
                    }
                });
            }

        } catch (Exception ex) {
            LOGGER.error("Couldn't check unassign authorization for the object: {}, {}", obj.getName(), ex.getLocalizedMessage());
            if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)) {
                menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIconBuilder getIconCompositedBuilder() {
                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return getMultivalueContainerListPanel().createDeleteColumnAction();
                    }
                });
            }
        }
        menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return getMultivalueContainerListPanel().createEditColumnAction();
            }
        });
        menuItems.add(new ButtonInlineMenuItem(createStringResource("AssignmentPanel.viewTargetObject")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_NAVIGATE_ARROW);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<AssignmentType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<AssignmentType> assignmentContainer = getRowModel().getObject();
                        PrismReferenceWrapper<ObjectReferenceType> targetRef;
                        try {
                            targetRef = assignmentContainer.findReference(ItemPath.create(AssignmentType.F_TARGET_REF));
                        } catch (SchemaException e) {
                            getSession().error("Couldn't show details page. More information provided in log.");
                            LOGGER.error("Couldn't show details page, no targetRef reference wrapper found: {}", e.getMessage(), e);
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        if (targetRef != null && targetRef.getValues() != null && targetRef.getValues().size() > 0) {
                            PrismReferenceValueWrapperImpl<ObjectReferenceType> refWrapper = targetRef.getValues().get(0);
                            if (!StringUtils.isEmpty(refWrapper.getNewValue().getOid())) {
                                Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(refWrapper.getRealValue().getType()).getClassDefinition();
                                WebComponentUtil.dispatchToObjectDetailsPage(targetClass, refWrapper.getNewValue().getOid(), AssignmentPanel.this, false);
                            }
                        }
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
        return menuItems;
    }

    @SuppressWarnings("unchecked")
    protected MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentCandidatesSpecification> getMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<AssignmentType, AssignmentCandidatesSpecification>) get(ID_ASSIGNMENTS));
    }

    protected TableId getTableId() {
        return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
    }

    protected PageBase getParentPage() {
        return getPageBase();
    }

    private IModel<String> getAssignmentsLimitReachedUnassignTitleModel() {
        return new LoadableModel<String>(true) {
            @Override
            protected String load() {
                return isAssignmentsLimitReached() ?
                        AssignmentPanel.this.getPageBase().createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle",
                                assignmentsRequestsLimit).getString() : createStringResource("pageAdminFocus.menu.unassign").getString();
            }
        };
    }

    protected boolean isAssignmentsLimitReached() {
        return isAssignmentsLimitReached(0, false);
    }

    @SuppressWarnings("deprecation")
    protected boolean isAssignmentsLimitReached(int selectedAssignmentsCount, boolean actionPerformed) {
        if (assignmentsRequestsLimit < 0) {
            return false;
        }
        int changedItems = 0;
        List<PrismContainerValueWrapper<AssignmentType>> assignmentsList = getModelObject().getValues();
        for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsList) {
            if (assignment.hasChanged()) {
                changedItems++;
            }
        }
        return actionPerformed ? (changedItems + selectedAssignmentsCount) > assignmentsRequestsLimit :
                (changedItems + selectedAssignmentsCount) >= assignmentsRequestsLimit;
    }

    protected boolean isInducement() {
        return getModelObject() != null && getModelObject().getPath().containsNameExactly(AbstractRoleType.F_INDUCEMENT);
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }
}
