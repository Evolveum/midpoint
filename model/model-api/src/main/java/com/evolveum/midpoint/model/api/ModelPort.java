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
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
public interface ModelPort {

	String CLASS_NAME_WITH_DOT = ModelPortType.class.getName() + ".";
	String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
	String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
	String LIST_OBJECTS = CLASS_NAME_WITH_DOT + "listObjects";
	String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
	String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
	String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";
	String GET_PROPERTY_AVAILABLE_VALUES = CLASS_NAME_WITH_DOT + "getPropertyAvailableValues";
	String LIST_ACCOUNT_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "listAccountShadowOwner";
	String LIST_RESOURCE_OBJECT_SHADOWS = CLASS_NAME_WITH_DOT + "listResourceObjectShadows";
	String LIST_RESOURCE_OBJECTS = CLASS_NAME_WITH_DOT + "listResourceObjects";
	String TEST_RESOURCE = CLASS_NAME_WITH_DOT + "testResource";
	String IMPORT_FROM_RESOURCE = CLASS_NAME_WITH_DOT + "importFromResource";
}
