package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator {

    public enum Operation {ADD, ADD_WITH_OVERWRITE, MODIFY}

    /**
     * Method inserts id for prism container values, which didn't have ids,
     * also returns all container values which has generated id
     */
    public IdGeneratorResult generate(@NotNull PrismObject object, @NotNull Operation operation) {
        IdGeneratorResult result = new IdGeneratorResult();
        boolean adding = Operation.ADD.equals(operation);
        result.setGeneratedOid(adding);

        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);
            result.setGeneratedOid(true);
        }
        generateContainerIds(getContainersToGenerateIdsFor(object), result, operation);
        return result;
    }

    private void generateContainerIds(List<PrismContainer<?>> containers, IdGeneratorResult result, Operation operation) {
        Set<Long> usedIds = new HashSet<>();
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }

        Long nextId = 1L;
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() != null) {
                    if (operation == Operation.ADD) {
                        result.getValues().add(val);
                    }
                } else {
                    while (usedIds.contains(nextId)) {
                        nextId++;
                    }
                    val.setId(nextId);
                    usedIds.add(nextId);
                    if (operation == Operation.ADD) {
                        result.getValues().add(val);
                    }
                }
            }
        }
    }

    // TODO: This seems to be wrong. We want to generate IDs for all multivalue containers
    // This is maybe some historic code. It has to be cleaned up.
    // MID-3869
    private List<PrismContainer<?>> getContainersToGenerateIdsFor(PrismObject parent) {
        List<PrismContainer<?>> containers = new ArrayList<>();
        if (ObjectType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(ObjectType.F_TRIGGER));
        }

        if (LookupTableType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(LookupTableType.F_ROW));
        }

        if (AccessCertificationCampaignType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            PrismContainer<?> caseContainer = parent.findContainer(AccessCertificationCampaignType.F_CASE);
            CollectionUtils.addIgnoreNull(containers, caseContainer);
            if (caseContainer != null) {
                for (PrismContainerValue<?> casePcv : caseContainer.getValues()) {
                    CollectionUtils.addIgnoreNull(containers, casePcv.findContainer(AccessCertificationCaseType.F_WORK_ITEM));
                }
            }
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(AccessCertificationCampaignType.F_STAGE));
        }

        if (FocusType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(FocusType.F_ASSIGNMENT));
        }

        if (AbstractRoleType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(AbstractRoleType.F_INDUCEMENT));
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(AbstractRoleType.F_EXCLUSION));
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(AbstractRoleType.F_AUTHORIZATION));
            PrismContainer policyConstraints = parent.findContainer(AbstractRoleType.F_POLICY_CONSTRAINTS);
            if (policyConstraints != null){
                CollectionUtils.addIgnoreNull(containers, policyConstraints.findContainer(PolicyConstraintsType.F_MAX_ASSIGNEES));
                CollectionUtils.addIgnoreNull(containers, policyConstraints.findContainer(PolicyConstraintsType.F_MIN_ASSIGNEES));
            }
        }

        if (ShadowType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            CollectionUtils.addIgnoreNull(containers, parent.findContainer(ShadowType.F_PENDING_OPERATION));
        }

        return containers;
    }

    public IdGeneratorResult generate(Containerable containerable, Operation operation) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (!(containerable instanceof AccessCertificationCaseType)) {
            return result;
        }
        AccessCertificationCaseType aCase = (AccessCertificationCaseType) containerable;
        List<PrismContainer<?>> containers = new ArrayList<>();
        CollectionUtils.addIgnoreNull(containers, aCase.asPrismContainerValue().findContainer(AccessCertificationCaseType.F_WORK_ITEM));
		generateContainerIds(containers, result, operation);
        return result;
    }
}
