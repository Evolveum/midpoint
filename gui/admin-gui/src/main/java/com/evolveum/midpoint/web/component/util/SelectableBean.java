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
import com.evolveum.midpoint.web.page.admin.server.dto.TaskInformationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.apache.wicket.model.IModel;

public interface SelectableBean<T extends Serializable> extends SelectableRow<T>, Serializable, DebugDumpable {

    T getValue();

    void setModel(IModel<T> value);

//    void setValue(T value);

    OperationResult getResult();

    void setResult(OperationResult result);

    void setResult(OperationResultType resultType) throws SchemaException;

    void setSelected(boolean selected);

    boolean isSelected();

    /**
     * Obtains custom data related to T (e.g. information extracted from the value of T) into the bean.
     * Currently used to store {@link TaskInformationUtil} for tasks.
     *
     * FIXME: TEMPORARY SOLUTION! Replace by subclassing {@link SelectableBeanImpl} for tasks!
     */
    Object getCustomData();

    /**
     * Stores custom data, see {@link #getCustomData()}.
     *
     * FIXME: TEMPORARY SOLUTION!
     */
    void setCustomData(Object data);

}
