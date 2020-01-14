package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import java.io.Serializable;

public interface SelectableBean<T extends Serializable> extends Serializable, InlineMenuable, DebugDumpable {

    T getValue();

    void setValue(T value);

    OperationResult getResult();

    void setResult(OperationResult result);

    void setResult(OperationResultType resultType) throws SchemaException;

    void setSelected(boolean selected);

    boolean isSelected();
}
