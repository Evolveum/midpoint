/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.dialog.ConfigureTaskConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO: should be really reviewed
public abstract class AbstractRoleMemberPanel<R extends AbstractRoleType> extends BasePanel<R> {

    private static final long serialVersionUID = 1L;

    protected enum QueryScope {
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
    private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_MEMBER_TABLE = "memberTable";

//    private SearchBoxConfigurationHelper abstractRoleMemberSearchConfiguration;
    private GuiObjectListPanelConfigurationType additionalPanelConfig;

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

    public AbstractRoleMemberPanel(String id, IModel<R> model, PageBase parentPage) {
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
        this.additionalPanelConfig = getAdditionalPanelConfig();
//        this.abstractRoleMemberSearchConfiguration = new SearchBoxConfigurationHelper(additionalPanelConfig, getSupportedRelations());
        initMemberTable(form);
        setOutputMarkupId(true);
    }

    protected Form<?> getForm() {
        return (Form) get(ID_FORM);
    }

    private void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<>(
                ID_MEMBER_TABLE, getDefaultObjectTypeClass(), getSearchOptions()) {

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
            protected List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
                MultifunctionalButton assignButton = createAssignButton(buttonId);
                buttonsList.add(1, assignButton);
                return buttonsList;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected String getStorageKey() {
                return AbstractRoleMemberPanel.this.createStorageKey();
            }

//            @Override
//            protected ContainerTypeSearchItem getTypeItem(Class<? extends ObjectType> type, List<DisplayableValue<Class<? extends ObjectType>>> allowedValues) {
//
//            }

            protected PageStorage getPageStorage(String storageKey){
                return getSession().getSessionStorage().getPageStorageMap().get(storageKey);
            }

            @Override
            protected Search createSearch(Class<ObjectType> type) {
                return createMemberSearch(type, getTypeItem(type, getAllowedTypes()));
            }

            @Override
            protected ISelectableDataProvider createProvider() {
                SelectableBeanObjectDataProvider provider = createSelectableBeanObjectDataProvider(() -> getCustomizedQuery(getSearchModel().getObject()), null);
                provider.setIsMemberPanel(true);
                provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleMemberPanel.this.getModelObject());
                return provider;
            }

            @Override
            public void refreshTable(AjaxRequestTarget target) {
                if (getSearchModel().isLoaded() && getSearchModel().getObject()!= null
                        && getSearchModel().getObject().isTypeChanged()) {
                    clearCache();
                }
                super.refreshTable(target);
            }

            @Override
            protected MultifunctionalButton createCreateNewObjectButton(String buttonId) {
                return AbstractRoleMemberPanel.this.createCreateNewObjectButton(buttonId);
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    private <O extends ObjectType> Class<O> getDefaultObjectTypeClass() {
        QName objectTypeQname = getMemberPanelStorage().getDefaultObjectType();
        return (Class<O>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), objectTypeQname);
    }

    private Search createMemberSearch(Class<ObjectType> type, ContainerTypeSearchItem typeItem) {
        Search search = null;

        MemberPanelStorage memberPanelStorage = getMemberPanelStorage();
        if (memberPanelStorage == null) { //normally, this should not happen
            ContainerTypeSearchItem<ObjectType> defaultTypeItem = new ContainerTypeSearchItem<ObjectType>(type);
            search = SearchFactory.createSearch(defaultTypeItem, null, null, null, getPageBase(), null, true, true, Search.PanelType.MEMBER_PANEL);
            return search;
        }

//        ContainerTypeSearchItem oldTypeItem;
//        if (search != null) {
//            if (!search.isTypeChanged()) {
//                return search;
//            }
//            oldTypeItem = search.getType();
//        } else {
//            oldTypeItem = typeItem;
//        }

        if (memberPanelStorage.getSearch() != null) {
            return memberPanelStorage.getSearch();
        }

        ContainerTypeSearchItem searchTypeItem = new ContainerTypeSearchItem(WebComponentUtil.qnameToClass(getPrismContext(), memberPanelStorage.getDefaultObjectType()));
        searchTypeItem.setConfiguration(memberPanelStorage.getObjectTypeSearchItem());
        searchTypeItem.setVisible(true);

        search = SearchFactory.createSearch(searchTypeItem, null, null, null, getPageBase(), null, true, true, Search.PanelType.MEMBER_PANEL);

        AbstractRoleCompositedSearchItem compositedSearchItem = new AbstractRoleCompositedSearchItem(search, memberPanelStorage) {

            @Override
            protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
                return getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                        .findReferenceDefinition(refName);
            }

            @Override
            protected R getAbstractRoleObject() {
                return AbstractRoleMemberPanel.this.getModelObject();
            }

        };
        search.addCompositedSpecialItem(compositedSearchItem);

