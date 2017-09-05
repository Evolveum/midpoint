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
package com.evolveum.midpoint.model.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
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

	public static final QName OBJECTCLASS_PROPERTY_NAME = new QName(ModelConstants.NS_EXTENSION, "objectclass");

	public static final QName KIND_PROPERTY_NAME = new QName(ModelConstants.NS_EXTENSION, "kind");

	public static final QName INTENT_PROPERTY_NAME = new QName(ModelConstants.NS_EXTENSION, "intent");

	public static final QName FILENAME_PROPERTY_NAME = new QName(ModelConstants.NS_EXTENSION, "filename");


}
