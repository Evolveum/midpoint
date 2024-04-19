/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.F_APPROVAL_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType.F_DELTAS_TO_APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType.F_RESULTING_DELTAS;

/**
 * Methods generally useful for Primary change processor and its components.
 */
@Component
public class PcpGeneralHelper {
    private static final Trace LOGGER = TraceManager.getTrace(PcpGeneralHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public ObjectTreeDeltas<?> retrieveDeltasToApprove(CaseType aCase) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = aCase.asPrismObject()
                .findProperty(ItemPath.create(F_APPROVAL_CONTEXT, F_DELTAS_TO_APPROVE));
        if (deltaTypePrismProperty != null) {
            return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue());
        } else {
            throw new SchemaException("No deltas to process in case; case = " + aCase);
        }
    }

    public void storeResultingDeltas(CaseType aCase, ObjectTreeDeltas<?> deltas, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
        if (aCase.getApprovalContext() == null) {
            throw new IllegalStateException("No approval context in " + aCase);
        }
        aCase.getApprovalContext().setResultingDeltas(deltasType);
        ItemDefinition<?> def = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(ApprovalContextType.class)
                .findPropertyDefinition(F_RESULTING_DELTAS);
        ItemPath path = ItemPath.create(F_APPROVAL_CONTEXT, F_RESULTING_DELTAS);

        repositoryService.modifyObject(CaseType.class, aCase.getOid(),
                prismContext.deltaFor(CaseType.class)
                        .item(path, def).replace(deltasType)
                        .asItemDeltas(), result);
        LOGGER.trace("Stored deltas into case {}:\n{}", aCase, deltas); // TODO debug dump
    }

    public ObjectTreeDeltas<?> retrieveResultingDeltas(CaseType aCase) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = aCase.asPrismObject()
                .findProperty(ItemPath.create(F_APPROVAL_CONTEXT, F_RESULTING_DELTAS));
        if (deltaTypePrismProperty != null) {
            return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue());
        } else {
            return null;
        }
    }

    void addPrerequisites(CaseType subcase, List<CaseType> prerequisites, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_PREREQUISITE_REF)
                .addRealValues(
                        prerequisites.stream()
                                .map(c -> ObjectTypeUtil.createObjectRef(c))
                                .collect(Collectors.toList()))
                .asItemDeltas();
        repositoryService.modifyObject(CaseType.class, subcase.getOid(), modifications, result);
    }

    private static final int MAX_LEVEL = 5;

    public CaseType getRootCase(CaseType aCase, OperationResult result) throws SchemaException, ObjectNotFoundException {
        CaseType origin = aCase;
        if (aCase.getParentRef() == null || aCase.getParentRef().getOid() == null) {
            throw new IllegalArgumentException("Case " + aCase + " has no parent case although it should have one");
        }
        for (int level = 0; level < MAX_LEVEL; level++) {
            aCase = repositoryService.getObject(CaseType.class, aCase.getParentRef().getOid(), null, result).asObjectable();
            if (aCase.getParentRef() == null) {
                return aCase;
            }
        }
        throw new IllegalStateException("Too many parent levels for " + origin);
    }
}
