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

package com.evolveum.midpoint.model.test.util.equal;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;

/**
 *
 * @author lazyman
 */
public class ObjectReferenceTypeComparator extends Equals<ObjectReferenceType> {

    @Override
    public boolean areEqual(ObjectReferenceType o1, ObjectReferenceType o2) {
        if (o1 == o2) {
            return true;
        }
        if (!(o1 == null ? o2 == null : o2 != null)) {
            return false;
        }
        return areStringEqual(o1.getOid(), o2.getOid()) && areQNameEqual(o1.getType(), o2.getType());
    }
}
