/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface ObjectReference extends Serializable {

    String F_TARGET_OID = "targetOid";

    String F_RELATION_NAMESPACE = "relationNamespace";

    String F_RELATION_LOCAL_PART = "relationLocalPart";

    String F_TYPE = "type";

    String getTargetOid();

    String getRelationNamespace();

    String getRelationLocalPart();

    String getDescription();

    String getFilter();

    RContainerType getType();
}
