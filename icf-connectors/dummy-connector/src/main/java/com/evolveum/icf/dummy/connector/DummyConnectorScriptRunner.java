/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import com.evolveum.icf.dummy.resource.DummyResource;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import java.io.FileNotFoundException;

/**
 * Connector for the Dummy Resource, just for script running. It does not have any object-related operations.
 * It still has "test" operation.
 *
 * @see DummyResource
 *
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME", configurationClass = DummyConfiguration.class)
public class DummyConnectorScriptRunner extends AbstractBaseDummyConnector implements PoolableConnector,
        ScriptOnConnectorOp, ScriptOnResourceOp, TestOp, InstanceNameAware {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(DummyConnectorScriptRunner.class);

    private String instanceName;

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {

        try {
            return resource.runScript(request.getScriptLanguage(), request.getScriptText(), request.getScriptArguments());
        } catch (IllegalArgumentException e) {
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        }
    }

}
