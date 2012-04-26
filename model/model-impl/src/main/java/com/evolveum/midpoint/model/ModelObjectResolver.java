/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * @author semancik
 *
 */
@Component
public class ModelObjectResolver implements ObjectResolver {

	@Autowired(required = true)
	private transient ProvisioningService provisioning;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
	private transient PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelObjectResolver.class);
	
	@Override
	public <T extends ObjectType> T resolve(ObjectReferenceType ref, Class<T> expectedType, String contextDescription, 
			OperationResult result) throws ObjectNotFoundException, SchemaException {
				String oid = ref.getOid();
				Class<?> typeClass = null;
				QName typeQName = ref.getType();
				if (typeQName != null) {
					typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
				}
				if (typeClass != null && expectedType.isAssignableFrom(typeClass)) {
					expectedType = (Class<T>) typeClass;
				}
				return getObject(expectedType, oid, result);
	}

	public PrismObject<?> resolve(PrismReferenceValue refVal, String string, OperationResult result) throws ObjectNotFoundException {
		String oid = refVal.getOid();
		Class<?> typeClass = ObjectType.class;
		QName typeQName = refVal.getTargetType();
		if (typeQName == null && refVal.getParent() != null && refVal.getParent().getDefinition() != null) {
			PrismReferenceDefinition refDef = (PrismReferenceDefinition) refVal.getParent().getDefinition();
			typeQName = refDef.getTargetTypeName();
		}
		if (typeQName != null) {
			typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
		}
		return ((ObjectType) getObject((Class)typeClass, oid, result)).asPrismObject();
	}

	
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, OperationResult result) throws ObjectNotFoundException {
		T objectType = null;
		try {
			PrismObject<T> object = null;
			if (ObjectTypes.isClassManagedByProvisioning(clazz)) {
				object = provisioning.getObject(clazz, oid, result);
				if (object == null) {
					throw new SystemException("Got null result from provisioning.getObject while looking for "+clazz.getSimpleName()
							+" with OID "+oid+"; using provisioning implementation "+provisioning.getClass().getName());
				}
			} else {
				object = cacheRepositoryService.getObject(clazz, oid, result);
				if (object == null) {
					throw new SystemException("Got null result from repository.getObject while looking for "+clazz.getSimpleName()
							+" with OID "+oid+"; using repository implementation "+cacheRepositoryService.getClass().getName());
				}
			}
			objectType = object.asObjectable();
			if (!clazz.isInstance(objectType)) {
				throw new ObjectNotFoundException("Bad object type returned for referenced oid '" + oid
						+ "'. Expected '" + clazz + "', but was '"
						+ (objectType == null ? "null" : objectType.getClass()) + "'.");
			}

		} catch (SystemException ex) {
			throw ex;
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (CommonException ex) {
			LoggingUtils.logException(LOGGER, "Error resolving object with oid {}", ex, oid);
			// Add to result only a short version of the error, the details will be in subresults
			result.recordFatalError(
					"Couldn't get object with oid '" + oid + "': "+ex.getOperationResultMessage(), ex);
			throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Error resolving object with oid {}, expected type was {}.", ex,
					oid, clazz);
			throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
		} finally {
			result.computeStatus();
		}

		return objectType;
	}
	
}
