/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismObjectValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public interface PrismObjectWrapper<O extends ObjectType> extends PrismContainerWrapper<O> {


    ObjectDelta<O> getObjectDelta() throws SchemaException;

    PrismObject<O> getObject();

    PrismObject<O> getObjectOld();

    PrismObject<O> getObjectApplyDelta() throws SchemaException;

    String getOid();

    PrismObjectValueWrapper<O> getValue();
}
