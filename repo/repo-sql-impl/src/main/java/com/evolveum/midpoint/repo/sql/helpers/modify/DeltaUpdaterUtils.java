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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeltaUpdaterUtils {

    public static void replaceValues(Collection existing, Collection valuesToReplace) {
        // todo fix as the extension replace
        // remove all items from existing which don't exist in valuesToReplace
        // add items from valuesToReplace to existing, only those which aren't already there

        // Collection contains objects (non containers) and containers without id, Set contains only available container ids
        Pair<Collection<Object>, Set<Long>> split = splitPrismEntityPairs(valuesToReplace);
        Collection<Object> repositoryObjects = split.getLeft();
        Set<Long> containerIds = split.getRight();

        Collection skipAddingTheseObjects = new ArrayList();
        Collection skipAddingTheseIds = new ArrayList();
        Collection toDelete = new ArrayList();
        for (Object obj : existing) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                long id = container.getId().longValue();
                if (!containerIds.contains(id)) {
                    toDelete.add(container);
                } else {
                    skipAddingTheseIds.add(id);
                }

                if (!repositoryObjects.contains(obj)) {
                    toDelete.add(container);
                } else {
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

                // todo this will fail as container.getId() returns null at this time
                // new id was not generated yet
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

    public static Pair<Collection<Object>, Set<Long>> splitPrismEntityPairs(Collection<PrismEntityPair<?>> collection) {
        Collection<Object> repositoryObjects = new ArrayList<>();
        Set<Long> containerIds = new HashSet<>();
        for (PrismEntityPair pair : collection) {
            if (pair.getRepository() instanceof Container) {
                Container container = (Container) pair.getRepository();

                Integer id = container.getId();
                if (id == null) {
                    repositoryObjects.add(pair.getRepository());
                    continue;
                }

                containerIds.add(id.longValue());
            }

            repositoryObjects.add(pair.getRepository());
        }

        return new ImmutablePair<>(repositoryObjects, containerIds);
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
