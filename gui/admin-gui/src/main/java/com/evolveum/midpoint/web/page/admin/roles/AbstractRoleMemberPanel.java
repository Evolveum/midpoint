/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ConfigureTaskConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;

public abstract class AbstractRoleMemberPanel<R extends AbstractRoleType> extends BasePanel<R> {

    private static final long serialVersionUID = 1L;

    protected enum QueryScope {
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
    private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";
    protected static final String OPERATION_LOAD_ARCHETYPE_OBJECT = DOT_CLASS + "loadArchetypeObject";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_CHILD_TABLE = "childUnitTable";
    protected static final String ID_MEMBER_TABLE = "memberTable";

    private static final String ID_OBJECT_TYPE = "type";
    private static final String ID_TENANT = "tenant";
    private static final String ID_PROJECT = "project";
    private static final String ID_INDIRECT_MEMBERS = "indirectMembers";

    protected static final String ID_SEARCH_SCOPE = "searchScope";
    protected SearchBoxScopeType scopeDefaultValue = null;
    protected QName objectTypeDefaultValue = null;

    protected static final String ID_SEARCH_BY_RELATION = "searchByRelation";

    private static final Map<QName, Map<String, String>> AUTHORIZATIONS = new HashMap<>();
    private static final Map<QName, UserProfileStorage.TableId> TABLES_ID = new HashMap<>();

    static {
        TABLES_ID.put(RoleType.COMPLEX_TYPE, UserProfileStorage.TableId.ROLE_MEMBER_PANEL);
        TABLES_ID.put(ServiceType.COMPLEX_TYPE, UserProfileStorage.TableId.SERVICE_MEMBER_PANEL);
        TABLES_ID.put(OrgType.COMPLEX_TYPE, UserProfileStorage.TableId.ORG_MEMBER_PANEL);
        TABLES_ID.put(ArchetypeType.COMPLEX_TYPE, UserProfileStorage.TableId.ARCHETYPE_MEMBER_PANEL);
    }

