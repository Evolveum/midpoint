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
 * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#accountType
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class AccountObjectClassDefinition extends ResourceObjectDefinition {
    
    public static final String code_id = "$Id$";

    /**
     * Flag which says that this is default account object class
     * (read from schema handling, or by default from schema)
     */
    private boolean isDefault = false;

    public AccountObjectClassDefinition(QName qname) {
        super(qname);
    }

    public AccountObjectClassDefinition(QName qname, String nativeObjectClass) {
        super(qname, nativeObjectClass);
    }
    
    public ResourceAttributeDefinition getPasswordAttribute(){
        for (ResourceAttributeDefinition ra : getAttributes()){
            if (ra.isPasswordAttribute()) {
                return ra;
            }
        }
        return null;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
