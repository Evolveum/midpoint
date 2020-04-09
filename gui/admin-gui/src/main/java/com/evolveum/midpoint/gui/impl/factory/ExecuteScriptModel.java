/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

public class ExecuteScriptModel implements IModel<String> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ExecuteScriptModel.class);

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
            // TODO handle!!!!
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize script", e);
            ThreadContext.getSession().error("Cannot serialize script: " + e.getMessage());
//                getSession().error("Cannot serialize filter");
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
            // TODO handle!!!!
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse script", e);
            ThreadContext.getSession().error("Cannot parse script: " + e.getMessage());
//                getSession().error("Cannot parse filter");
        }
    }
}