    static {
        AUTHORIZATIONS.put(RoleType.COMPLEX_TYPE, GuiAuthorizationConstants.ROLE_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(ServiceType.COMPLEX_TYPE, GuiAuthorizationConstants.SERVICE_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(OrgType.COMPLEX_TYPE, GuiAuthorizationConstants.ORG_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(ArchetypeType.COMPLEX_TYPE, GuiAuthorizationConstants.ARCHETYPE_MEMBERS_AUTHORIZATIONS);
    }

    public AbstractRoleMemberPanel(String id, IModel<R> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> form = new MidpointForm(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        initDefaultSearchParameters();
        initSearch(form);
        initMemberTable(form);
        setOutputMarkupId(true);

    }

    private void initDefaultSearchParameters() {
        GuiObjectListPanelConfigurationType additionalPanel = getAdditionalPanelConfig();
        if (additionalPanel != null && additionalPanel.getSearchBoxConfiguration() != null) {
            scopeDefaultValue = additionalPanel.getSearchBoxConfiguration().getDefaultScope();
            objectTypeDefaultValue = additionalPanel.getSearchBoxConfiguration().getDefaultObjectType();
        }
        if (scopeDefaultValue == null) {
            scopeDefaultValue = SearchBoxScopeType.ONE_LEVEL;
        }
        if (objectTypeDefaultValue == null) {
            objectTypeDefaultValue = WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType());
        }
        if (getMemberPanelStorage() != null) {
            if (getMemberPanelStorage().getOrgSearchScope() == null) {
                getMemberPanelStorage().setOrgSearchScope(scopeDefaultValue);
            }
            if (getMemberPanelStorage().getType() == null) {
                getMemberPanelStorage().setType(ObjectTypes.getObjectType(objectTypeDefaultValue.getLocalPart()));
            }
        }
    }

    protected Form<?> getForm() {
        return (Form) get(ID_FORM);
    }

    private void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        PageBase pageBase = getPageBase();
        Class type = getMemberPanelStorage() != null && getMemberPanelStorage().getType() != null ?
                getMemberPanelStorage().getType().getClassDefinition() : ObjectType.class;
        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
                ID_MEMBER_TABLE, type, getSearchOptions()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return AbstractRoleMemberPanel.this.getTableId(getComplexTypeQName());
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
                detailsPerformed(target, object);
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ObjectType>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null
                        || rowModel.getObject().getValue() == null) {
                    return false;
                }
                Class<?> objectClass = rowModel.getObject().getValue().getClass();
                return WebComponentUtil.hasDetailsPage(objectClass);
            }

            @Override
            protected DisplayType getNewObjectButtonSpecialDisplayType() {
                return getCreateMemberButtonDisplayType();
            }

            @Override
            protected DisplayType getNewObjectButtonStandardDisplayType() {
                return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                        createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
            }

            @Override
            protected List<MultiFunctinalButtonDto> loadButtonDescriptions() {
                return createAdditionalButtonsDescription();
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                AbstractRoleMemberPanel.this.createFocusMemberPerformed(target, relation);
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
                MultifunctionalButton assignButton = createAssignButton(buttonId);
                buttonsList.add(1, assignButton);
                return buttonsList;
            }

            @Override
            protected IColumn<SelectableBean<ObjectType>, String> createIconColumn() {
                return ColumnUtils.createIconColumn(pageBase);
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectType>, String>> createDefaultColumns() {
                return (List) createMembersColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected Search createSearch() {
                return getMemberPanelStorage() != null && getMemberPanelStorage().getSearch() != null ?
                        getMemberPanelStorage().getSearch() : SearchFactory.createSearch(getDefaultObjectType(), pageBase);
            }

            @Override
            protected ObjectQuery customizeContentQuery(ObjectQuery query) {

                ObjectQuery members = AbstractRoleMemberPanel.this.createContentQuery();

                List<ObjectFilter> filters = new ArrayList<>();

                if (query != null && query.getFilter() != null) {
                    filters.add(query.getFilter());
                }

                if (members != null && members.getFilter() != null) {
                    filters.add(members.getFilter());
                }

                QueryFactory queryFactory = pageBase.getPrismContext().queryFactory();
                if (filters.size() == 1) {
                    return queryFactory.createQuery(filters.iterator().next());
                } else {
                    return queryFactory.createQuery(queryFactory.createAnd(filters));
                }
            }

            @Override
            protected ObjectQuery createQuery() {
                ObjectQuery q = super.createQuery();

                ObjectQuery members = AbstractRoleMemberPanel.this.createContentQuery();

                List<ObjectFilter> filters = new ArrayList<>();

                if (q != null && q.getFilter() != null) {
                    filters.add(q.getFilter());
                }

                if (members != null && members.getFilter() != null) {
                    filters.add(members.getFilter());
                }

                QueryFactory queryFactory = pageBase.getPrismContext().queryFactory();
                if (filters.size() == 1) {
                    return queryFactory.createQuery(filters.iterator().next());
                } else {
                    return queryFactory.createQuery(queryFactory.createAnd(filters));
                }
            }

            @Override
            protected boolean isAdditionalPanel() {
                return true;
            }

            protected boolean isTypeChanged(Class<ObjectType> newTypeClass) {
                return true;
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    private List<MultiFunctinalButtonDto> createAdditionalButtonsDescription() {
        List<MultiFunctinalButtonDto> multiFunctinalButtonDtos = new ArrayList<>();
        List<AssignmentObjectRelation> loadedRelations = loadMemberRelationsList();
        if (CollectionUtils.isNotEmpty(loadedRelations)) {
            List<AssignmentObjectRelation> relations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadedRelations);
            relations.forEach(relation -> {
                MultiFunctinalButtonDto buttonDto = new MultiFunctinalButtonDto();
                DisplayType additionalButtonDisplayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation,
                        "abstractRoleMemberPanel.menu.createMember");
                buttonDto.setAdditionalButtonDisplayType(additionalButtonDisplayType);
                buttonDto.setCompositedIcon(createCompositedIcon(relation, additionalButtonDisplayType));
                buttonDto.setAssignmentObjectRelation(relation);
                multiFunctinalButtonDtos.add(buttonDto);
            });
        }
        return multiFunctinalButtonDtos;
    }

    private CompositedIcon createCompositedIcon(AssignmentObjectRelation relation, DisplayType additionalButtonDisplayType) {
        CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(getPageBase(), relation,
                additionalButtonDisplayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green"));
        if (builder == null) {
            return null;
        }
        return builder.build();
    }

    private MultifunctionalButton createAssignButton(String buttonId) {
        MultifunctionalButton assignButton = new MultifunctionalButton(buttonId, createAssignmentAdditionalButtons()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                List<QName> relations = relation != null && !CollectionUtils.isEmpty(relation.getRelations())
                        ? Collections.singletonList(relation.getRelations().get(0))
                        : getSupportedRelations().getAvailableRelationList();
                AvailableRelationDto availableRelations = new AvailableRelationDto(relations, getSupportedRelations().getDefaultRelation());
                List<QName> objectTypes = relation != null && !CollectionUtils.isEmpty(relation.getObjectTypes()) ?
                        relation.getObjectTypes() : null;
                List<ObjectReferenceType> archetypeRefList = relation != null && !CollectionUtils.isEmpty(relation.getArchetypeRefs()) ?
                        relation.getArchetypeRefs() : null;
                assignMembers(target, availableRelations, objectTypes, archetypeRefList, relation == null);
            }

            @Override
            protected DisplayType getMainButtonDisplayType() {
                return getAssignMemberButtonDisplayType();
            }

            @Override
            protected DisplayType getDefaultObjectButtonDisplayType() {
                return getAssignMemberButtonDisplayType();
            }

        };
        assignButton.add(AttributeAppender.append("class", "btn-margin-right"));

        return assignButton;
    }

    private List<MultiFunctinalButtonDto> createAssignmentAdditionalButtons() {
        List<MultiFunctinalButtonDto> additionalAssignmentButtons = new ArrayList<>();
        List<AssignmentObjectRelation> assignmentObjectRelations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadMemberRelationsList());
        if (assignmentObjectRelations == null) {
            return additionalAssignmentButtons;
        }
        assignmentObjectRelations.forEach(relation -> {
            MultiFunctinalButtonDto buttonDto = new MultiFunctinalButtonDto();
            buttonDto.setAssignmentObjectRelation(relation);

            DisplayType additionalDispayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(AbstractRoleMemberPanel.this.getPageBase(), relation,
                    "abstractRoleMemberPanel.menu.assignMember");
            //TODO null additinalDisplayType
            CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(AbstractRoleMemberPanel.this.getPageBase(), relation,
                    additionalDispayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green"));
            CompositedIcon icon = builder.build();
            buttonDto.setAdditionalButtonDisplayType(additionalDispayType);
            buttonDto.setCompositedIcon(icon);
            additionalAssignmentButtons.add(buttonDto);
        });

        return additionalAssignmentButtons;

    }

    protected UserProfileStorage.TableId getTableId(QName complextType) {
        return TABLES_ID.get(complextType);
    }

    protected Map<String, String> getAuthorizations(QName complexType) {
        return AUTHORIZATIONS.get(complexType);
    }

    protected QName getComplexTypeQName() {
        return getModelObject().asPrismObject().getComplexTypeDefinition().getTypeName();
    }

    private DisplayType getCreateMemberButtonDisplayType() {
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
    }

    private DisplayType getAssignMemberButtonDisplayType() {
        return WebComponentUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green",
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.assignMember", "", "").getString());
    }

    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createAssignMemberRowAction(menu);

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unassignMembersPerformed(target);
                        }
                    };

                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_UNASSIGN);
                }
            });
        }

        createRecomputeMemberRowAction(menu);

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.create")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            createFocusMemberPerformed(target);
                        }
                    };
                }
            });
        }
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_DELETE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteMembersPerformed(target);
                        }
                    };
                }

            });
        }
        return menu;
    }

    protected void createAssignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.assign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            MemberOperationsHelper.assignMembers(getPageBase(), AbstractRoleMemberPanel.this.getModelObject(), target, getSupportedRelations(), null);
                        }
                    };
                }
            });
        }
    }

    protected void createRecomputeMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_RECOMPUTE)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.recompute")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            recomputeMembersPerformed(target);
                        }
                    };
                }

