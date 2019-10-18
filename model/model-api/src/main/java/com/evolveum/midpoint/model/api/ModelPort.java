/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    public static final QName GET_OBJECT_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "getObjectResponse");
    public static final QName SEARCH_OBJECTS_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "searchObjectsResponse");
    public static final QName EXECUTE_CHANGES_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "executeChangesResponse");
    public static final QName FIND_SHADOW_OWNER_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "findShadowOwnerResponse");
    public static final QName TEST_RESOURCE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "testResourceResponse");
    public static final QName EXECUTE_SCRIPTS_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "executeScriptsResponse");
    public static final QName IMPORT_FROM_RESOURCE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "importFromResourceResponse");
    public static final QName NOTIFY_CHANGE_RESPONSE = new QName(SchemaConstants.NS_MODEL_WS, "notifyChangeResponse");
}
