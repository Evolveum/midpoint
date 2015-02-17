/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.AccessCertificationCase;
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRunType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Service
public class CertificationManagerImpl implements CertificationManager {

    public static final String INTERFACE_DOT = CertificationManager.class.getName();
    public static final String OPERATION_START_CERTIFICATION_RUN = INTERFACE_DOT + "startCertificationRun";

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    private Map<String,CertificationHandler> registeredHandlers = new HashMap<>();

    public void registerHandler(String typeUri, CertificationHandler handler) {
        if (registeredHandlers.containsKey(typeUri)) {
            throw new IllegalStateException("There is already a handler for certification type " + typeUri);
        }
        registeredHandlers.put(typeUri, handler);
    }

    public CertificationHandler findCertificationHandler(AccessCertificationTypeType accessCertificationTypeType) {
        if (StringUtils.isBlank(accessCertificationTypeType.getUri())) {
            throw new IllegalArgumentException("No type URI for access certification type " + accessCertificationTypeType);
        }
        CertificationHandler handler = registeredHandlers.get(accessCertificationTypeType.getUri());
        if (handler == null) {
            throw new IllegalStateException("No handler for certification type " + accessCertificationTypeType.getUri());
        }
        return handler;
    }

    @Override
    public AccessCertificationRunType startCertificationRun(AccessCertificationTypeType certificationTypeType, AccessCertificationRunType runType, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {
        Validate.notNull(certificationTypeType, "certificationTypeType");
        Validate.notNull(certificationTypeType.getOid(), "certificationTypeType.oid");
        if (runType != null && runType.getOid() != null) {
            throw new IllegalArgumentException("Certification run with non-null OID is not permitted.");
        }

        OperationResult result = parentResult.createSubresult(OPERATION_START_CERTIFICATION_RUN);
        try {
            CertificationHandler handler = findCertificationHandler(certificationTypeType);

            AccessCertificationRunType newRunType = handler.createCertificationRunType(certificationTypeType, runType, task, result);
            addObject(newRunType, task, result);

            handler.recordRunStarted(newRunType, task, result);

            return newRunType;
        } finally {
            result.computeStatus();
        }
    }

    private void addObject(ObjectType objectType, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<?>> ops = modelService.executeChanges((Collection) Arrays.asList(objectDelta), null, task, result);
        ObjectDeltaOperation odo = ops.iterator().next();
        objectType.setOid(odo.getObjectDelta().getOid());
    }


    @Override
    public List<AccessCertificationCase> getCertificationCasesToDecide(String certifierOid, ObjectPaging paging) throws SchemaException {
        return null;
    }
}
