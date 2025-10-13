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
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

public class ExecuteScriptModel implements IModel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteScriptModel.class);

    private IModel<ExecuteScriptType> baseModel;
    private ModelServiceLocator locator;

    public ExecuteScriptModel(IModel<ExecuteScriptType> model, ModelServiceLocator locator) {
        this.baseModel = model;
        this.locator = locator;
    }

    @Override
    public String getObject() {
        try {
            ExecuteScriptType value = baseModel.getObject();
            if (value == null) {
                return null;
            }

            return locator.getPrismContext().xmlSerializer().serializeRealValue(value);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize script", e);
            ThreadContext.getSession().error("Cannot serialize script: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setObject(String object) {
        if (StringUtils.isBlank(object)) {
            return;
        }

        try {
            ExecuteScriptType script = locator.getPrismContext().parserFor(object).parseRealValue(ExecuteScriptType.class);
            baseModel.setObject(script);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse script", e);
            ThreadContext.getSession().error("Cannot parse script: " + e.getMessage());
        }
    }
}
