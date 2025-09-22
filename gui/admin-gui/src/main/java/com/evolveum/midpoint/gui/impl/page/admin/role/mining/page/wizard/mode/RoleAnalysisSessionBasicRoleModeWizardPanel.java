/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.mode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.component.FocusTypeAssignmentPopupTabPanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RangeSliderPanelSimpleModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class RoleAnalysisSessionBasicRoleModeWizardPanel
        extends AbstractWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String ID_CARD_TITLE = "card-title";
    //    private static final String ID_TITLE_ARCHETYPE = "title-archetype";
    private static final String ID_LABEL_ARCHETYPE = "label-archetype";
    private static final String ID_INPUT_ARCHETYPE = "input-archetype";
    private static final String ID_SELECTED_ARCHETYPE = "selected-archetype";
    private static final String ID_REMOVE_ARCHETYPE = "remove-archetype";

    //    private static final String ID_TITLE_MEMBERSHIP = "title-membership";
    private static final String ID_LABEL_MEMBERSHIP = "label-membership";
    private static final String ID_INPUT_MEMBERSHIP = "input-membership";

    //    private static final String ID_TITLE_ROLES = "title-roles";
    private static final String ID_LABEL_ROLES = "label-roles";
    private static final String ID_SELECTED_ROLES = "selected-roles";

    double defaultPercentageMembership = 60.0;

    LoadableModel<Integer> totalUsersModel = new LoadableModel<>(true) {
        @Contract(pure = true)
        @Override
        protected @NotNull Integer load() {
            return 0;
        }
    };

    LoadableModel<String> archetypeNameModel = new LoadableModel<>(false) {
        @Contract(pure = true)
        @Override
        protected @NotNull String load() {
            return "Not selected";
        }
    };

    ItemRealValueModel<Integer> minMembersOverlapModel;

    public RoleAnalysisSessionBasicRoleModeWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        resolveMaximumUserCount(null);

        initLayout();
    }

    private int resolveMaximumUserCount(ObjectQuery query) {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        Task task = getPageBase().createSimpleTask("Count user owned role assignment");
        OperationResult result = new OperationResult("Count user owned role assignment");
        Integer totalUserOwnedRole = roleAnalysisService.countObjects(UserType.class, query, null, task, result);
        if (totalUserOwnedRole == null) {
            totalUserOwnedRole = 0;
        }
        totalUsersModel.setObject(totalUserOwnedRole);
        return totalUserOwnedRole;
    }

    private void initLayout() {
        IconWithLabel cardTitle = new IconWithLabel(ID_CARD_TITLE,
                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.card.title.label")) {
            @Override
            protected String getIconCssClass() {
                return GuiStyleConstants.CLASS_SCHEMA_BASIC_SETTINGS_PANEL_ICON;
            }
        };
        cardTitle.setOutputMarkupId(true);
        add(cardTitle);

        initArchetypeSelectionPanel();
        initMembershipSelectionPanel();
        initRolesSelectionPanel();
    }

    protected void onSubmit(AjaxRequestTarget target, SearchFilterType filter, String archetypeOid) {
        ObjectQuery query = null;

        if (archetypeOid != null) {
            query = PrismContext.get().queryFor(UserType.class)
                    .item(AssignmentHolderType.F_ARCHETYPE_REF)
                    .ref(archetypeOid).build();
        }

        int totalUserOwnedRole = resolveMaximumUserCount(query);
        totalUsersModel.setObject(totalUserOwnedRole);

        setSearchFilterOption(filter);

        int minOverlap = 0;
        if(totalUserOwnedRole != 0){
            minOverlap = (int) Math.round(totalUserOwnedRole * defaultPercentageMembership / 100);
        }

        minMembersOverlapModel.setObject(minOverlap);
        target.add(getArchetypeSelectionLabel());
        target.add(getMembershipSelectionInput());
    }

    private void setSearchFilterOption(SearchFilterType filter) {
        AssignmentHolderDetailsModel<RoleAnalysisSessionType> detailsModel = getDetailsModel();
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper = detailsModel.getObjectWrapperModel();
        try {
            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> primaryOptions = getPrimaryOptionContainerFormModel(
                    objectWrapper).getObject().getValue();
            setupQueryProperty(primaryOptions, filter);
        } catch (SchemaException e) {
            throw new SystemException("Cannot set query filter", e);
        }
    }

    private void initArchetypeSelectionPanel() {
//        IconWithLabel archetypeTitlePanel = new IconWithLabel(ID_TITLE_ARCHETYPE,
//                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.archetype.title.label")) {
//            @Override
//            protected String getIconCssClass() {
//                return GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON;
//            }
//        };
//        archetypeTitlePanel.setOutputMarkupId(true);
//        add(archetypeTitlePanel);

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_LABEL_ARCHETYPE,
                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.archetype.label")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.archetype.label.help");
            }
        };
        add(labelWithHelpPanel);

        Label selectedArchetype = new Label(ID_SELECTED_ARCHETYPE, archetypeNameModel);
        selectedArchetype.setOutputMarkupId(true);
        add(selectedArchetype);

        AjaxIconButton changeArchetypeButton = createChangeArchetypeButton();
        changeArchetypeButton.setOutputMarkupId(true);
        add(changeArchetypeButton);

        AjaxIconButton removeArchetypeButton = new AjaxIconButton(ID_REMOVE_ARCHETYPE,
                Model.of(GuiStyleConstants.CLASS_ICON_TRASH),
                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.archetype.remove")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                archetypeNameModel.setObject("Not selected");
                onSubmit(target, null, null);
            }
        };
        removeArchetypeButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm"));
        removeArchetypeButton.setOutputMarkupId(true);
        removeArchetypeButton.showTitleAsLabel(false);

        add(removeArchetypeButton);
    }

    private void initMembershipSelectionPanel() {
//        IconWithLabel membershipTitlePanel = new IconWithLabel(ID_TITLE_MEMBERSHIP,
//                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.membership.title.label")) {
//            @Override
//            protected String getIconCssClass() {
//                return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
//            }
//        };
//        membershipTitlePanel.setOutputMarkupId(true);
//        add(membershipTitlePanel);

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_LABEL_MEMBERSHIP,
                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.membership.label")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.membership.label.help");
            }
        };
        add(labelWithHelpPanel);

        minMembersOverlapModel = new ItemRealValueModel<>((IModel<? extends PrismValueWrapper<Integer>>) () -> {
            try {

                PrismContainerValueWrapper<Containerable> parent = getObjectWrapper().findContainerValue(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS);
                if (parent != null) {
                    PrismPropertyWrapper<Integer> objectClass = parent.findProperty(
                            AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP);
                    return objectClass.getValue();
                }
                return null;

            } catch (SchemaException e) {
                this.error("Couldn't find object class property.");
                return null;
            }
        }) {
            @Override
            public void setObject(Integer object) {
                super.setObject(object);
            }
        };

        RangeSliderPanelSimpleModel rangeSliderPanel = new RangeSliderPanelSimpleModel(ID_INPUT_MEMBERSHIP,
                minMembersOverlapModel, totalUsersModel);
        rangeSliderPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        rangeSliderPanel.setOutputMarkupId(true);
        add(rangeSliderPanel);

    }

    private void initRolesSelectionPanel() {
//        IconWithLabel archetypeTitlePanel = new IconWithLabel(ID_TITLE_ROLES,
//                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.roles.title.label")) {
//            @Override
//            protected String getIconCssClass() {
//                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
//            }
//        };
//        archetypeTitlePanel.setOutputMarkupId(true);
//        add(archetypeTitlePanel);

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_LABEL_ROLES,
                createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.roles.label")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.roles.label.help");
            }
        };
        add(labelWithHelpPanel);

        ItemRealValueModel<Integer> minMembersModel = new ItemRealValueModel<>((IModel<? extends PrismValueWrapper<Integer>>) () -> {
            try {

                PrismContainerValueWrapper<Containerable> parent = getObjectWrapper().findContainerValue(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS);
                if (parent != null) {
                    PrismPropertyWrapper<Integer> objectClass = parent.findProperty(
                            AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT);
                    return objectClass.getValue();
                }
                return null;

            } catch (SchemaException e) {
                this.error("Couldn't find object class property.");
                return null;
            }
        }) {
            @Override
            public void setObject(Integer object) {
                super.setObject(object);
                if (object != null) {
                    PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = getObjectWrapper();
                    PrismContainer<Containerable> property = objectWrapper.getObject()
                            .findContainer(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION);
                    property.findProperty(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                            .setRealValue(object);
                }

            }
        };

        TextPanel<Integer> components = new TextPanel<>(ID_SELECTED_ROLES,
                minMembersModel, Integer.class, false) {

        };
        components.setOutputMarkupId(true);
        add(components);

    }

    private PrismObjectWrapper<RoleAnalysisSessionType> getObjectWrapper() {
        return getDetailsModel().getObjectWrapper();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("RoleAnalysisSessionBasicRoleModeWizardPanel.subText");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getPrimaryOptionContainerFormModel(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel) {
        RoleAnalysisSessionType session = objectWrapperModel.getObject().getObject().asObjectable();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        }
        return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
    }

    private void setupQueryProperty(@NotNull PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType,
            Object realValue) throws SchemaException {
        sessionType.findProperty(AbstractAnalysisSessionOptionType.F_USER_SEARCH_FILTER).getValue().setRealValue(realValue);
    }

    private @NotNull AjaxIconButton createChangeArchetypeButton() {
        IconType iconType = new IconType();
        iconType.setCssClass(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
        AjaxIconButton changeArchetype = new AjaxIconButton(ID_INPUT_ARCHETYPE,
                Model.of(GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON),
                createStringResource("PageAdminObjectDetails.button.changeArchetype")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                changeArchetypeButtonClicked(target);
            }
        };
        changeArchetype.showTitleAsLabel(true);
        changeArchetype.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm"));
        changeArchetype.setOutputMarkupId(true);
        return changeArchetype;
    }

    private void changeArchetypeButtonClicked(AjaxRequestTarget target) {

        AssignmentPopup changeArchetypePopup = new AssignmentPopup(getPageBase().getMainPopupBodyId(),
                Model.of(new AssignmentPopupDto(null))) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                performOnArchetypeSelection(target, newAssignmentsList);

            }

            @Override
            protected @NotNull List<ITab> createAssignmentTabs(AssignmentObjectRelation assignmentObjectRelation) {
                List<ITab> tabs = new ArrayList<>();

                tabs.add(new PanelTab(getPageBase().createStringResource("ObjectTypes.ARCHETYPE"),
                        new VisibleBehaviour(() -> true)) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel<ArchetypeType>(panelId, ObjectTypes.ARCHETYPE, null) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<QName> getSupportedRelations() {
                                return Collections.singletonList(SchemaConstants.ORG_DEFAULT);
                            }

                            @Override
                            protected ObjectQuery addFilterToContentQuery() {
                                ObjectQuery query = getPrismContext().queryFactory().createQuery();
                                List<String> archetypeOidsList = WebComponentUtil.getArchetypeOidsListByHolderType(
                                        UserType.class, getPageBase());
                                ObjectFilter filter = getPrismContext().queryFor(ArchetypeType.class)
                                        .id(archetypeOidsList.toArray(new String[0]))
                                        .buildFilter();
                                query.addFilter(filter);
                                return query;
                            }

                            @SuppressWarnings("rawtypes")
                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target,
                                    List<IModel<SelectableBean<ArchetypeType>>> rowModelList,
                                    DataTable dataTable) {
                                target.add(getObjectListPanel());
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(
                                    IModel<SelectableBean<ArchetypeType>> rowModel) {
                                if (rowModel == null) {
                                    return Model.of(false);
                                }
                                List<?> selectedObjects = getPreselectedObjects();
                                return Model.of(selectedObjects == null || selectedObjects.isEmpty()
                                        || (rowModel.getObject() != null && rowModel.getObject().isSelected()));
                            }

                            @Override
                            protected ObjectTypes getObjectType() {
                                return ObjectTypes.ARCHETYPE;
                            }
                        };
                    }
                });
                return tabs;
            }

            @Contract(pure = true)
            @Override
            protected @Nullable IModel<String> getWarningMessageModel() {
                return null;
            }
        };

        changeArchetypePopup.setOutputMarkupPlaceholderTag(true);
        getPageBase().showMainPopup(changeArchetypePopup, target);
    }

    private void performOnArchetypeSelection(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
        if (newAssignmentsList != null && newAssignmentsList.size() == 1) {
            AssignmentType assignmentType = newAssignmentsList.get(0);
            if (assignmentType.getTargetRef() != null) {
                ObjectReferenceType archetypeRef = assignmentType.getTargetRef();
                if (archetypeRef.getOid() != null) {
                    archetypeNameModel.setObject(archetypeRef.getTargetName().getOrig());
                    ObjectFilter query = PrismContext.get().queryFor(UserType.class)
                            .item(AssignmentHolderType.F_ARCHETYPE_REF)
                            .ref(archetypeRef.getOid()).buildFilter();

                    SearchFilterType queryType;
                    try {
                        queryType = PrismContext.get().getQueryConverter().createSearchFilterType(query);
                    } catch (SchemaException e) {
                        throw new IllegalStateException("Couldn't create query type from query: " + query, e);
                    }
                    onSubmit(target, queryType, archetypeRef.getOid());
                }
            }
        }
    }

    public Component getArchetypeSelectionLabel() {
        return get(getPageBase().createComponentPath(ID_SELECTED_ARCHETYPE));
    }

    public Component getMembershipSelectionInput() {
        return get(getPageBase().createComponentPath(ID_INPUT_MEMBERSHIP));
    }
}
