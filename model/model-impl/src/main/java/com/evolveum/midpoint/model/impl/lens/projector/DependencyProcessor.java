/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.getDependencyStrictness;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isForceLoadDependentShadow;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.util.PrettyPrinter.prettyPrintLazily;

import java.util.*;

import com.evolveum.midpoint.model.api.context.ProjectionContextFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
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
                determineProjectionWave(context, projectionContext, null, null, task, result);
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

    private <F extends ObjectType> LensProjectionContext determineProjectionWave(
            LensContext<F> context,
            LensProjectionContext projectionContext,
            ResourceObjectTypeDependencyType inDependency,
            Deque<ResourceObjectTypeDependencyType> depPath,
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        if (!projectionContext.isWaveIncomplete()) {
            // This was already processed
            return projectionContext;
        }
        if (projectionContext.isDelete()) {
            // When deprovisioning (deleting) the dependencies needs to be processed in reverse
            return determineProjectionWaveDeprovision(context, projectionContext, inDependency, depPath, task, result);
        } else {
            return determineProjectionWaveProvision(context, projectionContext, inDependency, depPath, task, result);
        }
    }

    private <F extends ObjectType> LensProjectionContext determineProjectionWaveProvision(
            LensContext<F> context,
            LensProjectionContext projCtx,
            ResourceObjectTypeDependencyType inDependency,
            Deque<ResourceObjectTypeDependencyType> depPath,
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        if (depPath == null) {
            depPath = new ArrayDeque<>();
        }
        LOGGER.trace("Determining wave for (provision): {}; path: {}", lazy(projCtx::getHumanReadableName), depPath);
        int determinedWave = 0;
        int determinedOrder = 0;
        for (ResourceObjectTypeDependencyType outDependency: projCtx.getDependencies()) {
            LOGGER.trace("  processing dependency: {}", prettyPrintLazily(outDependency));
            if (inDependency != null && isHigherOrder(outDependency, inDependency)) {
                // There is incoming dependency. Deal only with dependencies of this order and lower
                // otherwise we can end up in endless loop even for legal dependencies.
                LOGGER.trace("  -> ignoring (higher order)");
                continue;
            }
            checkForCircular(depPath, outDependency, projCtx);
            depPath.addLast(outDependency);
            ProjectionContextFilter upstreamCtxFilter = ProjectionContextFilter.fromDependency(outDependency);
            LensProjectionContext upstreamCtx = findUpstreamContext(context, upstreamCtxFilter, or0(outDependency.getOrder()));
            LOGGER.trace("  -> filter: {}, found upstream context: {}", upstreamCtxFilter, upstreamCtx);
            if (upstreamCtx == null || upstreamCtx.isDelete()) {
                ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(outDependency);
                if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT) {
                    LOGGER.trace("  -> unsatisfied strict dependency");
                    throw new PolicyViolationException(
                            "Unsatisfied strict dependency of [" + getDownstreamDescription(projCtx) + "] dependent on ["
                                    + getUpstreamDescription(upstreamCtxFilter, task, result) + "]: Account not provisioned");
                } else if (strictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
                    LOGGER.trace("  -> unsatisfied lax dependency");
                    // independent object not in the context, just ignore it
                    LOGGER.debug("Unsatisfied lax dependency of [{}] dependent on [{}]: dependency skipped",
                            lazy(() -> getDownstreamDescription(projCtx)),
                            lazy(() -> getUpstreamDescription(upstreamCtxFilter, task, result)));
                } else if (strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
                    LOGGER.trace("  -> unsatisfied relaxed dependency");
                    // independent object not in the context, just ignore it
                    LOGGER.debug("Unsatisfied relaxed dependency of [{}] dependent on [{}}]: dependency skipped",
                            lazy(() -> getDownstreamDescription(projCtx)),
                            lazy(() -> getUpstreamDescription(upstreamCtxFilter, task, result)));
                } else {
                    throw new IllegalArgumentException(
                            "Unknown dependency strictness " + outDependency.getStrictness() + " in a dependency to "
                                    + getUpstreamDescription(upstreamCtxFilter, task, result));
                }
            } else {
                LOGGER.trace("  -> satisfied dependency");
                upstreamCtx =
                        determineProjectionWave(context, upstreamCtx, outDependency, depPath, task, result);
                LOGGER.trace("    dependency projection wave: {}", upstreamCtx.getWave());
                if (upstreamCtx.getWave() + 1 > determinedWave) {
                    determinedWave = upstreamCtx.getWave() + 1;
                    determinedOrder = or0(outDependency.getOrder());
                }
                LOGGER.trace("    -> determined dependency wave: {} (order={})", determinedWave, determinedOrder);
            }
            depPath.removeLast();
        }
        LensProjectionContext resultCtx = projCtx;
        if (projCtx.getWave() >= 0 && projCtx.getWave() != determinedWave) {
            // Wave for this context was set during the run of this method (it was not set when we
            // started, we checked at the beginning). Therefore this context must have been visited again.
            // therefore there is a circular dependency. Therefore we need to create another context to split it.
            if (projCtx.getOrder() != determinedOrder) {
                resultCtx = spawnWithNewOrder(context, projCtx, determinedOrder);
            }
        }
        resultCtx.setWave(determinedWave);
        return resultCtx;
    }

    private String getDownstreamDescription(LensProjectionContext projectionContext) {
        return projectionContext.getKey().toHumanReadableDescription(false)
                + " resource " + projectionContext.getResourceName()
                + "(oid:" + projectionContext.getResourceOid() + ")";
    }

    private String getUpstreamDescription(
            ProjectionContextFilter filter, Task task, OperationResult result) {
        String resourceName = getResourceName(filter.getResourceOid(), task, result);
        if (resourceName == null) {
            return filter.toHumanReadableDescription(true);
        } else {
            return filter.toHumanReadableDescription(false)
                    + " resource " + resourceName
                    + "(oid:" + filter.getResourceOid() + ")";
        }
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

    private <F extends ObjectType> LensProjectionContext determineProjectionWaveDeprovision(
            LensContext<F> context,
            LensProjectionContext projCtx,
            ResourceObjectTypeDependencyType inDependency,
            Deque<ResourceObjectTypeDependencyType> depPath,
            Task task,
            OperationResult result) throws PolicyViolationException, SchemaException, ConfigurationException {
        if (depPath == null) {
            depPath = new ArrayDeque<>();
        }
        LOGGER.trace("Determining wave for (deprovision): {}, path: {}", lazy(projCtx::getHumanReadableName), depPath);
        int determinedWave = 0;
        int determinedOrder = 0;

        // This needs to go in the reverse. We need to figure out who depends on us.
        for (DependencyAndSource ds : findReverseDependencies(context, projCtx)) {
            LensProjectionContext dependencySourceContext = ds.sourceProjectionContext;
            ResourceObjectTypeDependencyType outDependency = ds.dependency;
            if (inDependency != null && isHigherOrder(outDependency, inDependency)) {
                // There is incoming dependency. Deal only with dependencies of this order and lower
                // otherwise we can end up in endless loop even for legal dependencies.
                LOGGER.trace("  processing (reversed) dependency: {}: ignore (higher order)", prettyPrintLazily(outDependency));
                continue;
            }

            if (!dependencySourceContext.isDelete()) {
                ResourceObjectTypeDependencyStrictnessType outDependencyStrictness = getDependencyStrictness(outDependency);
                if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.STRICT) {
                    LOGGER.trace("  processing (reversed) dependency: {}: unsatisfied strict dependency",
                            prettyPrintLazily(outDependency));
                    throw new PolicyViolationException(
                            "Unsatisfied strict reverse dependency of account " + dependencySourceContext.getKey()
                                    + " dependent on " + projCtx.getKey()
                                    + ": Account is provisioned, but the account that it depends on is going to be deprovisioned");
                } else if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
                    LOGGER.trace("  processing (reversed) dependency: {}: unsatisfied lax dependency",
                            prettyPrintLazily(outDependency));
                    // independent object not in the context, just ignore it
                    LOGGER.debug("Unsatisfied lax reversed dependency of account {} dependent on {}; dependency skipped",
                            dependencySourceContext.getKey(),
                            projCtx.getKey());
                } else if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
                    LOGGER.trace("  processing (reversed) dependency: {}: unsatisfied relaxed dependency",
                            prettyPrintLazily(outDependency));
                    // independent object not in the context, just ignore it
                    LOGGER.debug("Unsatisfied relaxed dependency of account {} dependent on {}; dependency skipped",
                            dependencySourceContext.getKey(),
                            projCtx.getKey());
                } else {
                    throw new IllegalArgumentException("Unknown dependency strictness " + outDependency.getStrictness()
                            + " in " + dependencySourceContext.getKey());
                }
            } else {
                LOGGER.trace("  processing (reversed) dependency: {}: satisfied", prettyPrintLazily(outDependency));
                checkForCircular(depPath, outDependency, projCtx);
                depPath.addLast(outDependency);
                dependencySourceContext =
                        determineProjectionWave(context, dependencySourceContext, outDependency, depPath, task, result);
                LOGGER.trace("    dependency projection wave: {}", dependencySourceContext.getWave());
                if (dependencySourceContext.getWave() + 1 > determinedWave) {
                    determinedWave = dependencySourceContext.getWave() + 1;
                    determinedOrder = or0(outDependency.getOrder());
                }
                LOGGER.trace("    determined dependency wave: {} (order={})", determinedWave, determinedOrder);
                depPath.removeLast();
            }
        }

        LensProjectionContext resultCtx = projCtx;
        if (projCtx.getWave() >= 0 && projCtx.getWave() != determinedWave) {
            // Wave for this context was set during the run of this method (it was not set when we
            // started, we checked at the beginning). Therefore this context must have been visited again.
            // therefore there is a circular dependency. Therefore we need to create another context to split it.
            if (!projCtx.isDelete()){
                resultCtx = spawnWithNewOrder(context, projCtx, determinedOrder);
            }
        }
        resultCtx.setWave(determinedWave);
        return resultCtx;
    }

    /**
     * Returns all contexts that depend on provided `targetProjectionContext`.
     */
    private <F extends ObjectType> Collection<DependencyAndSource> findReverseDependencies(LensContext<F> context,
            LensProjectionContext targetProjectionContext) throws SchemaException, ConfigurationException {
        Collection<DependencyAndSource> deps = new ArrayList<>();
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            for (ResourceObjectTypeDependencyType dependency: projectionContext.getDependencies()) {
                if (LensUtil.isDependencyTarget(targetProjectionContext, dependency)) {
                    DependencyAndSource ds = new DependencyAndSource();
                    ds.dependency = dependency;
                    ds.sourceProjectionContext = projectionContext;
                    deps.add(ds);
                }
            }
        }
        return deps;
    }

    private void checkForCircular(
            Deque<ResourceObjectTypeDependencyType> depPath,
            ResourceObjectTypeDependencyType outDependency,
            LensProjectionContext projectionContext) throws PolicyViolationException {
        for (ResourceObjectTypeDependencyType pathElement: depPath) {
            if (pathElement.equals(outDependency)) {
                throw new PolicyViolationException("Circular dependency in " + projectionContext.getHumanReadableName()
                        + ", path: " + getPathDescription(depPath));
            }
        }
    }

    private String getPathDescription(Deque<ResourceObjectTypeDependencyType> depPath) {
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

    private boolean isHigherOrder(ResourceObjectTypeDependencyType a, ResourceObjectTypeDependencyType b) {
        return or0(a.getOrder()) > or0(b.getOrder());
    }

    /**
     * Find upstream context that has the closest order to the dependency-to-be-spawned (given by dependency bean).
     */
    private LensProjectionContext findUpstreamContext(
            LensContext<?> context, ProjectionContextFilter upstreamContextFilter, int specifiedOrder) {
        LensProjectionContext selected = null;
        for (LensProjectionContext candidate : context.getProjectionContexts()) {
            // The upstream-candidate must have order that is not greater than the order of the context-to-be-spawned
            if (candidate.matches(upstreamContextFilter) && candidate.getOrder() <= specifiedOrder) {
                if (selected == null || candidate.getOrder() > selected.getKey().getOrder()) {
                    selected = candidate;
                }
            }
        }
        return selected;
    }

    private LensProjectionContext spawnWithNewOrder(
            LensContext<?> context,
            LensProjectionContext origCtx,
            int newOrder) {
        LensProjectionContext newCtx =
                context.createProjectionContext(
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

    private boolean areDependenciesSatisfied(LensProjectionContext projContext)
            throws SchemaException, ConfigurationException, PolicyViolationException {
        LOGGER.trace("Checking whether dependencies of {} are satisfied", projContext);
        LensContext<?> context = projContext.getLensContext();
        for (ResourceObjectTypeDependencyType dependency : projContext.getDependencies()) {
            // TODO This is a bit strange ... when we have two occurrences of given "coordinates"
            //  (resource + kind/intent), they share their dependency definition! So that dependency
            //  is checked twice (the first time probably unnecessarily)
            ProjectionContextKey upstreamContextKey = ProjectionContextKey.fromDependency(dependency);
            LensProjectionContext upstreamContext = context.findProjectionContextByKeyExact(upstreamContextKey);
            LOGGER.trace("Upstream context with key {} found: {}", upstreamContextKey, upstreamContext);
            ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(dependency);
            if (upstreamContext == null) {
                if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT) {
                    // This should not happen, it is checked before projection
                    throw new PolicyViolationException("Unsatisfied strict dependency of "
                            + projContext.getKey().toHumanReadableDescription()
                            + " dependent on " + upstreamContextKey.toHumanReadableDescription()
                            + ": No context in dependency check");
                } else if (strictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
                    // independent object not in the context, just ignore it
                    LOGGER.trace("Unsatisfied lax dependency of account " +
                            projContext.getKey().toHumanReadableDescription() +
                            " dependent on " + upstreamContextKey.toHumanReadableDescription() + "; dependency skipped");
                } else if (strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
                    // independent object not in the context, just ignore it
                    LOGGER.trace("Unsatisfied relaxed dependency of account "
                            + projContext.getKey().toHumanReadableDescription() +
                            " dependent on " + upstreamContextKey.toHumanReadableDescription() + "; dependency skipped");
                } else {
                    throw new IllegalArgumentException(
                            "Unknown dependency strictness " + dependency.getStrictness() + " in " + upstreamContextKey);
                }
            } else {
                // We have the context of the object that we depend on. We need to check if it was provisioned.
                if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT
                        || strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
                    if (wasUpstreamContextProvisioned(upstreamContext, context.getExecutionWave())) {
                        // everything OK
                    } else {
                        // We do not want to throw exception here. That would stop entire projection.
                        // Let's just mark the projection as broken and skip it.
                        LOGGER.warn("Unsatisfied dependency of {} dependent on {}: Projection not provisioned in dependency check"
                                        + " (execution wave {}, projection wave {}, dependency (upstream) projection wave {})",
                                projContext.getKey(), upstreamContextKey, context.getExecutionWave(),
                                projContext.getWave(), upstreamContext.getWave());
                        projContext.setBroken();
                        return false;
                    }
                } else if (strictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
                    // we don't care what happened, just go on
                } else {
                    throw new IllegalArgumentException(
                            "Unknown dependency strictness " + dependency.getStrictness() + " in " + upstreamContextKey);
                }
            }
        }
        return true;
    }

    <F extends ObjectType> void preprocessDependencies(LensContext<F> context) throws SchemaException, ConfigurationException {

        //in the first wave we do not have enough information to preprocess contexts
        if (context.getExecutionWave() == 0) {
            return;
        }

        for (LensProjectionContext projContext : context.getProjectionContexts()) {
            if (!projContext.isCanProject()) {
                continue;
            }

            for (ResourceObjectTypeDependencyType dependency: projContext.getDependencies()) {
                ProjectionContextKey refKey = ProjectionContextKey.fromDependency(dependency);
                LOGGER.trace("LOOKING FOR {}", refKey);
                LensProjectionContext dependencyAccountContext = context.findProjectionContextByKeyExact(refKey);
                ResourceObjectTypeDependencyStrictnessType strictness = getDependencyStrictness(dependency);
                if (dependencyAccountContext != null && dependencyAccountContext.isCanProject()) {
                    // We have the context of the object that we depend on. We need to check if it was provisioned.
                    if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT
                            || strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
                        if (wasExecuted(dependencyAccountContext)) {
                            // everything OK
                            if (isForceLoadDependentShadow(dependency) && !dependencyAccountContext.isDelete()) {
                                dependencyAccountContext.setDoReconciliation(true);
                                projContext.setDoReconciliation(true);
                            }
                        }
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

        for (LensProjectionContext accountContext: context.getProjectionContexts()) {
            checkDependencies(accountContext);
        }

        for (LensProjectionContext accountContext: context.getProjectionContexts()) {
            if (accountContext.isDelete()
                    || accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
                // It is OK if we depend on something that is not there if we are being removed
                // but we still need to check if others depends on me
                for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
                    if (projectionContext.isDelete()
                            || projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK
                            || projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN
                            || projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
                        // If someone who is being deleted depends on us then it does not really matter
                        continue;
                    }
                    for (ResourceObjectTypeDependencyType dependency: projectionContext.getDependencies()) {
                        String dependencyResourceOid = dependency.getResourceRef() != null ?
                                dependency.getResourceRef().getOid() : projectionContext.getResource().getOid();
                        // TODO what to do if dependencyResourceOid or accountContext.getResource() is null?
                        if (dependencyResourceOid != null
                                && accountContext.getResource() != null
                                && dependencyResourceOid.equals(accountContext.getResource().getOid())
                                && MiscSchemaUtil.equalsIntent(dependency.getIntent(), projectionContext.getKey().getIntent())) {
                            // Someone depends on us
                            if (getDependencyStrictness(dependency) == ResourceObjectTypeDependencyStrictnessType.STRICT) {
                                throw new PolicyViolationException("Cannot remove "+accountContext.getHumanReadableName()
                                        +" because "+projectionContext.getHumanReadableName()+" depends on it");
                            }
                        }
                    }
                }

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

    static class DependencyAndSource {
        ResourceObjectTypeDependencyType dependency;
        LensProjectionContext sourceProjectionContext;
    }
}
