package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ConnectorDevelopmentService {

    ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result);

    ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type);

    /**
     * Starts (or finds) the connector development for an existing low-code (manifest-based)
     * connector, so it can be further developed - the connector's bundle is later copied (with a
     * minor-bumped version) instead of a fresh framework template being downloaded.
     *
     * <p>When a development is already linked to the connector - either through its
     * {@code connector/connectorRef} or through its {@code connector/sourceConnectorRef} - that
     * development is returned untouched. Otherwise a new {@link ConnectorDevelopmentType} is
     * created, prefilled from the connector and its manifest (application name/description,
     * connector coordinates with a bumped version, scripts and object classes), and returned.
     *
     * @param sourceConnector the existing low-code connector to develop from
     * @return the existing or newly created development
     * @throws CommonException when the connector has no local bundle or is not manifest-based
     */
    ConnectorDevelopmentType startFromExisting(ConnectorType sourceConnector, Task task, OperationResult result)
            throws CommonException;

    /**
     * Whether the local bundle of the given connector is a low-code (manifest-based) connector,
     * i.e. its bundle directory carries a {@code connector.manifest.yaml} or
     * {@code connector.manifest.json} file. Such connectors can be imported into the connector
     * development (see {@link #startFromExisting}); anything else returns {@code false}.
     */
    boolean isManifestBasedConnector(ConnectorType connector, OperationResult result);

    StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    /**
     * Status of the copy-connector operation submitted via
     * {@link ConnectorDevelopmentOperation#submitCopyConnector(Task, OperationResult)}.
     */
    StatusInfo<ConnDevCreateConnectorResultType> getCopyConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevProcessDocumentationResultType> getProcessDocumentationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevFixObjectClassResultType> getFixObjectClassStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverObjectClassAttributesResultType> getDiscoverObjectClassAttributesStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverObjectClassEndpointsResultType> getDiscoverObjectClassEndpointsStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevRefreshSchemaResultType> getRefreshSchemaStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverConnectivityEndpointResultType> getDiscoverConnectivityEndpointStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevExportConnectorResultType> getExportConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevExportConnectorResultType> getUploadConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    /**
     * Cluster-aware download of an exported connector bundle jar, previously stored under
     * {@code fileName} in the midPoint home "export" directory of the node identified by
     * {@code nodeOid}. Falls back to a local file read if the file exists on this node.
     */
    InputStream getExportedConnectorFileStream(String fileName, String nodeOid, Task task, OperationResult result)
            throws CommonException, IOException;

    /**
     * Resolves the conndev documentation topics packaged into the documentation JARs on the
     * classpath (see {@code META-INF/conndev-doc/docs.yaml}) for the given stable topic {@code key}.
     *
     * <p>Topics are resolved strictly by protocol: the topics whose protocol equals {@code protocol}
     * come first, then the protocol-less (generic) topics of the same key. Topics of a different
     * protocol are never returned. When several documentation JARs declare the same key and
     * protocol, the topics of the first JAR on the classpath win.
     *
     * @param key      the stable topic identifier used by the GUI screen
     * @param protocol the integration protocol to resolve for ({@code scim}, {@code rest} or
     *                 {@code sql}), or {@code null} for the generic topics only
     * @return the matching topics, protocol-specific before generic; empty when no documentation
     *         JAR declares the key
     */
    List<ConnDevDocumentationTopic> getDocumentationTopics(String key, String protocol);

}
