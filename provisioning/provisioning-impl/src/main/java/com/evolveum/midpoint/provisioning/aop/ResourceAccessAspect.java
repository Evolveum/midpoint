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

package com.evolveum.midpoint.provisioning.aop;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.provisioning.integration.identityconnector.IdentityConnector;
import com.evolveum.midpoint.provisioning.integration.identityconnector.IdentityConnectorRAI;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.service.AttributeChange;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.DefaultResourceFactory;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.provisioning.service.ResourceFactory;
import com.evolveum.midpoint.provisioning.service.ResultHandler;
import com.evolveum.midpoint.provisioning.service.SynchronizationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType.SynchronizationState;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptType;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

/**
 * This class is for further use when the call can be intercepted by any
 * Aspect. It's just a gateway between the main provisioning and the
 * integrations project.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceAccessAspect implements ResourceAccessInterface<BaseResourceIntegration> {

    public static final String code_id = "$Id$";

    private ResourceAccessInterface proxyClass = null;

    private ResourceFactory factory = new DefaultResourceFactory();

    ResourceAccessConfigurationType configuration = null;

    BaseResourceIntegration connector = null;

    @Override
    public ResourceObject add(OperationalResultType result, ResourceObject resourceObject, ResourceObjectShadowType shadow) throws MidPointException {
        return proxyClass.add(result, resourceObject, shadow);
    }


    @Override
    public ResourceObject modify(OperationalResultType result, ResourceObject identifier, ResourceObjectDefinition resourceObjectDefinition, Set<AttributeChange> changes) throws MidPointException {
        return proxyClass.modify(result, identifier, resourceObjectDefinition, changes);
    }

    @Override
    public void delete(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        proxyClass.delete(result, resourceObject);
    }

    @Override
    public boolean test(OperationalResultType result, BaseResourceIntegration resource) throws MidPointException {
        return proxyClass.test(result, resource);
    }

    @Override
    public ResourceObject get(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        return proxyClass.get(result, resourceObject);
    }

    
    @Override
    public void executeScript(OperationalResultType result, ScriptType script) throws MidPointException {
        proxyClass.executeScript(result, script);
    }

    @Override
    public ResourceObjectIdentificationType authenticate(OperationalResultType result, ResourceObject resourceObject) throws MidPointException {
        return proxyClass.authenticate(result, resourceObject);
    }

    @Override
    public ResourceSchema schema(OperationalResultType result, BaseResourceIntegration resource) throws MidPointException {
        return proxyClass.schema(result, resource);
    }

    @Override
    public Collection<ResourceObject> search(OperationalResultType result, ResourceObjectDefinition resourceObjectDefinition) throws MidPointException {
        return proxyClass.search(result, resourceObjectDefinition);
    }

    @Override
    public Method custom(OperationalResultType result, Object... input) throws MidPointException {
        return proxyClass.custom(result, input);
    }

    @Override
    public boolean configure(ResourceAccessConfigurationType configuration) throws InitialisationException {
        this.configuration = configuration;
        return true;
    }

    @Override
    public <T extends ResourceAccessInterface<BaseResourceIntegration>> T initialise(Class<T> type, BaseResourceIntegration resourceInstance) throws InitialisationException {
        IdentityConnector con = new IdentityConnector(resourceInstance);
        proxyClass = factory.checkout(IdentityConnectorRAI.class, con, null);
        connector = resourceInstance;
        return (T) this;
    }

    @Override
    public boolean dispose() {
        factory.checkin(proxyClass);
        return true;
    }

    @Override
    public Class<BaseResourceIntegration> getConnectorClass(String targetNamespace) {
        return BaseResourceIntegration.class;
    }

    @Override
    public BaseResourceIntegration getConnector() {
        return connector;
    }

    @Override
    public SynchronizationResult synchronize(SynchronizationState token, OperationalResultType result, ResourceObjectDefinition rod) throws MidPointException {
        return proxyClass.synchronize(token, result, rod);
    }

    @Override
    public ResourceTestResultType test() throws MidPointException {
        return proxyClass.test();
    }

    @Override
    public void iterativeSearch(OperationalResultType result, ResourceObjectDefinition resourceObjectDefinition, ResultHandler handler) throws MidPointException {
        proxyClass.iterativeSearch(result, resourceObjectDefinition, handler);
    }
}
