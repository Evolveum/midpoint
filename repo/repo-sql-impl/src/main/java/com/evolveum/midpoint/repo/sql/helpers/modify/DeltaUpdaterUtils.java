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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtBase;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBase;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.util.exception.SystemException;

import java.util.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeltaUpdaterUtils {

    public static void addValues(Collection existing, Collection<PrismEntityPair<?>> valuesToAdd, PrismIdentifierGenerator idGenerator) {
        markNewOnesTransientAndAddToExisting(existing, valuesToAdd, idGenerator);
    }

    public static void deleteValues(Collection existing, Collection<PrismEntityPair<?>> valuesToDelete, Item item) {
        if (existing.isEmpty() || valuesToDelete.isEmpty()) {
            return;
        }

        Collection<PrismEntityPair<?>> existingPairs = createExistingPairs(existing, item);

        Collection toDelete = new ArrayList();
        for (PrismEntityPair toDeletePair : valuesToDelete) {
            PrismEntityPair existingPair = findMatch(existingPairs, toDeletePair);
            if (existingPair != null) {
                toDelete.add(existingPair.getRepository());
            }
        }

        existing.removeAll(toDelete);
    }

    private static PrismEntityPair findMatch(Collection<PrismEntityPair<?>> collection, PrismEntityPair pair) {
        boolean isContainer = pair.getRepository() instanceof Container;

        Object pairObject = pair.getRepository();

        for (PrismEntityPair item : collection) {
            if (isContainer) {
                Container c = (Container) item.getRepository();
                Container pairContainer = (Container) pairObject;

                if (Objects.equals(c.getId(), pairContainer.getId())
                        || pair.getPrism().equals(item.getPrism(), true)) {
                    return item;
                }
            } else {
                // e.g. RObjectReference
                if (Objects.equals(item.getRepository(), pairObject)) {
                    return item;
                }
            }
        }

        return null;
    }

    private static Collection<PrismEntityPair<?>> createExistingPairs(Collection existing, Item item) {
        Collection<PrismEntityPair<?>> pairs = new ArrayList<>();

        for (Object obj : existing) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                PrismValue value = (PrismValue) item.find(new ItemPath(container.getId().longValue()));

                pairs.add(new PrismEntityPair(value, container));
            } else {
                // todo improve somehow
                pairs.add(new PrismEntityPair(null, obj));
            }
        }

        return pairs;
    }

    public static void replaceValues(Collection existing, Collection<PrismEntityPair<?>> valuesToReplace, Item item, PrismIdentifierGenerator idGenerator) {
        if (existing.isEmpty()) {
            markNewOnesTransientAndAddToExisting(existing, valuesToReplace, idGenerator);
            return;
        }

        Collection<PrismEntityPair<?>> existingPairs = createExistingPairs(existing, item);

        Collection skipAddingTheseObjects = new ArrayList();
        Collection skipAddingTheseIds = new ArrayList();

        Collection toDelete = new ArrayList();

        // mark existing object for deletion, skip if they would be replaced with the same value
        for (PrismEntityPair existingPair : existingPairs) {
            PrismEntityPair toReplacePair = findMatch(valuesToReplace, existingPair);
            if (toReplacePair == null) {
                toDelete.add(existingPair.getRepository());
            } else {
                Object existingObject = existingPair.getRepository();
                if (existingObject instanceof Container) {
                    Container c = (Container) existingObject;
                    skipAddingTheseIds.add(c.getId());
                }
                skipAddingTheseObjects.add(existingObject);
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

        markNewOnesTransientAndAddToExisting(existing, valuesToReplace, idGenerator);
    }

    public static void markNewOnesTransientAndAddToExisting(Collection existing, Collection<PrismEntityPair<?>> newOnes, PrismIdentifierGenerator idGenerator) {
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

        for (PrismEntityPair item : newOnes) {
            if (item.getRepository() instanceof EntityState) {
                EntityState es = (EntityState) item.getRepository();
                es.setTransient(true);
            }

            if (item.getRepository() instanceof Container) {
                PrismContainerValue pcv = (PrismContainerValue) item.getPrism();

                if (pcv.getId() != null) {
                    Integer expectedId = pcv.getId().intValue();
                    if (usedIds.contains(expectedId)) {
                        throw new SystemException("Can't save prism container value with id '" + expectedId
                                + "', container with that id already exists.");
                    }

                    usedIds.add(expectedId);
                    ((Container) item.getRepository()).setId(expectedId);
                } else {
                    long nextId = idGenerator.nextId();
                    ((Container) item.getRepository()).setId((int) nextId);
                    ((PrismContainerValue) item.getPrism()).setId(nextId);
                }
            }

            existing.add(item.getRepository());
        }
    }

    public static void clearExtension(RAssignmentExtension extension) {
        clearExtensionCollection(extension.getBooleans());
        clearExtensionCollection(extension.getDates());
        clearExtensionCollection(extension.getLongs());
        clearExtensionCollection(extension.getPolys());
        clearExtensionCollection(extension.getReferences());
        clearExtensionCollection(extension.getStrings());

        updateExtensionCounts(extension);
    }

    public static void clearExtension(RObject obj, RObjectExtensionType extType) {
        clearExtensionCollection(obj.getBooleans(), extType);
        clearExtensionCollection(obj.getDates(), extType);
        clearExtensionCollection(obj.getLongs(), extType);
        clearExtensionCollection(obj.getPolys(), extType);
        clearExtensionCollection(obj.getReferences(), extType);
        clearExtensionCollection(obj.getStrings(), extType);

        updateExtensionCounts(obj);
    }

    private static void clearExtensionCollection(Collection<? extends RAExtBase> collection) {
        Iterator<? extends RAExtBase> iterator = collection.iterator();
        while (iterator.hasNext()) {
            RAExtBase base = iterator.next();
            iterator.remove();
        }
    }

    private static void clearExtensionCollection(Collection<? extends ROExtBase> collection, RObjectExtensionType extType) {
        Iterator<? extends ROExtBase> iterator = collection.iterator();
        while (iterator.hasNext()) {
            ROExtBase base = iterator.next();
            if (extType.equals(base.getOwnerType())) {
                iterator.remove();
            }
        }
    }

    public static void updateExtensionCounts(RAssignmentExtension extension) {
        extension.setStringsCount((short) extension.getStrings().size());
        extension.setDatesCount((short) extension.getDates().size());
        extension.setPolysCount((short) extension.getPolys().size());
        extension.setReferencesCount((short) extension.getReferences().size());
        extension.setLongsCount((short) extension.getLongs().size());
        extension.setBooleansCount((short) extension.getBooleans().size());
    }

    public static void updateExtensionCounts(RObject object) {
        object.setStringsCount((short) object.getStrings().size());
        object.setDatesCount((short) object.getDates().size());
        object.setPolysCount((short) object.getPolys().size());
        object.setReferencesCount((short) object.getReferences().size());
        object.setLongsCount((short) object.getLongs().size());
        object.setBooleansCount((short) object.getBooleans().size());
    }
}
