/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;

import javax.xml.namespace.QName;

/**
 *
 * @author lazyman
 *
 */
public interface ModelPort {

	String CLASS_NAME_WITH_DOT = ModelPortType.class.getName() + ".";
	String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
	String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
	String EXECUTE_CHANGES = CLASS_NAME_WITH_DOT + "executeChanges";
	String LIST_ACCOUNT_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "listAccountShadowOwner";
	String TEST_RESOURCE = CLASS_NAME_WITH_DOT + "testResource";
	String IMPORT_FROM_RESOURCE = CLASS_NAME_WITH_DOT + "importFromResource";
	String NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";
    String EXECUTE_SCRIPTS = CLASS_NAME_WITH_DOT + "executeScripts";

    QName GET_OBJECT_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "getObjectResponse");
    QName SEARCH_OBJECTS_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "searchObjectsResponse");
    QName EXECUTE_CHANGES_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "executeChangesResponse");
    QName FIND_SHADOW_OWNER_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "findShadowOwnerResponse");
    QName TEST_RESOURCE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "testResourceResponse");
    QName EXECUTE_SCRIPTS_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "executeScriptsResponse");
    QName IMPORT_FROM_RESOURCE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "importFromResourceResponse");
    QName NOTIFY_CHANGE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "notifyChangeResponse");
}
