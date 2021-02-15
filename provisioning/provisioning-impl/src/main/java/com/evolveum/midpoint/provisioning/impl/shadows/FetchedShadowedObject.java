package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.getClassWithMessage;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FETCH_RESULT;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.FetchedResourceObject;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Represents an object fetched from resource "shadowed" by connecting with repo shadow (updating the shadow if necessary).
 *
 * Currently limited to objects retrieved by `searchObjects` method call.
 *
 * The extension to objects retrieved by `getObject` will require some tweaks: such objects are referenced by shadow OID,
 * so we first obtain a shadow, then resource object, and only after that we update the shadow.
 */
public class FetchedShadowedObject implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(FetchedShadowedObject.class);

    /**
     * The resource object as obtained from the resource object converter. It has no connection to the repo.
     */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /** TODO */
    private final Object primaryIdentifierValue;

    /**
     * The object after "shadowization". TODO
     */
    private PrismObject<ShadowType> shadowedObject;

    /** State of the processing. */
    private final ProcessingState processingState;

    /** Information used to initialize this object. */
    @NotNull private final InitializationContext ictx;

    public FetchedShadowedObject(FetchedResourceObject fetchedResourceObject, ShadowsLocalBeans localBeans, ProvisioningContext ctx) {
        this.resourceObject = fetchedResourceObject.getResourceObject();
        this.primaryIdentifierValue = fetchedResourceObject.getPrimaryIdentifierValue();
        this.processingState = ProcessingState.fromLowerLevelState(fetchedResourceObject.getProcessingState());
        this.ictx = new InitializationContext(localBeans, ctx);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", shadowedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "processingState", String.valueOf(processingState), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", primaryIdentifierValue=" + primaryIdentifierValue +
                ", shadowedObject=" + shadowedObject +
                ", processingState=" + processingState +
                '}';
    }

    /**
     * Contains processing of an object that has been found on a resource before it is passed to the caller-provided handler.
     * We do basically the following:
     *
     * 1. apply definitions,
     * 2. acquire and update repo shadow (includes classification),
     * 3. construct resulting adopted object.
     */

    @Override
    public void initializeInternal(Task task, OperationResult result)
            throws CommonException, SkipProcessingException, EncryptionException {

        // The shadow does not have any kind or intent at this point.
        // But at least locate the definition using object classes.
        ProvisioningContext estimatedShadowCtx = ictx.localBeans.shadowCaretaker.reapplyDefinitions(ictx.ctx, resourceObject);

        PrismObject<ShadowType> repoShadow;
        try {
            repoShadow = ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(estimatedShadowCtx, resourceObject, false, result);
        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LOGGER.error("Couldn't acquire shadow for {}. Creating shadow in emergency mode. Error: {}", resourceObject, getClassWithMessage(e));
            shadowedObject = shadowResourceObjectInEmergency(result);
            throw e;
        }

        try {
            // This determines the definitions exactly. Now the repo shadow should have proper kind/intent
            ProvisioningContext shadowCtx = ictx.localBeans.shadowCaretaker.applyAttributesDefinition(ictx.ctx, repoShadow);

            // TODO: shadowState
            PrismObject<ShadowType> updatedRepoShadow = ictx.localBeans.shadowManager
                    .updateShadow(shadowCtx, resourceObject, null, repoShadow, null, result);

            // TODO do we want also to futurize the shadow like in getObject?

            shadowedObject = ictx.localBeans.shadowedObjectConstructionHelper
                    .constructShadowedObject(shadowCtx, updatedRepoShadow, resourceObject, result);

        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LOGGER.error("Couldn't initialize {}. Continuing with previously acquired repo shadow: {}. Error: {}",
                    resourceObject, repoShadow, getClassWithMessage(e));
            shadowedObject = repoShadow;
            throw e;
        }
    }

    /**
     * The object is somehow flawed. However, we should try to create some shadow.
     *
     * To avoid any harm, we are minimalistic here: If a shadow can be found, it is used "as is". No updates here.
     * If it cannot be found, it is created. We will skip kind/intent/tag determination. Most probably these would not be
     * correct anyway.
     */
    @Override
    public void skipInitialization(Task task, OperationResult result) throws CommonException, SkipProcessingException,
            EncryptionException {
        shadowedObject = shadowResourceObjectInEmergency(result);
    }

    @NotNull
    private PrismObject<ShadowType> shadowResourceObjectInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        LOGGER.trace("Acquiring repo shadow in emergency:\n{}", DebugUtil.debugDumpLazily(resourceObject, 1));
        try {
            return ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(ictx.ctx, resourceObject, true, result);
        } catch (Exception e) {
            shadowedObject = shadowResourceObjectInUltraEmergency(result);
            throw e;
        }
    }

    /**
     * Something prevents us from creating a shadow (most probably). Let us be minimalistic, and create
     * a shadow having only the primary identifier.
     */
    private PrismObject<ShadowType> shadowResourceObjectInUltraEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        PrismObject<ShadowType> minimalResourceObject = minimize(resourceObject);
        LOGGER.trace("Minimal resource object to acquire a shadow for:\n{}",
                DebugUtil.debugDumpLazily(minimalResourceObject, 1));
        if (minimalResourceObject != null) {
            return ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(ictx.ctx, minimalResourceObject, true, result);
        } else {
            return null;
        }
    }

    private PrismObject<ShadowType> minimize(PrismObject<ShadowType> resourceObject) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> minimized = resourceObject.clone();
        RefinedObjectClassDefinition ocDef = ictx.ctx.getObjectClassDefinition();
        ShadowUtil.removeAllAttributesExceptPrimaryIdentifier(minimized, ocDef);
        if (ShadowUtil.hasPrimaryIdentifier(minimized, ocDef)) {
            return minimized;
        } else {
            return null;
        }
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public ProcessingState getProcessingState() {
        return processingState;
    }

    @Override
    public void checkConsistence() {
        if (shadowedObject != null) {
            ProvisioningUtil.validateShadow(shadowedObject, true);
        } else {
            ProvisioningUtil.validateShadow(resourceObject, false);
        }
    }

    public @NotNull PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public PrismObject<ShadowType> getShadowedObject() {
        return shadowedObject;
    }

    public @NotNull PrismObject<ShadowType> getAdoptedOrOriginalObject() {
        return MoreObjects.firstNonNull(shadowedObject, resourceObject);
    }

    // TEMPORARY (for migration)
    public @NotNull PrismObject<ShadowType> getResourceObjectWithFetchResult() {
        if (processingState.isInitialized()) {
            return getAdoptedOrOriginalObject();
        } else {
            PrismObject<ShadowType> mostRelevantObject = getAdoptedOrOriginalObject();
            PrismObject<ShadowType> clone = mostRelevantObject.clone();
            if (clone.getName() == null) {
                if (CollectionUtils.isEmpty(ShadowUtil.getPrimaryIdentifiers(clone))) {
                    // HACK HACK HACK
                    clone.asObjectable().setName(PolyStringType.fromOrig(String.valueOf(primaryIdentifierValue)));
                } else {
                    try {
                        PolyString name = ShadowUtil.determineShadowName(clone);
                        if (name != null) {
                            clone.asObjectable().setName(new PolyStringType(name));
                        }
                    } catch (SchemaException e) {
                        LOGGER.warn("Couldn't determine the name for {}", clone, e);
                    }
                }
            }
            OperationResult result = new OperationResult("adoptObject"); // TODO HACK HACK HACK
            Throwable exceptionEncountered = processingState.getExceptionEncountered();
            // TODO HACK HACK
            result.recordFatalError(Objects.requireNonNullElseGet(
                    exceptionEncountered, () -> new IllegalStateException("Object was not initialized")));
            ObjectTypeUtil.recordFetchError(clone, result);
            return clone;
        }
    }

    // Maybe temporary
    public PrismObject<ShadowType> getResultingObject(FetchErrorReportingMethodType errorReportingMethod) {
        Throwable exception = processingState.getExceptionEncountered();
        if (exception == null) {
            return getAdoptedOrOriginalObject();
        } else if (errorReportingMethod != FETCH_RESULT) {
            throw new TunnelException(exception);
        } else {
            PrismObject<ShadowType> resultingObject = getResourceObjectWithFetchResult();
            LOGGER.error("An error occurred while processing resource object {}. Recording it into object "
                    + "fetch result: {}", resultingObject, exception.getMessage(), exception);
            return resultingObject;
        }
    }

    private static class InitializationContext {

        private final ShadowsLocalBeans localBeans;
        private final ProvisioningContext ctx;

        private InitializationContext(ShadowsLocalBeans localBeans, ProvisioningContext ctx) {
            this.localBeans = localBeans;
            this.ctx = ctx;
        }
    }
}
