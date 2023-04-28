/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.getProbabilitiesList;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.ProbabilityGenerator;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RoleMiningDataGenerator;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.component.mining.structure.ProbabilityStructure;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PruneDataGenerator {

    static int probabilityPermissionRBAM(int size) {
        ProbabilityGenerator probabilityGenerator = new ProbabilityGenerator();
        List<ProbabilityStructure> probabilitiesList = getProbabilitiesList();

        if (size >= 10) {
            if (probabilitiesList.isEmpty()) {
                probabilityGenerator.addGroupProbability(5, 0.2d);
                probabilityGenerator.addGroupProbability(20, 0.3d);
                probabilityGenerator.addGroupProbability(10, 0.3d);
                probabilityGenerator.addGroupProbability(3, 0.1d);
                probabilityGenerator.addGroupProbability(1, 0.1d);

            } else {
                for (ProbabilityStructure probabilityStructure : probabilitiesList) {
                    probabilityGenerator.addGroupProbability(probabilityStructure.getOverlap(), probabilityStructure.getProbability());
                }
            }
        } else {
            return 1;
        }
        return probabilityGenerator.getRandomNumber();
    }

    public void assignRoles(List<PrismObject<UserType>> userList, List<PrismObject<RoleType>> rolesList, PageBase pageBase) {
        OperationResult result = new OperationResult("Assign role");

        int userSize = userList.size();

        int bound = rolesList.size();
        for (int i = 0; i < bound; i++) {
            LOGGER.info("Assign ROLE: " + i + "/" + bound);
            if (!rolesList.get(i).getName().toString().contains("R_")) {
                continue;
            }
            double probabilityAssign = userSize / (double) 100 * probabilityPermissionRBAM(userSize);

            List<Integer> exist = new ArrayList<>();

            RoleType roleType = rolesList.get(i).asObjectable();
            for (int p = 0; p < (int) probabilityAssign; p++) {
                int number = new Random().nextInt(userSize);
                if (!exist.contains(number)) {
                    exist.add(number);

                    UserType userType = userList.get(number).asObjectable();

                    if (!userType.getName().toString().equals("administrator")) {

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

    public void assignInducements(List<PrismObject<RoleType>> rolesList, PageBase pageBase) {
        OperationResult result = new OperationResult("Assign role");

        for (PrismObject<RoleType> roleTypePrismObject : rolesList) {
            if (!roleTypePrismObject.getName().toString().equals("Superuser")) {

                if (roleTypePrismObject.asObjectable().getInducement().size() == 0) {
                    int groupSize = new RoleMiningDataGenerator().generateRolesGroupSize(rolesList.size());

                    int startIndex = new Random().nextInt(((rolesList.size() - groupSize)) + 1);

                    for (int i = 0; i < groupSize; i++) {
                        RoleType roleType = rolesList.get(startIndex + i).asObjectable();
                        RoleType roleObject = roleTypePrismObject.asObjectable();

                        if(roleType.equals(roleObject)){
                            continue;
                        }
                        try {

                            Task task = pageBase.createSimpleTask("Assign RoleType object");

                            ObjectDelta<UserType> objectDelta = pageBase.getPrismContext().deltaFor(RoleType.class)
                                    .item(RoleType.F_INDUCEMENT)
                                    .add(ObjectTypeUtil.createAssignmentTo(roleType.getOid(),
                                            ObjectTypes.ROLE, pageBase.getPrismContext()))
                                    .asObjectDelta(roleObject.getOid());

                            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                            pageBase.getModelService().executeChanges(deltas, null, task, result);
                        } catch (Throwable e) {
                            LOGGER.error("Error while assign object {}, {}", roleObject, e.getMessage(), e);
                        }

                    }
                }
            }
        }

        result.computeStatusComposite();
        pageBase.showResult(result);
    }
    public void assignAuthorizationMultiple(List<PrismObject<RoleType>> rolesList, int maxAuthorizations, PageBase pageBase) {

        ArrayList<String> authorizationTemp = new ArrayList<>();

        for (int i = 0; i < maxAuthorizations; i++) {
            authorizationTemp.add("P_" + i);
        }

        OperationResult result = new OperationResult("Add authorization");

        for (PrismObject<RoleType> role : rolesList) {
            if (!role.getName().toString().equals("Superuser")) {
                int authorizationCount = new RoleMiningFilter().getAuthorization(role.asObjectable()).size();

                if (authorizationCount == 0) {

                    int start = new Random().nextInt(maxAuthorizations);

                    List<String> generatedAuth = new ArrayList<>();
                    for (int u = 0; u < start; u++) {
                        int newAuth = new Random().nextInt(maxAuthorizations);
                        if (!generatedAuth.contains(authorizationTemp.get(newAuth))) {
                            generatedAuth.add(authorizationTemp.get(newAuth));
                        }
                    }

                    for (String s : generatedAuth) {
                        RoleType roleType = role.asObjectable();
                        AuthorizationType authorizationType = new AuthorizationType();
                        authorizationType.setName(s);

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

    public void assignAuthorization(List<PrismObject<RoleType>> rolesList, PageBase pageBase) {

        OperationResult result = new OperationResult("Add authorization");

        for (PrismObject<RoleType> role : rolesList) {
            if (!role.getName().toString().equals("Superuser")) {
                int authorizationCount = role.asObjectable().getAuthorization().size();

                if (authorizationCount == 0) {

                    RoleType roleType = role.asObjectable();
                    AuthorizationType authorizationType = new AuthorizationType();
                    authorizationType.setName("P_" + role.getName().toString());

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

    public void unassignAuthorizations(PageBase pageBase, List<PrismObject<RoleType>> roleList) {
        OperationResult result = new OperationResult("Unassign objects");

        for (PrismObject<RoleType> roleTypePrismObject : roleList) {
            if (roleTypePrismObject.getName().toString().contains("R_")) {
                RoleType roleObject = roleTypePrismObject.asObjectable();

                List<AuthorizationType> roleAuthorizations = roleObject.getAuthorization();

                if (roleAuthorizations == null) {
                    return;
                }

                OperationResult subResult = result.createSubresult("Unassign object");
                try {
                    Task task = pageBase.createSimpleTask("Unassign object");

                    ObjectDelta<RoleType> delta = pageBase.getPrismContext().deltaFor(RoleType.class)
                            .item(RoleType.F_AUTHORIZATION).replace()
                            .asObjectDelta(roleObject.getOid());

                    Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
                    pageBase.getModelService().executeChanges(deltas, null, task, result);
                    subResult.computeStatus();

                } catch (Throwable e) {
                    subResult.recomputeStatus();
                    subResult.recordFatalError("Cannot delete authorization object" + roleObject + ", " + e.getMessage(), e);
                    LOGGER.error("Error while deleting authorization,from {} {}", roleObject, e.getMessage(), e);
                }

            }
        }
        result.computeStatusComposite();
        pageBase.showResult(result);
    }

}



