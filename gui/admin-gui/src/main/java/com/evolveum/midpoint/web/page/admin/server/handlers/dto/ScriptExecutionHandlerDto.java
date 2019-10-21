/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.WebXmlUtil;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import org.apache.wicket.Application;

/**
 * @author mederly
 */
public class ScriptExecutionHandlerDto extends HandlerDto {

    public static final String F_SCRIPT = "script";

    public ScriptExecutionHandlerDto(TaskDto taskDto) {
        super(taskDto);
    }

    public String getScript() {
        ExecuteScriptType script = taskDto.getExtensionPropertyRealValue(SchemaConstants.SE_EXECUTE_SCRIPT, ExecuteScriptType.class);
        if (script == null) {
            return null;
        }
        PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
        try {
            return WebXmlUtil.stripNamespaceDeclarations(
                    prismContext.xmlSerializer().serializeAnyData(script, SchemaConstants.SE_EXECUTE_SCRIPT));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize script: " + e.getMessage(), e);
        }
    }

}