        if (additionalPanelConfig != null && additionalPanelConfig.getSearchBoxConfiguration() != null){
            search.setCanConfigure(!Boolean.FALSE.equals(additionalPanelConfig.getSearchBoxConfiguration().isAllowToConfigureSearchItems()));
        }
        return search;
    }

    private MultifunctionalButton createCreateNewObjectButton(String buttonId) {
        MultifunctionalButton createNewObjectButton = new MultifunctionalButton(buttonId, createAdditionalButtonsDescription()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                AbstractRoleMemberPanel.this.createFocusMemberPerformed(target, relation);
            }

        };
//        createNewObjectButton.add(new VisibleBehaviour(this::isCreateNewObjectEnabled));
        createNewObjectButton.add(AttributeAppender.append("class", "btn-margin-right"));
        return createNewObjectButton;
    }

    private ObjectQuery getCustomizedQuery(Search<ObjectType> search) {
        MemberPanelStorage memberPanelStorage = getMemberPanelStorage();
        if (!memberPanelStorage.isRelationVisible()
                && !memberPanelStorage.isIndirectVisible()
                && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof OrgType) || !memberPanelStorage.isSearchScopeVisible())
                && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof RoleType) || !memberPanelStorage.isTenantVisible())
                && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof RoleType) || !memberPanelStorage.isProjectVisible())) {
            PrismContext prismContext = getPageBase().getPrismContext();
            List relations = new ArrayList();
            if (QNameUtil.match(PrismConstants.Q_ANY, memberPanelStorage.getDefaultRelation())) {
                relations.addAll(memberPanelStorage.getSupportedRelations());
            } else {
                relations.add(memberPanelStorage.getDefaultRelation());
            }

            R object = AbstractRoleMemberPanel.this.getModelObject();
            Class type = search.getTypeClass();
            return prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                    .endBlock().build();
        }
        return null;
    }

    private String createStorageKey() {
        UserProfileStorage.TableId tableId = getTableId(getComplexTypeQName());
//        GuiObjectListPanelConfigurationType view = getAdditionalPanelConfig();
        String collectionName = additionalPanelConfig != null ? ("_" + additionalPanelConfig.getIdentifier()) : "";
        return tableId.name() + "_" + getStorageKeyTabSuffix() + collectionName;
    }




    private R getParentVariables(VariablesMap variables) {
        if (variables == null) {
            return null;
        }
        try {
            return (R) variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }


    protected Class<? extends ObjectType> getChoiceForAllTypes () {
        return FocusType.class;
    }

    private List<DisplayableValue<Class<? extends ObjectType>>> getAllowedTypes() {
        List<DisplayableValue<Class<? extends ObjectType>>> ret = new ArrayList<>();
        List<QName> types = getMemberPanelStorage().getSupportedObjectTypes();

        ret.add(new SearchValue<>(getChoiceForAllTypes(), "ObjectTypes.all"));

        String prefix = "ObjectType.";
        for (QName type : types) {
            @NotNull ObjectTypes objectType = ObjectTypes.getObjectType(type.getLocalPart());
            ret.add(new SearchValue<>(objectType.getClassDefinition(), prefix + objectType.getTypeQName().getLocalPart()));
        }
        return ret;
    }

    private LoadableModel<MultiFunctinalButtonDto> createAdditionalButtonsDescription() {

        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                MultiFunctinalButtonDto multiFunctinalButtonDto = new MultiFunctinalButtonDto();

                CompositedIconButtonDto mainButton = new CompositedIconButtonDto();
                DisplayType mainButtonDisplayType = getCreateMemberButtonDisplayType();
//                DisplayType mainButtonDisplayType = WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
//                        createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
                mainButton.setAdditionalButtonDisplayType(mainButtonDisplayType);

                CompositedIconBuilder builder = new CompositedIconBuilder();
                Map<IconCssStyle, IconType> layerIcons = WebComponentUtil.createMainButtonLayerIcon(mainButtonDisplayType);
                for (Map.Entry<IconCssStyle, IconType> icon : layerIcons.entrySet()) {
                    builder.appendLayerIcon(icon.getValue(), icon.getKey());
                }
                mainButton.setCompositedIcon(builder.build());
                multiFunctinalButtonDto.setMainButton(mainButton);

                List<AssignmentObjectRelation> loadedRelations = loadMemberRelationsList();
                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(loadedRelations)) {
                    List<AssignmentObjectRelation> relations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadedRelations);
                    relations.forEach(relation -> {
                        CompositedIconButtonDto buttonDto = new CompositedIconButtonDto();
                        DisplayType additionalButtonDisplayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation,
                                "abstractRoleMemberPanel.menu.createMember");
                        buttonDto.setAdditionalButtonDisplayType(additionalButtonDisplayType);
                        buttonDto.setCompositedIcon(createCompositedIcon(relation, additionalButtonDisplayType));
                        buttonDto.setAssignmentObjectRelation(relation);
                        additionalButtons.add(buttonDto);
                    });
                }
                multiFunctinalButtonDto.setAdditionalButtons(additionalButtons);
                return multiFunctinalButtonDto;
            }
        };
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
                        : getMemberPanelStorage().getSupportedRelations();
                List<QName> availableRelations = getMemberPanelStorage().getSupportedRelations();//new AvailableRelationDto(relations, getSupportedRelations().getDefaultRelation(), abstractRoleMemberSearchConfiguration.getDefaultRelationConfiguration());
                List<QName> objectTypes = relation != null && !CollectionUtils.isEmpty(relation.getObjectTypes()) ?
                        relation.getObjectTypes() : null;
                List<ObjectReferenceType> archetypeRefList = relation != null && !CollectionUtils.isEmpty(relation.getArchetypeRefs()) ?
                        relation.getArchetypeRefs() : null;
                assignMembers(target, getMemberPanelStorage().getRelationSearchItem(), objectTypes, archetypeRefList, relation == null);
            }



        };
        assignButton.add(AttributeAppender.append("class", "btn-margin-right"));

        return assignButton;
    }

    private LoadableModel<MultiFunctinalButtonDto> createAssignmentAdditionalButtons() {

        return new LoadableModel<MultiFunctinalButtonDto>() {
            @Override
            protected MultiFunctinalButtonDto load() {
                MultiFunctinalButtonDto multiFunctinalButtonDto = new MultiFunctinalButtonDto();
                CompositedIconButtonDto mainButton = new CompositedIconButtonDto();
                DisplayType mainButtonDisplayType = getAssignMemberButtonDisplayType();
                mainButton.setAdditionalButtonDisplayType(mainButtonDisplayType);
                CompositedIconBuilder mainButtonIconBuilder = new CompositedIconBuilder();
                mainButtonIconBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(mainButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                        .appendColorHtmlValue(WebComponentUtil.getIconColor(mainButtonDisplayType));
                mainButton.setCompositedIcon(mainButtonIconBuilder.build());
                multiFunctinalButtonDto.setMainButton(mainButton);

                List<CompositedIconButtonDto> additionalAssignmentButtons = new ArrayList<>();
                List<AssignmentObjectRelation> assignmentObjectRelations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadMemberRelationsList());
                if (assignmentObjectRelations == null) {
                    return multiFunctinalButtonDto;
                }
                assignmentObjectRelations.forEach(relation -> {
                    CompositedIconButtonDto buttonDto = new CompositedIconButtonDto();
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

                return multiFunctinalButtonDto;
            }
        };

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
                            MemberOperationsHelper.assignMembers(getPageBase(), AbstractRoleMemberPanel.this.getModelObject(), target, getMemberPanelStorage().getRelationSearchItem(), null);
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

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
                }

            });
        }
    }

    protected abstract List<QName> getSupportedRelations();

    protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig() {
        CompiledObjectCollectionView view = WebComponentUtil.getCollectionViewByObject(getModelObject(), getPageBase());
        if (view != null && view.getAdditionalPanels() != null) {
            return view.getAdditionalPanels().getMemberPanel();
        }
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

    protected void assignMembers(AjaxRequestTarget target, RelationSearchItemConfigurationType relationConfig,
            List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {
        MemberOperationsHelper.assignMembers(getPageBase(), getModelObject(), target, relationConfig,
                objectTypes, archetypeRefList, isOrgTreePanelVisible);
    }

    private void unassignMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();

        ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getMemberPanelStorage().getSupportedObjectTypes();//getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getMemberPanelStorage().getSupportedRelations();
            }

            @Override
            protected List<QName> getDefaultRelations() {
                List<QName> defaultRelations = new ArrayList<>();
                QName defaultRelation = AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation();
                if (defaultRelation != null) {
                    defaultRelations.add(AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation());
                } else {
                    defaultRelations.add(RelationTypes.MEMBER.getRelation());
                }
                return defaultRelations;

            }

            @Override
            protected boolean isFocusTypeSelectorVisible() {
                return !QueryScope.SELECTED.equals(scope);
            }

            protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                unassignMembersPerformed(type, SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && QueryScope.ALL.equals(scope) ? QueryScope.ALL_DIRECT : scope, relations, target);
            }

            @Override
            protected QName getDefaultObjectType() {
                if (QueryScope.SELECTED.equals(scope)) {
                    return FocusType.COMPLEX_TYPE;
                }
                return WebComponentUtil.classToQName(AbstractRoleMemberPanel.this.getPrismContext(),
                        AbstractRoleMemberPanel.this.getDefaultObjectType());
            }

            @Override
            protected IModel<String> getWarningMessageModel() {
                if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
                    return getPageBase().createStringResource("abstractRoleMemberPanel.unassign.warning.subtree");
                }
                return null;
            }

            @Override
            public int getHeight() {
                if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
                    return 325;
                }
                return 230;
            }
        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
    }

    private void deleteMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();
        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
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
                return AbstractRoleMemberPanel.this.getMemberPanelStorage().getSupportedObjectTypes();//getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getMemberPanelStorage().getSupportedRelations();
            }

            @Override
            protected List<QName> getDefaultRelations() {
                List<QName> defaultRelations = new ArrayList<>();
                QName defaultRelation = AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation();
                if (defaultRelation != null) {
                    defaultRelations.add(AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation());
                } else {
                    defaultRelations.add(RelationTypes.MEMBER.getRelation());
                }
                return defaultRelations;

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
                        getMemberPanelStorage().getSupportedObjectTypes().get(0); //getSupportedObjectTypes(false).get(0);
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
                    return AbstractRoleMemberPanel.this.getMemberPanelStorage().getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    List<QName> defaultRelations = new ArrayList<>();
                    QName defaultRelation = AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation();
                    if (defaultRelation != null) {
                        defaultRelations.add(AbstractRoleMemberPanel.this.getMemberPanelStorage().getDefaultRelation());
                    } else {
                        defaultRelations.add(RelationTypes.MEMBER.getRelation());
                    }
                    return defaultRelations;

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
                    return true;
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

    protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
        switch (scope) {
            case ALL:
                return createAllMemberQuery(relations);
            case ALL_DIRECT:
                return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType(), relations,
                        getMemberPanelStorage().getTenant(), getMemberPanelStorage().getProject(), getPrismContext());
            case SELECTED:
                return MemberOperationsHelper.createSelectedObjectsQuery(getMemberTable().getSelectedRealObjects(), getPrismContext());
        }

        return null;
    }

