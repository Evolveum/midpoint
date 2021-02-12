package com.evolveum.midpoint.provisioning.impl.shadowcache;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FETCH_RESULT;

import java.util.Objects;

import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;

import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.FetchedResourceObject;
import com.evolveum.midpoint.provisioning.impl.shadowcache.sync.SkipProcessingException;
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
 * Represents object fetched from resource "adopted" by connecting with repo shadow (updating the shadow if necessary).
 *
 * Currently limited to objects retrieved by `searchObjects` method call.
 *
 * The extension to objects retrieved by `getObject` will require some tweaks: such objects are referenced by shadow OID,
 * so we first obtain a shadow, then resource object, and only after that we update the shadow.
 *
 * TODO reconsider naming: Currently we use the term "adopted [resource] object" for both this structure
 *  and embedded PrismObject of ShadowType (holding resource object + shadow combination).
 */
public class AdoptedResourceObject implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(AdoptedResourceObject.class);

    /**
     * The resource object as obtained from the resource object converter. It has no connection to the repo.
     */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /** TODO */
    private final Object primaryIdentifierValue;

    /**
     * The object after "shadowization". TODO
     */
    private PrismObject<ShadowType> adoptedObject;

    /** State of the processing. */
    private final ProcessingState processingState;

    /** Information used to initialize this object. */
    @NotNull private final InitializationContext ictx;

    public AdoptedResourceObject(FetchedResourceObject fetchedResourceObject, LocalBeans localBeans, ProvisioningContext ctx) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "adoptedObject", adoptedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "processingState", String.valueOf(processingState), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AdoptedResourceObject{" +
                "resourceObject=" + resourceObject +
                ", primaryIdentifierValue=" + primaryIdentifierValue +
                ", adoptedObject=" + adoptedObject +
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

        adoptedObject = adoptResourceObject(estimatedShadowCtx, result);
    }

    /**
     * The object is somehow flawed. However, we should try to create some shadow.
     */
    @Override
    public void skipInitialization(Task task, OperationResult result) throws CommonException, SkipProcessingException,
            EncryptionException {
        // TODO create the shadow!
    }

    @NotNull
    private PrismObject<ShadowType> adoptResourceObject(ProvisioningContext estimatedShadowCtx,
            OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {

        AdoptionHelper adoptionHelper = getAdoptionHelper();
        ShadowManager shadowManager = getShadowManager();

        PrismObject<ShadowType> repoShadow = adoptionHelper.acquireRepoShadow(estimatedShadowCtx, resourceObject, result);

        // This determines the definitions exactly. Now the repo shadow should have proper kind/intent
        ProvisioningContext shadowCtx = ictx.localBeans.shadowCaretaker.applyAttributesDefinition(ictx.ctx, repoShadow);
        // TODO: shadowState
        PrismObject<ShadowType> updatedRepoShadow = shadowManager.updateShadow(shadowCtx, resourceObject, null, repoShadow, null, result);

        // TODO do we want also to futurize the shadow like in getObject?

        return adoptionHelper.constructAdoptedObject(shadowCtx, updatedRepoShadow, resourceObject, result);
    }

    private ShadowManager getShadowManager() {
        return ictx.localBeans.shadowManager;
    }

    private AdoptionHelper getAdoptionHelper() {
        return ictx.localBeans.adoptionHelper;
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
        if (adoptedObject != null) {
            ProvisioningUtil.validateShadow(adoptedObject, true);
        } else {
            ProvisioningUtil.validateShadow(resourceObject, false);
        }
    }

    public @NotNull PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public PrismObject<ShadowType> getAdoptedObject() {
        return adoptedObject;
    }

    public @NotNull PrismObject<ShadowType> getAdoptedOrOriginalObject() {
        return MoreObjects.firstNonNull(adoptedObject, resourceObject);
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

        private final LocalBeans localBeans;
        private final ProvisioningContext ctx;

        private InitializationContext(LocalBeans localBeans, ProvisioningContext ctx) {
            this.localBeans = localBeans;
            this.ctx = ctx;
        }
    }
}
