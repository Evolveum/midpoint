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
package com.evolveum.midpoint.model.sync;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;

/**
 * 
 * @author lazyman
 *
 */
//@Service
public class SynchronizationService implements ResourceObjectChangeListenerPortType {

	@Autowired(required=true)
	private ModelController model;
	
	@Override
	public EmptyType notifyChange(ResourceObjectShadowChangeDescriptionType change) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}
}
