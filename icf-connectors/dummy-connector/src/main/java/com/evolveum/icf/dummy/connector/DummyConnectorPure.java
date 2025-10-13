/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.connector;

import com.evolveum.icf.dummy.resource.DummyResource;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

/**
 * Connector for the Dummy Resource, pure version.
 * This version has just the "pure" object operations. It does NOT have scripting.
 *
 * @see DummyResource
 *
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME", configurationClass = DummyConfiguration.class)
public class DummyConnectorPure extends AbstractModernObjectDummyConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        SearchOp<Filter>, SyncOp, TestOp, UpdateDeltaOp, InstanceNameAware {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(DummyConnectorPure.class);


}
