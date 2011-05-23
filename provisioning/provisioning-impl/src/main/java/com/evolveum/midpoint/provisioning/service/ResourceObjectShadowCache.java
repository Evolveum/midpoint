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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Holder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class manages the "cache" of ResourceObjectShadows in the repository.
 * 
 * In short, this class takes care of aligning the shadow objects in repository
 * with the real state of the resource.
 * 
 * The repository is considered a cahce when it comes to Shadow objects. That's
 * why they are called shadows after all. When a new state (values) of the
 * resource object is detected, the shadow in the reposiroty should be updated.
 * No matter if that was detected by synchronization, reconciliation or an
 * ordinary get from resource. This class is supposed to do that.
 *
 * Now it assumes we are only storing primary identifier in the repository.
 * That should be made configurable later. It also only support Account objects
 * now.
 * 
 * This is work in progress, the methods of this object are quite ad-hoc now,
 * and it has to be improved later. But in the time this object was created the
 * ProvisioningService was quite a mess (OPENIDM-360) and was not usable by the
 * import task. So I figured it would be better to start the move in the correct
 * direction, alhough the result is far from perfect.
 *
 * @author semancik
 */
public class ResourceObjectShadowCache {

    private RepositoryPortType repositoryService;

    public ResourceObjectShadowCache() {
        repositoryService = null;
    }

    /**
     * Get the value of repositoryService.
     *
     * @return the value of repositoryService
     */
    public RepositoryPortType getRepositoryService() {
        return repositoryService;
    }

    /**
     * Set the value of repositoryService
     *
     * Expected to be injected.
     * 
     * @param repositoryService new value of repositoryService
     */
    public void setRepositoryService(RepositoryPortType repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Locates the appropriate Shadow in repository, updates it as necessary and
     * returns updated shadow.
     *
     * The returned shadow object has all the values from the provided resource
     * object (the implementation is merging values).
     *
     * This does not do any modifications. We are only storing identifiers in
     * the shadows and we do not support renames yet. So we don't need to modify
     * anything. The implementation only creates the shadow object if it does
     * not exist yet.
     *
     * @param resourceObject new state of the resource object
     * @return updated shadow object
     */
    public ResourceObjectShadowType update(ResourceObject resourceObject, ResourceType resource) throws FaultMessage {

        ResourceObjectShadowType shadow = getCurrentShadow(resourceObject, resource);

        ObjectValueWriter objectValueWriter = new ObjectValueWriter();

        if (shadow==null) {
            // We have to create new object
            //Holder holder = new Holder(new OperationalResultType());
            ObjectContainerType oct = new ObjectContainerType();
            // Account-only for now, more generic later
            shadow = new AccountShadowType();
            ObjectReferenceType resourceRef = new ObjectReferenceType();
            resourceRef.setOid(resource.getOid());
            shadow.setResourceRef(resourceRef);
            shadow.setAttributes(new ResourceObjectShadowType.Attributes());

            // Not sure about this ....
            objectValueWriter.merge(resourceObject.getIdentifier(), shadow.getAttributes().getAny(), false);

            shadow.setObjectClass(resourceObject.getDefinition().getQName());

            // TODO: This has to be smarter, later
            shadow.setName((String)resourceObject.getIdentifier().getSingleJavaValue());

            oct.setObject(shadow);

            String oid = getRepositoryService().addObject(oct);

            // Repository will assign OID, so put it back to the shadow
            shadow.setOid(oid);
        }

        // Merge all the values from resourceObject to shadow now

        objectValueWriter.merge(resourceObject,shadow.getAttributes().getAny(), false);

        // For shadow that we are returning we want to set resource instead of
        // resourceRef. Resource is easiet to use and as we have it here anyway
        // it is not harn setting it.
        shadow.setResource(resource);
        shadow.setResourceRef(null);
        
        return shadow;
    }

    /**
     * Locates the appropriate Shadow in repository that corresponds to the
     * provided resource object.
     *
     * No update is done, just the current repository state is returned. This
     * operation is NOT supposed for generic usage. It is expected to be used
     * in the rare cases where old repository state is required
     * (e.g. synchronization)
     *
     * TODO: Fix error handling (both runtime exceptions and Fault)
     * 
     * @param resourceObject any state of resource objects, it just to contain
     *        valid identifiers
     * @return current unchanged shadow object that corresponds to provided
     *         resource object or null if the object does not exist
     */
    public ResourceObjectShadowType getCurrentShadow(ResourceObject resourceObject, ResourceType resource) throws FaultMessage {

        QueryType query = createSearchShadowQuery(resourceObject);
        PagingType paging = new PagingType();
        
        // TODO: check for errors
        ObjectListType results;

        results = getRepositoryService().searchObjects(query, paging);

        if (results.getObject().size()==0) {
            return null;
        }
        if (results.getObject().size()>1) {
            // TODO: Better error handling later
            throw new IllegalStateException("More than one shadows found for "+resourceObject);
        }

        return (ResourceObjectShadowType) results.getObject().get(0);
    }


    protected QueryType createSearchShadowQuery(ResourceObject resourceObject) {

        // We are going to query for attributes, so setup appropriate
        // XPath for the filter
        XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
        List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
        xpathSegments.add(xpathSegment);
        XPathType xpath = new XPathType(xpathSegments);

        // Now we need to determine what is the identifer and set corrent
        // value for it in the filter
        ResourceAttribute identifier = resourceObject.getIdentifier();

        List<Node> idNodes = identifier.getValues();
        // Only one value is supported for an identifier
        if (idNodes.size()>1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("More than one identifier value is not supported");
        }
        if (idNodes.size()<1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("The identifier has no value");
        }
        Element idElement = (Element) idNodes.get(0);

        // We have all the data, we can construct the filter now
        Document doc = ShadowUtil.getXmlDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
                // TODO: The account type is hardcoded now, it should determined
                // from the shcema later, or maybe we can make it entirelly
                // generic (use ResourceObjectShadowType instead).
                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_TYPE)),
                QueryUtil.createEqualFilter(doc, xpath, idElement));

        QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
    }

}
