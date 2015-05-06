/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.ninja.action;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.cli.ninja.command.Bulk;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteScriptsOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;

/**
 * @author lazyman
 */
public class BulkAction extends Action<Bulk> {

    public BulkAction(Bulk params, JCommander commander) {
        super(params, commander);
    }

    @Override
    protected void executeAction() throws Exception {
        ModelPortType port = createModelPort();

        try {
            ExecuteScriptsOptionsType options = new ExecuteScriptsOptionsType();
            options.setExecuteAsynchronously(getParams().isAsync());
            options.setObjectLimit(getParams().getLimit());
            options.setOutputFormat(getParams().getOutput());

            ExecuteScriptsType parameters = new ExecuteScriptsType();
            parameters.setOptions(options);
            parameters.setMslScripts(getParams().getMslScript());
            //todo implement xml support
            //parameters.setXmlScripts(xml);

            ExecuteScriptsResponseType response = port.executeScripts(parameters);
            //todo implement
        } catch (FaultMessage ex) {
            handleError("Couldn't execute scripts", ex);
        }
    }
}
