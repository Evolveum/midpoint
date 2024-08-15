/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.mark;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.util.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;

@PanelType(name = "markedShadowList")
@PanelDisplay(label = "Members", order = 60)
public class ShadowMarkPanel extends AbstractObjectMainPanel<MarkType, ObjectDetailsModels<MarkType>> {

    private static final long serialVersionUID = 1L;

    public enum QueryScope { // temporarily public because of migration
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(ShadowMarkPanel.class);
    private static final String DOT_CLASS = ShadowMarkPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    private static final String OPERATION_UNASSIGN_OBJECTS = DOT_CLASS + "unassignObjects";
    private static final String OPERATION_UNASSIGN_OBJECT = DOT_CLASS + "unassignObject";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_RECOMPUTE_OBJECT = DOT_CLASS + "recomputeObject";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_MEMBER_TABLE = "memberTable";
    private static final Map<QName, Map<String, String>> AUTHORIZATIONS = new HashMap<>();
    private static final Map<QName, UserProfileStorage.TableId> TABLES_ID = new HashMap<>();

    private static final int DEFAULT_REFRESH_INTERVAL = 10;

    private boolean isRefreshEnabled = true;

    static {
        TABLES_ID.put(MarkType.COMPLEX_TYPE, UserProfileStorage.TableId.MARK_MARKED_SHADOWS_PANEL);
   }


    public ShadowMarkPanel(String id, ObjectDetailsModels<MarkType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        initMemberTable(form);
        setOutputMarkupId(true);
    }

    protected Form<?> getForm() {
        return (Form<?>) get(ID_FORM);
    }

    private <AH extends ObjectType> Class<AH> getDefaultObjectTypeClass() {
        return (Class<AH>) ShadowType.class;
    }

    protected <AH extends ObjectType> void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<AH> childrenListPanel = new MainObjectListPanel<>(
                ID_MEMBER_TABLE, getDefaultObjectTypeClass(), getPanelConfiguration()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return ShadowMarkPanel.this.getTableId(getComplexTypeQName());
            }

            @Override
            protected List<IColumn<SelectableBean<AH>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<AH>, String>> columns = super.createDefaultColumns();
                //columns.add(createRelationColumn());
                return columns;
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<AH>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null
                        || rowModel.getObject().getValue() == null) {
                    return false;
                }
                Class<?> objectClass = rowModel.getObject().getValue().getClass();
                return DetailsPageUtil.hasDetailsPage(objectClass);
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return ShadowMarkPanel.this.createToolbarButtonList(buttonId, super.createToolbarButtonsList(buttonId));
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected String getStorageKey() {
                return ShadowMarkPanel.this.createStorageKey();
            }

            protected PageStorage getPageStorage(String storageKey) {
                return getMemberPanelStorage();
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                return getDefaultMemberSearchBoxConfig();
            }

            @Override
            protected SelectableBeanObjectDataProvider<AH> createProvider() {
                var options = getPageBase().getOperationOptionsBuilder()
                        .distinct()
                        .raw()
                        .resolveNames()
                        .build();

                SelectableBeanObjectDataProvider<AH> provider = createSelectableBeanObjectDataProvider(
                        () -> getCustomizedQuery(getSearchModel().getObject()),
                        null,
                        options);
                provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, ObjectTypeUtil.createObjectRef(ShadowMarkPanel.this.getModelObject()));

