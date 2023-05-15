/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleMiningDataGenerator {

    public int generateRolesGroupSize(int size) {
        ProbabilityGenerator probabilityGenerator = new ProbabilityGenerator();
        if (size >= 8) {
            probabilityGenerator.addGroupProbability(7, 0.2d);
            probabilityGenerator.addGroupProbability(8, 0.3d);
            probabilityGenerator.addGroupProbability(3, 0.3d);
            probabilityGenerator.addGroupProbability(4, 0.5d);
            probabilityGenerator.addGroupProbability(5, 0.5d);
            probabilityGenerator.addGroupProbability(6, 0.3d);
        } else if (size >= 4) {
            probabilityGenerator.addGroupProbability(1, 0.3d);
            probabilityGenerator.addGroupProbability(2, 0.3d);
            probabilityGenerator.addGroupProbability(3, 0.3d);
            probabilityGenerator.addGroupProbability(4, 0.1d);
        } else {
            return 1;
        }
        return probabilityGenerator.getRandomNumber();
    }

    int generateAuthGroupSize(int size) {
        ProbabilityGenerator probabilityGenerator = new ProbabilityGenerator();
        if (size >= 4) {
            probabilityGenerator.addGroupProbability(1, 0.3d);
            probabilityGenerator.addGroupProbability(2, 0.3d);
            probabilityGenerator.addGroupProbability(3, 0.3d);
            probabilityGenerator.addGroupProbability(4, 0.1d);
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

            PolyStringType name = PolyStringType.fromOrig("R_" + iterator + "g");

            for (PrismObject<RoleType> roleTypePrismObject : roles) {
                PolyStringType roleName = PolyStringType.fromOrig(roleTypePrismObject.getName().getOrig());
                listOfNames.add(roleName);
            }

            while (listOfNames.contains(name)) {
                iterator++;
                name = PolyStringType.fromOrig("R_" + iterator + "g");
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
                .mapToObj(i -> new RoleMiningFilter().getRoleByOid(object.getRoleMembershipRef().get(i).getOid(), pageBase))
                .collect(Collectors.toList());
    }

    public void assignRoles(List<PrismObject<UserType>> userList, List<PrismObject<RoleType>> rolesList, PageBase pageBase) {
        OperationResult result = new OperationResult("Assign role");

        for (PrismObject<UserType> userTypePrismObject : userList) {
            if (!userTypePrismObject.getName().toString().equals("administrator")) {

                if (getRoleObjectReferenceTypes(userTypePrismObject.asObjectable(), pageBase).size() == 0) {
                    int groupSize = generateRolesGroupSize(rolesList.size());

                    int startIndex = new Random().nextInt(((rolesList.size() - groupSize)) + 1);

                    for (int i = 0; i < groupSize; i++) {
                        RoleType roleType = rolesList.get(startIndex + i).asObjectable();
                        UserType userType = userTypePrismObject.asObjectable();
                        LOGGER.info("Assign object iterator: {},",i);

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
                            LOGGER.error("Error while assign object {}, {}", userType, e.getMessage(), e);
                        }

                    }
                }
            }
        }

        result.computeStatusComposite();
        pageBase.showResult(result);
    }

    public void assignAuthorization(List<PrismObject<RoleType>> rolesList, PageBase pageBase) {
        ArrayList<String> authorizationTemp = new ArrayList<>();
        authorizationTemp.add("P_A");
        authorizationTemp.add("P_B");
        authorizationTemp.add("P_C");
        authorizationTemp.add("P_D");
        authorizationTemp.add("P_E");

        OperationResult result = new OperationResult("Add authorization");

        for (PrismObject<RoleType> role : rolesList) {
            if (!role.getName().toString().equals("Superuser")) {
                int authorizationCount = new RoleMiningFilter().getAuthorization(role.asObjectable()).size();

                if (authorizationCount == 0) {
                    int groupSize = generateAuthGroupSize(authorizationTemp.size());

                    int startIndex = new Random().nextInt(((authorizationTemp.size() - groupSize)) + 1);

                    for (int i = 0; i < groupSize; i++) {
                        RoleType roleType = role.asObjectable();
                        AuthorizationType authorizationType = new AuthorizationType();
                        authorizationType.setName(authorizationTemp.get(startIndex + i));

                        try {
                            Task task = pageBase.createSimpleTask("Add authorization");

                            ObjectDelta<RoleType> objectDelta = pageBase.getPrismContext().deltaFor(RoleType.class)
                                    .item(RoleType.F_AUTHORIZATION).add(Containerable.asPrismContainerValue(authorizationType))
                                    .asObjectDelta(roleType.getOid());

                            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                            pageBase.getModelService().executeChanges(deltas, null, task, result);

                        } catch (Throwable e) {
                            LOGGER.error("Error while adding authorization object {}, {}", roleType, e.getMessage(), e);
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



