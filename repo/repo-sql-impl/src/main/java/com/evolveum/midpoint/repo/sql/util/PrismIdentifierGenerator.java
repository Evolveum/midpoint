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

import org.apache.cxf.common.util.StringUtils;

import java.util.*;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator {

    public enum Operation {ADD, ADD_WITH_OVERWRITE, MODIFY}

    /**
     * Method inserts id for prism container values, which didn't have ids,
     * also returns all container values which has generated id
     *
     * @param object
     * @return
     */
    public IdGeneratorResult generate(PrismObject object, Operation operation) {
        IdGeneratorResult result = new IdGeneratorResult();
        boolean adding = Operation.ADD.equals(operation);
        result.setGeneratedOid(adding);

        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);

            result.setGeneratedOid(true);
        }

        generateIdForObject(object, result, operation);

        return result;
    }

    private void generateIdForObject(PrismObject object, IdGeneratorResult result, Operation operation) {
        if (object == null) {
            return;
        }
        List<PrismContainer> containers = getChildrenContainers(object);
        generateIdsForContainers(containers, result, operation);
    }

    private void generateIdsForContainers(List<PrismContainer> containers, IdGeneratorResult result, Operation operation) {
        Set<Long> usedIds = new HashSet<>();
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }
            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }

        Long nextId = 1L;
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }

            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    if (Operation.ADD.equals(operation)) {
                        result.getValues().add(val);
                    }
                    continue;
                }

                while (usedIds.contains(nextId)) {
                    nextId++;
                }

                val.setId(nextId);
                usedIds.add(nextId);

                if (!Operation.ADD_WITH_OVERWRITE.equals(operation)
                        && !Operation.MODIFY.equals(operation)) {
                    result.getValues().add(val);
                }
            }
        }
    }

    // TODO: This seems to be wrong. We want to generate IDs for all multivalue containers
    // This is maybe some historic code. It has to be cleaned up.
    // MID-3869
    private List<PrismContainer> getChildrenContainers(PrismObject parent) {
        List<PrismContainer> containers = new ArrayList<>();
        if (ObjectType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(ObjectType.F_TRIGGER));
        }

        if (LookupTableType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(LookupTableType.F_ROW));
        }

        if (AccessCertificationCampaignType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            PrismContainer caseContainer = parent.findContainer(AccessCertificationCampaignType.F_CASE);
            containers.add(caseContainer);
            if (caseContainer != null) {
                List<PrismContainerValue> casePcvList = caseContainer.getValues();
                for (PrismContainerValue casePcv : casePcvList) {
                    PrismContainer decisionContainer = casePcv.findContainer(AccessCertificationCaseType.F_DECISION);
                    containers.add(decisionContainer);
                }
            }
            containers.add(parent.findContainer(AccessCertificationCampaignType.F_STAGE));
        }

        if (FocusType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(FocusType.F_ASSIGNMENT));
        }

        if (AbstractRoleType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(AbstractRoleType.F_INDUCEMENT));
            containers.add(parent.findContainer(AbstractRoleType.F_EXCLUSION));
            containers.add(parent.findContainer(AbstractRoleType.F_AUTHORIZATION));
            PrismContainer policyConstraints = parent.findContainer(AbstractRoleType.F_POLICY_CONSTRAINTS);
            if (policyConstraints != null){
            	containers.add(policyConstraints.findContainer(PolicyConstraintsType.F_MAX_ASSIGNEES));
            	containers.add(policyConstraints.findContainer(PolicyConstraintsType.F_MIN_ASSIGNEES));
            }
        }
        
        if (ShadowType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(ShadowType.F_PENDING_OPERATION));
        }

        return containers;
    }

    public IdGeneratorResult generate(Containerable containerable, Operation operation) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (!(containerable instanceof AccessCertificationCaseType)) {
            return result;
        }
        AccessCertificationCaseType aCase = (AccessCertificationCaseType) containerable;
        PrismContainer decisionContainer = aCase.asPrismContainerValue().findContainer(AccessCertificationCaseType.F_DECISION);
        if (decisionContainer != null) {
            generateIdsForContainers(Arrays.asList(decisionContainer), result, operation);
        }
        return result;
    }
}
