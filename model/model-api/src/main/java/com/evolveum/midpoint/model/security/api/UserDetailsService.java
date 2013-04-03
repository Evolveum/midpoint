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
 */

package com.evolveum.midpoint.model.security.api;

import com.evolveum.midpoint.common.security.MidPointPrincipal;

/**
 * Service that exposes security functions for GUI and other spring-security-enabled authentication front-ends. 
 *
 * @author lazyman
 * @author Igor Farinic
 */
public interface UserDetailsService {
    
    String DOT_CLASS = UserDetailsService.class.getName() + ".";
    String OPERATION_GET_USER = DOT_CLASS + "getUser";
    String OPERATION_UPDATE_USER = DOT_CLASS + "updateUser";

    public MidPointPrincipal getUser(String principal);

    public void updateUser(MidPointPrincipal user);
}
