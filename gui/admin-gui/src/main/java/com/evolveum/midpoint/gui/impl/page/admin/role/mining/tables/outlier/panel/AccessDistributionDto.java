/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.gui.impl.util.AccessMetadataUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccessDistributionDto {

    int allAssignmentCount = 0;

    int directAssignment = 0;
    int indirectAssignment = 0;
    int duplicatedRoleAssignmentCount = 0;

    Set<String> directAssignmentOids = new HashSet<>();
    Set<String> indirectAssignmentOids = new HashSet<>();
    Set<String> duplicatedRoleAssignmentOids = new HashSet<>();

    double averageAccessPerUser = 0;

    public AccessDistributionDto(
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result,
            @NotNull Task task) {
        this.averageAccessPerUser = getAverageAccessPerUser(roleAnalysisService, result, task);
        initOutlierBasedModels(roleAnalysisService, outlier, result, task);
    }

    public AccessDistributionDto(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull PrismObject<UserType> prismUserObject,
            @NotNull OperationResult result,
            @NotNull Task task) {
        this.averageAccessPerUser = getAverageAccessPerUser(roleAnalysisService, result, task);
        initUserBasedModels(prismUserObject);
    }

    private void initOutlierBasedModels(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull OperationResult result,
            @NotNull Task task) {

        PrismObject<UserType> prismUser = roleAnalysisService
                .getUserTypeObject(outlier.getObjectRef().getOid(), task, result);

        initUserBasedModels(prismUser);
    }

    private void initUserBasedModels(@Nullable PrismObject<UserType> prismUserObject) {

        if (prismUserObject == null) {
            return;
        }

        UserType user = prismUserObject.asObjectable();
        List<ObjectReferenceType> refsToRoles = user.getRoleMembershipRef()
                .stream()
                .filter(ref -> QNameUtil.match(ref.getType(), RoleType.COMPLEX_TYPE)) //TODO maybe also check relation?
                .toList();

        this.allAssignmentCount = refsToRoles.size();

        for (ObjectReferenceType ref : refsToRoles) {
            List<AssignmentPathMetadataType> metadataPaths = AccessMetadataUtil.computeAssignmentPaths(ref);
            if (metadataPaths.size() == 1) {
                List<AssignmentPathSegmentMetadataType> segments = metadataPaths.get(0).getSegment();
                if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                    directAssignment++;
                    directAssignmentOids.add(ref.getOid());
                } else {
                    indirectAssignmentOids.add(ref.getOid());
                    indirectAssignment++;
                }
            } else {
                boolean foundDirect = false;
                boolean foundIndirect = false;
                for (AssignmentPathMetadataType metadata : metadataPaths) {
                    List<AssignmentPathSegmentMetadataType> segments = metadata.getSegment();
                    if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                        foundDirect = true;
                        if (foundIndirect) {
                            indirectAssignment--;
                            duplicatedRoleAssignmentCount++;
                            indirectAssignmentOids.remove(ref.getOid());
                            duplicatedRoleAssignmentOids.add(ref.getOid());
                        } else {
                            directAssignment++;
                            directAssignmentOids.add(ref.getOid());
                        }

                    } else {
                        foundIndirect = true;
                        if (foundDirect) {
                            directAssignment--;
                            duplicatedRoleAssignmentCount++;
                            directAssignmentOids.remove(ref.getOid());
                            duplicatedRoleAssignmentOids.add(ref.getOid());
                        } else {
                            indirectAssignment++;
                            indirectAssignmentOids.add(ref.getOid());
                        }
                    }
                }
            }

        }
    }

    private static double getAverageAccessPerUser(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result,
            @NotNull Task simpleTask) {
        int numberOfRoleToUserAssignment = roleAnalysisService.countUserOwnedRoleAssignment(result);

        int finalUsersInSystem = roleAnalysisService.countObjects(UserType.class, null, null, simpleTask, result);

        double averagePerUser = finalUsersInSystem > 0
                ? (double) numberOfRoleToUserAssignment / finalUsersInSystem
                : 0.0;

        BigDecimal averagePerUserRounded = BigDecimal.valueOf(averagePerUser)
                .setScale(2, RoundingMode.HALF_UP);
        averagePerUser = averagePerUserRounded.doubleValue();
        return averagePerUser;
    }

    public int getAllAssignmentCount() {
        return allAssignmentCount;
    }

    public int getDirectAssignment() {
        return directAssignment;
    }

    public int getIndirectAssignment() {
        return indirectAssignment;
    }

    public int getDuplicatedRoleAssignmentCount() {
        return duplicatedRoleAssignmentCount;
    }

    public Set<String> getDirectAssignmentOids() {
        return directAssignmentOids;
    }

    public Set<String> getIndirectAssignmentOids() {
        return indirectAssignmentOids;
    }

    public Set<String> getDuplicatedRoleAssignmentOids() {
        return duplicatedRoleAssignmentOids;
    }

    public double getAverageAccessPerUser() {
        return averageAccessPerUser;
    }
}
