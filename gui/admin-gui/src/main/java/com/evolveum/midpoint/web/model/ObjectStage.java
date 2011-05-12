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

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import java.io.Serializable;

/**
 *
 * @author semancik
 */
public class ObjectStage implements Serializable {

	private static final long serialVersionUID = 3135310115198604007L;

	//TODO: temporary ... stage JAXB object should be here instead
    transient ObjectType object;

    public ObjectType getObject()
    {
        return object;
    }

    public void setObject(ObjectType object) {
        this.object = object;
    }

}
