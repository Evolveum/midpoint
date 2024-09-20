/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.util.ExecutedDeltaPostProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * @author katka
 *
 */
public interface PrismObjectWrapper<O extends ObjectType> extends PrismContainerWrapper<O> {

    ObjectDelta<O> getObjectDelta() throws CommonException;

    PrismObject<O> getObject();

    PrismObject<O> getObjectOld();

    PrismObject<O> getObjectApplyDelta() throws CommonException;

    String getOid();

    PrismObjectValueWrapper<O> getValue();

    /**
     * Collect processor with deltas and consumer, that should be processed before basic deltas of showed object
     */
    Collection<ExecutedDeltaPostProcessor> getPreconditionDeltas(ModelServiceLocator serviceLocator, OperationResult result) throws CommonException;
}
