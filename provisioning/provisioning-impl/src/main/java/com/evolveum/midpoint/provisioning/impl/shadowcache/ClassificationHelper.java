package com.evolveum.midpoint.provisioning.impl.shadowcache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier.Classification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

@Component
@Experimental
class ClassificationHelper {

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final Trace LOGGER = TraceManager.getTrace(ClassificationHelper.class);

    /**
     * This is an externally-provided classifier implementation.
     * It is a temporary solution until classification is done purely in provisioning-impl.
     */
    private volatile ResourceObjectClassifier classifier;

    /**
     * Checks if the shadow needs the classification. This is to avoid e.g. needless preparation of the resource object.
     */
    public boolean needsClassification(PrismObject<ShadowType> shadow) {
        return ShadowUtil.isNotKnown(shadow.asObjectable().getKind())
                || ShadowUtil.isNotKnown(shadow.asObjectable().getIntent());
    }

    /**
     * Classifies the current shadow, based on information from the resource object.
     * As a result, the repository is updated.
     */
    public void classify(ProvisioningContext ctx, PrismObject<ShadowType> shadow, PrismObject<ShadowType> resourceObject,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        if (classifier == null) { // Occurs when model-impl is not present, i.e. in tests.
            LOGGER.trace("No classifier. Skipping classification of {}/{}", shadow, resourceObject);
            return;
        }

        argCheck(shadow.getOid() != null, "Shadow has no OID");

        Classification classification = classifier.classify(resourceObject, ctx.getResource().asPrismObject(), shadow,
                ctx.getTask(), result);

        if (isDifferent(classification, shadow)) {
            LOGGER.trace("New/updated classification of {} found: {}", shadow, classification);
            updateShadowClassification(shadow, classification, result);
        } else {
            LOGGER.trace("No change in classification of {}: {}", shadow, classification);
        }
    }

    private void updateShadowClassification(PrismObject<ShadowType> shadow, Classification classification, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_KIND).replace(classification.getKind())
                .item(ShadowType.F_INTENT).replace(classification.getIntent())
                .item(ShadowType.F_TAG).replace(classification.getTag())
                .asItemDeltas();
        try {
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException(e); // there is no rename, so no conflict should be there
        }
    }

    private boolean isDifferent(Classification classification, PrismObject<ShadowType> shadow) {
        return classification.getKind() != shadow.asObjectable().getKind() ||
                !Objects.equals(classification.getIntent(), shadow.asObjectable().getIntent()) ||
                !Objects.equals(classification.getTag(), shadow.asObjectable().getTag());
    }

    public void setResourceObjectClassifier(ResourceObjectClassifier classifier) {
        this.classifier = classifier;
    }
}
