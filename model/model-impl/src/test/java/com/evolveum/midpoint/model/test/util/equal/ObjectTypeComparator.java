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

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 *
 * @author lazyman
 */
public class ObjectTypeComparator extends Equals<ObjectType> {

    private boolean comparingUid;

    public ObjectTypeComparator() {
        this(true);
    }

    public ObjectTypeComparator(boolean comparingUid) {
        this.comparingUid = comparingUid;
    }

    public void setComparingUid(boolean comparingUid) {
        this.comparingUid = comparingUid;
    }

    public boolean isComparingUid() {
        return comparingUid;
    }

    @Override
    public boolean areEqual(ObjectType o1, ObjectType o2) {
        if (o1 == o2) {
            return true;
        }
        if (!(o1 == null ? o2 == null : o2 != null)) {
            return false;
        }

        if (comparingUid && !areStringEqual(o1.getOid(), o2.getOid())) {
            return false;
        }

        return areStringEqual(o1.getName(), o2.getName()) &&
                areStringEqual(o1.getVersion(), o2.getVersion());
    }
}
