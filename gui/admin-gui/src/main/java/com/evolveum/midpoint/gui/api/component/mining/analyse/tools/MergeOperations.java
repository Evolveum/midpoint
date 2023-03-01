package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class MergeOperations {

    PageBase pageBase;

    public MergeOperations(PageBase pageBase) {
        this.pageBase = pageBase;
    }

    public PageBase getPageBase() {
        return pageBase;
    }

    public List<PrismObject<RoleType>> searchExistingRoles(String name) {
        List<PrismObject<RoleType>> prismObjectList;
        String getMembers = DOT_CLASS + "searchRole";
        OperationResult result = new OperationResult(getMembers);

        ObjectQuery query = getPageBase().getPrismContext().queryFor(RoleType.class)
                .item(RoleType.F_NAME).eq(name).build();

        try {
            prismObjectList = pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(RoleType.class, query, null, result);
           return prismObjectList;
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }

    }

    public PrismObject<RoleType> generateRole(String roleName) {

        OperationResult result = new OperationResult("Generate RoleType object");
        Task task = pageBase.createSimpleTask("Add generated RoleType object");

        PrismObject<RoleType> role = null;
        try {
            role = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generate RoleType object,{}", e.getMessage(), e);
        }

        assert role != null;
        role.asObjectable().setName(PolyStringType.fromOrig(roleName));
        pageBase.getModelService().importObject(role, null, task, result);

        return role;
    }

    public void mergeProcess(List<PrismObject<RoleType>> roleForDelete, PrismObject<UserType> manageUser, CandidateRole candidateRole) {

        String candidateRoleName = "GENERATED-"+candidateRole.getKey();
        boolean existing;
        List<PrismObject<RoleType>> prismObjectList = searchExistingRoles(candidateRoleName);
        existing = prismObjectList.size() != 0;
        PrismObject<RoleType> roleTypePrismObject;
        if(!existing){
             roleTypePrismObject = generateRole(candidateRoleName);
        }else {
            roleTypePrismObject = prismObjectList.get(0);
        }

        assignRole(roleTypePrismObject, manageUser.asObjectable());
        unassignRole(roleForDelete, manageUser);
    }

    public void unassignRole(List<PrismObject<RoleType>> roleForDelete, PrismObject<UserType> manageUser) {
        OperationResult result = new OperationResult("Unassign objects");

        for (PrismObject<RoleType> objectRole : roleForDelete) {
            OperationResult subResult = result.createSubresult("Unassign object");
            try {
                Task task = pageBase.createSimpleTask("Unassign object");

                ObjectDelta<?> objectDelta = pageBase.getPrismContext()
                        .deltaFor(manageUser.asObjectable().getClass())
                        .item(UserType.F_ASSIGNMENT)
                        .delete(createAssignmentTo(objectRole.getOid(), ObjectTypes.ROLE, pageBase.getPrismContext()))
                        .asObjectDelta(manageUser.asObjectable().getOid());

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                pageBase.getModelService().executeChanges(deltas, null, task, result);
                subResult.computeStatus();
            } catch (Throwable e) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Cannot unassign object" + manageUser.asObjectable() + ", " + e.getMessage(), e);
                LOGGER.error("Error while unassigned object {},from {} {}", objectRole, manageUser.asObjectable(), e.getMessage(), e);
            }
        }
    }

    public void assignRole(PrismObject<RoleType> roleForAssign, UserType userObject) {
        OperationResult result = new OperationResult("Assign role");

        RoleType roleType = roleForAssign.asObjectable();

        try {
            Task task = pageBase.createSimpleTask("Assign RoleType object");

            ObjectDelta<UserType> objectDelta = pageBase.getPrismContext().deltaFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT)
                    .add(ObjectTypeUtil.createAssignmentTo(roleType.getOid(),
                            ObjectTypes.ROLE, pageBase.getPrismContext()))
                    .asObjectDelta(userObject.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            pageBase.getModelService().executeChanges(deltas, null, task, result);
        } catch (Throwable e) {
            LOGGER.error("Error while assign object {}, {}", userObject, e.getMessage(), e);
        }
    }
}