//    protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
//        if (!CollectionUtils.isEmpty(abstractRoleMemberSearchConfiguration.getDefaultObjectTypeConfiguration().getSupportedTypes())) {
//            return abstractRoleMemberSearchConfiguration.getDefaultObjectTypeConfiguration().getSupportedTypes();
//        }
//        return getDefaultSupportedObjectTypes(includeAbstractTypes);
//    }


    protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createFocusTypeList(includeAbstractTypes);
    }

    protected List<QName> getNewMemberObjectTypes() {
        return WebComponentUtil.createFocusTypeList();
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    protected QueryScope getQueryScope() {
        if (CollectionUtils.isNotEmpty(MemberOperationsHelper.getFocusOidToRecompute(getMemberTable().getSelectedRealObjects()))) {
            return QueryScope.SELECTED;
        }

        if (getMemberPanelStorage().getIndirect()
                || SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
            return QueryScope.ALL;
        }

        return QueryScope.ALL_DIRECT;
    }

    protected void recomputeMembersPerformed(AjaxRequestTarget target) {

        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
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
                        getActionQuery(getQueryScope(), getRelationsForRecomputeTask()), target);
                if (task == null) {
                    return null;
                }
                PrismObject<TaskType> recomputeTask = task.getRawTaskObjectClone();
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
                        getActionQuery(getQueryScope(), getRelationsForRecomputeTask()), target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    protected List<QName> getRelationsForRecomputeTask() {
        return getMemberPanelStorage().getSupportedRelations();
    }

    protected QName getSearchType(){
        return ObjectTypes.getObjectType(getMemberPanelStorage().getSearch().getTypeClass()).getTypeQName();
    }

    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    protected void detailsPerformed(AjaxRequestTarget target, ObjectType object) {
        if (WebComponentUtil.hasDetailsPage(object.getClass())) {
            WebComponentUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
        } else {
            error("Could not find proper response page");
            throw new RestartResponseException(getPageBase());
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return SelectorOptions
                .createCollection(GetOperationOptions.createDistinct());
    }

    protected Class<? extends ObjectType> getDefaultObjectType() {
        return FocusType.class;
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        String storageKey = createStorageKey();
        if (StringUtils.isEmpty(storageKey)) {
            return null;
        }
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            SearchBoxConfigurationHelper searchBoxCofig = new SearchBoxConfigurationHelper(additionalPanelConfig);
            searchBoxCofig.setDefaultSupportedRelations(getSupportedRelations());
            searchBoxCofig.setDefaultSupportedObjectTypes(getDefaultSupportedObjectTypes(false));
            searchBoxCofig.setDefaultObjectType(WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType()));
            storage = getSession().getSessionStorage().initMemberStorage(storageKey, searchBoxCofig);
        }
        return (MemberPanelStorage) storage;
    }


    protected String getStorageKeyTabSuffix(){
        return "";
    }

//    protected RelationSearchItemConfigurationType getDefaultRelationConfiguration() {
//        return abstractRoleMemberSearchConfiguration.getDefaultRelationConfiguration();
//    }
}