//                    @Override
//                    public IModel<String> getConfirmationMessageModel() {
//                        return getMemberTable().getSelectedObjectsCount() > 0 ?
//                                createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel")
//                                : createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");
//                    }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
                }

            });
        }
    }

    protected abstract AvailableRelationDto getSupportedRelations();

    protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig() {
        return null;
    }

    private boolean isAuthorized(String action) {
        Map<String, String> memberAuthz = getAuthorizations(getComplexTypeQName());
        return WebComponentUtil.isAuthorized(memberAuthz.get(action));
    }

    private List<AssignmentObjectRelation> loadMemberRelationsList() {
        AssignmentCandidatesSpecification spec = loadCandidateSpecification();
        return spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
    }

    private AssignmentCandidatesSpecification loadCandidateSpecification() {
        OperationResult result = new OperationResult(OPERATION_LOAD_MEMBER_RELATIONS);
        PrismObject obj = getModelObject().asPrismObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load member relations list for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    protected void assignMembers(AjaxRequestTarget target, AvailableRelationDto availableRelationList,
            List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {
        MemberOperationsHelper.assignMembers(getPageBase(), getModelObject(), target, availableRelationList,
                objectTypes, archetypeRefList, isOrgTreePanelVisible);
    }

    private void unassignMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();

        ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
            }

            @Override
            protected boolean isFocusTypeSelectorVisible() {
                return !QueryScope.SELECTED.equals(scope);
            }

            protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                unassignMembersPerformed(type, SearchBoxScopeType.SUBTREE.equals(getSearchScope()) && QueryScope.ALL.equals(scope) ?
                        QueryScope.ALL_DIRECT : scope, relations, target);
            }

            @Override
            protected QName getDefaultObjectType() {
                return WebComponentUtil.classToQName(AbstractRoleMemberPanel.this.getPrismContext(),
                        AbstractRoleMemberPanel.this.getDefaultObjectType());
            }
        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
    }

    private void deleteMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();
        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getSearchScope())) {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllSubtreeMembersConfirmationLabel");
        } else {
            confirmModel = getMemberTable().getSelectedObjectsCount() > 0 ?
                    createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel")
                    : createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");
        }
        ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
                confirmModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
            }

            protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                deleteMembersPerformed(scope, relations, target);
            }

            @Override
            protected boolean isFocusTypeSelectorVisible() {
                return !QueryScope.SELECTED.equals(scope);
            }

            @Override
            protected QName getDefaultObjectType() {
                return WebComponentUtil.classToQName(AbstractRoleMemberPanel.this.getPrismContext(),
                        AbstractRoleMemberPanel.this.getDefaultObjectType());
            }
        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
    }

    protected void createFocusMemberPerformed(AjaxRequestTarget target) {
        createFocusMemberPerformed(target, null);
    }

    protected void createFocusMemberPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        if (relationSpec != null) {
            try {
                List<ObjectReferenceType> newReferences = new ArrayList<>();
                if (CollectionUtils.isEmpty(relationSpec.getRelations())) {
                    relationSpec.setRelations(
                            Collections.singletonList(RelationTypes.MEMBER.getRelation()));
                }
                ObjectReferenceType memberRef = ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject(), relationSpec.getRelations().get(0));
                newReferences.add(memberRef);
                if (CollectionUtils.isNotEmpty(relationSpec.getArchetypeRefs())) {
                    newReferences.add(relationSpec.getArchetypeRefs().get(0));
                }
                QName newMemberType = CollectionUtils.isNotEmpty(relationSpec.getObjectTypes()) ? relationSpec.getObjectTypes().get(0) :
                        getSupportedObjectTypes(false).get(0);
                WebComponentUtil.initNewObjectWithReference(AbstractRoleMemberPanel.this.getPageBase(), newMemberType, newReferences);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    getPageBase().getMainPopupBodyId()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getNewMemberObjectTypes();
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (type == null) {
                        getSession().warn("No type was selected. Cannot create member");
                        target.add(this);
                        target.add(getPageBase().getFeedbackPanel());
                        return;
                    }
                    if (relations == null || relations.isEmpty()) {
                        getSession().warn("No relation was selected. Cannot create member");
                        target.add(this);
                        target.add(getPageBase().getFeedbackPanel());
                        return;
                    }
                    AbstractRoleMemberPanel.this.getPageBase().hideMainPopup(target);
                    try {
                        List<ObjectReferenceType> newReferences = new ArrayList<>();
                        for (QName relation : relations) {
                            newReferences.add(ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject(), relation));
                        }
                        WebComponentUtil.initNewObjectWithReference(AbstractRoleMemberPanel.this.getPageBase(), type, newReferences);
                    } catch (SchemaException e) {
                        throw new SystemException(e.getMessage(), e);
                    }

                }

                @Override
                protected QName getDefaultObjectType() {
                    if (relationSpec != null && CollectionUtils.isNotEmpty(relationSpec.getObjectTypes())) {
                        return relationSpec.getObjectTypes().get(0);
                    }
                    return super.getDefaultObjectType();
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return relationSpec == null;
                }
            };

            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    protected void deleteMembersPerformed(QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (relations == null || relations.isEmpty()) {
            getSession().warn("No relation was selected. Cannot perform delete members");
            target.add(this);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        MemberOperationsHelper.deleteMembersPerformed(getPageBase(), scope, getActionQuery(scope, relations), target);
    }

    protected void unassignMembersPerformed(QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (relations == null || relations.isEmpty()) {
            getSession().warn("No relation was selected. Cannot perform unassign members");
            target.add(this);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        MemberOperationsHelper.unassignMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, type, target);
    }

    private ObjectViewDto<OrgType> getParameter(String panelId) {
        ChooseTypePanel<OrgType> tenantChoice = (ChooseTypePanel) get(createComponentPath(ID_FORM, panelId));
        return tenantChoice.getModelObject();
    }

    protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
        switch (scope) {
            case ALL:
                return createAllMemberQuery(relations);
            case ALL_DIRECT:
                return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType().getTypeQName(), relations, getParameter(ID_TENANT), getParameter(ID_PROJECT), getPrismContext());
            case SELECTED:
                return MemberOperationsHelper.createSelectedObjectsQuery(getMemberTable().getSelectedRealObjects(), getPrismContext());
        }

        return null;
    }

    protected void initSearch(Form<?> form) {

        List<SearchBoxScopeType> scopeValues = Arrays.asList(SearchBoxScopeType.values());
        DropDownFormGroup<SearchBoxScopeType> searchScrope = createDropDown(ID_SEARCH_SCOPE,
                Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getOrgSearchScope() : scopeDefaultValue),
                scopeValues,
                WebComponentUtil.getEnumChoiceRenderer(AbstractRoleMemberPanel.this),
                "abstractRoleMemberPanel.searchScope", "abstractRoleMemberPanel.searchScope.tooltip", true);
        searchScrope.add(new VisibleBehaviour(() -> getModelObject() instanceof OrgType));
        form.add(searchScrope);

        List<QName> supportedTypes = getSupportedObjectTypes(false);
        DropDownFormGroup<QName> typeSelect = createDropDown(ID_OBJECT_TYPE,
                Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getType().getTypeQName() : WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType())),
                supportedTypes, new QNameObjectTypeChoiceRenderer() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(QName qname) {
                        if (qname == null || getObjectTypesListParentType().equals(qname)) {
                            return StringUtils.leftPad(createStringResource("ObjectTypes.all").getString(), 1);
                        } else {
                            return super.getDisplayValue(qname);
                        }
                    }

                    @Override
                    public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
                        QName qname = super.getObject(id, choices);
                        if (qname == null) {
                            return getObjectTypesListParentType();
                        }
                        return qname;
                    }

                },
                "abstractRoleMemberPanel.type", "abstractRoleMemberPanel.type.tooltip", false);
        form.add(typeSelect);

        RelationDropDownChoicePanel relationSelector = new RelationDropDownChoicePanel(ID_SEARCH_BY_RELATION,
                getMemberPanelStorage() != null ? getMemberPanelStorage().getRelation() : getSupportedRelations().getDefaultRelation(),
                getSupportedRelations().getAvailableRelationList(), true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onValueChanged(AjaxRequestTarget target) {
                refreshAll(target);
            }

            @Override
            protected String getNullValidDisplayValue() {
                return getString("RelationTypes.ANY");
            }
        };
        form.add(relationSelector);

        ChooseTypePanel<OrgType> tenant = createParameterPanel(ID_TENANT, true);
        form.add(tenant);
        tenant.add(new VisibleBehaviour(() -> getModelObject() instanceof RoleType));

        ChooseTypePanel<OrgType> project = createParameterPanel(ID_PROJECT, false);
        form.add(project);
        project.add(new VisibleBehaviour(() -> getModelObject() instanceof RoleType));

        CheckFormGroup includeIndirectMembers = new CheckFormGroup(ID_INDIRECT_MEMBERS,
                Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getIndirect() : false),
                createStringResource("abstractRoleMemberPanel.indirectMembers"), "abstractRoleMemberPanel.indirectMembers.tooltip", false, "col-md-4", "col-md-2");
        includeIndirectMembers.getCheck().add(new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                refreshAll(target);
            }

        });

        includeIndirectMembers.add(new VisibleBehaviour(() ->
                getSearchScopeValue().equals(SearchBoxScopeType.ONE_LEVEL) || !searchScrope.isVisible()));
        includeIndirectMembers.setOutputMarkupId(true);
        form.add(includeIndirectMembers);

    }

    protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createFocusTypeList(includeAbstractTypes);
    }

    protected QName getObjectTypesListParentType() {
        return FocusType.COMPLEX_TYPE;
    }

    protected List<QName> getNewMemberObjectTypes() {
        return WebComponentUtil.createFocusTypeList();
    }

    private ChooseTypePanel<OrgType> createParameterPanel(String id, boolean isTenant) {

        ChooseTypePanel<OrgType> orgSelector = new ChooseTypePanel<OrgType>(id, Model.of(new ObjectViewDto())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void executeCustomAction(AjaxRequestTarget target, OrgType object) {
                refreshAll(target);
            }

            @Override
            protected void executeCustomRemoveAction(AjaxRequestTarget target) {
                refreshAll(target);
            }

            @Override
            protected ObjectQuery getChooseQuery() {
                S_FilterEntryOrEmpty q = getPrismContext().queryFor(OrgType.class);
                if (isTenant) {
                    return q.item(OrgType.F_TENANT).eq(true).build();
                } else {
                    return q.not().item(OrgType.F_TENANT).eq(true).build();
                }
            }

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            public Class<OrgType> getObjectTypeClass() {
                return OrgType.class;
            }

            @Override
            protected AttributeAppender getInputStyleClass() {
                return AttributeAppender.append("class", "col-md-10");
            }

        };
        orgSelector.setOutputMarkupId(true);
        orgSelector.setOutputMarkupPlaceholderTag(true);
        return orgSelector;

    }

    private <V> DropDownFormGroup<V> createDropDown(String id, IModel<V> defaultModel, final List<V> values,
            IChoiceRenderer<V> renderer, String labelKey, String tooltipKey, boolean required) {
        DropDownFormGroup<V> listSelect = new DropDownFormGroup<V>(id, defaultModel, Model.ofList(values), renderer, createStringResource(labelKey),
                tooltipKey, false, "col-md-4", "col-md-8", required) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return getString("ObjectTypes.all");
            }
        };

        listSelect.getInput().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshAll(target);
            }
        });
        listSelect.setOutputMarkupId(true);
        return listSelect;
    }

    protected void refreshAll(AjaxRequestTarget target) {
        updateMembersPanelSessionStorage();

        DropDownFormGroup<QName> typeChoice = (DropDownFormGroup) get(createComponentPath(ID_FORM, ID_OBJECT_TYPE));
        QName type = getMemberPanelStorage() != null ? getMemberPanelStorage().getType().getTypeQName() : typeChoice.getModelObject();
        getMemberTable().clearCache();
        getMemberTable().refreshTable(WebComponentUtil.qnameToClass(getPrismContext(), type, FocusType.class), target);
        target.add(this);
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    protected QueryScope getQueryScope() {
        if (CollectionUtils.isNotEmpty(MemberOperationsHelper.getFocusOidToRecompute(getMemberTable().getSelectedRealObjects()))) {
            return QueryScope.SELECTED;
        }

        if (getIndirectmembersPanel().getValue() || SearchBoxScopeType.SUBTREE.equals(getSearchScope())) {
            return QueryScope.ALL;
        }

        return QueryScope.ALL_DIRECT;
    }

    private CheckFormGroup getIndirectmembersPanel() {
        return (CheckFormGroup) get(createComponentPath(ID_FORM, ID_INDIRECT_MEMBERS));
    }

    protected void recomputeMembersPerformed(AjaxRequestTarget target) {

        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getSearchScope())) {
            confirmModel = createStringResource("abstractRoleMemberPanel.recomputeAllSubtreeMembersConfirmationLabel");
        } else {
            confirmModel = getMemberTable().getSelectedObjectsCount() > 0 ?
                    createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel")
                    : createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");
        }
        ConfigureTaskConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                confirmModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected PrismObject<TaskType> getTask(AjaxRequestTarget target) {
                Task task = MemberOperationsHelper.createRecomputeMembersTask(getPageBase(), getQueryScope(),
                        getActionQuery(getQueryScope(), getSupportedRelations().getAvailableRelationList()), target);
                if (task == null) {
                    return null;
                }
                PrismObject<TaskType> recomputeTask = task.getClonedTaskObject();
                TaskType recomputeTaskType = recomputeTask.asObjectable();
                recomputeTaskType.getAssignment().add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(), ObjectTypes.ARCHETYPE, getPrismContext()));
                return recomputeTask;
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                MemberOperationsHelper.recomputeMembersPerformed(getPageBase(), getQueryScope(),
                        getActionQuery(getQueryScope(), getSupportedRelations().getAvailableRelationList()), target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    protected ObjectQuery createContentQuery() {
        CheckFormGroup isIndirect = getIndirectmembersPanel();
        List<QName> relations = QNameUtil.match(getSelectedRelation(), PrismConstants.Q_ANY)
                ? getSupportedRelations().getAvailableRelationList()
                : Collections.singletonList(getSelectedRelation());
        return createMemberQuery(isIndirect != null ? isIndirect.getValue() : false, relations);

    }

    protected QName getSelectedRelation() {
        MemberPanelStorage storage = getMemberPanelStorage();
        if (storage != null) {
            return storage.getRelation();
        }
        RelationDropDownChoicePanel relationDropDown = (RelationDropDownChoicePanel) get(createComponentPath(ID_FORM, ID_SEARCH_BY_RELATION));
        return relationDropDown.getRelationValue();
    }

    private SearchBoxScopeType getSearchScopeValue() {
        if (getMemberPanelStorage() != null) {
            return getMemberPanelStorage().getOrgSearchScope();
        }
        DropDownFormGroup<SearchBoxScopeType> searchScopeComponent = (DropDownFormGroup<SearchBoxScopeType>) get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
        return searchScopeComponent.getModelObject();
    }

    protected ObjectTypes getSearchType() {
        DropDownFormGroup<QName> searchByTypeChoice =
                (DropDownFormGroup<QName>) get(createComponentPath(ID_FORM, ID_OBJECT_TYPE));
        QName typeName = searchByTypeChoice.getModelObject();
        return ObjectTypes.getObjectTypeFromTypeQName(typeName);
    }

    protected ObjectQuery createMemberQuery(boolean indirect, Collection<QName> relations) {
        if (indirect) {
            return createAllMemberQuery(relations);
        }

        return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType().getTypeQName(), relations, getParameter(ID_TENANT), getParameter(ID_PROJECT), getPrismContext());
    }

    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    protected ObjectReferenceType createReference() {
        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject(), getPageBase().getPrismContext());
        return ref;
    }

    protected void detailsPerformed(AjaxRequestTarget target, ObjectType object) {
        if (WebComponentUtil.hasDetailsPage(object.getClass())) {
            WebComponentUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
        } else {
            error("Could not find proper response page");
            throw new RestartResponseException(getPageBase());
        }
    }

    protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("TreeTablePanel.fullName.displayName")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                SelectableBean<ObjectType> bean = rowModel.getObject();
                ObjectType object = bean.getValue();
                cellItem.add(new Label(componentId, getMemberObjectDisplayName(object, true)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getMemberObjectDisplayName(rowModel.getObject().getValue(), true));
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("TreeTablePanel.identifier.description")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                SelectableBean<ObjectType> bean = rowModel.getObject();
                ObjectType object = bean.getValue();
                cellItem.add(new Label(componentId, getMemberObjectIdentifier(object)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getMemberObjectIdentifier(rowModel.getObject().getValue()));
            }

        };
        columns.add(column);
