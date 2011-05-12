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

import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorScript {

    public static final String code_id = "$Id$";
    private ScriptContextBuilder _scriptContextBuilder;
    private OperationOptionsBuilder _operationOptionsBuilder;
    private String _actionName;
    private String _execMode;
    private String _resourceName;

    public String getResourceName() {
        return this._resourceName;
    }

    public void setResourceName(String resourceName) {
        this._resourceName = resourceName;
    }

    public ScriptContextBuilder getScriptContextBuilder() {
        if (this._scriptContextBuilder == null) {
            this._scriptContextBuilder = new ScriptContextBuilder();
        }

        return this._scriptContextBuilder;
    }

    public String getActionName() {
        return this._actionName;
    }

    public void setActionName(String actionName) {
        this._actionName = actionName;
    }

    public String getExecMode() {
        return this._execMode;
    }

    public void setExecMode(String execMode) {
        this._execMode = execMode;
    }

    public OperationOptionsBuilder getOperationOptionsBuilder() {
        if (this._operationOptionsBuilder == null) {
            this._operationOptionsBuilder = new OperationOptionsBuilder();
        }

        return this._operationOptionsBuilder;
    }
}
