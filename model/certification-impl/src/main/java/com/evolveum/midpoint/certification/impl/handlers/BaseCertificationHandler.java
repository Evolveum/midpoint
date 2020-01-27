/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.certification.impl.AccCertExpressionHelper;
import com.evolveum.midpoint.certification.impl.AccCertGeneralHelper;
import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.certification.impl.AccCertReviewersHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;

/**
 * @author mederly
 */
@Component
public abstract class BaseCertificationHandler implements CertificationHandler {

    private static final Trace LOGGER = TraceManager.getTrace(BaseCertificationHandler.class);

    @Autowired protected PrismContext prismContext;
    @Autowired protected ModelService modelService;
    @Autowired @Qualifier("modelObjectResolver") protected ObjectResolver objectResolver;
    @Autowired protected CertificationManagerImpl certificationManager;
    @Autowired protected AccCertGeneralHelper helper;
    @Autowired protected AccCertResponseComputationHelper computationHelper;
    @Autowired protected AccCertReviewersHelper reviewersHelper;
    @Autowired protected AccCertExpressionHelper expressionHelper;

    // default implementation, depending only on the expressions provided
    public <F extends FocusType> Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<F> object, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public QName getDefaultObjectType() {
        return null;
    }

    // TODO move to some helper?
    void revokeAssignmentCase(AccessCertificationAssignmentCaseType assignmentCase,
            AccessCertificationCampaignType campaign, OperationResult caseResult, Task task)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        String objectOid = assignmentCase.getObjectRef().getOid();
        Class<? extends Objectable> clazz = ObjectTypes.getObjectTypeFromTypeQName(assignmentCase.getObjectRef().getType()).getClassDefinition();
        //noinspection unchecked
        PrismContainerValue<AssignmentType> cval = assignmentCase.getAssignment().asPrismContainerValue().clone();

        ContainerDelta assignmentDelta;
        if (Boolean.TRUE.equals(assignmentCase.isIsInducement())) {
            assignmentDelta = prismContext.deltaFactory().container().createModificationDelete(AbstractRoleType.F_INDUCEMENT, clazz,
                    cval);
        } else {
            assignmentDelta = prismContext.deltaFactory().container().createModificationDelete(FocusType.F_ASSIGNMENT, clazz, cval);
        }
        @SuppressWarnings({ "unchecked", "raw" })
        ObjectDelta<? extends ObjectType> objectDelta = (ObjectDelta<? extends ObjectType>) prismContext.deltaFactory().object().createModifyDelta(objectOid,
                Collections.singletonList(assignmentDelta), clazz);
        LOGGER.info("Going to execute delta: {}", objectDelta.debugDump());
        modelService.executeChanges(Collections.singletonList(objectDelta), null, task, caseResult);
        LOGGER.info("Case {} in {} ({} {} of {}) was successfully revoked",
                assignmentCase.asPrismContainerValue().getId(), ObjectTypeUtil.toShortString(campaign),
                Boolean.TRUE.equals(assignmentCase.isIsInducement()) ? "inducement":"assignment",
                cval.getId(), objectOid);
    }
}
