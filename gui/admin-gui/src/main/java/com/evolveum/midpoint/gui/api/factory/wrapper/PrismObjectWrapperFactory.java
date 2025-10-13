/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public interface PrismObjectWrapperFactory<O extends ObjectType> {

    PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws SchemaException;

    void updateWrapper(PrismObjectWrapper<O> wrapper, WrapperContext context) throws SchemaException;

}
