/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator implements DebugDumpable {

    public enum Operation {ADD, ADD_WITH_OVERWRITE, MODIFY}

    private final Operation operation;
    private final Set<Long> usedIds = new HashSet<>();

    private Long lastId = null;

    public PrismIdentifierGenerator(@NotNull Operation operation) {
        this.operation = operation;
    }

    /**
     * Method inserts id for prism container values, which didn't have ids,
     * also returns all container values which has generated id
     */
    public IdGeneratorResult generate(@NotNull PrismObject<?> object) {
        IdGeneratorResult result = new IdGeneratorResult();
        boolean adding = Operation.ADD.equals(operation);
        result.setGeneratedOid(adding);

        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);
            result.setGeneratedOid(true);
        }

        List<PrismContainer<?>> values = listAllPrismContainers(object);
        generateContainerIds(values, result);

        return result;
    }

    public IdGeneratorResult generate(Containerable containerable) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (!(containerable instanceof AccessCertificationCaseType)) {
            return result;
        }
        AccessCertificationCaseType aCase = (AccessCertificationCaseType) containerable;

        List<PrismContainer<?>> values = listAllPrismContainers(aCase.asPrismContainerValue());
        generateContainerIds(values, result);

        return result;
    }

    // used both for PrismObjects nad PrismContainerValues
    private List<PrismContainer<?>> listAllPrismContainers(Visitable<?> object) {
        List<PrismContainer<?>> values = new ArrayList<>();

        object.accept(visitable -> {
            if (!(visitable instanceof PrismContainer)) {
                return;
            }

            if (visitable instanceof PrismObject) {
                return;
            }

            PrismContainer<?> container = (PrismContainer<?>) visitable;
            PrismContainerDefinition<?> def = container.getDefinition();
            if (def.isSingleValue()) {
                return;
            }

            values.add((PrismContainer<?>) visitable);
        });

        return values;
    }

    public void collectUsedIds(@NotNull PrismObject<?> object) {
        collectUsedIds(listAllPrismContainers(object));
    }

    private void collectUsedIds(List<PrismContainer<?>> containers) {
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }
    }

    private void generateContainerIds(List<PrismContainer<?>> containers, IdGeneratorResult result) {
        collectUsedIds(containers);
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() == null) {
                    val.setId(nextId());
                }
                if (operation == Operation.ADD) {
                    result.getValues().add(val);
                }
            }
        }
    }

    public long nextId() {
        if (lastId == null) {
            lastId = getStartId();
        }
        lastId++;
        return lastId;
    }

    private long getStartId() {
        if (usedIds.isEmpty()) {
            return 0L;
        }
        return Collections.max(usedIds);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(PrismIdentifierGenerator.class, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "operation", operation, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "lastId", lastId, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "usedIds", usedIds, indent + 1);
        return sb.toString();
    }
}
