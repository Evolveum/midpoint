/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemCollectionsUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectDiscriminator {

    private QName objectClass;
    private Collection<? extends ResourceAttribute<?>> primaryIdentifiers;

    public ResourceObjectDiscriminator(QName objectClass, Collection<? extends ResourceAttribute<?>> primaryIdentifiers) {
        super();
        this.objectClass = objectClass;
        this.primaryIdentifiers = primaryIdentifiers;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    public boolean matches(PrismObject<ShadowType> shadow) {
        ShadowType shadowType = shadow.asObjectable();
        if (!objectClass.equals(shadowType.getObjectClass())) {
            return false;
        }
        Collection<ResourceAttribute<?>> shadowIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        return ItemCollectionsUtil.compareCollectionRealValues(primaryIdentifiers, shadowIdentifiers);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((primaryIdentifiers == null) ? 0 : primaryIdentifiers.hashCode());
        result = prime * result + ((objectClass == null) ? 0 : objectClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceObjectDiscriminator other = (ResourceObjectDiscriminator) obj;
        if (primaryIdentifiers == null) {
            if (other.primaryIdentifiers != null)
                return false;
        } else if (!primaryIdentifiers.equals(other.primaryIdentifiers))
            return false;
        if (objectClass == null) {
            if (other.objectClass != null)
                return false;
        } else if (!objectClass.equals(other.objectClass))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ResourceObjectDiscriminator(" + objectClass + ": " + primaryIdentifiers + ")";
    }

}
