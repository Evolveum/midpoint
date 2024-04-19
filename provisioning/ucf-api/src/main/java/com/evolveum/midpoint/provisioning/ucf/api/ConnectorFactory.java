/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Interface to Unified Connector functionality.
 *
 * This is considered the "main" interface of the Unified Connector
 * Framework (UCF) API. It can be used to instantiate connectors which in
 * turn can be used to invoke operations on resources.
 *
 * The UCF interface is considered to be an abstraction internal to the
 * midPoint project and not really reusable. It is using objects from the
 * midPoint data model. But the longer-term ambition is to use this
 * interface as an prototype of ICF replacement, as ICF is burdened by
 * numerous problems.
 *
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.
 *
 * @author Radovan Semancik
 */
public interface ConnectorFactory {

    String OPERATION_LIST_CONNECTORS = ConnectorFactory.class.getName()+".listConnectors";

    /**
     * Generates a {@link ConnectorSchema} for the connector configuration (mainly holding connector-specific properties).
     * Returns `null` if there is no schema provided by ConnId.
     */
    @Nullable ConnectorSchema generateConnectorConfigurationSchema(@NotNull ConnectorType connectorBean)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Creates new unconfigured instance of the connector.
     *
     * This factory is NOT required to cache or pool the connector instances.
     * Call to this method may create new connector instance each time it is
     * called unless an underlying framework is pooling connector instances.
     *
     * @return configured and initialized connector instance
     * @throws ObjectNotFoundException if the specified connector was not found
     * @throws SchemaException if there's any schema issue
     */
    @NotNull ConnectorInstance createConnectorInstance(ConnectorType connectorBean, String instanceName, String desc)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Returns a list of all known connectors.
     *
     * The returned list contains all connectors known to the system, whether
     * they are used or not, whether they are configured or not. It should
     * be used to list the "capabilities" of the system.
     *
     * Returned connector objects are "virtual". They may not be stored in the
     * persistent repository and they may disappear if the connector disappears
     * from the system. The returned connector objects are immutable.
     *
     * @param host definition of a connector host or null for local connector list
     * @return list of all known connectors.
     */
    Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentResult) throws CommunicationException;

    /**
     * Execute self-test for each connector framework that is capable of executing tests.
     */
    void selfTest(OperationResult parentTestResult);

    boolean supportsFramework(String frameworkIdentifier);

    /**
     * Return a version of the framework.
     * TODO reconsider when there will be more than one framework
     */
    String getFrameworkVersion();

    /**
     * Registers an listener for automatic connector discovery
     *
     * @param listener Listener which will be notified once new connector is discovered
     */
    void registerDiscoveryListener(ConnectorDiscoveryListener listener);

    void shutdown();
}
