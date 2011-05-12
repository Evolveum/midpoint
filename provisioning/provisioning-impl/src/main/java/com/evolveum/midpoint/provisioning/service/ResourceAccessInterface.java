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

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
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
 * Common interface to communicate with any resource handler such as ICF.
 *
 * It should be generic and not ICF specific.
 *
 * @author elek
 */
public interface ResourceAccessInterface<C extends ResourceConnector<?>> {

    // Do we really need to return ResourceObject after this operation?
    public ResourceObject add(OperationalResultType result, ResourceObject resourceObject, ResourceObjectShadowType shadow) throws MidPointException;

    /*
     * Idea for improvement: Change the modify operation signature: It will not have just Set<AttributeChange> changes,
     * but rathe something like Set<Change> changes. Change should be abstract/interface, AttributeChange is just one of
     * the subclasses/implementations. PasswordChange (or CredentialsChange) should be another one. And ActivationChange
     * yet another one (later).
     * This should give a nice support for concepts that are common for all the resources - and slowly begin to
     * "standardize" the connector interface beyond generic things like "get attribute/set attribute".
     */

    // Do we really need to return ResourceObject after this operation?
    public ResourceObject modify(OperationalResultType result,  ResourceObject identifier, ResourceObjectDefinition resourceObjectDefinition, Set<AttributeChange> changes) throws MidPointException;

    public void delete(OperationalResultType result, ResourceObject resourceObject) throws MidPointException;

    public boolean test(OperationalResultType result, C resource) throws MidPointException;

    public ResourceObject get(OperationalResultType result, ResourceObject resourceObject) throws MidPointException;

    public SynchronizationResult synchronize(SynchronizationState toke, OperationalResultType result, ResourceObjectDefinition rod) throws MidPointException;

    public void executeScript(OperationalResultType result, ScriptType script) throws MidPointException;

    public ResourceObjectIdentificationType authenticate(OperationalResultType result, ResourceObject resourceObject) throws MidPointException;

    public ResourceSchema schema(OperationalResultType result, C resource) throws MidPointException;

    public ResourceTestResultType test() throws MidPointException;

    /**
     * Temporary hack: Search operation must be the key function but now it only lists all object.
     *
     * This method must be redesigned but due to the extreme pressure it must be implemented.
     * 
     * @param result
     * @param resourceObjectDefinition
     * @return
     * @throws MidPointException
     */
    public Collection<ResourceObject> search(OperationalResultType result, ResourceObjectDefinition resourceObjectDefinition) throws MidPointException;

    /**
     * Search through objects iterativly, using callback to deliver the results.
     * This is useful when searching for many objects (potentially all objects).
     *
     * There is no way how to specify search criteria now, except for object type.
     * That should be improved later.
     *
     * The MidPointException is also all wrong here. A more specific exception
     * should be declared here.
     *
     * @param result
     * @param resourceObjectDefinition
     * @param handler instace that will receive and handle each search result.
     * @throws MidPointException in case of any error.
     */
    public void iterativeSearch(OperationalResultType result, ResourceObjectDefinition resourceObjectDefinition, ResultHandler handler) throws MidPointException;

    public Method custom(OperationalResultType result, Object... input) throws MidPointException;

    public Class<C> getConnectorClass(String targetNamespace);
    
    /**
     * Initialise the new instance with the global configuration.
     *
     * @param configuration
     * @throws InitialisationException
     */
    public boolean configure(ResourceAccessConfigurationType configuration) throws InitialisationException;

    /**
     * This is the first method called by the provisioner before the first use.
     * Later the instances can be fetched from a pool and only the instance
     * specific initialisation is required because the global parameteres are cached.
     *
     * @param resourceInstance
     * @return
     */
    public <T extends ResourceAccessInterface<C>> T initialise(Class<T> type, C resourceInstance) throws InitialisationException;


    public  C getConnector();

    /**
     * Clean up method right agter the instanced is in the pool again.
     */
    public boolean dispose();
}
