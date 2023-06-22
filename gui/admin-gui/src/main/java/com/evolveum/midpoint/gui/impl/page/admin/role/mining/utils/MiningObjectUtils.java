/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class MiningObjectUtils {

    public static PrismObject<MiningType> generateMiningGroup(List<String> rolesOid, String userOid, @NotNull PageBase pageBase) {

        PrismObject<MiningType> miningTypePrismObject = null;
        try {
            miningTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(MiningType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generate MiningType object,{}", e.getMessage(), e);
        }
        assert miningTypePrismObject != null;

        List<String> roles = miningTypePrismObject.asObjectable().getRoles();
        Collections.sort(rolesOid);
        roles.addAll(rolesOid);

        String mergedRolesOid = String.join("", rolesOid);

        String uuid = generateUniqueUUID(mergedRolesOid);

        miningTypePrismObject.asObjectable().setName(PolyStringType.fromOrig(uuid));

        miningTypePrismObject.asObjectable().setOid(uuid);
        miningTypePrismObject.asObjectable().setMembersCount(1);
        miningTypePrismObject.asObjectable().setRolesCount(roles.size());

        miningTypePrismObject.asObjectable().getMembers().add(userOid);
        return miningTypePrismObject;
    }

    public static String generateUniqueUUID(@NotNull String inputString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(inputString.getBytes(StandardCharsets.UTF_8));
            byte[] combinedBytes = new byte[33];
            System.arraycopy(hashBytes, 0, combinedBytes, 0, 32);
            combinedBytes[32] = 0;

            UUID uniqueUUID = UUID.nameUUIDFromBytes(combinedBytes);

            long mostSignificantBits = uniqueUUID.getMostSignificantBits();
            mostSignificantBits &= 0xFFFFFFFFFFFF0FFFL;
            mostSignificantBits |= 0x0000000000004000L;
            uniqueUUID = new UUID(mostSignificantBits, uniqueUUID.getLeastSignificantBits());

            return String.valueOf(uniqueUUID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void generateMiningGroup2(List<String> rolesOid, List<String> usersOid,
            @NotNull PageBase pageBase, Task task, OperationResult result) {
        PrismObject<MiningType> miningTypePrismObject = null;
        try {
            miningTypePrismObject = pageBase.getPrismContext().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(MiningType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generating MiningType object: {}", e.getMessage(), e);
        }

        assert miningTypePrismObject != null;

        List<String> roles = miningTypePrismObject.asObjectable().getRoles();
        Collections.sort(rolesOid);
        roles.addAll(rolesOid);

        String mergedRolesOid = String.join("", rolesOid);

        String uuid = generateUniqueUUID(mergedRolesOid);

        MiningType miningType = miningTypePrismObject.asObjectable();
        miningType.setName(PolyStringType.fromOrig(uuid));
        miningType.setOid(uuid);
        miningType.setMembersCount(usersOid.size());
        miningType.setRolesCount(roles.size());
        miningType.getMembers().addAll(usersOid);

        pageBase.getModelService().importObject(miningTypePrismObject, null, task, result);

    }

    public static Map<List<String>, List<String>> prepareExactUsersSetByRoles(OperationResult result, @NotNull PageBase pageBase, int minRolesCount) throws Exception {

        Map<List<String>, List<String>> userTypeMap = new HashMap<>();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                List<String> rolesOid = new ArrayList<>();
                List<AssignmentType> assignments = object.asObjectable().getAssignment();
                for (AssignmentType assignment : assignments) {
                    ObjectReferenceType targetRef = assignment.getTargetRef();
                    if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                        rolesOid.add(targetRef.getOid());
                    }
                }
                if (rolesOid.size() >= minRolesCount) {
                    Collections.sort(rolesOid);
                    userTypeMap.computeIfAbsent(rolesOid, k -> new ArrayList<>()).add(object.asObjectable().getOid());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();
        repositoryService.searchObjectsIterative(UserType.class, null, resultHandler, optionsBuilder.build(), true, result);
        Task task = pageBase.createSimpleTask("Import MiningType object");

        System.out.println(userTypeMap.size());

        for (Map.Entry<List<String>, List<String>> entry : userTypeMap.entrySet()) {
            List<String> rolesOid = entry.getKey();
            List<String> usersOid = entry.getValue();

            generateMiningGroup2(rolesOid, usersOid, pageBase, task, result);

        }

        return userTypeMap;
    }

    public static void importMiningGroups(OperationResult result, @NotNull PageBase pageBase, int minRolesCount) throws Exception {

        Task task = pageBase.createSimpleTask("Import MiningType object");

        Task taskCheck = pageBase.createSimpleTask("Check for existing MiningType object");
        ResultHandler<UserType> handler = (object, parentResult) -> {

            List<String> rolesOid = new ArrayList<>();
            List<AssignmentType> assignment = object.asObjectable().getAssignment();
            for (AssignmentType assignmentObject : assignment) {
                ObjectReferenceType targetRef = assignmentObject.getTargetRef();
                if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())) {
                    rolesOid.add(targetRef.getOid());
                }
            }

            if (rolesOid.size() >= minRolesCount) {
                PrismObject<MiningType> miningTypePrismObject = generateMiningGroup(rolesOid, object.getOid(), pageBase);
                modifyMembersPart(result, pageBase, task, taskCheck, object, miningTypePrismObject);
            }

            return true;
        };

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();
        repositoryService.searchObjectsIterative(UserType.class, null, handler,
                optionsBuilder.build(), true, result);

    }

    private static void modifyMembersPart(OperationResult result, @NotNull PageBase pageBase, Task task, Task taskCheck,
            @NotNull PrismObject<UserType> object, @NotNull PrismObject<MiningType> miningTypePrismObject) {
        try {
            PrismObject<MiningType> prismObject = pageBase.getModelService().getObject(MiningType.class,
                    miningTypePrismObject.asObjectable().getOid(), null, taskCheck, result);

            ObjectDelta<MiningType> objectDelta = pageBase.getPrismContext().deltaFor(MiningType.class)
                    .item(MiningType.F_MEMBERS).add(object.getOid())
                    .item(MiningType.F_MEMBERS_COUNT).replace(prismObject.asObjectable().getMembers().size() + 1)
                    .asObjectDelta(miningTypePrismObject.asObjectable().getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            pageBase.getModelService().executeChanges(deltas, null, task, result);

        } catch (ObjectNotFoundException e) {
            pageBase.getModelService().importObject(miningTypePrismObject, null, task, result);
        } catch (CommonException e) {
            LOGGER.error("Check for existing MiningType object failed,{}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static void deleteMiningObjects(OperationResult result, @NotNull PageBase pageBase) throws Exception {
        ResultHandler<AssignmentHolderType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();

        ObjectFilter filter = pageBase.getPrismContext().queryFor(MiningType.class).buildFilter();
        ObjectQuery queryType = pageBase.getPrismContext().queryFor(AssignmentHolderType.class)
                .type(MiningType.class).filter(filter).build();

        service.searchObjectsIterative(AssignmentHolderType.class, queryType, handler, optionsBuilder.build(),
                pageBase.createSimpleTask("Search iterative miningType objects"), result);

    }

    public static void similarityUpdaterIntersection(@NotNull PageBase pageBase, int minIntersection) throws Exception {

        HashMap<String, List<HashMap<String, List<String>>>> hashMap = new HashMap<>();

        List<MiningType> miningList = getMiningList(pageBase);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < miningList.size(); i++) {
            MiningType miningTypeA = miningList.get(i);
            String oidA = miningTypeA.asPrismObject().getOid();
            Set<String> rolesA = new HashSet<>(miningTypeA.getRoles());

            hashMap.putIfAbsent(oidA, new ArrayList<>());

            for (int j = i + 1; j < miningList.size(); j++) {
                MiningType miningTypeB = miningList.get(j);
                Set<String> rolesB = new HashSet<>(miningTypeB.getRoles());

                Set<String> intersection = RoleUtils.intersection(rolesA, rolesB);

                if (intersection.size() >= minIntersection) {

                    String oidB = miningTypeB.asPrismObject().getOid();

                    HashMap<String, List<String>> innerMapA = new HashMap<>();
                    innerMapA.put(oidB, new ArrayList<>(intersection));

                    hashMap.get(oidA).add(innerMapA);

                    HashMap<String, List<String>> innerMapB = new HashMap<>();
                    innerMapB.put(oidA, new ArrayList<>(intersection));

                    hashMap.computeIfAbsent(oidB, k -> new ArrayList<>()).add(innerMapB);

                }
            }

            updateSimilarityCount(pageBase, hashMap, oidA);

            System.out.println(i);
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Elapsed time: " + elapsedSeconds + " seconds END");

    }

    public static void similarityUpdaterOidJaccard(@NotNull PageBase pageBase, int minIntersection, double jaccardIndex)
            throws Exception {

        HashMap<String, List<String>> hashMap = new HashMap<>();

        List<MiningType> miningList = getMiningList(pageBase);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < miningList.size(); i++) {
            MiningType miningTypeA = miningList.get(i);
            String oidA = miningTypeA.asPrismObject().getOid();
            Set<String> rolesA = new HashSet<>(miningTypeA.getRoles());

            hashMap.putIfAbsent(oidA, new ArrayList<>());

            for (int j = i + 1; j < miningList.size(); j++) {
                MiningType miningTypeB = miningList.get(j);
                Set<String> rolesB = new HashSet<>(miningTypeB.getRoles());

                Set<String> intersection = RoleUtils.intersection(rolesA, rolesB);

                double v = RoleUtils.jacquardSimilarity(rolesA, rolesB, intersection);
                if (v >= jaccardIndex && intersection.size() >= minIntersection) {

                    String oidB = miningTypeB.asPrismObject().getOid();

                    hashMap.get(oidA).add(oidB);

                    hashMap.computeIfAbsent(oidB, k -> new ArrayList<>()).add(oidA);

                }
            }

            updateSimilarityGroup(pageBase, hashMap.get(oidA), oidA);

            System.out.println(i);
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Elapsed time: " + elapsedSeconds + " seconds END");

    }

    private static void updateSimilarityGroup(@NotNull PageBase pageBase, List<String> similarGroups, String oidA) {

        try {
            PrismContext prismContext = pageBase.getPrismContext();
            Task task = pageBase.createSimpleTask("Update similarity count parameter");
            String string = DOT_CLASS + "similarityCount";
            OperationResult result = new OperationResult(string);

            String[] similarGroupsArray = similarGroups.toArray(new String[0]);

            ObjectDelta<MiningType> objectDelta = prismContext.deltaFor(MiningType.class)
                    .item(MiningType.F_SIMILAR_GROUPS_COUNT).replace(similarGroups.size())
                    .item(MiningType.F_SIMILAR_GROUPS_ID).replace((Object[]) similarGroupsArray)
                    .asObjectDelta(oidA);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            pageBase.getModelService().executeChanges(deltas, null, task, result);

        } catch (CommonException e) {
            LOGGER.error("Update similarity count parameter failed,{}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private static void updateSimilarityCount(@NotNull PageBase pageBase, HashMap<String, List<HashMap<String,
            List<String>>>> hashMap, String oidA) {
        try {
            PrismContext prismContext = pageBase.getPrismContext();
            Task task = pageBase.createSimpleTask("Update similarity count parameter");
            String string = DOT_CLASS + "similarityCount";
            OperationResult result = new OperationResult(string);

            ObjectDelta<MiningType> objectDelta = prismContext.deltaFor(MiningType.class)
                    .item(MiningType.F_SIMILAR_GROUPS_COUNT).replace(hashMap.get(oidA).size())
                    .asObjectDelta(oidA);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            pageBase.getModelService().executeChanges(deltas, null, task, result);

        } catch (CommonException e) {
            LOGGER.error("Update similarity count parameter failed,{}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static List<MiningType> getMiningList(@NotNull PageBase pageBase) throws Exception {
        long startTime = System.currentTimeMillis();

        String string = DOT_CLASS + "filterMiningType";
        OperationResult result = new OperationResult(string);

        List<MiningType> roles = new ArrayList<>();
        ResultHandler<MiningType> handler = (object, parentResult) -> {

            roles.add(object.asObjectable());

            return true;
        };

        ModelService service = pageBase.getModelService();
        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();
        service.searchObjectsIterative(MiningType.class, null, handler, optionsBuilder.build(),
                pageBase.createSimpleTask("Search iterative miningType objects"), result);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Elapsed time for get MiningType object: " + elapsedSeconds + " seconds");

        return roles;
    }

    public static @NotNull PrismObject<MiningType> getMiningObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(MiningType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PrismObject<MiningType>> filterMiningTypeObjects(PageBase pageBase) {

        String string = DOT_CLASS + "filterMiningTypes";
        OperationResult result = new OperationResult(string);

        List<PrismObject<MiningType>> prismObjectList = null;
        try {
            prismObjectList = pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(MiningType.class, null, null, result);
        } catch (CommonException e) {
            e.printStackTrace();
        }

        return prismObjectList;
    }

}
