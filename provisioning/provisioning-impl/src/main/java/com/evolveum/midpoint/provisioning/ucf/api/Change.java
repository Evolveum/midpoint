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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public final class Change {
	
    private Set<Property> identifiers;
    private ObjectChangeType change;
    private Token token;

    public Change(Set<Property> identifiers, ObjectChangeType change, Token token) {
        this.identifiers = identifiers;
        this.change = change;
        this.token = token;
    }

    public ObjectChangeType getChange() {
        return change;
    }

    public void setChange(ObjectChangeType change) {
        this.change = change;
    }

    // Is ResourceObject really the right type here?
    // TODO: If yes then explain why
    public Set<Property> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Set<Property> identifiers) {
        this.identifiers = identifiers;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

}