//        if (isRelationColumnVisible()){
        columns.add(createRelationColumn());
//        }
        return columns;
    }

    protected IColumn<SelectableBean<ObjectType>, String> createRelationColumn() {
        return new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("roleMemberPanel.relation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                cellItem.add(new Label(componentId,
                        getRelationValue(rowModel.getObject().getValue())));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getRelationValue(rowModel.getObject().getValue()));
            }

        };
    }

    protected boolean isRelationColumnVisible() {
        return false;
    }

    private String getMemberObjectDisplayName(ObjectType object, boolean translate) {
        if (object == null) {
            return "";
        }
        if (object instanceof UserType) {
            return WebComponentUtil.getTranslatedPolyString(((UserType) object).getFullName());
        } else if (object instanceof AbstractRoleType) {
            return WebComponentUtil.getTranslatedPolyString(((AbstractRoleType) object).getDisplayName());
        } else {
            return "";
        }
    }

    private String getMemberObjectIdentifier(ObjectType object) {
        if (object == null) {
            return "";
        }
        if (object instanceof UserType) {
            return ((UserType) object).getEmailAddress();
        } else if (object instanceof AbstractRoleType) {
            return ((AbstractRoleType) object).getIdentifier();
        } else {
            return object.getDescription();
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return SelectorOptions
                .createCollection(GetOperationOptions.createDistinct());
    }

    protected <O extends ObjectType> Class<O> getDefaultObjectType() {
        return (Class<O>) FocusType.class;
    }

    protected Form getFormComponent() {
        return (Form) get(ID_FORM);
    }

    private String getRelationValue(ObjectType focusObject) {
        String relation = "";
        if (FocusType.class.isAssignableFrom(focusObject.getClass())) {
            // Do NOT take relation from an assignment. Use roleMembershipRef instead. Reasons:
            // 1. Authorizations (MID-4893). User may be authorized just for roleMemberhsipRef and not for assignment
            //    Authorization for roleMembershipRef is enough to display member panel.
            // 2. There may be assignments that are not valid. We do not want to display relation for those.
            for (ObjectReferenceType roleMembershipRef : getMembershipReferenceList((FocusType) focusObject)) {
                relation = buildRelation(roleMembershipRef, relation);
            }

        }
        return relation;

    }

    protected List<ObjectReferenceType> getMembershipReferenceList(FocusType focusObject) {
        return focusObject.getRoleMembershipRef();
    }

    private String buildRelation(ObjectReferenceType roleMembershipRef, String relation) {
        if (roleMembershipRef.getOid().equals(getModelObject().getOid())) {
            QName assignmentRelation = roleMembershipRef.getRelation();
            if (getSupportedRelations().getAvailableRelationList().stream().anyMatch(r -> QNameUtil.match(r, assignmentRelation))) {
                if (!StringUtils.isBlank(relation)) {
                    relation += ",";
                }
                String relationDisplayName = WebComponentUtil.getRelationHeaderLabelKeyIfKnown(assignmentRelation);
                relation += StringUtils.isNotEmpty(relationDisplayName) ?
                        getPageBase().createStringResource(relationDisplayName).getString() :
                        getPageBase().createStringResource(assignmentRelation.getLocalPart()).getString();
            }
        }
        return relation;
    }

    protected void updateMembersPanelSessionStorage() {
        MemberPanelStorage storage = getMemberPanelStorage();
        if (storage != null) {
            storage.setType(getSearchType());

            RelationDropDownChoicePanel relationDropDown = (RelationDropDownChoicePanel) get(createComponentPath(ID_FORM, ID_SEARCH_BY_RELATION));
            storage.setRelation(relationDropDown.getRelationValue());

            CheckFormGroup indirectPanel = getIndirectmembersPanel();
            if (indirectPanel != null) {
                storage.setIndirect(indirectPanel.getValue());
            }

            DropDownFormGroup<SearchBoxScopeType> searchScopeComponent =
                    (DropDownFormGroup<SearchBoxScopeType>) get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
            storage.setOrgSearchScope(searchScopeComponent.getModelObject());
        }
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        return null;
    }

    protected SearchBoxScopeType getSearchScope() {
        DropDownFormGroup<SearchBoxScopeType> searchOrgScope =
                (DropDownFormGroup<SearchBoxScopeType>) get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
        return searchOrgScope.getModelObject();
    }
}
