/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil.resolveNames;

/**
 *  Uses cache repository service to resolve object names.
 */
@Component
public class AuditHelper {

    @Autowired private AuditService auditService;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    /**
     * @param externalNameResolver Name resolver that should be tried first. It should be fast. If it returns null it means
     *                             "I don't know".
     */
    public void audit(AuditEventRecord record, ObjectDeltaSchemaLevelUtil.NameResolver externalNameResolver, Task task,
            OperationResult parentResult) {
        // TODO we could obtain names for objects that were deleted e.g. from the lens context (MID-5501)
        if (record.getDeltas() != null) {
            for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : record.getDeltas()) {
                ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();
                ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> {
                    OperationResult result = parentResult.subresult(AuditHelper.class.getName() + ".resolveName")
                            .setMinor()
                            .build();
                    try {
                        if (record.getNonExistingReferencedObjects().contains(oid)) {
                            // This information could come from upper layers (not now, but maybe in the future).
                            return null;
                        }
                        if (externalNameResolver != null) {
                            PolyString externallyResolvedName = externalNameResolver.getName(objectClass, oid);
                            if (externallyResolvedName != null) {
                                return externallyResolvedName;
                            }
                        }
                        // we use only cache-compatible options here, in order to utilize the local or global repository cache
                        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                                .allowNotFound().build();
                        PrismObject<? extends ObjectType> object = repositoryService.getObject(objectClass, oid, options, result);
                        return object.getName();
                    } catch (ObjectNotFoundException e) {
                        record.addNonExistingReferencedObject(oid);
                        return null;        // we will NOT record an error here
                    } catch (Throwable t) {
                        result.recordFatalError(t.getMessage(), t);
                        throw t;
                    } finally {
                        result.computeStatusIfUnknown();
                    }
                };
                resolveNames(delta, nameResolver, prismContext);
            }
        }
        auditService.audit(record, task);
    }
}
