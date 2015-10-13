/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

/**
 * Model constants referenced from the outside.
 * (TODO reconsider with regards to SchemaConstants)
 *
 * @author mederly
 */
public class ModelPublicConstants {
	
	public static final String NS_SYNCHRONIZATION_PREFIX = SchemaConstants.NS_MODEL +"/synchronization";
	public static final String NS_SYNCHRONIZATION_TASK_PREFIX = NS_SYNCHRONIZATION_PREFIX + "/task";
	public static final String DELETE_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/delete/handler-3";

}
