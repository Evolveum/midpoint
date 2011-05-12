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

package com.evolveum.midpoint.provisioning.schema;

import javax.xml.namespace.QName;

/**
 * Enum of modifier flags to use for attributes. Note that
 * this enum is designed for configuration by exception such that
 * an empty set of flags are the defaults:
 * <ul>
 *     <li>updateable</li>
 *     <li>creatable</li>
 *     <li>returned by default</li>
 *     <li>readable</li>
 *     <li>single-valued</li>
 *     <li>optional</li>
 * </ul>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public enum AttributeFlag {

    /**
     * I will add more enums later. This is a string list so anytime can be
     * extended without any change.
     */
    AUDITED("AUDITED"),
    NOT_QUERYABLE("NOT_QUERYABLE"),
    PASSWORD("PASSWORD"),
    /**
     * The attribute will be stored in repository
     */
    STORE_IN_REPOSITORY("STORE_IN_REPOSITORY"),
    NOT_CREATABLE("NOT_CREATABLE"),
    NOT_UPDATEABLE("NOT_UPDATEABLE"),
    NOT_READABLE("NOT_READABLE"),
    NOT_RETURNED_BY_DEFAULT("NOT_RETURNED_BY_DEFAULT"),
    /**
     * Custom attribute that is not belong to the resource but the business
     * requires this
     */
    IGNORE_ATTRIBUTE("IGNORE"),
    /**
     * The attribute value is generated outside the midPoint and it must be read
     * before it sent to the resource
     */
    REMOTE_ATTRIBUTE("REMOTE"),
    SOMETHING("key");
    private String value;
    
    /**
     * TODO: This is not part of the Schema, the provisioner use it.
     *
     */
    private QName operation;

    private AttributeFlag(String _key) {
        this.value = _key;
    }

    public String getKey() {
        return value;

    }

    public static AttributeFlag findByKey(String value) {
        for (AttributeFlag f : AttributeFlag.values()) {
            if (f.getKey().equals(value)) {
                return f;
            }
        }
        return null;
    }
}
