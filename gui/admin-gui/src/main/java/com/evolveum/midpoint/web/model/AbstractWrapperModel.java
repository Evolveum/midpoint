/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.model;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public abstract class AbstractWrapperModel<T, O extends ObjectType> implements IModel<T> {

    private final IModel<PrismObjectWrapper<O>> wrapperModel;

    public AbstractWrapperModel(IModel<PrismObjectWrapper<O>> wrapperModel) {
        Validate.notNull(wrapperModel, "Wrapper model must not be null.");
        this.wrapperModel = wrapperModel;
    }

    public IModel<PrismObjectWrapper<O>> getWrapperModel() {
        return wrapperModel;
    }

    public PrismObjectWrapper<O> getWrapper() {
        return wrapperModel.getObject();
    }

    public O getObjectType() {
        return wrapperModel.getObject().getObject().asObjectable();
    }

    public PrismObject<O> getPrismObject() {
        return wrapperModel.getObject().getObject();
    }

    @Override
    public void detach() {
    }

}
