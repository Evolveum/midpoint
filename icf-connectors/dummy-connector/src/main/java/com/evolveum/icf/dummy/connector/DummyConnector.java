/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.*;

import java.io.FileNotFoundException;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.PoolableConnector;

import com.evolveum.icf.dummy.resource.DummyResource;

/**
 * Connector for the Dummy Resource, modern version (with delta update), scripting and everything else.
 *
 * @see DummyResource
 *
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME", configurationClass = DummyConfiguration.class)
public class DummyConnector extends AbstractModernObjectDummyConnector implements PoolableConnector, AuthenticateOp,
        ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<Filter>, SyncOp,
        TestOp, UpdateDeltaOp, InstanceNameAware {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(DummyConnector.class);

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
