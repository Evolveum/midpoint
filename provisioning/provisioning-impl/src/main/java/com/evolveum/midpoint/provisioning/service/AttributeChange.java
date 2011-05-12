/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;

/**
 *
 * TODO: Documentation
 *
 * @author semancik
 */
public class AttributeChange {

    protected PropertyModificationTypeType changeType;

    protected ResourceAttribute attribute;


    /**
     * Get the value of changeType
     *
     * TODO: this is not really OK to use JAXB type
     * PropertyChangeTypeType here. But it is lesser
     * evil for now.
     *
     * @return the value of changeType
     */
    public PropertyModificationTypeType getChangeType() {
        return changeType;
    }

    /**
     * Set the value of changeType
     *
     * TODO: this is not really OK to use JAXB type
     * PropertyChangeTypeType here. But it is lesser
     * evil for now.
     *
     * @param changeType new value of changeType
     */
    public void setChangeType(PropertyModificationTypeType changeType) {
        this.changeType = changeType;
    }

    /**
     * Get the value of attribute
     *
     * @return the value of attribute
     */
    public ResourceAttribute getAttribute() {
        return attribute;
    }

    /**
     * Set the value of attribute
     *
     * @param attribute new value of attribute
     */
    public void setAttribute(ResourceAttribute attribute) {
        this.attribute = attribute;
    }


}
