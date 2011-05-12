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

import com.evolveum.midpoint.provisioning.integration.identityconnector.converter.ICFConverterFactory;
import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.integration.identityconnector.script.ConnectorScript;
import com.evolveum.midpoint.provisioning.integration.identityconnector.script.ConnectorScriptBuilder;
import com.evolveum.midpoint.provisioning.integration.identityconnector.script.ConnectorScriptExecutor;
import com.evolveum.midpoint.util.MidPointResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.springframework.core.convert.converter.Converter;
import org.w3c.dom.Element;

/**
 * Facade to query the resources over Identity Connector Framework.
 * 
 * @author elek
 */
public class IdentityConnectorService {

    private Trace logger = TraceManager.getTrace(IdentityConnectorService.class);

    public Uid doCreateConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Set<Attribute> attrs, MidPointResult result) throws MidPointException {
        logger.debug("Entry Create Operation: {}", objectClass.getObjectClassValue());
        CreateApiOp createOp = createOperation(connector, CreateApiOp.class);
        Uid uid = createOp.create(objectClass, attrs, (new OperationOptionsBuilder()).build());
        //TODO handle MidPointResult
        logger.debug("Exit Create Operation: {}", uid.getUidValue());
        return uid;
    }

    public ConnectorObject doGetConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Uid uid, Set<Attribute> attrsToGet, MidPointResult result) throws MidPointException {
        return createOperation(connector, GetApiOp.class).getObject(objectClass, uid, createOperationOptions(attrsToGet));
    }

    public Uid doModifyReplaceConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Uid uid, Set<Attribute> attrs, MidPointResult result) throws MidPointException {
        UpdateApiOp updateOp = (UpdateApiOp) connector.getOperation(UpdateApiOp.class);
        return updateOp.update(objectClass, uid, attrs, (new OperationOptionsBuilder()).build());
    }

    public Uid doModifyAddConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Uid uid, Set<Attribute> attrs, MidPointResult result) throws MidPointException {
        UpdateApiOp updateOp = (UpdateApiOp) connector.getOperation(UpdateApiOp.class);
        return updateOp.addAttributeValues(objectClass, uid, attrs, (new OperationOptionsBuilder()).build());
    }

    public Uid doModifyDeleteConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Uid uid, Set<Attribute> attrs, MidPointResult result) throws MidPointException {
        UpdateApiOp updateOp = (UpdateApiOp) connector.getOperation(UpdateApiOp.class);
        return updateOp.removeAttributeValues(objectClass, uid, attrs, (new OperationOptionsBuilder()).build());
    }

    public void doDeleteConnectorObject(ConnectorFacade connector, ObjectClass objectClass, Uid uid, MidPointResult result) throws MidPointException {
        DeleteApiOp deleteOp = createOperation(connector, DeleteApiOp.class);
        deleteOp.delete(objectClass, uid, (new OperationOptionsBuilder()).build());

    }

    public Collection<ConnectorObject> doListAllConnectorObject(ConnectorFacade connector, ObjectClass objectClass, MidPointResult result) throws MidPointException {
        SearchApiOp op = createOperation(connector, SearchApiOp.class);
        final Collection<ConnectorObject> resultList = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject arg0) {
                return resultList.add(arg0);
            }
        };
        op.search(objectClass, null, handler, (new OperationOptionsBuilder()).build());
        return resultList;
    }

    public void doIterativeListAllConnectorObjects(ConnectorFacade connector, ObjectClass objectClass, MidPointResult result, ResultsHandler handler) throws MidPointException {
        SearchApiOp op = createOperation(connector, SearchApiOp.class);
        op.search(objectClass, null, handler, (new OperationOptionsBuilder()).build());
    }


    protected Collection<SyncDelta> doSyncronization(ConnectorFacade connector, ObjectClass objectClass, Object syncToken) {
        OperationOptionsBuilder oob = new OperationOptionsBuilder();

        final Collection<SyncDelta> result = new HashSet<SyncDelta>();


        SyncApiOp syncOp = (SyncApiOp) connector.getOperation(SyncApiOp.class);

        syncOp.sync(objectClass, syncToken == null? null : new SyncToken(syncToken), new SyncResultsHandler() {

            @Override
            public boolean handle(SyncDelta sd) {
                result.add(sd); //TODO
                return true;

            }
        }, oob.build());

        return result;
    }

    public void doCreateAction(ResourceObjectShadowType shadow, String timing, ConnectorFacade connector, Set<Attribute> attrs, MidPointResult result) {
        String defExecMode = ConnectorUtil.getDefaultExecMode(connector);
        List<ConnectorScript> connScripts = ConnectorScriptBuilder.buildAll(null, shadow, defExecMode, "create", timing);
        if ((connScripts != null) && (connScripts.size() > 0)) {
            ConnectorScriptExecutor executor = new ConnectorScriptExecutor();
            for (ConnectorScript connScript : connScripts) {
                ScriptContextBuilder ctxBuilder = connScript.getScriptContextBuilder();
                OperationOptionsBuilder ooBuilder = connScript.getOperationOptionsBuilder();
                Object scriptResult = executor.execute(connector, connScript);
            }
        }
    }

    public void doExecuteScript(ConnectorFacade connector, ConnectorScript script, MidPointResult result) {
        ScriptContext ctxt = new ScriptContext(script.getScriptContextBuilder().getScriptLanguage(), script.getScriptContextBuilder().getScriptText(), script.getScriptContextBuilder().getScriptArguments());
        if ("connector".equals(script.getExecMode())) {
            ScriptOnConnectorApiOp scriptOnConnector = (ScriptOnConnectorApiOp) createOperation(connector, ScriptOnConnectorApiOp.class);
            scriptOnConnector.runScriptOnConnector(ctxt, new OperationOptionsBuilder().build());
        } else if ("resource".equals(script.getExecMode())) {
            ScriptOnResourceApiOp scriptOnResource = (ScriptOnResourceApiOp) createOperation(connector, ScriptOnResourceApiOp.class);
            scriptOnResource.runScriptOnResource(ctxt, new OperationOptionsBuilder().build());
        }
    }

    private QName nameToQName(String name, String connectorNamespace) {
        //FIXME handle the globan ICC specific properties with ICC namespace.
        return new QName(name, connectorNamespace);
    }

