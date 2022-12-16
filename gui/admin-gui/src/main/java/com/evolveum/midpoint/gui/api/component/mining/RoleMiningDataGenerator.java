/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleMiningDataGenerator {

    int generateRolesGroupSize(int size) {
        ProbabilityGenerator probabilityGenerator = new ProbabilityGenerator();
        if (size > 5) {
            probabilityGenerator.addGroupProbability(7, 0.2d);
            probabilityGenerator.addGroupProbability(8, 0.3d);
            probabilityGenerator.addGroupProbability(3, 0.4d);
            probabilityGenerator.addGroupProbability(4, 0.5d);
            probabilityGenerator.addGroupProbability(5, 0.5d);
            probabilityGenerator.addGroupProbability(6, 0.3d);
        } else {
            return 1;
        }
        return probabilityGenerator.getRandomNumber();
    }

    public void generateUser(PageBase pageBase, int count, List<PrismObject<UserType>> userList) {
        OperationResult result = new OperationResult("Generate UserType object");
        Task task = pageBase.createSimpleTask("Add generated UserType object");

        List<PolyStringType> listOfNames = new ArrayList<>();

        int iterator = 1;
        for (int i = 0; i < count; i++) {
            OperationResult subResult = result.createSubresult("Add object");

            PolyStringType name = PolyStringType.fromOrig("U" + iterator + "g");

            for (PrismObject<UserType> userTypePrismObject : userList) {
                PolyStringType userName = PolyStringType.fromOrig(userTypePrismObject.getName().getOrig());
                listOfNames.add(userName);
            }

            while (listOfNames.contains(name)) {
                iterator++;
                name = PolyStringType.fromOrig("U" + iterator + "g");
            }


            PrismObject<UserType> user = null;
            try {
                user = pageBase.getPrismContext()
                        .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
            } catch (SchemaException e) {
                LOGGER.error("Error while generate UserType object,{}", e.getMessage(), e);
            }

            if (user != null) {
                iterator++;
                user.asObjectable().setName(name);
                pageBase.getModelService().importObject(user, null, task, result);
            }
            subResult.computeStatus();
        }
        result.computeStatusComposite();
        pageBase.showResult(result);
    }

    public void generateRole(PageBase pageBase, int count, List<PrismObject<RoleType>> roles) {
        OperationResult result = new OperationResult("Generate RoleType object");
        Task task = pageBase.createSimpleTask("Add generated RoleType object");

        List<PolyStringType> listOfNames = new ArrayList<>();

        int iterator = 1;
        for (int i = 0; i < count; i++) {
            OperationResult subResult = result.createSubresult("Add object");

            PolyStringType name = PolyStringType.fromOrig("R" + iterator + "g");

            for (PrismObject<RoleType> roleTypePrismObject : roles) {
                PolyStringType roleName = PolyStringType.fromOrig(roleTypePrismObject.getName().getOrig());
                listOfNames.add(roleName);
            }

            while (listOfNames.contains(name)) {
                iterator++;
                name = PolyStringType.fromOrig("R" + iterator + "g");
            }


            PrismObject<RoleType> role = null;
            try {
                role = pageBase.getPrismContext()
                        .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
            } catch (SchemaException e) {
                LOGGER.error("Error while generate RoleType object,{}", e.getMessage(), e);
            }

            if (role != null) {
                iterator++;
                role.asObjectable().setName(name);
                pageBase.getModelService().importObject(role, null, task, result);
            }
            subResult.computeStatus();
        }
        result.computeStatusComposite();
        pageBase.showResult(result);
    }

    private List<PrismObject<RoleType>> getRoleObjectReferenceTypes(AssignmentHolderType object, PageBase pageBase) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType"))
                .mapToObj(i -> getRoleByOid(object.getRoleMembershipRef().get(i).getOid(), pageBase))
                .collect(Collectors.toList());

    }

    protected PrismObject<RoleType> getRoleByOid(String oid, PageBase pageBase) {
        String getRole = DOT_CLASS + "getRole";
        OperationResult result = new OperationResult(getRole);
        Task task = pageBase.createSimpleTask(getRole);
        PrismObject<RoleType> prismObject = null;
        try {
            prismObject = pageBase.getModelService().getObject(RoleType.class, oid, null, task, result);
        } catch (Throwable e) {
            LOGGER.error("Error while getting role object by id {}, {}", oid, e.getMessage(), e);
        }
        return prismObject;
    }

    public void assignRoles(List<PrismObject<UserType>> userList, List<PrismObject<RoleType>> rolesList, PageBase pageBase) {
        OperationResult result = new OperationResult("Assign role");

        for (PrismObject<UserType> userTypePrismObject : userList) {
            if (!userTypePrismObject.getName().toString().equals("administrator")) {

                if (getRoleObjectReferenceTypes(userTypePrismObject.asObjectable(), pageBase).size() == 0) {
                    int groupSize = generateRolesGroupSize(rolesList.size());
                    int startIndex = new Random().nextInt(rolesList.size() - 1 - groupSize);

                    for (int i = 0; i < groupSize; i++) {
                        RoleType roleType = rolesList.get(startIndex + i).asObjectable();
                        UserType userType = userTypePrismObject.asObjectable();

                        try {
                            Task task = pageBase.createSimpleTask("Assign RoleType object");

                            ObjectDelta<UserType> objectDelta = pageBase.getPrismContext().deltaFor(UserType.class)
                                    .item(UserType.F_ASSIGNMENT)
                                    .add(ObjectTypeUtil.createAssignmentTo(roleType.getOid(),
                                            ObjectTypes.ROLE, pageBase.getPrismContext()))
                                    .asObjectDelta(userType.getOid());

                            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                            pageBase.getModelService().executeChanges(deltas, null, task, result);
                        } catch (Throwable e) {
                            System.out.println("something is wrong");
                            LOGGER.error("Error while assign object {}, {}", userType, e.getMessage(), e);
                        }

                    }
                }
            }
        }

        result.computeStatusComposite();
        pageBase.showResult(result);
    }

    public void unassignRoles(PageBase pageBase, List<PrismObject<UserType>> userList) {
        OperationResult result = new OperationResult("Unassign objects");

        for (PrismObject<UserType> userTypePrismObject : userList) {
            if (!userTypePrismObject.getName().toString().equals("administrator")) {
                UserType userObject = userTypePrismObject.asObjectable();

                List<PrismObject<RoleType>> userObjectRolesList = getRoleObjectReferenceTypes(userObject, pageBase);

                if (userObjectRolesList == null) {
                    return;
                }

                for (PrismObject<RoleType> objectRole : userObjectRolesList) {
                    OperationResult subResult = result.createSubresult("Unassign object");
                    try {
                        Task task = pageBase.createSimpleTask("Unassign object");

                        ObjectDelta<?> objectDelta = pageBase.getPrismContext()
                                .deltaFor(userObject.getClass())
                                .item(UserType.F_ASSIGNMENT)
                                .delete(createAssignmentTo(objectRole.getOid(), ObjectTypes.ROLE, pageBase.getPrismContext()))
                                .asObjectDelta(userObject.getOid());

                        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                        pageBase.getModelService().executeChanges(deltas, null, task, result);
                        subResult.computeStatus();
                    } catch (Throwable e) {
                        subResult.recomputeStatus();
                        subResult.recordFatalError("Cannot unassign object" + userObject + ", " + e.getMessage(), e);
                        LOGGER.error("Error while unassigned object {},from {} {}", objectRole, userObject, e.getMessage(), e);
                    }
                }

            }
        }
        result.computeStatusComposite();
        pageBase.showResult(result);
    }

}



