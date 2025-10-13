/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ObjectDeltaModel implements IModel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaModel.class);

    private IModel<ObjectDeltaType> baseModel;
    private ModelServiceLocator locator;

    public ObjectDeltaModel(IModel<ObjectDeltaType> model, ModelServiceLocator locator) {
        this.baseModel = model;
        this.locator = locator;
    }

    @Override
    public String getObject() {
        try {
            ObjectDeltaType value = baseModel.getObject();
            if (value == null) {
                return null;
            }

            return locator.getPrismContext().xmlSerializer().serializeRealValue(value);
        } catch (Exception e) {
            // TODO handle!!!!
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize delta", e);
            ThreadContext.getSession().error("Cannot serialize delta: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setObject(String object) {
        if (StringUtils.isBlank(object)) {
            return;
        }

        try {
            ObjectDeltaType script = locator.getPrismContext().parserFor(object).parseRealValue(ObjectDeltaType.class);
            baseModel.setObject(script);
        } catch (Exception e) {
            // TODO handle!!!!
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse delta", e);
            ThreadContext.getSession().error("Cannot parse delta: " + e.getMessage());
        }
    }
}
