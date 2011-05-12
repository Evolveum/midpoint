/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector.script;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorScriptExecutor {

    public static final String code_id = "$Id$";

    public Object execute(ConnectorFacade connector, ConnectorScript script)
            throws MidPointException {

        String method = "execute";

        ScriptContextBuilder builder = script.getScriptContextBuilder();
        //String scriptLanguage = builder.getScriptLanguage();
        //String actionName = script.getActionName();
        String execMode = script.getExecMode();
        //log("execute", "Executing " + scriptLanguage + " resource action '" + actionName + "'");

        ScriptOnResourceApiOp scriptOnResource = (ScriptOnResourceApiOp) connector.getOperation(ScriptOnResourceApiOp.class);
        ScriptOnConnectorApiOp scriptOnConnector = (ScriptOnConnectorApiOp) connector.getOperation(ScriptOnConnectorApiOp.class);       

        ScriptContext scriptContext = builder.build();
        OperationOptions opOptions = script.getOperationOptionsBuilder().build();

        Object scriptResult = null;

        if (execMode.equals("connector")) {
            if (scriptOnConnector == null) {
                //(Severity.ERROR, "ERR_UNSUPPORTED_CONN_OP", "ScriptOnConnector");
                throw new MidPointException();
            }
            try {
                scriptResult = scriptOnConnector.runScriptOnConnector(scriptContext, opOptions);
            } catch (Exception e) {
                //(Severity.ERROR, "ERR_CONN_SCRIPT_EXEC_FAILED", new Object[]{actionName, script.getResourceName()});
                throw new MidPointException();
            }

        }

        if (execMode.equals("resource")) {
            if (scriptOnResource == null) {
                //(Severity.ERROR, "ERR_UNSUPPORTED_CONN_OP", "ScriptOnResource");
                throw new MidPointException();
            }
            try {
                scriptResult = scriptOnResource.runScriptOnResource(scriptContext, opOptions);
            } catch (Exception e) {
                //(Severity.ERROR, "ERR_CONN_SCRIPT_EXEC_FAILED", new Object[]{actionName, script.getResourceName()});
                throw new MidPointException();
            }

        }

        return scriptResult;
    }
}
