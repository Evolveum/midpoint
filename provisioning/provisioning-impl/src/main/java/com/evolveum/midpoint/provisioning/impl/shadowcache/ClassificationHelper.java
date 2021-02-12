package com.evolveum.midpoint.provisioning.impl.shadowcache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier.Classification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
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

    public void setResourceObjectClassifier(ResourceObjectClassifier classifier) {
        this.classifier = classifier;
    }

    public void classify(ProvisioningContext ctx, PrismObject<ShadowType> shadow, PrismObject<ShadowType> resourceObject,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (classifier == null) {
            LOGGER.trace("No classifier. Skipping classification of {}/{}", shadow, resourceObject);
            return;
        }

        argCheck(shadow.getOid() != null, "Shadow has no OID");

        Classification classification = classifier.classify(resourceObject, null, null, ctx.getTask(), result);
        if (isDifferent(classification, shadow)) {
            LOGGER.trace("New/updated classification of {} found: {}", shadow, classification);
            updateShadow(shadow, classification, result);
        } else {
            LOGGER.trace("No change in classification of {}: {}", shadow, classification);
        }
    }

    private void updateShadow(PrismObject<ShadowType> shadow, Classification classification, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_KIND).replace(classification.getKind())
                .item(ShadowType.F_INTENT).replace(classification.getIntent())
                .asItemDeltas();
        try {
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException(e); // there is no rename, so no conflict should be there
        }
    }

    private boolean isDifferent(Classification classification, PrismObject<ShadowType> shadow) {
        return classification.getKind() != shadow.asObjectable().getKind() ||
                Objects.equals(classification.getIntent(), shadow.asObjectable().getIntent());
    }

    public boolean needsClassification(PrismObject<ShadowType> shadow) {
        ShadowKindType kind = shadow.asObjectable().getKind();
        String intent = shadow.asObjectable().getIntent();
        return kind == null || kind == ShadowKindType.UNKNOWN || SchemaConstants.INTENT_UNKNOWN.equals(intent);
    }
}
