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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * Classes implementing this interface are used to handle iterative results.
 *
 * It is only used to handle iterative search results now. It may be resused for
 * other purposes as well.
 * 
 * @author Radovan Semancik
 */
public interface ResultHandler<T extends ObjectType> {

    /**
     * Handle a single result.
     * @param object Resource object to process.
     * @return true if the operation should proceed, false if it should stop
     */
    public boolean handle(PrismObject<T> object, OperationResult parentResult);
    
}