                return provider;
            }


            @Override
            public void refreshTable(AjaxRequestTarget target) {
                if (reloadPageOnRefresh()) {
                    throw new RestartResponseException(getPage().getClass());
                } else {
                    super.refreshTable(target);
                }
            }

            @Override
            protected boolean showNewObjectCreationPopup() {
                return false;
            }

            @Override
            protected List<ObjectReferenceType> getNewObjectReferencesList(CompiledObjectCollectionView collectionView, AssignmentObjectRelation relation) {
                List<ObjectReferenceType> refList = super.getNewObjectReferencesList(collectionView, relation);
                if (refList == null) {
                    refList = new ArrayList<>();
                }
                if (relation != null && CollectionUtils.isNotEmpty(relation.getArchetypeRefs())) {
                    refList.addAll(relation.getArchetypeRefs());
                }
                ObjectReferenceType membershipRef = new ObjectReferenceType();
                membershipRef.setOid(ShadowMarkPanel.this.getModelObject().getOid());
                membershipRef.setType(ShadowMarkPanel.this.getModelObject().asPrismObject().getComplexTypeDefinition().getTypeName());
                membershipRef.setRelation(relation != null && CollectionUtils.isNotEmpty(relation.getRelations()) ?
                        relation.getRelations().get(0) : null);
                refList.add(membershipRef);
                return refList;
            }

            @Override
            protected LoadableModel<MultiFunctinalButtonDto> loadButtonDescriptions() {
                return loadMultiFunctionalButtonModel(true);
            }

            @Override
            public ContainerPanelConfigurationType getPanelConfiguration() {
                return ShadowMarkPanel.this.getPanelConfiguration();
            }

            @Override
            protected String getTitleForNewObjectButton() {
                return createStringResource("TreeTablePanel.menu.createMember").getString();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected boolean isImportObjectButtonVisible() {
                return false;
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    protected <AH extends AssignmentHolderType> SearchContext getDefaultMemberSearchBoxConfig() {
        SearchContext ctx = new SearchContext();
        ctx.setPanelType(getPanelType());
        return ctx;
    }

    protected CollectionPanelType getPanelType() {
        String panelId = getPanelConfiguration().getIdentifier();
        return CollectionPanelType.getPanelType(panelId);
    }

    protected List<Component> createToolbarButtonList(String buttonId, List<Component> defaultToolbarList) {
        return defaultToolbarList;
    }

    protected boolean reloadPageOnRefresh() {
        return false;
    }

    protected <AH extends AssignmentHolderType> ObjectQuery getCustomizedQuery(Search search) {
        PrismContext prismContext = getPageBase().getPrismContext();
        return getPageBase().getPrismContext().queryFor((Class<? extends Containerable>) search.getTypeClass())
                .item(ObjectType.F_EFFECTIVE_MARK_REF)
                .ref(getModelObject().getOid())
                .build();
    }

    private String createStorageKey() {
        UserProfileStorage.TableId tableId = getTableId(getComplexTypeQName());
        String collectionName = getPanelConfiguration() != null ? ("_" + getPanelConfiguration().getIdentifier()) : "";
        return tableId.name() + "_" + getStorageKeyTabSuffix() + collectionName;
    }

    protected LoadableModel<MultiFunctinalButtonDto> loadMultiFunctionalButtonModel(boolean useDefaultObjectRelations) {

        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                MultiFunctinalButtonDto multiFunctinalButtonDto = new MultiFunctinalButtonDto();

                DisplayType mainButtonDisplayType = getCreateMemberButtonDisplayType();
                CompositedIconBuilder builder = new CompositedIconBuilder();
                Map<IconCssStyle, IconType> layerIcons = WebComponentUtil.createMainButtonLayerIcon(mainButtonDisplayType);
                for (Map.Entry<IconCssStyle, IconType> icon : layerIcons.entrySet()) {
                    builder.appendLayerIcon(icon.getValue(), icon.getKey());
                }
                CompositedIconButtonDto mainButton = createCompositedIconButtonDto(mainButtonDisplayType, null, builder.build());
                multiFunctinalButtonDto.setMainButton(mainButton);

                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();
                multiFunctinalButtonDto.setAdditionalButtons(additionalButtons);
                return multiFunctinalButtonDto;
            }
        };
    }


    protected void showMessageWithoutLinkForTask(Task task, AjaxRequestTarget target) {
        getSession().getFeedbackMessages().clear(message -> message.getMessage() instanceof OpResult
                && OperationResultStatus.IN_PROGRESS.equals(((OpResult) message.getMessage()).getStatus()));

        if (!task.getResult().isInProgress()) {
            getPageBase().showResult(task.getResult());
        } else {
            getPageBase().info(createStringResource(
                    "ShadowMarkPanel.message.info.created.task",
                    task.getResult().getOperation())
                    .getString());
//            OperationResult showedResult = new OperationResult(task.getResult().getOperation());
//            showedResult.setStatus(task.getResult().getStatus());
//            getPageBase().showResult(showedResult);
        }

        refreshTable(target);
        target.add(getFeedback());
    }

    protected AjaxIconButton createUnassignButton(String buttonId) {
        AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_UNASSIGN),
                createStringResource("TreeTablePanel.menu.removeMembers")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // remove this mark
            }
        };
        assignButton.add(new VisibleBehaviour(() -> isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)));
        assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return assignButton;
    }

    private CompositedIconButtonDto createCompositedIconButtonDto(DisplayType buttonDisplayType, AssignmentObjectRelation relation, CompositedIcon icon) {
        CompositedIconButtonDto compositedIconButtonDto = new CompositedIconButtonDto();
        compositedIconButtonDto.setAdditionalButtonDisplayType(buttonDisplayType);
        if (icon != null) {
            compositedIconButtonDto.setCompositedIcon(icon);
        } else {
            CompositedIconBuilder mainButtonIconBuilder = new CompositedIconBuilder();
            mainButtonIconBuilder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(buttonDisplayType), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(buttonDisplayType));
            compositedIconButtonDto.setCompositedIcon(mainButtonIconBuilder.build());
        }
        compositedIconButtonDto.setAssignmentObjectRelation(relation);
        return compositedIconButtonDto;
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
        return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                ShadowMarkPanel.this.createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
    }

    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createUnassignMemberRowAction(menu);
        createDeleteMemberRowAction(menu);
        return menu;
    }

    private void createUnassignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            InlineMenuItem menuItem = new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unassignMembersPerformed(getRowModel(), target);
                        }


                    };

                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_UNASSIGN);
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader ? true : containsDirectApplyStatement(rowModel, isHeader));
            menu.add(menuItem);
        }
    }

    private boolean containsDirectApplyStatement(IModel<?> rowModel, boolean isHeader) {
        ObjectType obj = getObjectTypeFromRow(rowModel);

        if (obj == null) {
            return isHeader;
        }
        MarkType role = getModelObject();
        List<PolicyStatementType> statements = obj.getPolicyStatement();
        for (PolicyStatementType statement : statements) {
            if (statement != null && PolicyStatementTypeType.APPLY.equals(statement.getType()) && statement.getMarkRef().getOid().equals(role.getOid())) {
                return true;
            }
        }
        return false;
    }

    private void unassignMembersPerformed(IModel<Serializable> rowModel, AjaxRequestTarget target) {
        // FIXME: Unassign all shadow

    }

    private ObjectType getObjectTypeFromRow(IModel<?> rowModel) {
        if (rowModel != null && (rowModel.getObject() instanceof SelectableBean)
                && ((SelectableBean<?>) rowModel.getObject()).getValue() instanceof ObjectType) {
            return (ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue();
        }
        return null;
    }

    protected void createDeleteMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_DELETE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteMembersPerformed(getRowModel(), target);
                        }
                    };
                }

            });
        }
    }

    protected List<QName> getSupportedRelations() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getSupportedRelations();
        }
        return new ArrayList<>();
    }

    private <AH extends AssignmentHolderType> List<QName> getSupportedObjectTypes() {
        Search search = getMemberPanelStorage().getSearch();
        return search.getAllowedTypeList();
    }

    private boolean isAuthorized(String action) {
        Map<String, String> memberAuthz = getAuthorizations(getComplexTypeQName());
        return true; // FIXME: Before commit
        //return WebComponentUtil.isAuthorized(memberAuthz.get(action));
    }




    private List<QName> getDefaultRelationsForActions() {
        List<QName> defaultRelations = new ArrayList<>();
        QName defaultRelation = getRelationValue();


        if (defaultRelation != null) {
            defaultRelations.add(getRelationValue());
        } else {
            defaultRelations.add(RelationTypes.MEMBER.getRelation());
        }

        if (defaultRelations.contains(PrismConstants.Q_ANY)) {
            defaultRelations = getSupportedRelations();
        }

        return defaultRelations;
    }

    private void deleteMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        StringResourceModel confirmModel;

        if (rowModel != null || getSelectedObjectsCount() > 0) {

            String deleteActionTranslated =
                    createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject.delete")
                            .getString();
            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    deleteActionTranslated, ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            //executeSimpleDeleteOperation(rowModel, confirmModel, target);
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");

            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    ((PageBase) getPage()).getMainPopupBodyId(), confirmModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return ShadowMarkPanel.this.getSupportedObjectTypes();
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    return ShadowMarkPanel.this.getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    return getDefaultRelationsForActions();
                }

                @Override
                protected IModel<String> getWarningMessageModel() {
                    return null;
                }

                @Override
                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    deleteMembersPerformed(rowModel, type, relations, target);
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return getObjectTypeFromRow(rowModel) == null;
                }

                @Override
                protected QName getDefaultObjectType() {
                    return getSearchType();
                }

            };
            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    protected int getSelectedObjectsCount() {
        return getMemberTable().getSelectedObjectsCount();
    }


    protected void executeDelete(AssignmentHolderType object, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask("Delete object");
            ObjectDelta<?> deleteDelta = getPrismContext()
                    .deltaFactory()
                    .object()
                    .createDeleteDelta(object.getClass(), object.getOid());
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(deleteDelta);
            getPageBase().getModelService().executeChanges(deltas, null, task, result);
        } catch (Throwable e) {
            result.recordFatalError("Cannot delete object" + object + ", " + e.getMessage(), e);
            LOGGER.error("Error while deleting object {}, {}", object, e.getMessage(), e);
            target.add(getFeedback());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected Component getFeedback() {
        return getPageBase().getFeedbackPanel();
    }

    protected void executeRecompute(AssignmentHolderType object, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask("Recompute object");
            getPageBase().getModelService().recompute(object.getClass(), object.getOid(), null, task, result);
        } catch (Throwable e) {
            result.recordFatalError("Cannot recompute object" + object + ", " + e.getMessage(), e);
            LOGGER.error("Error while recomputing object {}, {}", object, e.getMessage(), e);
            target.add(getFeedback());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected void executeUnassign(AssignmentHolderType object, QName relation, AjaxRequestTarget target) {
        List<AssignmentType> assignmentTypeList = getObjectAssignmentTypes(object, relation);
        OperationResult result = new OperationResult(
                assignmentTypeList.size() == 1 ? OPERATION_UNASSIGN_OBJECT : OPERATION_UNASSIGN_OBJECTS);
        for (AssignmentType assignmentType : assignmentTypeList) {
            OperationResult subResult = result.createSubresult(OPERATION_UNASSIGN_OBJECT);
            try {
                Task task = getPageBase().createSimpleTask("Unassign object");

                ObjectDelta<?> objectDelta = getPrismContext()
                        .deltaFor(object.getClass())
                        .item(OrgType.F_ASSIGNMENT).delete(assignmentType.clone())
                        .asObjectDelta(object.getOid());

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                getPageBase().getModelService().executeChanges(deltas, null, task, result);
                subResult.computeStatus();
            } catch (Throwable e) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Cannot unassign object" + object + ", " + e.getMessage(), e);
                LOGGER.error("Error while unassigned object {}, {}", object, e.getMessage(), e);
                target.add(getFeedback());
            }
        }
        result.computeStatusComposite();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected void refreshTable(AjaxRequestTarget target) {
        getMemberTable().refreshTable(target);
    }

    private String getTargetOrganizationOid() {
        ObjectReferenceType memberRef = ObjectTypeUtil.createObjectRef(ShadowMarkPanel.this.getModelObject());
        return memberRef.getOid();
    }

    private List<AssignmentType> getObjectAssignmentTypes(AssignmentHolderType object, QName relation) {
        return object.getAssignment().stream()
                .filter(
                        assignment -> assignment.getTargetRef() != null
                                && assignment.getTargetRef().getOid().equals(getTargetOrganizationOid())
                                && (relation == null || QNameUtil.match(relation, assignment.getTargetRef().getRelation())))
                .collect(Collectors.toList());
    }

    protected void deleteMembersPerformed(IModel<?> rowModel, QName memberType, Collection<QName> relations, AjaxRequestTarget target) {

//        MemberOperationsTaskCreator.createAndSubmitDeleteMembersTask(
//                getModelObject(),
//                memberType,
//                target, getPageBase());
    }

    protected void unassignMembersPerformed(IModel<?> rowModel, QName type, Collection<QName> relations, AjaxRequestTarget target) {

//        MemberOperationsTaskCreator.createAndSubmitUnassignMembersTask(
//                getModelObject(),
//                type,
//                relations,
//                target, getPageBase());
        target.add(this);
    }

    protected @NotNull List<QName> getRelationsForRecomputeTask() {
        return getSupportedRelations();
    }


    protected List<? extends ObjectType> getSelectedRealObjects() {
        return getMemberTable().getSelectedRealObjects();
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    protected WebMarkupContainer getMemberContainer() {
        return (WebMarkupContainer) get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER));
    }

    protected @NotNull QName getSearchType() {
        //noinspection unchecked
        return ObjectTypes.getObjectType(getMemberPanelStorage().getSearch().getTypeClass())
                .getTypeQName();
    }

    protected SearchBoxScopeType getScopeValue() {
        AbstractRoleSearchItemWrapper memberSearchitem = getMemberSearchItems();
        if (memberSearchitem != null) {
            return memberSearchitem.getScopeValue();
        }
        return null;
    }

    private AbstractRoleSearchItemWrapper getMemberSearchItems() {
        return getMemberPanelStorage().getSearch().findMemberSearchItem();
    }

    protected QName getRelationValue() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getRelationValue();
        }
        return null;
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        String storageKey = createStorageKey();
        if (StringUtils.isEmpty(storageKey)) {
            return null;
        }
        PageStorage storage = getPageStorage(storageKey);
        if (storage == null) {
            storage = getSessionStorage().initMemberStorage(storageKey);
        }
        return (MemberPanelStorage) storage;
    }

    private PageStorage getPageStorage(String storageKey) {
        return getSessionStorage().getPageStorageMap().get(storageKey);
    }

    private SessionStorage getSessionStorage() {
        return getPageBase().getSessionStorage();
    }

    protected String getStorageKeyTabSuffix() {
        return getPanelConfiguration().getIdentifier();
    }

    public MarkType getModelObject() {
        return getObjectDetailsModels().getObjectType();
    }

    private boolean isRefreshEnabled() {
        return Objects.requireNonNullElse(isRefreshEnabled, true);
    }


    protected AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshTable(target);
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-outline-primary ml-2"));
        refreshIcon.showTitleAsLabel(true);
        return refreshIcon;
    }

    protected AjaxIconButton createPlayPauseButton(String buttonId) {
        AjaxIconButton playPauseIcon = new AjaxIconButton(buttonId, getRefreshPausePlayButtonModel(),
                getRefreshPausePlayButtonTitleModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                isRefreshEnabled = !isRefreshEnabled;
                refreshTable(target);
            }
        };
        playPauseIcon.add(AttributeAppender.append("class", "btn btn-outline-primary ml-2"));
        playPauseIcon.showTitleAsLabel(true);
        return playPauseIcon;
    }

    private IModel<String> getRefreshPausePlayButtonModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return GuiStyleConstants.CLASS_PAUSE;
            }

            return GuiStyleConstants.CLASS_PLAY;
        };
    }

    private IModel<String> getRefreshPausePlayButtonTitleModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return createStringResource("MainObjectListPanel.refresh.pause").getString();
            }
            return createStringResource("MainObjectListPanel.refresh.start").getString();
        };
    }
}
