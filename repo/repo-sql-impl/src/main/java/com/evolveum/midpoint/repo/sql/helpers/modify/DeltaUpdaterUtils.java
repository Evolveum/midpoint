/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import java.util.Collection;
import java.util.Iterator;

import jakarta.persistence.EntityManager;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtBase;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBase;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeltaUpdaterUtils {

    public static void clearExtension(RAssignmentExtension extension, EntityManager em) {
        clearExtensionCollection(extension.getBooleans(), em);
        clearExtensionCollection(extension.getDates(), em);
        clearExtensionCollection(extension.getLongs(), em);
        clearExtensionCollection(extension.getPolys(), em);
        clearExtensionCollection(extension.getReferences(), em);
        clearExtensionCollection(extension.getStrings(), em);
    }

    public static void clearExtension(RObject obj, RObjectExtensionType extType, EntityManager em) {
        clearExtensionCollection(obj.getBooleans(), extType, em);
        clearExtensionCollection(obj.getDates(), extType, em);
        clearExtensionCollection(obj.getLongs(), extType, em);
        clearExtensionCollection(obj.getPolys(), extType, em);
        clearExtensionCollection(obj.getReferences(), extType, em);
        clearExtensionCollection(obj.getStrings(), extType, em);
    }

    private static void clearExtensionCollection(Collection<? extends RAExtBase<?>> dbCollection, EntityManager em) {
        Iterator<? extends RAExtBase> iterator = dbCollection.iterator();
        while (iterator.hasNext()) {
            RAExtBase dbValue = iterator.next();
            // we cannot filter on assignmentExtensionType because it is not present in database (yet)
            iterator.remove();
        }
    }

    private static void clearExtensionCollection(Collection<? extends ROExtBase<?>> dbCollection, RObjectExtensionType typeToDelete,
            EntityManager em) {
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
