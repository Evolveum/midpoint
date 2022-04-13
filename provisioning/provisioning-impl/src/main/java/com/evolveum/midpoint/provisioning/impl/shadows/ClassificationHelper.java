/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier.Classification;
import com.evolveum.midpoint.provisioning.api.ShadowTagGenerator;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Helps with resource object classification, i.e. determining their kind, intent, and tag.
 */
@Component
@Experimental
class ClassificationHelper {

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final Trace LOGGER = TraceManager.getTrace(ClassificationHelper.class);

    /**
     * This is an externally-provided classifier implementation.
     * It is a temporary solution until classification is done purely in provisioning-impl. (If that will happen.)
     */
    private volatile ResourceObjectClassifier classifier;

    /**
     * This is an externally-provided shadow tag generator implementation.
     * It is a temporary solution until this process is done purely in provisioning-impl. (If that will happen.)
     */
    private volatile ShadowTagGenerator shadowTagGenerator;

    /**
     * Classifies the current shadow, based on information from the resource object.
     * As a result, the repository is updated.
     */
    void classify(ProvisioningContext ctx, PrismObject<ShadowType> shadow, PrismObject<ShadowType> resourceObject,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        if (classifier == null) { // Occurs when model-impl is not present, i.e. in tests.
            LOGGER.trace("No classifier. Skipping classification of {}/{}", shadow, resourceObject);
            return;
        }

        argCheck(shadow.getOid() != null, "Shadow has no OID");

        // The classifier code works with the "combined" version of resource object and its shadow.
        // This is NOT a full shadowization. Just good enough for the classifier to work.
        ShadowType combinedObject = combine(resourceObject, shadow);

        Classification classification = classifier.classify(
                combinedObject,
                ctx.getResource(),
                ctx.getTask(),
                result);

        ResourceObjectTypeDefinition definition = classification.getDefinition();
        if (definition == null) {
            LOGGER.trace("Classification was not successful for {}", shadow);
        } else if (isDifferent(classification, shadow)) {
            LOGGER.trace("New/updated classification of {} found: {}", shadow, classification);
            updateShadowClassificationAndTag(combinedObject, definition, ctx, result);
        } else {
            LOGGER.trace("No change in classification of {}: {}", shadow, classification);
        }
    }

    /**
     * The combination simply takes attributes from the resource object, and the rest from the shadow.
     * It is much simplified version of what is done in {@link ShadowedObjectConstruction}. We hope if will suffice for now.
     * In particular, we hope that the object class is roughly OK, and things like entitlement, credentials, and so on
     * are not needed.
     */
    private ShadowType combine(PrismObject<ShadowType> resourceObject, PrismObject<ShadowType> shadow)
            throws SchemaException {
        PrismObject<ShadowType> combined = shadow.clone();
        ResourceAttributeContainer fullAttributes = ShadowUtil.getAttributesContainer(resourceObject);
        if (fullAttributes != null) {
            combined.removeContainer(ShadowType.F_ATTRIBUTES);
            combined.add(fullAttributes.clone());
        }
        LOGGER.trace("Combined object:\n{}", combined.debugDumpLazily(1));
        return combined.asObjectable();
    }

    /** We update the tag as well, because it may depend on the object type. */
    private void updateShadowClassificationAndTag(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceObjectTypeDefinition definition,
            @NotNull ProvisioningContext ctx,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException {
        String tag = shadowTagGenerator != null ?
                shadowTagGenerator.generateTag(combinedObject, ctx.getResource(), definition, ctx.getTask(), result) :
                null;
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_KIND).replace(definition.getKind())
                .item(ShadowType.F_INTENT).replace(definition.getIntent())
                .item(ShadowType.F_TAG).replace(tag)
                .asItemDeltas();
        try {
            repositoryService.modifyObject(ShadowType.class, combinedObject.getOid(), itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when updating classification and tag");
        }
    }

    private boolean isDifferent(Classification classification, PrismObject<ShadowType> shadow) {
        return classification.getKind() != shadow.asObjectable().getKind()
                || !Objects.equals(classification.getIntent(), shadow.asObjectable().getIntent());
    }

    void setResourceObjectClassifier(ResourceObjectClassifier classifier) {
        this.classifier = classifier;
    }

    void setShadowTagGenerator(ShadowTagGenerator shadowTagGenerator) {
        this.shadowTagGenerator = shadowTagGenerator;
    }
}
