/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;

public class ContainerableFromPrismObjectModel<O extends ObjectType> implements IModel<O> {

    private IModel<PrismObject<O>> prismObjectModel;

    public ContainerableFromPrismObjectModel(IModel<PrismObject<O>> prismObjectModel) {
        Validate.notNull(prismObjectModel);
        this.prismObjectModel = prismObjectModel;
    }

    @Override
    public O getObject() {
        PrismObject<O> object = prismObjectModel.getObject();
        return object != null ? object.asObjectable() : null;
    }

    @Override
    public void setObject(O o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void detach() {
    }
}
