package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
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

        List<PrismContainer<?>> values = listAllPrismContainers(object);
        generateContainerIds(values, result, operation);

        return result;
    }

    private List<PrismContainer<?>> listAllPrismContainers(Visitable object) {
        List<PrismContainer<?>> values = new ArrayList<>();

        object.accept(visitable -> {
            if (!(visitable instanceof PrismContainer)) {
                return;
            }

            if (visitable instanceof PrismObject) {
                return;
            }

            values.add((PrismContainer) visitable);
        });

        return values;
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

    public IdGeneratorResult generate(Containerable containerable, Operation operation) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (!(containerable instanceof AccessCertificationCaseType)) {
            return result;
        }
        AccessCertificationCaseType aCase = (AccessCertificationCaseType) containerable;

        List<PrismContainer<?>> values = listAllPrismContainers(aCase.asPrismContainerValue());
        generateContainerIds(values, result, operation);

        return result;
    }
}
