/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision.*;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDependencyTypeUtil.*;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDependencyTypeUtil.getDependencyStrictness;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDependencyTypeUtil.isForceLoadDependentShadow;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.util.PrettyPrinter.prettyPrint;
import static com.evolveum.midpoint.util.PrettyPrinter.prettyPrintLazily;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType.*;

import java.util.*;

import com.evolveum.midpoint.schema.util.ResourceObjectTypeDependencyTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class DependencyProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(DependencyProcessor.class);

    private static final String OP_SORT_PROJECTIONS_TO_WAVES = DependencyProcessor.class.getName() + ".sortProjectionsToWaves";

    @Autowired private ProvisioningService provisioningService;

    public <F extends ObjectType> void sortProjectionsToWaves(LensContext<F> context, Task task, OperationResult parentResult)
            throws PolicyViolationException, SchemaException, ConfigurationException {
        OperationResult result = parentResult.createMinorSubresult(OP_SORT_PROJECTIONS_TO_WAVES);
        try {

            // Create a snapshot of the projection collection at the beginning of computation.
            // The collection may be changed during computation (projections may be added). We do not want to process
            // these added projections. They are already processed inside the computation.
            // This also avoids ConcurrentModificationException
            LensProjectionContext[] projectionArray = context.getProjectionContexts().toArray(new LensProjectionContext[0]);

            // Reset incomplete flag for those contexts that are not yet computed
            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                if (projectionContext.getWave() < 0) {
                    projectionContext.setWaveIncomplete(true);
                }
            }

            for (LensProjectionContext projectionContext : projectionArray) {
                determineProjectionWave(projectionContext, null, new DependencyPath(), task, result);
                projectionContext.setWaveIncomplete(false);
            }

            logTheResult(context);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private <F extends ObjectType> void logTheResult(LensContext<F> context) {
        if (LOGGER.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                sb.append("\n");
                sb.append(projectionContext.getKey());
                sb.append(": ");
                sb.append(projectionContext.getWave());
            }
            LOGGER.trace("Projections sorted to waves (projection wave {}, execution wave {}):{}",
                    context.getProjectionWave(), context.getExecutionWave(), sb);
        }
    }

    private LensProjectionContext determineProjectionWave(
            LensProjectionContext projectionContext,
            ResourceObjectTypeDependencyType inDependency,
            DependencyPath depPath,
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        if (!projectionContext.isWaveIncomplete()) {
            // This was already processed
            return projectionContext;
        }
        if (projectionContext.isDelete()) {
            // When deprovisioning (deleting) the dependencies needs to be processed in reverse
            return determineProjectionWaveDeprovision(projectionContext, inDependency, depPath, task, result);
        } else {
            return determineProjectionWaveProvision(projectionContext, inDependency, depPath, task, result);
        }
    }

    /**
     * Determines the wave for projCtx. Returns either the same context, or its higher-order copy - to avoid
     * cycles among projection contexts when there are cycles among object types.
     */
    private LensProjectionContext determineProjectionWaveProvision(
            LensProjectionContext projCtx, // Context for which we compute wave
            ResourceObjectTypeDependencyType inDependency, // How we've got here (to projCtx)
            DependencyPath depPath, // to detect cycles in resolution
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        LensContext<?> context = projCtx.getLensContext();
        LOGGER.trace("Determining wave for (provision): {}; path: {}", lazy(projCtx::getHumanReadableName), depPath);
        int determinedWave = 0; // Computed as max(upstream.wave) + 1
        int determinedOrder = 0; // Order of the dependency that determined the final wave - this becomes an order of projCtx
        for (ResourceObjectTypeDependencyType outDependency : projCtx.getDependencies()) {
            LOGGER.trace("  processing dependency: {}", prettyPrintLazily(outDependency));
            if (inDependency != null && isHigherOrder(outDependency, inDependency)) {
                // There is incoming dependency. Deal only with dependencies of this order and lower
                // otherwise we can end up in endless loop even for legal dependencies.
                LOGGER.trace("  -> ignoring (higher order)");
                continue;
            }
            depPath.add(outDependency, projCtx);
            // Terminology: upstream means "upwards the stream of processing" - i.e. predecessor (~ independent);
            // downstream is successor in the stream of processing (~ dependent).
            LensProjectionContext upstreamCtx = findNearestUpstreamContext(context, outDependency);
            LOGGER.trace("  -> found nearest upstream context: {}", upstreamCtx);
            if (upstreamCtx == null || upstreamCtx.isDelete()) {
                ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(outDependency);
                if (strictness == STRICT) {
                    LOGGER.trace("  -> unsatisfied strict dependency");
                    throw new PolicyViolationException(
                            String.format("Unsatisfied strict dependency of [%s] dependent on [%s]: Account not provisioned",
                                    getDownstreamDescription(projCtx),
                                    getUpstreamDescription(outDependency, task, result)));
                } else if (strictness == LAX || strictness == RELAXED) {
                    LOGGER.trace("  -> unsatisfied {} dependency", strictness);
                    // independent (upstream) projection context not present, just ignore it
                    LOGGER.debug("Unsatisfied {} dependency of [{}] dependent on [{}]: dependency skipped",
                            strictness,
                            lazy(() -> getDownstreamDescription(projCtx)),
                            lazy(() -> getUpstreamDescription(outDependency, task, result)));
                } else {
                    throw new IllegalArgumentException(
                            "Unknown dependency strictness " + outDependency.getStrictness() + " in a dependency to "
                                    + getUpstreamDescription(outDependency, task, result));
                }
            } else {
                LOGGER.trace("  -> satisfied dependency");
                upstreamCtx = determineProjectionWave(upstreamCtx, outDependency, depPath, task, result);
                LOGGER.trace("    dependency projection wave: {}", upstreamCtx.getWave());
                if (upstreamCtx.getWave() + 1 > determinedWave) {
                    determinedWave = upstreamCtx.getWave() + 1;
                    determinedOrder = or0(outDependency.getOrder());
                    LOGGER.trace("    -> updated wave being computed for {}: {} (order={})",
                            projCtx, determinedWave, determinedOrder);
                }
            }
            depPath.remove();
        }
        LensProjectionContext resultCtx;
        if (projCtx.getWave() >= 0
                && projCtx.getWave() != determinedWave
                && projCtx.getOrder() != determinedOrder) {
            // Wave for this context was set during the run of this method (it was not set when we
            // started, we checked at the beginning). Therefore this context must have been visited again.
            // therefore there is a circular dependency. Therefore we need to create another context to split it.
            resultCtx = spawnWithNewOrder(projCtx, determinedOrder);
        } else {
            resultCtx = projCtx;
        }
        resultCtx.setWave(determinedWave);
        return resultCtx;
    }

    private String getDownstreamDescription(LensProjectionContext projectionContext) {
        return projectionContext.getKey().toHumanReadableDescription(false)
                + " resource " + projectionContext.getResourceName()
                + "(oid:" + projectionContext.getResourceOid() + ")";
    }

    private String getUpstreamDescription(ResourceObjectTypeDependencyType dependency, Task task, OperationResult result) {
        String resourceOid = getResourceOidRequired(dependency);
        String resourceName = getResourceName(resourceOid, task, result);
        return ResourceObjectTypeDependencyTypeUtil.describe(dependency, resourceName);
    }

    private String getResourceName(String resourceOid, Task task, OperationResult result) {
        try {
            var options = SchemaService.get().getOperationOptionsBuilder()
                    .noFetch()
                    .readOnly()
                    .allowNotFound()
                    .build();
            return provisioningService
                    .getObject(ResourceType.class, resourceOid, options, task, result)
                    .getName().getOrig();
        } catch (Exception e) {
            // Nothing critical here, so no re-throwing.
            LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't determine the name of resource {}", e, resourceOid);
            return null;
        }
    }

    private LensProjectionContext determineProjectionWaveDeprovision(
            LensProjectionContext projCtx,
            ResourceObjectTypeDependencyType inDependency,
            DependencyPath depPath,
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        LOGGER.trace("Determining wave for (deprovision): {}, path: {}", lazy(projCtx::getHumanReadableName), depPath);
        int determinedWave = 0;
        int determinedOrder = 0;

        // This needs to go in the reverse. We need to figure out who depends on us.
        for (DependencyAndSource ds : findReverseDependencies(projCtx)) {
            LensProjectionContext dependentCtx = ds.sourceProjectionContext; // ~ downstream (seen from the "provisioning" view)
            ResourceObjectTypeDependencyType outDependency = ds.dependency;
            if (inDependency != null && isHigherOrder(outDependency, inDependency)) {
                // There is incoming dependency. Deal only with dependencies of this order and lower
                // otherwise we can end up in endless loop even for legal dependencies.
                LOGGER.trace("  processing (reversed) dependency: {}: ignore (higher order)", prettyPrintLazily(outDependency));
                continue;
            }

            if (!dependentCtx.isDelete()) {
                ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(outDependency);
                if (strictness == STRICT) {
                    LOGGER.trace("  processing (reversed) dependency: {}: unsatisfied strict dependency",
                            prettyPrintLazily(outDependency));
                    throw new PolicyViolationException(
                            "Unsatisfied strict reverse dependency of account " + dependentCtx.getKey()
                                    + " dependent on " + projCtx.getKey()
                                    + ": Account is provisioned, but the account that it depends on is going to be deprovisioned");
                } else if (strictness == LAX || strictness == RELAXED) {
                    LOGGER.trace("  processing (reversed) dependency: {}: unsatisfied {} dependency",
                            prettyPrintLazily(outDependency), strictness);
                    // independent object not in the context, just ignore it
                    LOGGER.debug("Unsatisfied lax reversed dependency of account {} dependent on {}; dependency skipped",
                            dependentCtx.getKey(), projCtx.getKey());
                } else {
                    throw new IllegalArgumentException("Unknown dependency strictness " + outDependency.getStrictness()
                            + " in " + dependentCtx.getKey());
                }
            } else {
                LOGGER.trace("  processing (reversed) dependency: {}: satisfied", prettyPrintLazily(outDependency));
                depPath.add(outDependency, projCtx);
                dependentCtx =
                        determineProjectionWave(dependentCtx, outDependency, depPath, task, result);
                LOGGER.trace("    dependency projection wave: {}", dependentCtx.getWave());
                if (dependentCtx.getWave() + 1 > determinedWave) {
                    determinedWave = dependentCtx.getWave() + 1;
                    determinedOrder = or0(outDependency.getOrder());
                }
                LOGGER.trace("    determined dependency wave: {} (order={})", determinedWave, determinedOrder);
                depPath.remove();
            }
        }

        LensProjectionContext resultCtx;
        if (projCtx.getWave() >= 0
                && projCtx.getWave() != determinedWave
                && !projCtx.isDelete()) {
            // Wave for this context was set during the run of this method (it was not set when we
            // started, we checked at the beginning). Therefore this context must have been visited again.
            // therefore there is a circular dependency. Therefore we need to create another context to split it.
            resultCtx = spawnWithNewOrder(projCtx, determinedOrder);
        } else {
            resultCtx = projCtx;
        }
        resultCtx.setWave(determinedWave);
        return resultCtx;
    }

    /**
     * Returns all contexts that depend on provided `targetContext`:
     */
    private Collection<DependencyAndSource> findReverseDependencies(
            LensProjectionContext targetContext) throws SchemaException, ConfigurationException {
        Collection<DependencyAndSource> deps = new ArrayList<>();
        for (LensProjectionContext candidateSource : targetContext.getLensContext().getProjectionContexts()) {
            for (ResourceObjectTypeDependencyType dependency : candidateSource.getDependencies()) {
                if (targetContext.isDependencyTarget(dependency)) {
                    deps.add(
                            new DependencyAndSource(dependency, candidateSource));
                }
            }
        }
        return deps;
    }

    private boolean isHigherOrder(ResourceObjectTypeDependencyType a, ResourceObjectTypeDependencyType b) {
        return or0(a.getOrder()) > or0(b.getOrder());
    }

    private LensProjectionContext spawnWithNewOrder(LensProjectionContext origCtx, int newOrder) {
        LensProjectionContext newCtx =
                origCtx.getLensContext().createProjectionContext(
                        origCtx.getKey().withOrder(newOrder));
        newCtx.setResource(origCtx.getResource());
        // Force recon for the new context. This is a primitive way how to avoid phantom changes.
        newCtx.setDoReconciliation(true);
        return newCtx;
    }

    /**
     * Check that the dependencies are still satisfied. Also check for high-orders vs low-order operation consistency
     * and stuff like that.
     */
    boolean checkDependencies(LensProjectionContext projContext)
            throws PolicyViolationException, SchemaException, ConfigurationException {

        LOGGER.trace("checkDependencies starting for {}", projContext);

        if (projContext.isDelete()) {
            // It is OK if we depend on something that is not there if we are being removed ... for now
            LOGGER.trace("Context is (being) deleted -> no deps check");
            return true;
        }

        alignWithLowerOrderContext(projContext);
        return areDependenciesSatisfied(projContext);
    }

    /**
     * Setting OID and adapting the synchronization policy decision (if necessary).
     */
    private void alignWithLowerOrderContext(LensProjectionContext projContext) {
        if (projContext.getOid() != null && projContext.getSynchronizationPolicyDecision() != SynchronizationPolicyDecision.ADD) {
            LOGGER.trace("No need to align with lower-order context");
            return;
        }
        LensProjectionContext lowerOrderContext = findLowerOrderContextWithOid(projContext);
        if (lowerOrderContext == null) {
            LOGGER.trace("No relevant lower-order context with OID");
            return;
        }
        LOGGER.trace("Relevant lower-order context: {}", lowerOrderContext);
        if (projContext.getOid() == null) {
            LOGGER.trace("Setting OID based on lower-order context: {}", lowerOrderContext.getOid());
            projContext.setOid(lowerOrderContext.getOid());
        }
        if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD) {
            // This context cannot be ADD. There is a lower-order context with an OID
            // it means that the lower-order projection exists, we cannot add it twice
            LOGGER.trace("Switching policy decision from ADD to KEEP");
            projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
        }
        if (lowerOrderContext.isDelete()) { // TODO is this OK?
            LOGGER.trace("Lower-order context is (being) deleted, switching policy decision to DELETE");
            projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
        }
    }

    private LensProjectionContext findLowerOrderContextWithOid(LensProjectionContext projContext) {
        for (LensProjectionContext candidate : projContext.getLensContext().getProjectionContexts()) {
            if (candidate != projContext && candidate.isLowerOrderOf(projContext)) {
                if (candidate.getOid() != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the dependencies of the current projection contexts are satisfied.
     *
     * Throws a {@link PolicyViolationException} only if a strict dependency is not satisfied - but this
     * is something that should have been checked earlier, so it's actually not expected.
     */
    private boolean areDependenciesSatisfied(LensProjectionContext projContext)
            throws SchemaException, ConfigurationException, PolicyViolationException {
        LOGGER.trace("Checking whether dependencies of {} are satisfied", projContext);
        for (ResourceObjectTypeDependencyType dependency : projContext.getDependencies()) {
            if (!isDependencySatisfied(projContext, dependency)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDependencySatisfied(LensProjectionContext projContext, ResourceObjectTypeDependencyType dependency)
            throws PolicyViolationException {
        LOGGER.trace("Checking the following dependency: {}", prettyPrintLazily(dependency));
        LensContext<?> context = projContext.getLensContext();
        LensProjectionContext upstreamContext = findFirstUpstreamContext(projContext, dependency);
        LOGGER.trace("Upstream context found: {}", upstreamContext);
        ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(dependency);
        if (upstreamContext == null) {
            if (strictness == STRICT) {
                // This should not happen, it is checked before projection
                throw new PolicyViolationException("Unsatisfied strict dependency of "
                        + projContext.getKey().toHumanReadableDescription()
                        + " dependent on " + prettyPrint(dependency)
                        + ": upstream projection context was not found");
            } else if (strictness == LAX || strictness == RELAXED) {
                // independent object not in the context, just ignore it
                LOGGER.trace("Unsatisfied {} dependency of account {} dependent on {}; dependency skipped",
                        strictness, projContext.getKey().toHumanReadableDescription(), prettyPrintLazily(dependency));
            } else {
                throw new IllegalArgumentException(getUnknownStrictnessMessage(dependency));
            }
        } else {
            // We have the context of the object that we depend on. We need to check if it was provisioned.
            if (strictness == STRICT || strictness == RELAXED) {
                if (wasUpstreamContextProvisioned(upstreamContext, context.getExecutionWave())) {
                    // everything OK
                } else {
                    // We do not want to throw exception here. That would stop entire projection.
                    // Let's just mark the projection as broken and skip it.
                    LOGGER.warn("Unsatisfied dependency of {} dependent on {}: Projection not provisioned in dependency check"
                                    + " (execution wave {}, projection wave {}, dependency (upstream) projection wave {})",
                            projContext.getKey(), upstreamContext.getKey(), context.getExecutionWave(),
                            projContext.getWave(), upstreamContext.getWave());
                    projContext.setBroken();
                    return false;
                }
            } else if (strictness == LAX) {
                // we don't care what happened, just go on
            } else {
                throw new IllegalArgumentException(getUnknownStrictnessMessage(dependency));
            }
        }
        return true;
    }

    @NotNull
    private String getUnknownStrictnessMessage(ResourceObjectTypeDependencyType dependency) {
        return "Unknown dependency strictness " + dependency.getStrictness() + " in " + prettyPrint(dependency);
    }

    /**
     * This method is used when determining projection wave for provisioning.
     *
     * It resolves the dependency by looking for relevant upstream context.
     * We try to find one that has the largest order but not above the order of the dependency.
     *
     * TODO compare with {@link #findFirstUpstreamContext(LensProjectionContext, ResourceObjectTypeDependencyType)}
     */
    private LensProjectionContext findNearestUpstreamContext(
            LensContext<?> context, ResourceObjectTypeDependencyType dependency) {
        int dependencyOrder = or0(dependency.getOrder());
        return context.getProjectionContexts().stream()
                .filter(ctx -> ctx.getOrder() <= dependencyOrder)
                .filter(ctx -> !ctx.isGone()) // this is logical, and also it's present in pre-4.6 behavior
                .filter(ctx -> matches(ctx, dependency))
                .max(Comparator.comparingInt(LensProjectionContext::getOrder))
                .orElse(null);
    }

    /**
     * This method is used when checking the dependency satisfaction
     *
     * TODO method name
     */
    private LensProjectionContext findFirstUpstreamContext(
            @NotNull LensProjectionContext projContext,
            @NotNull ResourceObjectTypeDependencyType dependency) {
        return projContext.getLensContext().getProjectionContexts().stream()
                .filter(ctx -> ctx != projContext)
                .filter(ctx -> ctx.getOrder() <= projContext.getOrder()) // before 4.6, we strictly matched order=0, this is more logical, though
                .filter(ctx -> !ctx.isGone()) // this is questionable; but preserving pre-4.6 behavior
                .filter(ctx -> matches(ctx, dependency))
                .findFirst()
                .orElse(null);
    }

    public static boolean matches(LensProjectionContext ctx, ResourceObjectTypeDependencyType dependency) {
        return getResourceOidRequired(dependency).equals(ctx.getResourceOid())
                && getKindRequired(dependency) == ctx.getKind()
                && intentMatches(ctx, dependency)
                && ctx.getTag() == null; // This is how it was before 4.6.
    }

    private static boolean intentMatches(LensProjectionContext ctx, ResourceObjectTypeDependencyType dependency) {
        String explicitIntent = dependency.getIntent();
        if (explicitIntent != null) {
            return explicitIntent.equals(ctx.getKey().getIntent());
        } else {
            return ctx.isDefaultForKind(dependency.getKind());
        }
    }

    /**
     * Treats dependencies with "force load" flags: sets do-reconciliation flag for both source and target of such a dependency.
     */
    void preprocessDependencies(LensContext<?> context) throws SchemaException, ConfigurationException {

        // In the first wave we do not have enough information to preprocess contexts
        if (context.getExecutionWave() == 0) {
            return;
        }

        for (LensProjectionContext currentCtx : context.getProjectionContexts()) {
            if (!currentCtx.isCanProject()) {
                continue;
            }

            for (ResourceObjectTypeDependencyType dependency : currentCtx.getDependencies()) {
                ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(dependency);
                if (isForceLoadDependentShadow(dependency)
                        && (strictness == STRICT || strictness == RELAXED)) { // TODO why not when LAX?
                    // Before 4.6, we used RSD to match the contexts. It matched e.g. only contexts with order = 0.
                    // This one should be more appropriate.
                    LensProjectionContext upstreamCtx = findFirstUpstreamContext(currentCtx, dependency);
                    if (upstreamCtx != null
                            && upstreamCtx.isCanProject()
                            && !upstreamCtx.isDelete()
                            && wasExecuted(upstreamCtx)) {
                        upstreamCtx.setDoReconciliation(true);
                        currentCtx.setDoReconciliation(true);
                    }
                }
            }
        }
    }

    /**
     * Original comment (since 2014):
     *   Finally checks for all the dependencies. Some dependencies cannot be checked during wave computations as
     *   we might not have all activation decisions yet.
     *
     * However, for almost five years this method is called at end of each projection wave, i.e. not
     * only at the real end. (With the exception of previewChanges mode.) So let's keep executing
     * it in this way in both normal + preview modes.
     */
    <F extends ObjectType> void checkDependenciesFinal(LensContext<F> context)
            throws PolicyViolationException, SchemaException, ConfigurationException {

        LOGGER.trace("checkDependenciesFinal starting");

        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            checkDependencies(projCtx);
        }

        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx.isDelete()
                    || projCtx.getSynchronizationPolicyDecision() == UNLINK) {
                checkIfCanRemove(projCtx);
            }
        }
    }

    private boolean wasUpstreamContextProvisioned(LensProjectionContext dependentContext, int executionWave) {
        int depContextWave = dependentContext.getWave();
        if (depContextWave >= executionWave) {
            LOGGER.trace("Dependent context had no chance to be provisioned yet, so we assume it will be provisioned; "
                            + " its projection wave ({}) is >= current execution wave ({})", depContextWave, executionWave);
            return true;
        }

        // TODO are these checks relevant?
//        if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN
//                || projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
//            LOGGER.trace("wasProvisioned = false because of synchronizationPolicyDecision={}",
//                    projectionContext.getSynchronizationPolicyDecision());
//            return false;
//        }

        PrismObject<ShadowType> objectCurrent = dependentContext.getObjectCurrent();
        if (objectCurrent == null) {
            LOGGER.trace("wasUpstreamContextProvisioned = false because its objectCurrent is null");
            return false;
        }

        if (!dependentContext.isExists()) {
            LOGGER.trace("wasUpstreamContextProvisioned = false because its isExists is false");
            return false;
        }

        // This is getting tricky now. We cannot simply check for projectionContext.isExists() here.
        // entire projection is loaded with pointInTime=future. Therefore this does NOT
        // reflect actual situation. If there is a pending operation to create the object then
        // isExists will in fact be true even if the projection was not provisioned yet.
        // We need to check pending operations to see if there is pending add delta.
        if (hasPendingAddOperation(objectCurrent)) {
            LOGGER.trace("wasUpstreamContextProvisioned = false because there are pending add operations");
            return false;
        }

        return true;
    }

    private boolean hasPendingAddOperation(PrismObject<ShadowType> objectCurrent) {
        List<PendingOperationType> pendingOperations = objectCurrent.asObjectable().getPendingOperation();
        for (PendingOperationType pendingOperation: pendingOperations) {
            if (pendingOperation.getExecutionStatus() != PendingOperationExecutionStatusType.EXECUTING) {
                continue;
            }
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta == null) {
                continue;
            }
            if (delta.getChangeType() != ChangeTypeType.ADD) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean wasExecuted(LensProjectionContext accountContext) {
        if (accountContext.isAdd()) {
            return accountContext.getOid() != null &&
                    !accountContext.getExecutedDeltas().isEmpty();
        } else {
            return true;
        }
    }

    private void checkIfCanRemove(LensProjectionContext ctxBeingRemoved)
            throws SchemaException, ConfigurationException, PolicyViolationException {
        // It is OK if we depend on something that is not there if we are being removed
        // but we still need to check if others depends on me (~ are downstream relative to us)
        for (LensProjectionContext candidateDependentCtx : ctxBeingRemoved.getLensContext().getProjectionContexts()) {
            if (candidateDependentCtx.isDelete()
                    || candidateDependentCtx.getSynchronizationPolicyDecision() == UNLINK
                    || candidateDependentCtx.getSynchronizationPolicyDecision() == BROKEN
                    || candidateDependentCtx.getSynchronizationPolicyDecision() == IGNORE) {
                // If someone who is being deleted depends on us then it does not really matter
                continue;
            }
            for (ResourceObjectTypeDependencyType dependency : candidateDependentCtx.getDependencies()) {
                if (matches(ctxBeingRemoved, dependency) && isStrict(dependency)) {
                    throw new PolicyViolationException("Cannot remove " + ctxBeingRemoved.getHumanReadableName()
                            + " because " + candidateDependentCtx.getHumanReadableName() + " depends on it");
                }
            }
        }
    }

    static class DependencyAndSource {
        @NotNull private final ResourceObjectTypeDependencyType dependency;
        @NotNull private final LensProjectionContext sourceProjectionContext;

        DependencyAndSource(
                @NotNull ResourceObjectTypeDependencyType dependency, @NotNull LensProjectionContext sourceProjectionContext) {
            this.dependency = dependency;
            this.sourceProjectionContext = sourceProjectionContext;
        }
    }

    static class DependencyPath {

        private final Deque<ResourceObjectTypeDependencyType> depPath = new ArrayDeque<>();

        public void add(ResourceObjectTypeDependencyType dependency, LensProjectionContext projCtx)
                throws PolicyViolationException {
            checkForCircular(dependency, projCtx);
            depPath.addLast(dependency);
        }

        public void remove() {
            depPath.removeLast();
        }

        private void checkForCircular(
                ResourceObjectTypeDependencyType outDependency,
                LensProjectionContext projectionContext) throws PolicyViolationException {
            for (ResourceObjectTypeDependencyType pathElement : depPath) {
                if (pathElement.equals(outDependency)) {
                    throw new PolicyViolationException(
                            "Circular dependency in " + projectionContext.getHumanReadableName()
                                    + ", path: " + getPathDescription());
                }
            }
        }

        private String getPathDescription() {
            StringBuilder sb = new StringBuilder();
            Iterator<ResourceObjectTypeDependencyType> iterator = depPath.iterator();
            while (iterator.hasNext()) {
                ResourceObjectTypeDependencyType el = iterator.next();
                ObjectReferenceType resourceRef = el.getResourceRef();
                if (resourceRef != null) {
                    sb.append(resourceRef.getOid());
                }
                sb.append("(").append(el.getKind()).append("/");
                sb.append(el.getIntent()).append(")");
                if (iterator.hasNext()) {
                    sb.append("->");
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.valueOf(depPath);
        }
    }
}
