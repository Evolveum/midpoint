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

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.annotations.ResourceAccessImplementation;
import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.conversion.SyncTokenConverter;
import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.provisioning.integration.identityconnector.schema.ResourceUtils;
import com.evolveum.midpoint.provisioning.integration.identityconnector.script.ConnectorScript;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.AttributeChange;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.provisioning.service.ResultHandler;
import com.evolveum.midpoint.provisioning.service.SynchronizationResult;
import com.evolveum.midpoint.util.MidPointResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType.SynchronizationState;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.Uid;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Basic implementation to communicate resources over Identity Connector Framework.
 *
 * @author elek
 */
@ResourceAccessImplementation(targetNamespace = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resourceaccessconfiguration-1.xsd")
public class IdentityConnectorRAI implements ResourceAccessInterface<IdentityConnector> {

    public static final QName SYNC_TOKEN_ATTRIBUTE = new QName(ResourceUtils.NS_ICF_CONFIGURATION, "syncToken", "icc");
    private static final Trace logger = TraceManager.getTrace(IdentityConnectorRAI.class);
    private static final String serviceNamespace = "http://midpoint.evolveum.com/xml/ns/public/service/provisioning/identityconnector/IdentityConnectorRAI-1.xsd";
    private IdentityConnectorService helper = new IdentityConnectorService();
    private ResourceAccessConfigurationType resourceAccessConfiguration = null;
    private IdentityConnector resourceInstance = null;
    private ResourceUtils resourceUtils = ResourceUtils.getInstance();
    ConnectorFacade connector = null;
    private LdapActivityAdapter adapter = new LdapActivityAdapter();

    @Override
    public boolean configure(ResourceAccessConfigurationType configuration) throws InitialisationException {
        this.resourceAccessConfiguration = configuration;
        return true;
    }

    @Override
    public IdentityConnector getConnector() {
        return resourceInstance;
    }

    @Override
    public boolean dispose() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceAccessInterface<IdentityConnector>> T initialise(Class<T> type, IdentityConnector resourceInstance) throws InitialisationException {
        this.resourceInstance = resourceInstance;
        try {
            connector = ConnectorUtil.createConnectorFacade(resourceInstance);
        } catch (MidPointException ex) {
            logger.error("initialise ICF RAI", ex);
            throw new InitialisationException(ex.getMessage(), ex);
        }
        return (T) this;
    }

    @Override
    public Class<IdentityConnector> getConnectorClass(String targetNamespace) {
        return IdentityConnector.class;
    }

    // Note: this is NOT called from modify operation. This should be fixed
    private Set<Attribute> convertAttributes(ResourceObject resourceObject) {
        Set<Attribute> attributes = new HashSet<Attribute>();
        adapter.preConvertAttributes(connector, resourceObject, attributes);
        resourceUtils.convertAttributes(resourceObject, attributes);
        return attributes;
    }

    @Override
    public ResourceObject add(OperationalResultType result, ResourceObject resourceObject, ResourceObjectShadowType shadow) throws MidPointException {
        //attributes to get
        Set<Attribute> attributes = convertAttributes(resourceObject);

        MidPointResult operationResult = new MidPointResult(result, new QName(serviceNamespace, "add"));

        //Remove the UID and keep only the NAME
        attributes = AttributeUtil.filterUid(attributes);

        Uid uidAfter = helper.doCreateConnectorObject(connector, resourceUtils.mapObjectClass(resourceObject.getDefinition()), attributes, operationResult);

        if (uidAfter == null) {
            throw new MidPointException("ResourceObject creation failed");
        }

        ConnectorObject co = helper.doGetConnectorObject(connector, resourceUtils.mapObjectClass(resourceObject.getDefinition()), uidAfter, attributes, operationResult);

        if (co == null) {
            throw new MidPointException("No such resource object in the provisioned resource. uid: " + uidAfter);
        }
        return resourceUtils.buildResourceObject(co, resourceObject.getDefinition());
    }

    @Override
    public ResourceObjectIdentificationType authenticate(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Method custom(OperationalResultType result, Object... input) throws MidPointException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        Set<Attribute> attributes = resourceUtils.buildConnectorObject(resourceObject);
        Uid uid = AttributeUtil.getUidAttribute(attributes);
        MidPointResult operationResult = new MidPointResult(result, new QName(serviceNamespace, "delete"));
        try {
            if (uid == null) {
                throw new FaultMessage("Required attribute UID not found in identification set", new IllegalArgumentFaultType());
            }
            helper.doDeleteConnectorObject(connector, resourceUtils.mapObjectClass(resourceObject.getDefinition()), uid, operationResult);

        } catch (Exception ex) {
            logger.error("Error on query identity connector", ex);
            throw new MidPointException(ex);
        }


    }

    @Override
    public void executeScript(OperationalResultType result, ScriptType script) throws MidPointException {
        MidPointResult operationResult = new MidPointResult(result, new QName(serviceNamespace, "executeScript"));
        ConnectorScript connScript = new ConnectorScript();
        connScript.setExecMode(script.getHost().value());
        for (ScriptArgumentType arg : script.getArgument()) {
            JAXBElement<ValueConstructionType.Value> val = arg.getValue();
            connScript.getScriptContextBuilder().getScriptArguments().put(arg.getName(), val.getValue().getContent());
        }
        connScript.getScriptContextBuilder().setScriptLanguage(script.getLanguage());
        connScript.getScriptContextBuilder().setScriptText(script.getCode());

        try {
            helper.doExecuteScript(connector, connScript, operationResult);
        } catch (IllegalArgumentException ex) {
            // Happens if some of the argumets are wrong, e.g. unsupported language.
            throw new MidPointException("Script execution failed: "+ex.getMessage(),ex);
        }
    }

    @Override
    public ResourceObject get(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        Set<Attribute> attributes = resourceUtils.buildConnectorObject(resourceObject);

        Uid uid = AttributeUtil.getUidAttribute(attributes);
        MidPointResult operationResult = new MidPointResult(result, new QName(serviceNamespace, "operationGET"));

        if (uid == null) {
            throw new MidPointException("Required attribute UID not found in identification set");
        }
        ConnectorObject co = helper.doGetConnectorObject(connector, resourceUtils.mapObjectClass(resourceObject.getDefinition()), uid, attributes, operationResult);

        if (co == null) { //TODO We should not throw exception because this method is be used to check if the object is exists or not. OR it needs a special ObjectNotFoundException
            //throw new MidPointException("No such resource object in the provisioned resource. uid: " + uid);
            return null;
        } else {
            return resourceUtils.buildResourceObject(co, resourceObject.getDefinition());
        }

    }

    @Override
    public ResourceObject modify(OperationalResultType result, ResourceObject identifier, ResourceObjectDefinition resourceObjectDefinition, Set<AttributeChange> changes) throws MidPointException {

        MidPointResult operationResult = new MidPointResult(result, new QName(serviceNamespace, "operationMODIFY"));

        Set<Attribute> idAttrs = resourceUtils.buildConnectorObject(identifier);
        ObjectClass icfObjectClass = resourceUtils.mapObjectClass(resourceObjectDefinition);

        Uid uidBefore = AttributeUtil.getUidAttribute(idAttrs);
        Uid uidAfter = uidBefore;

        // Inefficient now. One operation is called for every change.
        // Later we should group the changes by changeType and call just one
        // connector operation for each changetype

        for (AttributeChange change : changes) {

            Attribute attribute = resourceUtils.resourceAttributeToIcfAttribute(change.getAttribute(), null);
            Set<Attribute> attrs = new HashSet<Attribute>();
            attrs.add(attribute);

            if (change.getChangeType().equals(change.getChangeType().replace)) {
                uidAfter = helper.doModifyReplaceConnectorObject(connector, icfObjectClass, uidBefore, attrs, operationResult);

            } else if (change.getChangeType().equals(change.getChangeType().add)) {
                uidAfter = helper.doModifyAddConnectorObject(connector, icfObjectClass, uidBefore, attrs, operationResult);

            } else if (change.getChangeType().equals(change.getChangeType().delete)) {
                uidAfter = helper.doModifyDeleteConnectorObject(connector, icfObjectClass, uidBefore, attrs, operationResult);

            } else {
                throw new UnsupportedOperationException("Change type " + change.getChangeType() + " is not supported");
            }

        }

        // TODO: fix returning of changed object ... if needed at all
        //reload
        ConnectorObject co = helper.doGetConnectorObject(connector, icfObjectClass, uidAfter, null, operationResult);

        if (co == null) {
            throw new MidPointException("No such resource object in the provisioned resource. uid: " + uidAfter);
        }
        return resourceUtils.buildResourceObject(co, resourceObjectDefinition);
    }

    @Override
    public ResourceSchema schema(OperationalResultType result, IdentityConnector resource) throws MidPointException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<ResourceObject> search(OperationalResultType result, ResourceObjectDefinition resourceObjectDefinition) throws MidPointException {
        Collection<ResourceObject> resultList = new ArrayList<ResourceObject>();
        for (ConnectorObject co : helper.doListAllConnectorObject(connector, new ObjectClass(resourceObjectDefinition.getNativeObjectClass()), null)) {
            resultList.add(resourceUtils.buildResourceObject(co, resourceObjectDefinition));
        }
        return resultList;
    }

    @Override
    public void iterativeSearch(OperationalResultType result, final ResourceObjectDefinition resourceObjectDefinition, final ResultHandler handler) throws MidPointException {

        ResultsHandler icfHandler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                // Convert ICF-specific connetor object to a generic ResourceObject
                ResourceObject resourceObject = resourceUtils.buildResourceObject(connectorObject,resourceObjectDefinition);
                // .. and pass it to the handler
                return handler.handle(resourceObject);
            }
        };

        helper.doIterativeListAllConnectorObjects(connector, new ObjectClass(resourceObjectDefinition.getNativeObjectClass()), null,icfHandler);

    }

    @Override
    public SynchronizationResult synchronize(SynchronizationState token, OperationalResultType result, ResourceObjectDefinition rod) throws MidPointException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder loader = factory.newDocumentBuilder();
            Document doc = loader.newDocument();
            SynchronizationResult robjects = new SynchronizationResult();

            Object tokenValue = null;
            SyncTokenConverter stc = new SyncTokenConverter();
            if (token != null) {
                tokenValue = stc.convertToJava(token.getAny().get(0));
            }
            Collection<SyncDelta> deltas = helper.doSyncronization(connector, ObjectClass.ACCOUNT, tokenValue);
            for (SyncDelta delta : deltas) {
                logger.debug("Processing delta {} for UID {}: {}",new Object[]{delta.getDeltaType(),delta.getUid().getUidValue(),delta.getObject()});

                SynchronizationState newToken = null;
                if (token != null) {
                    token.getAny().clear();
                    token.getAny().add((Element) stc.convertToXML(IdentityConnectorRAI.SYNC_TOKEN_ATTRIBUTE, delta.getToken().getValue()));
                } else {
                    newToken = new ResourceStateType.SynchronizationState();
                    newToken.getAny().add((Element) stc.convertToXML(IdentityConnectorRAI.SYNC_TOKEN_ATTRIBUTE, delta.getToken().getValue()));
                }
                SynchronizationResult.Change change = null;
                if (SyncDeltaType.DELETE.equals(delta.getDeltaType())) {
                    ObjectChangeDeletionType ocd = new ObjectChangeDeletionType();
                    ocd.setOid(delta.getUid().getUidValue());
                    change = new SynchronizationResult.Change(null, ocd, token == null ? newToken : token);
                } else {
                    ObjectChangeModificationType oct = new ObjectChangeModificationType();
                    ResourceObject object = resourceUtils.buildResourceObject(delta.getObject(), rod);

                    ObjectModificationType oc = new ObjectModificationType();

                    //oc.setOid(object.getValue(ResourceUtils.ATTRIBUTE_UID).getSingleJavaValue(String.class));
                    for (ResourceAttribute attr : object.getValues()) {
                        PropertyModificationType pmt = new PropertyModificationType();
                        Value v = new Value();
                        for (Node n : attr.getValues()) {
                            v.getAny().add((Element) n);
                        }
                        pmt.setValue(v);
                        pmt.setModificationType(PropertyModificationTypeType.add);

                        //TODO
                        List<XPathSegment> segments = new ArrayList<XPathSegment>();
                        XPathSegment attrSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
                        segments.add(attrSegment);
                        XPathType t = new XPathType(segments);
                        Element xpathElement = t.toElement(SchemaConstants.I_PROPERTY_CONTAINER_REFERENCE_PATH, doc);
                        pmt.setPath(xpathElement);

                        oc.getPropertyModification().add(pmt);
                    }
                    oct.setObjectModification(oc);
                    change = new SynchronizationResult.Change(object, oct, token == null ? newToken : token);
                }

                robjects.addChange(change);
            }
            return robjects;
        } catch (ParserConfigurationException ex) {
            throw new MidPointException("Error on createnig XML document factory", ex);
        }
    }

    @Override
    public boolean test(OperationalResultType result, IdentityConnector resource) throws MidPointException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setConnector(ConnectorFacade connector) {
        this.connector = connector;
    }

    protected ResourceObjectDefinition createSyncTokenContainer() {
        ResourceSchema schema = new ResourceSchema(ResourceUtils.NS_ICF_CONFIGURATION);

        ResourceObjectDefinition definition = new ResourceObjectDefinition(new QName(ResourceUtils.NS_ICF_CONFIGURATION, "syncTokenContainer"));

        ResourceAttributeDefinition def = new ResourceAttributeDefinition(SYNC_TOKEN_ATTRIBUTE);
        def.setType(new QName("http://www.w3.org/2001/XMLSchema", "int"));

        definition.addAttribute(def);
        schema.addObjectClass(definition);

        return definition;
    }

    protected void getIdentificationDefinition() {
    }

    @Override
    public ResourceTestResultType test() throws MidPointException {

        ResourceTestResultType result = new ResourceTestResultType();

        TestResultType testResult = new TestResultType();
        result.setConnectorConnection(testResult);
        try {
            connector.test();
            testResult.setSuccess(true);
        } catch (RuntimeException ex) {
            testResult.setSuccess(false);
            List<JAXBElement<DiagnosticsMessageType>> errorOrWarning = testResult.getErrorOrWarning();
            DiagnosticsMessageType message = new DiagnosticsMessageType();
            message.setMessage(ex.getClass().getName()+": "+ex.getMessage());
            // TODO: message.setDetails();
            JAXBElement<DiagnosticsMessageType> element = new JAXBElement<DiagnosticsMessageType>(SchemaConstants.I_DIAGNOSTICS_MESSAGE_ERROR,DiagnosticsMessageType.class,message);
            errorOrWarning.add(element);
        }
        
        return result;
    }
}
