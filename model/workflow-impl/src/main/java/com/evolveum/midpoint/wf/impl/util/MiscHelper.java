/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

@Component
public class MiscHelper {

    private static final Trace LOGGER = TraceManager.getTrace(MiscHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public List<CaseType> getSubcases(CaseType rootCase, OperationResult result) throws SchemaException {
        return getSubcases(rootCase.getOid(), result);
    }

    public List<CaseType> getSubcases(String oid, OperationResult result) throws SchemaException {
        return repositoryService.searchObjects(CaseType.class,
                prismContext.queryFor(CaseType.class)
                    .item(CaseType.F_PARENT_REF).ref(oid)
                    .build(),
                null,
                result)
                .stream()
                    .map(o -> o.asObjectable())
                    .collect(Collectors.toList());
    }

    public LensContext<?> getModelContext(CaseType aCase, Task task, OperationResult result) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        LensContextType modelContextBean = aCase.getModelContext();
        if (modelContextBean != null) {
            return LensContext.fromLensContextBean(modelContextBean, task, result);
        } else {
            return null;
        }
    }

    public PrismObject resolveObjectReference(ObjectReferenceType ref, OperationResult result) {
        return resolveObjectReference(ref, false, result);
    }

    private PrismObject resolveObjectReference(ObjectReferenceType ref, boolean storeBack, OperationResult result) {
        if (ref == null) {
            return null;
        }
        if (ref.asReferenceValue().getObject() != null) {
            return ref.asReferenceValue().getObject();
        }
        try {
            PrismObject object = repositoryService.getObject((Class) prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()), ref.getOid(), null, result);
            if (storeBack) {
                ref.asReferenceValue().setObject(object);
            }
            return object;
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get reference {} details because it couldn't be found", e, ref);
            return null;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get reference {} details due to schema exception", e, ref);
            return null;
        }
    }

    public ObjectReferenceType resolveObjectReferenceName(ObjectReferenceType ref, OperationResult result) {
        if (ref == null || ref.getTargetName() != null) {
            return ref;
        }
        PrismObject<?> object;
        if (ref.asReferenceValue().getObject() != null) {
            object = ref.asReferenceValue().getObject();
        } else {
            object = resolveObjectReference(ref, result);
            if (object == null) {
                return ref;
            }
        }
        ref = ref.clone();
        ref.setTargetName(PolyString.toPolyStringType(object.getName()));
        return ref;
    }
}
