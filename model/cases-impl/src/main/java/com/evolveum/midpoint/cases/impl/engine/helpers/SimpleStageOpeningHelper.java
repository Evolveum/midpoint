/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.helpers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates work items based on {@link SimpleCaseSchemaType}.
 */
@Component
public class SimpleStageOpeningHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleStageOpeningHelper.class);

    private static final String DEFAULT_ASSIGNEE_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private Clock clock;

    public List<CaseWorkItemType> createWorkItems(
            @Nullable SimpleCaseSchemaType schema,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {

        LOGGER.trace("Creating work items based on schema:\n{}", DebugUtil.debugDumpLazily(schema, 1));

        Collection<ObjectReferenceType> allAssignees = getAssigneeRefs(schema, result);

        List<CaseWorkItemType> workItems = createWorkItems(allAssignees, operation.getCurrentCase(), schema);

        LOGGER.trace("Work items created:\n{}", DebugUtil.debugDumpLazily(workItems, 1));
        return workItems;
    }

    private List<CaseWorkItemType> createWorkItems(
            Collection<ObjectReferenceType> allAssignees,
            CaseType aCase,
            SimpleCaseSchemaType schema) {

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        XMLGregorianCalendar deadline;
        if (schema != null && schema.getDuration() != null) {
            deadline = CloneUtil.clone(now);
            deadline.add(schema.getDuration());
        } else {
            deadline = null;
        }

        return allAssignees.stream()
                .map(assignee -> new CaseWorkItemType(prismContext)
                        .originalAssigneeRef(assignee.clone())
                        .assigneeRef(assignee.clone())
                        .name(aCase.getName().getOrig())
                        .createTimestamp(now)
                        .deadline(deadline))
                .collect(Collectors.toList());
    }

    private @NotNull Collection<ObjectReferenceType> getAssigneeRefs(
            @Nullable SimpleCaseSchemaType schema, @NotNull OperationResult result) {
        if (schema == null) {
            return List.of(getDefaultAssigneeRef());
        }

        Collection<ObjectReferenceType> allAssignees = resolveAssigneeRefs(schema.getAssigneeRef(), result);
        if (allAssignees.isEmpty() && schema.getDefaultAssigneeName() != null) {
            allAssignees.addAll(
                    resolveDefaultAssignee(
                            schema.getDefaultAssigneeName(), result));
        }
        if (allAssignees.isEmpty()) {
            allAssignees.add(getDefaultAssigneeRef());
        }
        return allAssignees;
    }

    private ObjectReferenceType getDefaultAssigneeRef() {
        return new ObjectReferenceType().oid(DEFAULT_ASSIGNEE_OID).type(UserType.COMPLEX_TYPE);
    }

    private Collection<ObjectReferenceType> resolveAssigneeRefs(List<ObjectReferenceType> assigneeRefs, OperationResult result) {
        return assigneeRefs.stream()
                .flatMap(ref -> resolveAssigneeRef(ref, result).stream())
                .collect(Collectors.toSet());
    }

    private Collection<ObjectReferenceType> resolveAssigneeRef(@NotNull ObjectReferenceType assigneeRef, OperationResult result) {
        if (assigneeRef.getType().equals(RoleType.COMPLEX_TYPE)
                || assigneeRef.getType().equals(OrgType.COMPLEX_TYPE)) {

            ObjectQuery membersQuery = prismContext.queryFor(UserType.class)
                    .type(UserType.class)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF)
                    .ref(prismContext.itemFactory().createReferenceValue(assigneeRef.getOid()))
                    .build();

            List<PrismObject<UserType>> linkedUsers = List.of();

            try {
                linkedUsers = repositoryService.searchObjects(UserType.class, membersQuery, null, result);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Could not find members in abstract role {} ({})",
                        e, assigneeRef.getOid(), assigneeRef.getType());
            }

            return ObjectTypeUtil.objectListToReferences(
                    selectRealMembers(linkedUsers, assigneeRef.getOid()));
        } else {
            return List.of(assigneeRef);
        }
    }

    private List<PrismObject<UserType>> selectRealMembers(List<PrismObject<UserType>> allLinkedUsers, String roleOid) {
        return allLinkedUsers.stream()
                .filter(user -> isRealMember(user, roleOid))
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isRealMember(PrismObject<UserType> user, String roleOid) {
        return user.asObjectable().getRoleMembershipRef().stream()
                .anyMatch(ref -> roleOid.equals(ref.getOid()) && relationRegistry.isMember(ref.getRelation()));
    }

    private Collection<ObjectReferenceType> resolveDefaultAssignee(String name, OperationResult result) {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly(name).matchingOrig()
                .build();

        List<PrismObject<UserType>> defaultAssignees;
        try {
            defaultAssignees = repositoryService.searchObjects(UserType.class, query, null, result);
            assert defaultAssignees.size() <= 1;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not resolve default assignee (user '{}'), "
                    + "using system-wide default instead.", e, name);
            return List.of();
        }

        if (defaultAssignees.isEmpty()) {
            LOGGER.warn("Default assignee named '{}' was not found; using system-wide default instead.", name);
            return List.of();
        } else {
            return ObjectTypeUtil.objectListToReferences(defaultAssignees);
        }
    }
}
