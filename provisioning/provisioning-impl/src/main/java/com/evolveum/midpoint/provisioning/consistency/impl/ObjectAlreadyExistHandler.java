package com.evolveum.midpoint.provisioning.consistency.impl;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

@Component
public class ObjectAlreadyExistHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	@Autowired
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private PrismContext prismContext;

	@Override
	public void handleError(ResourceObjectShadowType shadow, Exception ex) throws SchemaException,
			GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
		OperationResult handleErrorResult = parentResult.createSubresult(ObjectAlreadyExistHandler.class
				+ ".handleError");

		// shadow = ShadowCacheUtil.completeShadow(shadow, null,
		// shadow.getResource(), parentResult);
		//
		// String oid = cacheRepositoryService.addObject(shadow.asPrismObject(),
		// parentResult);
		// shadow.setOid(oid);
		//

		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
		if (shadow instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) shadow;
			account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(),
					parentResult));
		}
		change.setObjectDelta(null);
		change.setResource(shadow.getResource().asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

		QueryType query = createQueryByIcfName(shadow);
		final List<PrismObject<AccountShadowType>> foundAccount = getExistingAccount(query, parentResult);

		if (!foundAccount.isEmpty() && foundAccount.size() == 1) {
			change.setCurrentShadow(foundAccount.get(0));
			
			changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);
		}

		List<PrismObject<AccountShadowType>> foundAccountAfterSync = getExistingAccount(query, parentResult);

		if (foundAccountAfterSync.isEmpty()) {
			provisioningService.addObject(shadow.asPrismObject(), null, parentResult);
		} 
		else {
			shadow.setOid(foundAccount.get(0).getOid());
		}

		// changeNotificationDispatcher.notifyChange(change, null,
		// handleErrorResult);

		// try{
		// provisioningService.addObject(shadow, null, parentResult);
		// } catch(ObjectAlreadyExistsException e){
		//
		// }

	}

	private QueryType createQueryByIcfName(ResourceObjectShadowType shadow) throws SchemaException {
		// TODO: error handling
		Document doc = DOMUtil.getDocument();
		XPathHolder holder = ObjectTypeUtil.createXPathHolder(SchemaConstants.I_ATTRIBUTES);
		PrismProperty nameProperty = shadow.getAttributes().asPrismContainerValue().findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA, "name"));
		Element nameFilter = QueryUtil.createEqualFilter(doc, holder, nameProperty.getName(), (String) nameProperty.getValue().getValue());
		Element resourceFilter = QueryUtil.createEqualRefFilter(doc, null, ResourceObjectShadowType.F_RESOURCE_REF, shadow.getResourceRef().getOid());
		Element objectClassFilter = QueryUtil.createEqualFilter(doc, null, ResourceObjectShadowType.F_OBJECT_CLASS, ResourceObjectShadowUtil.getObjectClassDefinition(shadow).getComplexTypeDefinition().getTypeName());
		Element filter = QueryUtil.createAndFilter(doc, new Element[]{nameFilter, resourceFilter, objectClassFilter});
		return QueryUtil.createQuery(filter);
	}

	private List<PrismObject<AccountShadowType>> getExistingAccount(QueryType query,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			ConfigurationException, SchemaException, SecurityViolationException {
		final List<PrismObject<AccountShadowType>> foundAccount = new ArrayList<PrismObject<AccountShadowType>>();
		ResultHandler<AccountShadowType> handler = new ResultHandler() {

			@Override
			public boolean handle(PrismObject object, OperationResult parentResult) {
				// TODO Auto-generated method stub
				return foundAccount.add(object);
			}

		};

		provisioningService.searchObjectsIterative(AccountShadowType.class, query, new PagingType(), handler,
				parentResult);

		return foundAccount;
	}

}