//    private QName icObjectClassToXsdObjectClass(IdentityConnector resource, ObjectClass objectClass) {
//        String resourceInstanceNamespace = resource.getNamespace();
//        if (ObjectClass.ACCOUNT.equals(objectClass)) {
//            return new QName(resourceInstanceNamespace, ShadowUtil.IC_DEFAULT_ACCOUNT_XSD_OBJECTCLASS_LOCAL_NAME);
//        }
//        if (ObjectClass.GROUP.equals(objectClass)) {
//            return new QName(resourceInstanceNamespace, ShadowUtil.IC_DEFAULT_GROUP_XSD_OBJECTCLASS_LOCAL_NAME);
//        }
//
//        // TODO support all object classes later. But do that only if this is
//        // moved to a correct place and we have real resource schema support.
//
//        throw new IllegalArgumentException("Too bad, we do not support " + objectClass.toString() + " object class yet");
//    }
    /**
     * Parse XML tags to IC attribtues.
     *
     * @todo maybe some conversion is required.
     * @param index
     * @return
     */
    public Set<Attribute> convertAnyElementsToConnectorAttributes(Map<String, Set<Element>> index) {
        Set<Attribute> attributes = new HashSet();
        logger.debug("[Provisioning] index = {}", index);
        for (String attributeName : index.keySet()) {
            logger.debug("[Provisioning] attributeName = {}", attributeName);
            List<Object> values = new ArrayList();
            for (Element e : index.get(attributeName)) {
                //todo may be some conversion is needed.
                logger.debug("[Provisioning] index.get(attributeName) = {}", index.get(attributeName));
                logger.debug("[Provisioning] e.getTextContent() = {}", e.getTextContent());
                if (OperationalAttributes.PASSWORD_NAME.equals(attributeName)) {
                    Converter converter = ICFConverterFactory.getInstance().getConverter(GuardedString.class, e.getTextContent());
                    values.add(converter.convert(e.getTextContent()));
                    continue;
                }
                values.add(e.getTextContent());
            }
            Attribute a = AttributeBuilder.build(attributeName, values);
            attributes.add(a);
        }
        return attributes;
    }

    /**
     * Generate generic definition type object from ICF specific descriptor.
     *
     * This is a dangerous call because not all bundle support the schema
     *
     * 
     * @param schema
     * @param connectorNamespace
     * @param type
     * @return
     */
//    protected ResourceObjectDefinition createDefinitionFromSchema(Schema schema, String connectorNamespace, String type) {
//        for (ObjectClassInfo info : schema.getObjectClassInfo()) {
//            //FIXME
//            if (info.getType().equals(type)) {
//                ResourceObjectDefinition rod = new ResourceObjectDefinition(nameToQName(info.getType(), connectorNamespace));
//                for (AttributeInfo ai : info.getAttributeInfo()) {
//                    ResourceAttributeDefinition raf = new ResourceAttributeDefinition(new QName(connectorNamespace, ai.getName()));
//                    raf.setType(ResourceUtils.translateClassName(ai.getType()));
//                    rod.addAttribute(raf);
//                }
//                rod.addAttribute(new ResourceAttributeDefinition(new QName(connectorNamespace, "__UID__")));
//                return rod;
//            }
//        }
//        return null;
//    }
    protected <T extends APIOperation> T createOperation(ConnectorFacade connector, Class<T> t) {
        T op = (T) connector.getOperation(t);
        if (op == null) {
            throw new MidPointException("Unsupported opeation " + t + " on connector " + connector);
        }
        return op;
    }

    protected OperationOptions createOperationOptions(Set<Attribute> attributes) {
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        //TODO: we dont' get the correct valus so for now get all
//        List<String> attrsToGet = new ArrayList<String>();
//        for (Attribute attribute : attributes) {
//            attrsToGet.add(attribute.getName());
//        }
//        oob.setAttributesToGet(attrsToGet);
        OperationOptions operOpts = oob.build();
        return operOpts;
    }
}
