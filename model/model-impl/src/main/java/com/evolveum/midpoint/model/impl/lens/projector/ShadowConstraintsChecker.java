/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.schema.processor.CompositeObjectDefinition;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author semancik
 *
 */
public class ShadowConstraintsChecker<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowConstraintsChecker.class);

    private final LensProjectionContext projectionContext;
    private LensContext<F> context;
    private PrismContext prismContext;
    private ProvisioningService provisioningService;
    private boolean satisfiesConstraints;
    private ConstraintsCheckingResult constraintsCheckingResult;

    ShadowConstraintsChecker(LensProjectionContext accountContext) {
        this.projectionContext = accountContext;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    public void setProvisioningService(ProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    public LensContext<F> getContext() {
        return context;
    }

    public void setContext(LensContext<F> context) {
        this.context = context;
    }

    boolean isSatisfiesConstraints() {
        return satisfiesConstraints;
    }

    public String getMessages() {
        return constraintsCheckingResult.getMessages();
    }

    PrismObject<ShadowType> getConflictingShadow() {
        //noinspection unchecked
        return constraintsCheckingResult.getConflictingShadow();
    }

    public void check(Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        CompositeObjectDefinition projOcDef = projectionContext.getCompositeObjectDefinition();
        PrismObject<ShadowType> projectionNew = projectionContext.getObjectNew();
        if (projectionNew == null) {
            // This must be delete
            LOGGER.trace("No new object in projection context. Current shadow satisfy constraints");
            satisfiesConstraints = true;
            return;
        }

        PrismContainer<?> attributesContainer = projectionNew.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            // No attributes no constraint violations
            LOGGER.trace("Current shadow does not contain attributes, skipping checking uniqueness.");
            satisfiesConstraints = true;
            return;
        }

        ConstraintViolationConfirmer confirmer = (conflictingShadowCandidate) -> {
            // If the conflicting shadow is gone, we can (almost) safely assume this is NOT a real conflict (uniqueness violation)
            // But we must inspect all projection contexts. TODO - can we be really sure then?
            String candidateOid = conflictingShadowCandidate.getOid();
            List<LensProjectionContext> matchingContexts = context.findProjectionContextsByOid(candidateOid);
            if (matchingContexts.isEmpty()) {
                LOGGER.trace("No contexts found for {}; this looks like a real constraint violation", candidateOid);
                return true;
            }
            if (matchingContexts.stream().allMatch(LensProjectionContext::isGoneOrReaping)) {
                LOGGER.trace("All {} context(s) of {} are gone or being reaped. This looks like a phantom constraint violation",
                        matchingContexts.size(), candidateOid);
                return false;
            } else {
                LOGGER.trace("There are {} context(s) for {}, not all gone or being reaped. Confirming the constraint violation.",
                        matchingContexts.size(), candidateOid);
                return true;
            }
        };

        constraintsCheckingResult = provisioningService.checkConstraints(
                projOcDef,
                projectionNew,
                projectionContext.getObjectOld(),
                projectionContext.getResource(),
                projectionContext.getOid(),
                confirmer,
                context.getProjectionConstraintsCheckingStrategy(),
                task, result);

        if (constraintsCheckingResult.isSatisfiesConstraints()) {
            satisfiesConstraints = true;
            return;
        }
        for (QName checkedAttributeName: constraintsCheckingResult.getCheckedAttributes()) {
            if (constraintsCheckingResult.getConflictingAttributes().contains(checkedAttributeName)) {
                if (isInDelta(checkedAttributeName, projectionContext.getPrimaryDelta())) {
                    throw new ObjectAlreadyExistsException("Attribute " + checkedAttributeName
                            + " conflicts with existing object (and it is present in primary"
                            + " account delta therefore no iteration is performed)");
                }
            }
        }
        //noinspection RedundantIfStatement
        if (projectionContext.isGone()) {
            satisfiesConstraints = true;
        } else {
            satisfiesConstraints = false;
        }
    }


    private boolean isInDelta(QName attrName, ObjectDelta<ShadowType> delta) {
        if (delta == null) {
            return false;
        }
        return delta.hasItemDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, attrName));
    }

}
