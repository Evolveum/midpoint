/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author semancik
 *
 */
public class ModelConstants {

	public static final String NS_MODEL_TRIGGER_PREFIX = SchemaConstants.NS_MODEL +"/trigger";

	public static final String NS_SYNCHRONIZATION_TASK_PREFIX = ModelPublicConstants.NS_SYNCHRONIZATION_TASK_PREFIX;

	public static final String NS_IMPORT_OBJECTS_PREFIX = SchemaConstants.NS_MODEL +"/import-objects";
	public static final String NS_IMPORT_OBJECTS_TASK_PREFIX = NS_IMPORT_OBJECTS_PREFIX + "/task";

	public static final String NS_EXTENSION = SchemaConstants.NS_MODEL + "/extension-3";

	public static final ItemName OBJECTCLASS_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "objectclass");

	public static final ItemName KIND_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "kind");

	public static final ItemName INTENT_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "intent");

	public static final ItemName FILENAME_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "filename");


}
