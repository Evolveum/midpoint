/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeltaUpdaterUtils {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaUpdaterUtils.class);

    public static void deleteValues(Collection existing, Collection<PrismEntityPair<?>> valuesToDelete) {
        if (existing.isEmpty() || valuesToDelete.isEmpty()) {
            return;
        }

        Collection<Object> repositoryObjects = valuesToDelete.stream().map(pair -> pair.getRepository()).collect(Collectors.toList());

        Pair<Collection<Container>, Set<Long>> split = splitContainers(valuesToDelete);
        Collection<Container> containersWithoutIds = split.getLeft();
        if (!containersWithoutIds.isEmpty()) {
            LOGGER.warn("Container without id found in delete delta, potential operation slowdown as we " +
                    "need to compare full container against database");
        }
        Set<Long> containerIds = split.getRight();

        Collection toDelete = new ArrayList();
        for (Object obj : existing) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                long id = container.getId().longValue();
                if (containerIds.contains(id)
                        || (!containersWithoutIds.isEmpty() && containersWithoutIds.contains(container))) {
                    toDelete.add(container);
                }
            } else {
                // e.g. RObjectReference
                if (repositoryObjects.contains(obj)) {
                    toDelete.add(obj);
                }
            }
        }

        existing.removeAll(toDelete);
    }

    public static void replaceValues(Collection existing, Collection<PrismEntityPair<?>> valuesToReplace) {
        if (existing.isEmpty()) {
            markNewOnesTransientAndAddToExisting(existing, valuesToReplace);
            return;
        }

        Collection<Object> repositoryObjects = valuesToReplace.stream().map(pair -> pair.getRepository()).collect(Collectors.toList());

        Pair<Collection<Container>, Set<Long>> split = splitContainers(valuesToReplace);
        Collection<Container> containersWithoutIds = split.getLeft();
        if (!containersWithoutIds.isEmpty()) {
            LOGGER.warn("Container without id found in replace delta, potential operation slowdown as we " +
                    "need to compare full container against database");
        }
        Set<Long> containerIds = split.getRight();

        Collection skipAddingTheseObjects = new ArrayList();
        Collection skipAddingTheseIds = new ArrayList();

        Collection toDelete = new ArrayList();

        // mark existing object for deletion, skip if they would be replaced with the same value
        for (Object obj : existing) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                long id = container.getId().longValue();
                if (!containerIds.contains(id)
                        || (!containersWithoutIds.isEmpty() && !containersWithoutIds.contains(container))) {
                    toDelete.add(container);
                } else {
                    skipAddingTheseIds.add(id);
                    skipAddingTheseObjects.add(container);
                }
            } else {
                // e.g. RObjectReference
                if (!repositoryObjects.contains(obj)) {
                    toDelete.add(obj);
                } else {
                    skipAddingTheseObjects.add(obj);
                }
            }
        }
        existing.removeAll(toDelete);

        Iterator<PrismEntityPair<?>> iterator = valuesToReplace.iterator();
        while (iterator.hasNext()) {
            PrismEntityPair pair = iterator.next();
            Object obj = pair.getRepository();
            if (obj instanceof Container) {
                Container container = (Container) obj;

                if (container.getId() == null && skipAddingTheseObjects.contains(container)) {
                    iterator.remove();
                    continue;
                }

                if (skipAddingTheseIds.contains(container.getId())) {
                    iterator.remove();
                }
            } else {
                if (skipAddingTheseObjects.contains(obj)) {
                    iterator.remove();
                }
            }
        }

        markNewOnesTransientAndAddToExisting(existing, valuesToReplace);
    }

    public static Pair<Collection<Container>, Set<Long>> splitContainers(Collection<PrismEntityPair<?>> collection) {
        Collection<Container> containers = new ArrayList<>();
        Set<Long> containerIds = new HashSet<>();
        for (PrismEntityPair pair : collection) {
            if (!(pair.getRepository() instanceof Container)) {
                continue;
            }

            Container container = (Container) pair.getRepository();

            Integer id = container.getId();
            if (id != null) {
                containerIds.add(id.longValue());
            } else {
                containers.add(container);
            }
        }

        return new ImmutablePair<>(containers, containerIds);
    }

    public static void markNewOnesTransientAndAddToExisting(Collection existing, Collection<PrismEntityPair<?>> newOnes) {
        Set<Integer> usedIds = new HashSet<>();
        for (Object obj : existing) {
            if (!(obj instanceof Container)) {
                continue;
            }

            Container c = (Container) obj;
            if (c.getId() != null) {
                usedIds.add(c.getId());
            }
        }

        Integer nextId = 1;
        for (PrismEntityPair item : newOnes) {
            if (item.getRepository() instanceof EntityState) {
                EntityState es = (EntityState) item.getRepository();
                es.setTransient(true);
            }

            if (item.getRepository() instanceof Container) {
                while (usedIds.contains(nextId)) {
                    nextId++;
                }

                usedIds.add(nextId);
                ((Container) item.getRepository()).setId(nextId);
                ((PrismContainerValue) item.getPrism()).setId(nextId.longValue());
            }

            existing.add(item.getRepository());
        }
    }
}
