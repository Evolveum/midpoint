/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public interface SelectableBean<T extends Serializable> extends Serializable, DebugDumpable {

    T getValue();

    void setValue(T value);

    OperationResult getResult();

    void setResult(OperationResult result);

    void setResult(OperationResultType resultType) throws SchemaException;

    void setSelected(boolean selected);

    boolean isSelected();
}
