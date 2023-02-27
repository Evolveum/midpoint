/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.util;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ObjectCollectionViewUtil {

    public static List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionView) {
        if (!isArchetypedCollectionView(collectionView)) {
            return null;
        }

        ObjectReferenceType ref = collectionView.getCollection().getCollectionRef();
        return Collections.singletonList(ref);
    }

    public static boolean isArchetypedCollectionView(CompiledObjectCollectionView view) {
        if (view == null) {
            return false;
        }

        CollectionRefSpecificationType collectionRefSpecificationType = view.getCollection();
        if (collectionRefSpecificationType == null) {
            return false;
        }

        ObjectReferenceType collectionRef = collectionRefSpecificationType.getCollectionRef();
        if (collectionRef == null) {
            return false;
        }

        return QNameUtil.match(ArchetypeType.COMPLEX_TYPE, collectionRef.getType());
    }
}
