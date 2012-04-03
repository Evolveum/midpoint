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

package com.evolveum.midpoint.model.security.api;

/**
 * Temporary place, till we create special component for it
 *
 * @author lazyman
 * @author Igor Farinic
 */
public interface UserDetailsService {
    
    String DOT_CLASS = UserDetailsService.class.getName() + ".";
    String OPERATION_GET_USER = DOT_CLASS + "getUser";
    String OPERATION_UPDATE_USER = DOT_CLASS + "updateUser";

    public PrincipalUser getUser(String principal);

    public void updateUser(PrincipalUser user);
}
