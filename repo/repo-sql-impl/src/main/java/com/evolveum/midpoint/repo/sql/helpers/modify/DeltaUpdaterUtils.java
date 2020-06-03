/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
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
import org.hibernate.Session;

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
                        || pair.getPrism().equals(item.getPrism(), EquivalenceStrategy.IGNORE_METADATA)) {  //todo
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

                PrismValue value = (PrismValue) item.find(ItemPath.create(container.getId()));

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

    public static void clearExtension(RAssignmentExtension extension, Session session) {
        clearExtensionCollection(extension.getBooleans(), session);
        clearExtensionCollection(extension.getDates(), session);
        clearExtensionCollection(extension.getLongs(), session);
        clearExtensionCollection(extension.getPolys(), session);
        clearExtensionCollection(extension.getReferences(), session);
        clearExtensionCollection(extension.getStrings(), session);
    }

    public static void clearExtension(RObject obj, RObjectExtensionType extType, Session session) {
        clearExtensionCollection(obj.getBooleans(), extType, session);
        clearExtensionCollection(obj.getDates(), extType, session);
        clearExtensionCollection(obj.getLongs(), extType, session);
        clearExtensionCollection(obj.getPolys(), extType, session);
        clearExtensionCollection(obj.getReferences(), extType, session);
        clearExtensionCollection(obj.getStrings(), extType, session);
    }

    private static void clearExtensionCollection(Collection<? extends RAExtBase<?>> dbCollection, Session session) {
        Iterator<? extends RAExtBase> iterator = dbCollection.iterator();
        while (iterator.hasNext()) {
            RAExtBase dbValue = iterator.next();
            // we cannot filter on assignmentExtensionType because it is not present in database (yet)
            iterator.remove();
        }
    }

    private static void clearExtensionCollection(Collection<? extends ROExtBase<?>> dbCollection, RObjectExtensionType typeToDelete,
            Session session) {
        Iterator<? extends ROExtBase> iterator = dbCollection.iterator();
        //noinspection Java8CollectionRemoveIf
        while (iterator.hasNext()) {
            ROExtBase dbValue = iterator.next();
            if (typeToDelete.equals(dbValue.getOwnerType())) {
                iterator.remove();
            }
        }
    }
}
