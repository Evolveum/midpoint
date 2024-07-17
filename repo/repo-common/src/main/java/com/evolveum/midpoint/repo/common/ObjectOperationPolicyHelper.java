package com.evolveum.midpoint.repo.common;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceObjectPattern;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.base.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.*;

@Component
public class ObjectOperationPolicyHelper {

    private static final String OP_COMPUTE_EFFECTIVE_POLICY =
            ObjectOperationPolicyHelper.class.getName() + ".computeEffectivePolicy";

    private abstract static class Impl {

        protected abstract Collection<ObjectReferenceType> getEffectiveMarkRefs(ObjectType shadow, OperationResult result);

        protected abstract boolean isProtectedByResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs);

        protected abstract boolean policyNotExcluded(ObjectType shadow, String markProtectedShadowOid);

        protected abstract @NotNull ObjectOperationPolicyType computeEffectivePolicy(
                Collection<ObjectReferenceType> effectiveMarkRefs,
                ObjectType shadow, OperationResult result);

        protected abstract void setEffectiveMarks(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs);

        protected ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType repoShadow, ItemDelta<?, ?> modification) throws SchemaException {
            return null;
        }

        protected EvaluatedPolicyStatements computeEffectiveMarkDelta(Map<PlusMinusZero, Collection<PolicyStatementType>> policyStatements) throws SchemaException {
            return null;
        }

        public ItemDelta<?, ?> computeEffectiveMarkDelta(@NotNull ObjectType repoShadow,
                List<ObjectReferenceType> effectiveMarkRef) throws SchemaException {
            return null;
        }

    }

    private static final String MARK_PROTECTED_SHADOW_OID = SystemObjectsType.MARK_PROTECTED.value();

    private static ObjectOperationPolicyHelper instance = null;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

    private Impl behaviour;

    @PostConstruct
    public void init() {
        behaviour = cacheRepositoryService.supportsMarks() ? new MarkSupport() : new Legacy();
        instance = this;
    }

    @PreDestroy
    public void destroy() {
        instance = null;
    }

    public Collection<MarkType> getShadowMarks(Collection<ObjectReferenceType> tagRefs, @NotNull OperationResult result) {
        // FIXME: Consider caching of all shadow marks and doing post-filter only
        if (!cacheRepositoryService.supportsMarks() || tagRefs.isEmpty()) {
            return List.of();
        }
        String[] tagRefIds = tagRefs.stream().map(t -> t.getOid()).collect(Collectors.toList()).toArray(new String[0]);
        ObjectQuery query = prismContext.queryFor(MarkType.class)
            //.item(TagType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_SHADOW_MARK.value())
             // Tag is Shadow Marks
            .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value())
            .and()
            // Tag is assigned to shadow
            .id(tagRefIds)
            .build();
        try {
            return asObjectables(cacheRepositoryService.searchObjects(MarkType.class, query, null, result));
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    public static ObjectOperationPolicyHelper get() {
        return instance;
    }

    public @NotNull ObjectOperationPolicyType getEffectivePolicy(ObjectType shadow, OperationResult result) {
        var policy = shadow.getEffectiveOperationPolicy();
        if (policy != null) {
            return policy;
        }
        return computeEffectivePolicy(shadow, result);
    }

    public @NotNull ObjectOperationPolicyType computeEffectivePolicy(ObjectType shadow, OperationResult parentResult) {
        var result = parentResult.createMinorSubresult(OP_COMPUTE_EFFECTIVE_POLICY);
        try {
            Collection<ObjectReferenceType> effectiveMarkRefs = behaviour.getEffectiveMarkRefs(shadow, result);
            return behaviour.computeEffectivePolicy(effectiveMarkRefs, shadow, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public void updateEffectiveMarksAndPolicies(
            Collection<ResourceObjectPattern> protectedAccountPatterns,
            AbstractShadow shadow, OperationResult result) throws SchemaException {

        ShadowType bean = shadow.getBean();

        Collection<ObjectReferenceType> effectiveMarkRefs = behaviour.getEffectiveMarkRefs(bean, result);

        if (behaviour.isProtectedByResourcePolicy(bean, effectiveMarkRefs)) {
            // Account was originally marked by resource policy
            // removing mark, so we can recompute if it still applies
            removeRefByOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID);
        }

        if (needsToEvaluateResourcePolicy(bean, effectiveMarkRefs)) {
            // Resource protection policy was not explicitly excluded
            // so we need to check if shadow is protected
            if (ResourceObjectPattern.matches(shadow, protectedAccountPatterns)) {
                // Shadow is protected by resource protected object configuration
                effectiveMarkRefs.add(resourceProtectedShadowMark());
            }
        }

        var effectivePolicy = behaviour.computeEffectivePolicy(effectiveMarkRefs, bean, result);
        updateShadowObject(bean, effectiveMarkRefs, effectivePolicy);
    }


    private ObjectReferenceType resourceProtectedShadowMark() {
        // TODO Maybe add metadata with provenance pointing that this was added by resource configuration
        ObjectReferenceType ret = new ObjectReferenceType();
        ret.setOid(MARK_PROTECTED_SHADOW_OID);
        ret.setType(MarkType.COMPLEX_TYPE);
        return ret;
    }

    private static void removeRefByOid(Collection<ObjectReferenceType> refs, String oid) {
        var refIter = refs.iterator();
        while (refIter.hasNext()) {
            var current = refIter.next();
            if (oid.equals(current.getOid())) {
                refIter.remove();
            }
        }
    }

    private boolean needsToEvaluateResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {

        if (containsOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID)) {
            return false;
        }
        return behaviour.policyNotExcluded(shadow, MARK_PROTECTED_SHADOW_OID);
    }

    private static boolean containsOid(Collection<ObjectReferenceType> refs, @NotNull String oid) {
        for (var ref : refs) {
            if (oid.equals(ref.getOid())) {
                return true;
            }
        }
        return false;
    }

    private void updateShadowObject(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs,
            ObjectOperationPolicyType effectivePolicy) {
        behaviour.setEffectiveMarks(shadow, effectiveMarkRefs);

        shadow.setEffectiveOperationPolicy(effectivePolicy);
        if (!effectivePolicy.getAdd().isEnabled()
                && !effectivePolicy.getModify().isEnabled()
                && !effectivePolicy.getDelete().isEnabled()
                && !effectivePolicy.getSynchronize().getInbound().isEnabled()
                && !effectivePolicy.getSynchronize().getOutbound().isEnabled()
                ) {
            shadow.setProtectedObject(true);
        }
    }

    private class MarkSupport extends Impl {

        @Override
        protected void setEffectiveMarks(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            shadow.getEffectiveMarkRef().clear();
            shadow.getEffectiveMarkRef().addAll(effectiveMarkRefs);
        }

        @Override
        protected Collection<ObjectReferenceType> getEffectiveMarkRefs(ObjectType shadow, OperationResult result) {
            return computeEffectiveMarkRefs(shadow.getEffectiveMarkRef(), shadow);
        }

        private Collection<ObjectReferenceType> computeEffectiveMarkRefs(List<ObjectReferenceType> proposed, ObjectType object) {
            List<ObjectReferenceType> ret = new ArrayList<>();
            for (var mark : proposed) {
                if (mark.getOid() != null && policyNotExcluded(object, mark.getOid())) {
                    // Mark is effective if it was not excluded
                    ret.add(mark);
                }
            }

            for (var statement : object.getPolicyStatement()) {
                if (APPLY.equals(statement.getType())
                        && statement.getMarkRef() != null && statement.getMarkRef().getOid() != null) {
                        // Add to effective refs
                        ret.add(statement.getMarkRef().clone());
                }
            }
            return ret;
        }

        @Override
        protected boolean isProtectedByResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            if (containsPolicyStatement(shadow, MARK_PROTECTED_SHADOW_OID, APPLY)) {
                // Protected Shadow Mark was added manually
                return false;
            }
            return containsOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID);
        }

        @Override
        protected boolean policyNotExcluded(ObjectType shadow, String markOid) {
            return !containsPolicyStatement(shadow, markOid, EXCLUDE);
        }

        protected boolean containsPolicyStatement(@NotNull ObjectType shadow, @NotNull String markOid, @NotNull PolicyStatementTypeType policyType) {
            for (var statement : shadow.getPolicyStatement()) {
                if (policyType.equals(statement.getType())) {
                    var markRef = statement.getMarkRef();
                    if (markRef != null && markOid.equals(markRef.getOid())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected @NotNull ObjectOperationPolicyType computeEffectivePolicy(Collection<ObjectReferenceType> effectiveMarkRefs,
                ObjectType shadow, OperationResult result) {
            var ret = new ObjectOperationPolicyType();
            Collection<MarkType>  marks = getShadowMarks(effectiveMarkRefs, result);

            ret.setSynchronize(new SynchronizeOperationPolicyConfigurationType()
                    .inbound(firstNonDefaultValue(marks,
                            m -> m.getSynchronize() != null ? m.getSynchronize().getInbound(): null,
                                    true))
                    .outbound(firstNonDefaultValue(marks,
                            m -> m.getSynchronize() != null ? m.getSynchronize().getOutbound(): null,
                                    true))
                    );

            ret.setAdd(firstNonDefaultValue(marks, ObjectOperationPolicyType::getAdd, true));
            ret.setModify(firstNonDefaultValue(marks, ObjectOperationPolicyType::getModify, true));
            ret.setDelete(firstNonDefaultValue(marks, ObjectOperationPolicyType::getDelete, true));
            return ret;
        }

        @Override
        protected ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType orig, ItemDelta<?, ?> modification) throws SchemaException {
            PrismObject<? extends ObjectType> after = orig.clone().asPrismObject();

            var effectiveMarks = new ArrayList<>(orig.getEffectiveMarkRef());
            List<ObjectReferenceType> refsToDelete = new ArrayList<>();
            List<ObjectReferenceType> refsToAdd = new ArrayList<>();
            var deletedStatements = (Collection<PolicyStatementType>) modification.getRealValuesToDelete();
            if (deletedStatements != null) {
                for (var deleted : deletedStatements) {
                     if (APPLY.equals(deleted.getType())) {
                         var valueToDelete = findEffectiveImpliedByStatement(effectiveMarks, deleted.getMarkRef().getOid());
                         if (valueToDelete != null) {
                             refsToDelete.add(valueToDelete.clone());
                         }
                     }
                }
            }
            modification.applyTo(after);
            ObjectType afterObj = after.asObjectable();
            Collection<ObjectReferenceType> finalEffectiveMarks = computeEffectiveMarkRefs(effectiveMarks, afterObj);
            for (var finalMark : finalEffectiveMarks) {
                if (!containsRef(orig.getEffectiveMarkRef(), finalMark)) {
                    refsToAdd.add(finalMark.clone());
                }
            }
            if (refsToDelete.isEmpty() && refsToAdd.isEmpty()) {
                // Nothing to add or remove.
                return null;
            }
            return PrismContext.get().deltaFor(ObjectType.class)
                    .item(ObjectType.F_EFFECTIVE_MARK_REF)
                    .deleteRealValues(refsToDelete)
                    .addRealValues(refsToAdd)
                    .asItemDelta();
        }

        @Override
        public ItemDelta<?, ?> computeEffectiveMarkDelta(@NotNull ObjectType repoObject,
                List<ObjectReferenceType> effectiveMarks) throws SchemaException {
            List<ObjectReferenceType> refsToDelete = new ArrayList<>();
            List<ObjectReferenceType> refsToAdd = new ArrayList<>();
            for (ObjectReferenceType mark : effectiveMarks) {
                if (policyNotExcluded(repoObject, mark.getOid()) && !containsRef(repoObject.getEffectiveMarkRef(), mark)) {
                    // Computed mark is not explicitly excluded, we may need to add it
                    // if not present in repository
                    refsToAdd.add(mark.clone());

                }
            }

            // Shadow is not protected by resource policy
            //   - We need to check repository shadow if it contains protected mark,
            //   - which was maybe introduced by previously being protected by resource policy
            //repoShadow
            if (repoObject instanceof ShadowType repoShadow) {
                if (!containsOid(effectiveMarks, MARK_PROTECTED_SHADOW_OID)
                        && isProtectedByResourcePolicy(repoShadow, repoObject.getEffectiveMarkRef())) {
                    refsToDelete.add(new ObjectReferenceType().oid(MARK_PROTECTED_SHADOW_OID).type(MarkType.COMPLEX_TYPE));
                }
            }


            if (refsToDelete.isEmpty() && refsToAdd.isEmpty()) {
                // Nothing to add or remove.
                return null;
            }
            return PrismContext.get().deltaFor(ObjectType.class)
                    .item(ObjectType.F_EFFECTIVE_MARK_REF)
                    .deleteRealValues(refsToDelete)
                    .addRealValues(refsToAdd)
                    .asItemDelta();


        }

        private ObjectReferenceType findEffectiveImpliedByStatement(ArrayList<ObjectReferenceType> effectiveMarks, String oid) {
            for (ObjectReferenceType mark : effectiveMarks) {
                if (oid.equals(mark.getOid()) && isImpliedByStatement(mark)) {
                    return mark;
                }
            }
            return null;
        }

        private boolean isImpliedByStatement(ObjectReferenceType mark) {
            // Currently all marks are implied by statements
            return true;
        }

        private boolean containsRef(List<ObjectReferenceType> effectiveMarkRef, ObjectReferenceType finalMark) {
            return containsOid(effectiveMarkRef, finalMark.getOid());
        }

    }

    private class Legacy extends Impl {

        @Override
        protected Collection<ObjectReferenceType> getEffectiveMarkRefs(ObjectType shadow, OperationResult result) {
            return new ArrayList<>();
        }

        @Override
        protected boolean isProtectedByResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            return false;
        }

        @Override
        protected boolean policyNotExcluded(ObjectType shadow, String markProtectedShadowOid) {
            return true;
        }

        @Override
        protected @NotNull ObjectOperationPolicyType computeEffectivePolicy(Collection<ObjectReferenceType> effectiveMarkRefs,
                ObjectType shadow, OperationResult result) {
            if (containsOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID)) {
                return new ObjectOperationPolicyType()
                        .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                .inbound(op(false, OperationPolicyViolationSeverityType.INFO))
                                .outbound(op(false, OperationPolicyViolationSeverityType.INFO))
                        )
                        .add(op(false, OperationPolicyViolationSeverityType.ERROR))
                        .modify(op(false, OperationPolicyViolationSeverityType.ERROR))
                        .delete(op(false, OperationPolicyViolationSeverityType.ERROR));
            }
            return new ObjectOperationPolicyType()
                    .synchronize(new SynchronizeOperationPolicyConfigurationType()
                            .inbound(op(true, null))
                            .outbound(op(true, null))
                    )
                    .add(op(true, null))
                    .modify(op(true, null))
                    .delete(op(true, null));
        }

        private OperationPolicyConfigurationType op(boolean value, OperationPolicyViolationSeverityType severity) {
            var ret = new OperationPolicyConfigurationType();
            ret.setEnabled(value);
            if (!value) {
                ret.setSeverity(severity);
            }
            return ret;
        }

        @Override
        protected void setEffectiveMarks(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            // NOOP, since marks are not supported by repository
        }
    }

    // FIXME what about severity? We should perhaps select the highest one
    public static OperationPolicyConfigurationType firstNonDefaultValue(Collection<MarkType> marks,
            Function<ObjectOperationPolicyType, OperationPolicyConfigurationType> extractor, boolean defaultValue) {
        for (var mark : marks) {
            if (mark.getObjectOperationPolicy() != null) {
                var value = extractor.apply(mark.getObjectOperationPolicy());
                if (value == null) {
                    continue;
                }
                var enabled = value.isEnabled();
                // If value is different from default, we return and use it
                if (enabled != null && !Objects.equal(defaultValue, enabled)) {
                    return value.clone();
                }
            }
        }
        return new OperationPolicyConfigurationType().enabled(defaultValue);
    }

    public ItemDelta<?, ?> computeEffectiveMarkDelta(ObjectType repoShadow, ItemDelta<?, ?> modification) throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(repoShadow, modification);
    }

    public EvaluatedPolicyStatements computeEffectiveMarkDelta(ObjectType repoShadow, Map<PlusMinusZero, Collection<PolicyStatementType>> policyStatements) throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(policyStatements);
    }

    public ItemDelta<?, ?> computeEffectiveMarkDelta(@NotNull ObjectType repoShadow, List<ObjectReferenceType> effectiveMarkRef) throws SchemaException {
        return behaviour.computeEffectiveMarkDelta(repoShadow, effectiveMarkRef);
    }
}
