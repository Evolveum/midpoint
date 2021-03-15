/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Generator assigning missing IDs to PCVs of multi-value containers.
 */
public class ContainerValueIdGenerator {

    private final PrismObject<?> object;
    private final Set<Long> usedIds = new HashSet<>();
    private final List<PrismContainerValue<?>> pcvsWithoutId = new ArrayList<>();

    private long maxUsedId = 0; // tracks max CID (set to duplicate CID if found)

    public ContainerValueIdGenerator(@NotNull PrismObject<?> object) {
        this.object = object;
    }

    /** Method inserts IDs for prism container values without IDs and returns highest CID. */
    public long generate() throws SchemaException {
        processContainers();
        generateContainerIds();
        return maxUsedId;
    }

    private void processContainers() throws SchemaException {
        try {
            //noinspection unchecked
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

                processContainer(container);
            });
        } catch (DuplicateContainerIdException e) {
            throw new SchemaException("CID " + maxUsedId + " is used repeatedly in the object!");
        }
    }

    private void processContainer(PrismContainer<?> container) {
        for (PrismContainerValue<?> val : container.getValues()) {
            if (val.getId() != null) {
                Long cid = val.getId();
                if (!usedIds.add(cid)) {
                    maxUsedId = cid;
                    throw DuplicateContainerIdException.INSTANCE;
                }
                maxUsedId = Math.max(maxUsedId, cid);
            } else {
                pcvsWithoutId.add(val);
            }
        }
    }

    private void generateContainerIds() {
        for (PrismContainerValue<?> val : pcvsWithoutId) {
            val.setId(nextId());
        }
    }

    public long nextId() {
        maxUsedId++;
        return maxUsedId;
    }

    private static class DuplicateContainerIdException extends RuntimeException {
        static final DuplicateContainerIdException INSTANCE = new DuplicateContainerIdException();

        private DuplicateContainerIdException() {
            super(null, null, false, false);
        }
    }
}
