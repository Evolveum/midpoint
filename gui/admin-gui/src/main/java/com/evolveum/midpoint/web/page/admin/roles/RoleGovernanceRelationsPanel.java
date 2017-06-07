package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class RoleGovernanceRelationsPanel extends RoleMemberPanel<RoleType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleGovernanceRelationsPanel.class);
    private static final String DOT_CLASS = RoleGovernanceRelationsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_APPROVER_RELATION_OBJECTS = DOT_CLASS + "loadApproverRelationObjects";
    private static final String OPERATION_LOAD_OWNER_RELATION_OBJECTS = DOT_CLASS + "loadOwnerRelationObjects";
    private static final String OPERATION_LOAD_MANAGER_RELATION_OBJECTS = DOT_CLASS + "loadManagerRelationObjects";

    private LoadableModel<List<String>> approverRelationObjects;
    private LoadableModel<List<String>> ownerRelationObjects;
    private LoadableModel<List<String>> managerRelationObjects;

    private boolean areModelsInitialized = false;

    public RoleGovernanceRelationsPanel(String id, IModel<RoleType> model, List<RelationTypes> relations, PageBase pageBase) {
        super(id, model, relations, pageBase);
    }

    @Override
    protected List<InlineMenuItem> newMemberInlineMenuItems() {
        List<InlineMenuItem> newMemberMenuItems = new ArrayList<>();
        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createApprover"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.APPROVER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignApprovers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.APPROVER.getRelation(), target);
                    }
                }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createOwner"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.OWNER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignOwners"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.OWNER.getRelation(), target);
                    }
                }));
        newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createManager"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.MANAGER.getRelation(), target);
            }
        }));

        newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addManagers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.MANAGER.getRelation(), target);
                    }
                }));
        return newMemberMenuItems;
    }

    @Override
    protected List<InlineMenuItem> createUnassignMemberInlineMenuItems() {
        return super.createUnassignMemberInlineMenuItems();
    }

    @Override
    protected List<InlineMenuItem> createMemberRecomputeInlineMenuItems() {
        return new ArrayList<>();
    }

    @Override
    protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = super.createMembersColumns();
        IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("roleMemberPanel.relation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                                     String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                cellItem.add(new Label(componentId,
                        getRelationValue((FocusType) rowModel.getObject().getValue())));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getRelationValue((FocusType) rowModel.getObject().getValue()));
            }

        };
        columns.add(column);
        return columns;
    }

    private String getRelationValue(FocusType focusObject){
        StringBuilder relations = new StringBuilder();
        if (focusObject == null){
            return "";
        }
        if (!areModelsInitialized) {
            initLoadableModels();
            areModelsInitialized = true;
        }
        if (approverRelationObjects.getObject().contains(focusObject.getOid())){
            relations.append(createStringResource("RelationTypes.APPROVER").getString());
        }
        if (ownerRelationObjects.getObject().contains(focusObject.getOid())){
            relations.append(relations.length() > 0 ? ", " : "");
            relations.append(createStringResource("RelationTypes.OWNER").getString());
        }
        if (managerRelationObjects.getObject().contains(focusObject.getOid())){
            relations.append(relations.length() > 0 ? ", " : "");
            relations.append(createStringResource("RelationTypes.MANAGER").getString());
        }
        return relations.toString();
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions(){
        return SelectorOptions
                .createCollection(GetOperationOptions.createDistinct());
    }

    private void initLoadableModels(){
        approverRelationObjects = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_APPROVER_RELATION_OBJECTS);

                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.APPROVER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> approverRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(approverRelationObjects);
            }
        };

        ownerRelationObjects = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_OWNER_RELATION_OBJECTS);

                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.OWNER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> ownerRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(ownerRelationObjects);
            }
        };

        managerRelationObjects = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_MANAGER_RELATION_OBJECTS);

                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.MANAGER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> managerRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(managerRelationObjects);
            }
        };
    }

    private List<String> getObjectOidsList(List<PrismObject<FocusType>> objectList){
        List<String> oidsList = new ArrayList<>();
        if (objectList == null){
            return oidsList;
        }
        for (PrismObject<FocusType> object : objectList){
            if (object == null){
                continue;
            }
            if (!oidsList.contains(object.getOid())){
                oidsList.add(object.getOid());
            }
        }
        return oidsList;
    }


}
