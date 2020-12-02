/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyModificationOperationType;

/**
 * Abstract operation for a connector. Subclasses of this class
 * represent specific operations such as attribute modification,
 * script execution and so on.
 *
 * This class is created primarily for type safety, but it may be
 * extended later on.
 *
 * @author Radovan Semancik
 *
 */
public abstract class Operation implements DebugDumpable {

    /** Converts this Operation into respective xType bean */
    public abstract PropertyModificationOperationType asBean(PrismContext prismContext) throws SchemaException;
}
