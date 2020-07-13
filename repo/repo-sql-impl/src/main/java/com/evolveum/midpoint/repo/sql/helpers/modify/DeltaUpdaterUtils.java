/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.modify;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.Session;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtBase;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBase;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeltaUpdaterUtils {

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
