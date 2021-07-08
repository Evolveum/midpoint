/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

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

    public static final ItemName OBJECTCLASS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_MODEL_EXTENSION, "objectclass");
    public static final ItemName KIND_PROPERTY_NAME = new ItemName(SchemaConstants.NS_MODEL_EXTENSION, "kind");
    public static final ItemName INTENT_PROPERTY_NAME = new ItemName(SchemaConstants.NS_MODEL_EXTENSION, "intent");

}
