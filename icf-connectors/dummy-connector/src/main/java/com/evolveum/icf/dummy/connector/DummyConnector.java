/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.icf.dummy.connector;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;

import static com.evolveum.icf.dummy.connector.Utils.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyDelta;
import com.evolveum.icf.dummy.resource.DummyDeltaType;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyOrg;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;

/**
 * Connector for the Dummy Resource, modern version (with delta update).
 *
 * @see DummyResource
 *
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME", configurationClass = DummyConfiguration.class)
public class DummyConnector extends AbstractDummyConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<Filter>, SyncOp, TestOp, UpdateDeltaOp {

	// We want to see if the ICF framework logging works properly
    private static final Log log = Log.getLog(DummyConnector.class);
    
    @Override
	public Set<AttributeDelta> updateDelta(final ObjectClass objectClass, final Uid uid, final Set<AttributeDelta> modifications, final OperationOptions options) {
        log.info("updateDelta::begin");
        validate(objectClass);
        validate(uid);
        
        final Set<AttributeDelta> sideEffectChanges = new HashSet<>();

        try {

	        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

		        final DummyAccount account;
		        if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
		        	account = resource.getAccountByUsername(uid.getUidValue(), false);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	account = resource.getAccountById(uid.getUidValue(), false);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
		        if (account == null) {
		        	throw new UnknownUidException("Account with UID "+uid+" does not exist on resource");
		        }

				// we do this before setting attribute values, in case when description itself would be changed
				resource.changeDescriptionIfNeeded(account);

		        for (AttributeDelta delta : modifications) {
		        	if (delta.is(Name.NAME)) {
		        		assertReplace(delta);
		        		String newName = getSingleReplaceValueMandatory(delta, String.class);
		        		try {
							resource.renameAccount(account.getId(), account.getName(), newName);
						} catch (ObjectDoesNotExistException e) {
							throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
						} catch (ObjectAlreadyExistsException e) {
							throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
						} catch (SchemaViolationException e) {
							throw new org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException("Schema exception: " + e.getMessage(), e);
						}
						// We need to change the returned uid here (only if the mode is not set to UUID)
						if (!(configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID))) {
							addUidChange(sideEffectChanges, newName);
						}
		        	} else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
		        		assertReplace(delta);
		        		changePassword(account, delta);

		        	} else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
		        		assertReplace(delta);
		        		account.setEnabled(getBoolean(delta));

		        	} else if (delta.is(OperationalAttributes.ENABLE_DATE_NAME)) {
		        		assertReplace(delta);
		        		account.setValidFrom(getDate(delta));

		        	} else if (delta.is(OperationalAttributes.DISABLE_DATE_NAME)) {
		        		assertReplace(delta);
		        		account.setValidTo(getDate(delta));

		        	} else if (delta.is(OperationalAttributes.LOCK_OUT_NAME)) {
		        		assertReplace(delta);
		        		account.setLockout(getBooleanMandatory(delta));

		        	} else if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equalsIgnoreCase(delta.getName())) {
		        		applyAuxiliaryObjectClassDelta(account, delta);

					} else {
						applyOrdinaryAttributeDelta(account, delta, null);
			        	
		        	}
		        }

	        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
	
	        	final DummyGroup group;
	        	if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
	        		group = resource.getGroupByName(uid.getUidValue(), false);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	group = resource.getGroupById(uid.getUidValue(), false);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
		        if (group == null) {
		        	throw new UnknownUidException("Group with UID "+uid+" does not exist on resource");
		        }

		        for (AttributeDelta delta : modifications) {
		        	if (delta.is(Name.NAME)) {
		        		assertReplace(delta);
		        		String newName = getSingleReplaceValueMandatory(delta, String.class);
		        		try {
							resource.renameGroup(group.getId(), group.getName(), newName);
						} catch (ObjectDoesNotExistException e) {
							throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
						} catch (ObjectAlreadyExistsException e) {
							throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
						}
		        		// We need to change the returned uid here
		        		addUidChange(sideEffectChanges, newName);
		        	} else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
		        		throw new InvalidAttributeValueException("Attempt to change password on group");
		
		        	} else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
		        		assertReplace(delta);
		        		group.setEnabled(getBoolean(delta));
		
		        	} else {
			        	String name = delta.getName();
			        	Function<List<Object>,List<Object>> valuesTransformer = null;
			        	
			        	if (delta.is(DummyGroup.ATTR_MEMBERS_NAME) && configuration.getUpCaseName()) {
			        		valuesTransformer = this::upcaseValues;
			        	}
						applyOrdinaryAttributeDelta(group, delta, valuesTransformer);
		        	}
		        }

	        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
	
	        	final DummyPrivilege priv;
	        	if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
	        		priv = resource.getPrivilegeByName(uid.getUidValue(), false);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	priv = resource.getPrivilegeById(uid.getUidValue(), false);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
		        if (priv == null) {
		        	throw new UnknownUidException("Privilege with UID "+uid+" does not exist on resource");
		        }

		        for (AttributeDelta delta : modifications) {
		        	if (delta.is(Name.NAME)) {
		        		assertReplace(delta);
		        		String newName = getSingleReplaceValueMandatory(delta, String.class);
		        		try {
							resource.renamePrivilege(priv.getId(), priv.getName(), newName);
						} catch (ObjectDoesNotExistException e) {
							throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
						} catch (ObjectAlreadyExistsException e) {
							throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
						}
		        		// We need to change the returned uid here
		        		addUidChange(sideEffectChanges, newName);
		        	} else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
		        		throw new InvalidAttributeValueException("Attempt to change password on privilege");
		
		        	} else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
		        		throw new InvalidAttributeValueException("Attempt to change enable on privilege");
		
		        	} else {
		        		applyOrdinaryAttributeDelta(priv, delta, null);
		        	}
		        }

	        } else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
	
	        	final DummyOrg org;
	        	if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
	        		org = resource.getOrgByName(uid.getUidValue(), false);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	org = resource.getOrgById(uid.getUidValue(), false);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
		        if (org == null) {
		        	throw new UnknownUidException("Org with UID "+uid+" does not exist on resource");
		        }

		        for (AttributeDelta delta : modifications) {
		        	if (delta.is(Name.NAME)) {
		        		assertReplace(delta);
		        		String newName = getSingleReplaceValueMandatory(delta, String.class);
		        		try {
							resource.renameOrg(org.getId(), org.getName(), newName);
						} catch (ObjectDoesNotExistException e) {
							throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
						} catch (ObjectAlreadyExistsException e) {
							throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
						}
		        		// We need to change the returned uid here
		        		addUidChange(sideEffectChanges, newName);
		        	} else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
		        		throw new InvalidAttributeValueException("Attempt to change password on org");
		
		        	} else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
		        		throw new InvalidAttributeValueException("Attempt to change enable on org");
		
		        	} else {
		        		applyOrdinaryAttributeDelta(org, delta, null);
		        	}
		        }


	        } else {
	        	throw new ConnectorException("Unknown object class "+objectClass);
	        }

		} catch (ConnectException e) {
	        log.info("update::exception "+e);
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.info("update::exception "+e);
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (SchemaViolationException e) {
			log.info("update::exception "+e);
			throw new InvalidAttributeValueException(e.getMessage(), e);
		} catch (ConflictException e) {
			log.info("update::exception "+e);
			throw new AlreadyExistsException(e);
		}

        log.info("update::end");
        return sideEffectChanges;
    }
    
	private void applyAuxiliaryObjectClassDelta(DummyObject dummyObject, AttributeDelta delta) {
    	List<String> replaceValues = getReplaceValues(delta, String.class);
		if (replaceValues != null) {
			dummyObject.replaceAuxiliaryObjectClassNames(replaceValues);
		}
		List<String> addValues = getAddValues(delta, String.class);
		if (addValues != null) {
			dummyObject.addAuxiliaryObjectClassNames(addValues);
		}
		List<String> deleteValues = getRemoveValues(delta, String.class);
		if (deleteValues != null) {
			dummyObject.deleteAuxiliaryObjectClassNames(deleteValues);
		}
    }

	private <T> void applyOrdinaryAttributeDelta(DummyObject dummyObject, AttributeDelta delta, Function<List<T>, List<T>> valuesTransformer) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		String attributeName = delta.getName();
    	try {
    		List<T> replaceValues = getReplaceValues(delta, null);
    		if (replaceValues != null) {
    			if (valuesTransformer != null) {
    				replaceValues = valuesTransformer.apply(replaceValues);
    			}
    			dummyObject.replaceAttributeValues(attributeName, (Collection)replaceValues);
    		}
    		List<T> addValues = getAddValues(delta, null);
    		if (addValues != null) {
    			if (valuesTransformer != null) {
    				addValues = valuesTransformer.apply(addValues);
    			}
    			dummyObject.addAttributeValues(attributeName, (Collection)addValues);
    		}
    		List<T> deleteValues = getRemoveValues(delta, null);
    		if (deleteValues != null) {
    			if (valuesTransformer != null) {
    				deleteValues = valuesTransformer.apply(deleteValues);
    			}
    			dummyObject.removeAttributeValues(attributeName, (Collection)deleteValues);
    		}
		} catch (SchemaViolationException e) {
			// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
			// The framework should deal with it ... somehow
			throw new InvalidAttributeValueException(e.getMessage(),e);
		}
		
	}

	private Boolean getBoolean(AttributeDelta delta) {
		return getSingleReplaceValue(delta, Boolean.class);
	}
	
	private Boolean getBooleanMandatory(AttributeDelta delta) {
		return getSingleReplaceValueMandatory(delta, Boolean.class);
	}
	
	protected Date getDate(AttributeDelta delta) {
		Long longValue = getSingleReplaceValue(delta, Long.class);
		return getDate(longValue);
	}

	private <T> T getSingleReplaceValueMandatory(AttributeDelta modification, Class<T> extectedClass) {
		T value = getSingleReplaceValue(modification, extectedClass);
		if (value == null) {
			throw new IllegalArgumentException("No value in relace set in delta for "+modification.getName());
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private <T> T getSingleReplaceValue(AttributeDelta modification, Class<T> extectedClass) {
		List<Object> valuesToReplace = modification.getValuesToReplace();
		if (valuesToReplace == null || valuesToReplace.isEmpty()) {
			return null;
		}
		if (valuesToReplace.size() > 1) {
			throw new IllegalArgumentException("More than one value in relace set in delta for "+modification.getName()+": "+valuesToReplace);
		}
		Object valueToReplace = valuesToReplace.get(0);
		if (valueToReplace == null) {
			return null;
		}
		if (!extectedClass.isAssignableFrom(valueToReplace.getClass())) {
			throw new IllegalArgumentException("Unexpected type of value in relace set in delta for "+modification.getName()+"; expected "+extectedClass.getSimpleName()+", but was "+valueToReplace.getClass().getSimpleName());
		}
		return (T) valueToReplace;
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> getReplaceValues(AttributeDelta modification, Class<T> extectedClass) {
		return assertTypes(modification, modification.getValuesToReplace(), "replace", extectedClass);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> getAddValues(AttributeDelta modification, Class<T> extectedClass) {
		return assertTypes(modification, modification.getValuesToAdd(), "add", extectedClass);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> getRemoveValues(AttributeDelta modification, Class<T> extectedClass) {
		return assertTypes(modification, modification.getValuesToRemove(), "remove", extectedClass);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> assertTypes(AttributeDelta modification, List<Object> deltaValues, String desc, Class<T> extectedClass) {
		if (deltaValues == null) {
			return null;
		}
		List<T> returnValues = new ArrayList<>(deltaValues.size());
		for (Object deltaValue: deltaValues) {
			if (deltaValue == null) {
				returnValues.add(null);
			} else {
				if (extectedClass != null && !extectedClass.isAssignableFrom(deltaValue.getClass())) {
					throw new IllegalArgumentException("Unexpected type of value in "+desc+" set in delta for "+modification.getName()+"; expected "+extectedClass.getSimpleName()+", but was "+deltaValue.getClass().getSimpleName()+", value: "+deltaValue);
				}
				returnValues.add((T)deltaValue);
			}
		}
		return returnValues;
	}

	private void assertReplace(AttributeDelta modification) {
		assertEmptyAdd(modification);
		assertEmptyRemove(modification);
	}

	private void assertEmptyAdd(AttributeDelta modification) {
		if (modification.getValuesToAdd() != null && !modification.getValuesToAdd().isEmpty()) {
			throw new IllegalArgumentException("Non-empty add set in delta for "+modification.getName());
		}
	}
	
	private void assertEmptyRemove(AttributeDelta modification) {
		if (modification.getValuesToRemove() != null && !modification.getValuesToRemove().isEmpty()) {
			throw new IllegalArgumentException("Non-empty remove set in delta for "+modification.getName());
		}
	}

    protected void changePassword(final DummyAccount account, AttributeDelta delta) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
    	GuardedString guardedPassword = getSingleReplaceValue(delta, GuardedString.class);
    	changePassword(account, guardedPassword);
	}
    
    private <T> List<T> upcaseValues(List<T> values) {
    	if (values == null) {
    		return null;
    	}
		List<String> newValues = new ArrayList<>(values.size());
		for (Object val: values) {
			newValues.add(StringUtils.upperCase((String)val));
		}
		return (List<T>) newValues;
    }
    
    private void addUidChange(Set<AttributeDelta> sideEffectChanges, String newUidValue) {
    	sideEffectChanges.add(
    			AttributeDeltaBuilder.build(Uid.NAME, newUidValue));
	}

}
