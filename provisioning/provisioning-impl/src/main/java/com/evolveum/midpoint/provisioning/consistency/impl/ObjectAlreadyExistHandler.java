package com.evolveum.midpoint.provisioning.consistency.impl;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

@Component
public class ObjectAlreadyExistHandler extends ErrorHandler{

	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	@Autowired
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ProvisioningService provisioningService;

	
	@Override
	public void handleError(ResourceObjectShadowType shadow, Exception ex) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException {
		
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
		OperationResult handleErrorResult = parentResult.createSubresult(ObjectAlreadyExistHandler.class+".handleError");
		
		shadow = ShadowCacheUtil.completeShadow(shadow, null, shadow.getResource(), parentResult);
		
		String oid = cacheRepositoryService.addObject(shadow.asPrismObject(), parentResult);
		shadow.setOid(oid);
		
		
		if (shadow instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) shadow;
//			ObjectDelta<AccountShadowType> delta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.ADD);
//			delta.setOid(account.getOid());
//			MidPointObject<AccountShadowType> midObj = new MidPointObject<AccountShadowType>(new QName(SchemaConstants.NS_C, "account"));
//			midObj.setObjectType(account);
//			delta.setObjectToAdd(midObj);
			change.setObjectDelta(null);
			change.setResource(shadow.getResource().asPrismObject());

			

			account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(), parentResult));
			
			
		}

		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

		XPathHolder holder = ObjectTypeUtil.createXPathHolder(SchemaConstants.I_ATTRIBUTES);
		Element filter = QueryUtil.createEqualFilter(DOMUtil.getDocument(), holder, shadow.getAttributes().asPrismContainerValue().findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA, "name")));
		QueryType query = QueryUtil.createQuery(filter);
		ResultList<PrismObject<AccountShadowType>> foundAccount = provisioningService.searchObjects(AccountShadowType.class, query, new PagingType(), parentResult);
		
		
		if (!foundAccount.isEmpty()){
			change.setCurrentShadow(foundAccount.get(0));
			changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);
		}
		
		ResultList<PrismObject<AccountShadowType>> foundAccountAfterSync = provisioningService.searchObjects(AccountShadowType.class, query, new PagingType(), parentResult);
		
		if (foundAccountAfterSync.isEmpty()){
			provisioningService.addObject(shadow.asPrismObject(), null, parentResult);
		} else{
			throw new ObjectAlreadyExistsException();
		}
		
//		changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);
		
//		try{
//			provisioningService.addObject(shadow, null, parentResult);
//		} catch(ObjectAlreadyExistsException e){
//			
//		}

		
	}

}
