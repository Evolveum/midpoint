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

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccessConfigurationType;

/**
 * Factory toe create ResourceConnector based on a resource type.
 *
 * @author elek
 */
public interface ResourceFactory {

    public <T extends ResourceAccessInterface<C>,C extends ResourceConnector<?>> T checkout(Class<T> c, C resource, ResourceAccessConfigurationType configuration) throws InitialisationException;

    public <T extends ResourceAccessInterface<?>> void checkin(T resourceAccessInterface);
}
