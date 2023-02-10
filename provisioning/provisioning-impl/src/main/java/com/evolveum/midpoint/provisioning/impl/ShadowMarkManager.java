package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceObjectPattern;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EffectiveShadowProvisioningPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowProvisioningPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.google.common.base.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType.*;

@Component
public class ShadowMarkManager {

    private abstract class Impl {

        protected abstract Collection<ObjectReferenceType> getEffectiveMarkRefs(ShadowType shadow, OperationResult result);

        protected abstract boolean isProtectedByResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs);

        protected abstract boolean policyNotExcluded(ShadowType shadow, String markProtectedShadowOid);

        protected abstract EffectiveShadowProvisioningPolicyType computeEffectivePolicy(
                Collection<ObjectReferenceType> effectiveMarkRefs,
                ShadowType shadow, OperationResult result);

        protected abstract void setEffectiveMarks(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs);

    }

    private static final String MARK_PROTECTED_SHADOW_OID = SystemObjectsType.MARK_PROTECTED_SHADOW.value();

    private static ShadowMarkManager instance = null;

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
            .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(SystemObjectsType.ARCHETYPE_SHADOW_MARK.value())
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

    public static ShadowMarkManager get() {
        return instance;
    }

    public void updateEffectiveMarksAndPolicies(Collection<ResourceObjectPattern> protectedAccountPatterns,
            ShadowType shadow, OperationResult result) throws SchemaException {

        Collection<ObjectReferenceType> effectiveMarkRefs = behaviour.getEffectiveMarkRefs(shadow, result);

        if (behaviour.isProtectedByResourcePolicy(shadow, effectiveMarkRefs)) {
            // Account was originally marked by resource policy
            // removing mark, so we can recompute if it still applies
            removeRefByOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID);
        }

        if (needsToEvaluateResourcePolicy(shadow, effectiveMarkRefs)) {
            // Resource protection policy was not explicitly excluded
            // so we need to check if shadow is protected
            if (ResourceObjectPattern.matches(shadow, protectedAccountPatterns)) {
                // Shadow is protected by resource protected object configuration
                effectiveMarkRefs.add(resourceProtectedShadowMark());
            }
        }

        var effectivePolicy = behaviour.computeEffectivePolicy(effectiveMarkRefs, shadow, result);
        updateShadowObject(shadow, effectiveMarkRefs, effectivePolicy);
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
            EffectiveShadowProvisioningPolicyType effectivePolicy) {
        behaviour.setEffectiveMarks(shadow, effectiveMarkRefs);

        shadow.setEffectiveProvisioningPolicy(effectivePolicy);
        if (Boolean.TRUE.equals(effectivePolicy.isProtected())) {
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
        protected Collection<ObjectReferenceType> getEffectiveMarkRefs(ShadowType shadow, OperationResult result) {
            List<ObjectReferenceType> ret = new ArrayList<>();
            for (var mark : shadow.getEffectiveMarkRef()) {
                if (mark.getOid() != null && policyNotExcluded(shadow, mark.getOid())) {
                    // Mark is effective if it was not excluded
                    ret.add(mark);
                }
            }

            for (var statement : shadow.getPolicyStatement()) {
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
        protected boolean policyNotExcluded(ShadowType shadow, String markOid) {
            return containsPolicyStatement(shadow, markOid, EXCLUDE);
        }

        protected boolean containsPolicyStatement(@NotNull ShadowType shadow, @NotNull String markOid, @NotNull PolicyStatementTypeType policyType) {
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
        protected EffectiveShadowProvisioningPolicyType computeEffectivePolicy(Collection<ObjectReferenceType> effectiveMarkRefs,
                ShadowType shadow, OperationResult result) {
            var ret = new EffectiveShadowProvisioningPolicyType();
            Collection<MarkType>  marks = getShadowMarks(effectiveMarkRefs, result);

            // Account is protected if any of shadow marks set it to protected.

            ret.setProtected(firstNonDefaultValue(marks, ShadowProvisioningPolicyType::isProtected, false));

            // If account is protected all other flags must be marked so.
            if (Boolean.TRUE.equals(ret.isProtected())) {
                ret.setReadOnly(true);
            } else {
                ret.setReadOnly(firstNonDefaultValue(marks, ShadowProvisioningPolicyType::isReadOnly, false));
            }
            return ret;
        }

    }

    private class Legacy extends Impl {

        @Override
        protected Collection<ObjectReferenceType> getEffectiveMarkRefs(ShadowType shadow, OperationResult result) {
            return new ArrayList<>();
        }

        @Override
        protected boolean isProtectedByResourcePolicy(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            return false;
        }

        @Override
        protected boolean policyNotExcluded(ShadowType shadow, String markProtectedShadowOid) {
            return true;
        }

        @Override
        protected EffectiveShadowProvisioningPolicyType computeEffectivePolicy(Collection<ObjectReferenceType> effectiveMarkRefs,
                ShadowType shadow, OperationResult result) {
            if (containsOid(effectiveMarkRefs, MARK_PROTECTED_SHADOW_OID)) {
                return new EffectiveShadowProvisioningPolicyType()
                        ._protected(true)
                        .readOnly(true);
            }
            return new EffectiveShadowProvisioningPolicyType()
                    ._protected(false)
                    .readOnly(false);
        }

        @Override
        protected void setEffectiveMarks(ShadowType shadow, Collection<ObjectReferenceType> effectiveMarkRefs) {
            // NOOP, since marks are not supported by repository
        }
    }

    public static boolean firstNonDefaultValue(Collection<MarkType> marks,
            Function<ShadowProvisioningPolicyType, Boolean> extractor, boolean defaultValue) {
        for (var mark : marks) {
            if (mark.getProvisioningPolicy() != null) {
                var value = extractor.apply(mark.getProvisioningPolicy());
                // If value is different from default, we return and use it
                if (value != null && !Objects.equal(defaultValue, value)) {
                    return value;
                }
            }
        }

        return defaultValue;
    }
}
