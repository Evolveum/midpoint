package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowProvisioningPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

@Component
public class ShadowMarkManager {

    private static ShadowMarkManager instance = null;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;


    @PostConstruct
    public void init() {
        instance = this;
    }

    @PreDestroy
    public void destroy() {
        instance = null;
    }

    public Collection<TagType> getShadowMarks(Collection<ObjectReferenceType> tagRefs, @NotNull OperationResult result) {
        // FIXME: Consider caching of all shadow marks and doing post-filter only
        if (!cacheRepositoryService.supportsTags() || tagRefs.isEmpty()) {
            return List.of();
        }
        String[] tagRefIds = tagRefs.stream().map(t -> t.getOid()).collect(Collectors.toList()).toArray(new String[0]);
        ObjectQuery query = prismContext.queryFor(TagType.class)
            //.item(TagType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_SHADOW_MARK.value())
             // Tag is Shadow Marks
            .item(TagType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(SystemObjectsType.ARCHETYPE_SHADOW_MARK.value())
            .and()
            // Tag is assigned to shadow
            .id(tagRefIds)
            .build();
        try {
            return asObjectables(cacheRepositoryService.searchObjects(TagType.class, query, null, result));
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    public ShadowProvisioningPolicyType getShadowMarkPolicy(Collection<ObjectReferenceType> tagRefs, @NotNull OperationResult result) {
        var ret = new ShadowProvisioningPolicyType();
        Collection<TagType>  marks = getShadowMarks(tagRefs, result);

        // Account is protected if any of shadow marks set it to protected.
        boolean isProtected = marks.stream().anyMatch(
                t -> t.getProvisioningPolicy() != null &&
                BooleanUtils.isTrue(t.getProvisioningPolicy().isProtected()));
        ret.setProtected(isProtected);

        return ret;
    }

    public static ShadowMarkManager get() {
        return instance;
    }

    public boolean isProtectedShadow(ShadowType shadow, @NotNull OperationResult result) {
        return getShadowMarkPolicy(shadow, result).isProtected();
    }

    private ShadowProvisioningPolicyType getShadowMarkPolicy(ShadowType shadow, @NotNull OperationResult result) {
        if (shadow.getPolicyStatement() == null && shadow.getPolicyStatement().isEmpty()) {
            return getShadowMarkPolicy(shadow.getEffectiveMarkRef(), result);
        }
        Set<ObjectReferenceType> tagRefs = new HashSet<>();
        tagRefs.addAll(shadow.getEffectiveMarkRef());
        for (var policy: shadow.getPolicyStatement()) {
            if (PolicyStatementTypeType.APPLY.equals(policy.getType())) {
                tagRefs.add(policy.getMarkRef());
            }
        }
        return getShadowMarkPolicy(tagRefs, result);
    }

    public boolean isProtectedShadowPolicyNotExcluded(ShadowType shadow) {
        if(shadow.getPolicyStatement() != null && !shadow.getPolicyStatement().isEmpty()) {
            for (var policy : shadow.getPolicyStatement()) {
                if ( PolicyStatementTypeType.EXCLUDE.equals(policy.getType())
                        && policy.getMarkRef() != null
                        && SystemObjectsType.TAG_PROTECTED_SHADOW.value().equals(policy.getMarkRef().getOid())) {
                    return false;
                }
            }
        }
        return true;
    }
}
