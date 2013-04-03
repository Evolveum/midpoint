package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;

@Component
public class ObjectAlreadyExistHandler extends ErrorHandler {

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private PrismContext prismContext;

	@Override
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate, 
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		if (!isDoDiscovery(shadow.getResource())){
			throw new ObjectAlreadyExistsException();
		}
		
		OperationResult operationResult = parentResult
				.createSubresult("Discovery for object already exists situation. Operation: " + op.name());
		operationResult.addParam("shadow", shadow);
		operationResult.addParam("currentOperation", op);
		operationResult.addParam("exception", ex.getMessage());

		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		change.setResource(shadow.getResource().asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));

		ObjectQuery query = createQueryByIcfName(shadow);
		final List<PrismObject<ResourceObjectShadowType>> foundAccount = getExistingAccount(query, task, operationResult);

		PrismObject<ResourceObjectShadowType> resourceAccount = null;
		if (!foundAccount.isEmpty() && foundAccount.size() == 1) {
			resourceAccount = foundAccount.get(0);
		}

		if (resourceAccount != null) {
			// Original object and found object share the same object class, therefore they must
			// also share a kind. We can use this short-cut.
			resourceAccount.asObjectable().setKind(shadow.getKind());
			change.setCurrentShadow(resourceAccount);
			// TODO: task initialization
//			Task task = taskManager.createTaskInstance();
			changeNotificationDispatcher.notifyChange(change, task, operationResult);
		}

		operationResult.computeStatus();

		if (operationResult.isSuccess()) {
			parentResult.recordSuccess();
		}
		
		if (compensate){
		throw new ObjectAlreadyExistsException(ex.getMessage(), ex);
		}
	
		return shadow;
	}

	private ObjectQuery createQueryByIcfName(ResourceObjectShadowType shadow) throws SchemaException {
		// TODO: error handling
		PrismProperty nameProperty = shadow.getAttributes().asPrismContainerValue()
				.findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA, "name"));
		EqualsFilter nameFilter = EqualsFilter.createEqual(new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES),
				nameProperty.getDefinition(), nameProperty.getValues());
		RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ResourceObjectShadowType.class,
				ResourceObjectShadowType.F_RESOURCE_REF, prismContext, shadow.getResourceRef().getOid());
		EqualsFilter objectClassFilter = EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext,
				ResourceObjectShadowType.F_OBJECT_CLASS, shadow.getObjectClass());

		ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(nameFilter, resourceRefFilter,
				objectClassFilter));

		return query;
	}

	private List<PrismObject<ResourceObjectShadowType>> getExistingAccount(ObjectQuery query, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException,
			SecurityViolationException {
		final List<PrismObject<ResourceObjectShadowType>> foundAccount = new ArrayList<PrismObject<ResourceObjectShadowType>>();
		ResultHandler<ResourceObjectShadowType> handler = new ResultHandler() {

			@Override
			public boolean handle(PrismObject object, OperationResult parentResult) {
				return foundAccount.add(object);
			}

		};

		provisioningService.searchObjectsIterative(ResourceObjectShadowType.class, query, handler, parentResult);

		return foundAccount;
	}

}
