/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import com.evolveum.icf.dummy.resource.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.identityconnectors.common.logging.Log;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Connector for the Dummy Resource, abstract superclass.
 *
 * This superclass adds delta update and other modern capabilities to the connector.
 * The connector does NOT have scripting capabilities.
 *
 * Dummy resource is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very
 * little overhead. This connector connects the Dummy resource to ICF.
 *
 * @see DummyResource
 *
 */
public abstract class AbstractModernObjectDummyConnector
        extends AbstractObjectDummyConnector
        implements UpdateDeltaOp, InstanceNameAware, PartialSchemaOp {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(AbstractModernObjectDummyConnector.class);

    private String instanceName;

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public Set<AttributeDelta> updateDelta(
            ObjectClass objectClass, Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        LOG.info("updateDelta::begin {0}", instanceName);
        validate(objectClass);
        validate(uid);
        validateModifications(objectClass, modifications);

        final Set<AttributeDelta> sideEffectChanges = new HashSet<>();
        DummyObject object;

        try {

            String objectClassName = fromConnIdObjectClass(objectClass);

            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

                final DummyAccount account;
                if (configuration.isUidBoundToName()) {
                    account = resource.getAccountByName(uid.getUidValue(), false);
                } else {
                    account = resource.getAccountById(uid.getUidValue(), false);
                }
                if (account == null) {
                    throw new UnknownUidException("Account with UID " + uid + " does not exist on resource");
                }
                object = account;
                applyModifyMetadata(account, options);

                // we do this before setting attribute values, in case when description itself would be changed
                resource.changeDescriptionIfNeeded(account);

                for (AttributeDelta delta : modifications) {
                    if (delta.is(Name.NAME)) {
                        assertReplace(delta);
                        String newName = getSingleReplaceValueMandatory(delta, String.class);
                        boolean doRename = handlePhantomRenames(objectClass, account, newName);
                        if (doRename) {
                            try {
                                resource.renameAccount(account.getId(), account.getName(), newName);
                            } catch (ObjectDoesNotExistException e) {
                                throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                            } catch (ObjectAlreadyExistsException e) {
                                throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                            } catch (SchemaViolationException e) {
                                throw new org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException("Schema exception: " + e.getMessage(), e);
                            }
                            // We need to change the returned uid here (only if the mode is set to Name)
                            if (configuration.isUidBoundToName()) {
                                addUidChange(sideEffectChanges, newName);
                            }
                        }
                    } else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
                        if (delta.getValuesToReplace() != null) {
                            // Password reset
                            assertReplace(delta);
                            changePassword(account, delta);
                        } else {
                            // Password change (self-service)
                            assertSelfService(options);
                            List<GuardedString> addValues = getAddValues(delta, GuardedString.class);
                            if (addValues == null || addValues.size() != 1) {
                                throw new InvalidAttributeValueException("Wrong add set in password delta: "+addValues);
                            }
                            GuardedString newPasswordGs = addValues.get(0);
                            List<GuardedString> removeValues = getRemoveValues(delta, GuardedString.class);
                            if (removeValues == null || removeValues.size() != 1) {
                                throw new InvalidAttributeValueException("Wrong remove set in password delta: "+removeValues);
                            }
                            GuardedString oldPasswordGs = removeValues.get(0);
                            assertPassword(account, oldPasswordGs);
                            changePassword(account, newPasswordGs);
                        }

                    } else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
                        assertReplace(delta);
                        account.setEnabled(getBoolean(delta));

                    } else if (delta.is(OperationalAttributes.ENABLE_DATE_NAME)) {
                        assertReplace(delta);
                        account.setValidFrom(getDate(delta));

                    } else if (delta.is(OperationalAttributes.DISABLE_DATE_NAME)) {
                        assertReplace(delta);
                        account.setValidTo(getDate(delta));

                    } else if (delta.is(PredefinedAttributes.LAST_LOGIN_DATE_NAME)) {
                        assertReplace(delta);
                        account.setLastLoginDate(getDate(delta));

                    } else if (delta.is(OperationalAttributes.LOCK_OUT_NAME)) {
                        assertReplace(delta);
                        account.setLockoutStatus(getBooleanMandatory(delta));

                    } else if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equalsIgnoreCase(delta.getName())) {
                        applyAuxiliaryObjectClassDelta(account, delta);

                    } else {
                        applyOrdinaryAttributeDelta(account, delta, null);

                    }
                }


            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {

                final DummyGroup group;
                if (configuration.isUidBoundToName()) {
                    group = resource.getGroupByName(uid.getUidValue(), false);
                } else {
                    group = resource.getGroupById(uid.getUidValue(), false);
                }
                if (group == null) {
                    throw new UnknownUidException("Group with UID "+uid+" does not exist on resource");
                }
                object = group;
                applyModifyMetadata(group, options);

                for (AttributeDelta delta : modifications) {
                    if (delta.is(Name.NAME)) {
                        assertReplace(delta);
                        String newName = getSingleReplaceValueMandatory(delta, String.class);
                        boolean doRename = handlePhantomRenames(objectClass, group, newName);
                        if (doRename) {
                            try {
                                resource.renameGroup(group.getId(), group.getName(), newName);
                            } catch (ObjectDoesNotExistException e) {
                                throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                            } catch (ObjectAlreadyExistsException e) {
                                throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                            }
                            // We need to change the returned uid here
                            if (configuration.isUidBoundToName()) {
                                addUidChange(sideEffectChanges, newName);
                            }
                        }
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


            } else {

                DummyObject dummyObject = findObjectByUidRequired(objectClassName, uid, false);
                object = dummyObject;
                applyModifyMetadata(dummyObject, options);

                for (AttributeDelta delta : modifications) {
                    if (delta.is(Name.NAME)) {
                        assertReplace(delta);
                        String newName = getSingleReplaceValueMandatory(delta, String.class);
                        boolean doRename = handlePhantomRenames(objectClass, dummyObject, newName);
                        if (doRename) {
                            try {
                                resource.renameObject(objectClassName, dummyObject.getId(), dummyObject.getName(), newName);
                            } catch (ObjectDoesNotExistException e) {
                                throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                            } catch (ObjectAlreadyExistsException e) {
                                throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                            }
                            // We need to change the returned uid here
                            if (configuration.isUidBoundToName()) {
                                addUidChange(sideEffectChanges, newName);
                            }
                        }
                    } else if (delta.is(OperationalAttributes.PASSWORD_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to change password on " + objectClassName);

                    } else if (delta.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to change enable on " + objectClassName);

                    } else {
                        applyOrdinaryAttributeDelta(dummyObject, delta, null);
                    }
                }
            }
        } catch (ConnectException e) {
            LOG.info("update::exception "+e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.info("update::exception "+e);
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("update::exception "+e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("update::exception "+e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("update::exception "+e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("update::exception "+e);
            throw new OperationTimeoutException(e);
        }

        resource.invokeHooks(h -> h.afterModifyOperation(object, modifications));

        LOG.info("update::end {0}", instanceName);
        return sideEffectChanges;
    }

    private boolean handlePhantomRenames(ObjectClass objectClass, DummyObject account, String newName) {
        LOG.info("XXX: RENAME {0} -> {1}", account.getName(), newName);
        if (isNameEquivalent(account.getName(), newName)) {
            if (ArrayUtils.contains(getConfiguration().getAlwaysRequireUpdateOfAttribute(), objectClass.getObjectClassValue() + ":" + Name.NAME)) {
                LOG.ok("Phantom rename of {0} {1}", objectClass.getObjectClassValue(), newName);
                return false;
            } else {
                throw new ConnectorException("Phantom rename of object " + newName);
            }
        }
        return true;
    }

    protected boolean isNameEquivalent(String oldName, String newName) {
        if (Objects.equals(oldName, newName)) {
            return true;
        }
        if (getConfiguration().getCaseIgnoreId()) {
            if (StringUtils.equalsIgnoreCase(oldName, newName)) {
                return true;
            }
        }
        return false;
    }

    protected void validateModifications(ObjectClass objectClass, Set<AttributeDelta> modifications) {
        String[] alwaysRequireUpdateOfAttributes = getConfiguration().getAlwaysRequireUpdateOfAttribute();
        for (String alwaysRequireUpdateOfAttributeSpec : alwaysRequireUpdateOfAttributes) {
            String[] split = alwaysRequireUpdateOfAttributeSpec.split(":");
            String objectClassName = split[0];
            String alwaysRequireUpdateOfAttribute = split[1];
            if (!objectClassName.equals(objectClass.getObjectClassValue())) {
                continue;
            }
            AttributeDelta modification = findModification(modifications, alwaysRequireUpdateOfAttribute);
            if (modification == null) {
                throw new InvalidAttributeValueException("Missing required attribute "+alwaysRequireUpdateOfAttribute+" in update operation");
            }
            if (modification.getValuesToAdd() != null) {
                throw new InvalidAttributeValueException("Unexpected add delta for attribute "+alwaysRequireUpdateOfAttribute+" in update operation");
            }
            if (modification.getValuesToRemove() != null) {
                throw new InvalidAttributeValueException("Unexpected remove delta for attribute "+alwaysRequireUpdateOfAttribute+" in update operation");
            }
            if (modification.getValuesToReplace() == null) {
                throw new InvalidAttributeValueException("No replace delta for attribute "+alwaysRequireUpdateOfAttribute+" in update operation");
            }
        }
    }

    private AttributeDelta findModification(Set<AttributeDelta> modifications, String attributeName) {
        if (modifications == null) {
            return null;
        }
        for (AttributeDelta modification : modifications) {
            if (modification.is(attributeName)) {
                return modification;
            }
        }
        return null;
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

    private <T> void applyOrdinaryAttributeDelta(DummyObject dummyObject, AttributeDelta delta, Function<List<T>, List<T>> valuesTransformer)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        String attributeName = delta.getName();
        try {
            List<T> replaceValues = getReplaceValues(delta, null);
            if (replaceValues != null) {
                if (valuesTransformer != null) {
                    replaceValues = valuesTransformer.apply(replaceValues);
                }
                if (dummyObject.isLink(attributeName)) {
                    replaceLinkValues(dummyObject, attributeName, replaceValues);
                } else {
                    dummyObject.replaceAttributeValues(attributeName, (Collection) replaceValues);
                }
            }
            List<T> addValues = getAddValues(delta, null);
            if (addValues != null) {
                if (valuesTransformer != null) {
                    addValues = valuesTransformer.apply(addValues);
                }
                if (dummyObject.isLink(attributeName)) {
                    addLinkValues(dummyObject, attributeName, addValues);
                } else {
                    dummyObject.addAttributeValues(attributeName, (Collection) addValues);
                }
            }
            List<T> deleteValues = getRemoveValues(delta, null);
            if (deleteValues != null) {
                if (valuesTransformer != null) {
                    deleteValues = valuesTransformer.apply(deleteValues);
                }
                if (dummyObject.isLink(attributeName)) {
                    deleteLinkValues(dummyObject, attributeName, deleteValues);
                } else {
                    dummyObject.removeAttributeValues(attributeName, (Collection) deleteValues);
                }
            }
        } catch (SchemaViolationException e) {
            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
            // The framework should deal with it ... somehow
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new OperationTimeoutException(e.getMessage(), e);
        }
    }

    private void replaceLinkValues(DummyObject dummyObject, String linkName, Collection<?> valuesToReplace)
            throws ConflictException, FileNotFoundException, SchemaViolationException,
            InterruptedException, ConnectException {
        dummyObject.deleteAllLinkValues(linkName);
        addLinkValues(dummyObject, linkName, valuesToReplace);
    }

    private void addLinkValues(DummyObject dummyObject, String linkName, Collection<?> valuesToAdd)
            throws ConflictException, FileNotFoundException, SchemaViolationException,
            InterruptedException, ConnectException {
        for (Object valueToAdd : valuesToAdd) {
            var targetObject = convertReferenceAttributeValueWhenAdding(valueToAdd);
            dummyObject.addLinkValue(linkName, targetObject);
        }
    }

    private void deleteLinkValues(DummyObject dummyObject, String linkName, Collection<?> valuesToDelete)
            throws SchemaViolationException, ConflictException, FileNotFoundException,
            InterruptedException, ConnectException {
        for (Object valueToDelete : valuesToDelete) {
            for (DummyObject linkedObject : dummyObject.getLinkedObjects(linkName)) {
                if (objectMatches(linkedObject, valueToDelete)) {
                    dummyObject.deleteLinkValue(linkName, linkedObject);
                }
            }
        }
    }

    private boolean objectMatches(DummyObject object, Object value) throws SchemaViolationException {
        if (!(value instanceof ConnectorObjectReference reference)) {
            throw new SchemaViolationException("Trying to delete non-reference link value: " + value);
        }
        for (var icfAttribute : reference.getValue().getAttributes()) {
            var attrName = icfAttribute.getName();
            if (icfAttribute.is(Uid.NAME)) {
                var currentUidValue = createUid(object).getUidValue();
                var expectedUidValue = Utils.getAttributeSingleValue(icfAttribute, String.class);
                if (!Objects.equals(currentUidValue, expectedUidValue)) {
                    return false;
                }
            } else if (icfAttribute.is(Name.NAME)) {
                var currentName = object.getName();
                var expectedName = convertIcfName(Utils.getAttributeSingleValue(icfAttribute, String.class));
                if (!Objects.equals(currentName, expectedName)) {
                    return false;
                }
            } else if (object.isLink(attrName)) {
                var currentLinkedObjects = object.getLinkedObjects(attrName);
                var expectedValues = emptyIfNull(icfAttribute.getValue());
                if (!linkValuesMatch(currentLinkedObjects, expectedValues)) {
                    return false;
                }
            } else {
                Set<Object> currentValues = new HashSet<>(emptyIfNull(object.getAttributeValues(attrName, Object.class)));
                Set<Object> expectedValues = new HashSet<>(emptyIfNull(icfAttribute.getValue()));
                if (!Objects.equals(currentValues, expectedValues)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean linkValuesMatch(Collection<DummyObject> currentLinkedObjects, List<Object> expectedValues)
            throws SchemaViolationException {
        var remainingExpectedValues = new ArrayList<>(expectedValues);
        for (DummyObject currentLinkedObject : currentLinkedObjects) {
            var matchFound = false;
            for (Object expectedValue : remainingExpectedValues) {
                if (objectMatches(currentLinkedObject, expectedValue)) {
                    matchFound = true;
                    remainingExpectedValues.remove(expectedValue);
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        return remainingExpectedValues.isEmpty();
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

    private <T> List<T> getReplaceValues(AttributeDelta modification, Class<T> extectedClass) {
        return assertTypes(modification, modification.getValuesToReplace(), "replace", extectedClass);
    }

    private <T> List<T> getAddValues(AttributeDelta modification, Class<T> extectedClass) {
        return assertTypes(modification, modification.getValuesToAdd(), "add", extectedClass);
    }

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
        //noinspection unchecked
        return (List<T>) newValues;
    }

    private void addUidChange(Set<AttributeDelta> sideEffectChanges, String newUidValue) {
        sideEffectChanges.add(
                AttributeDeltaBuilder.build(Uid.NAME, newUidValue));
    }

    protected void addAdditionalCommonAttributes(ConnectorObjectBuilder builder, DummyObject dummyObject) {
        super.addAdditionalCommonAttributes(builder, dummyObject);
        String connectorInstanceNameAttribute = getConfiguration().getConnectorInstanceNameAttribute();
        if (connectorInstanceNameAttribute != null) {
            LOG.info("Putting connectorInstance name into {0}: {1}", connectorInstanceNameAttribute, instanceName);
            builder.addAttribute(connectorInstanceNameAttribute, instanceName);
        }
    }

    @Override
    protected void extendSchema(SchemaBuilder builder) {
        super.extendSchema(builder);

        if (configuration.getSupportRunAs()) {
            LOG.ok("Adding runAs options to schema");
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithUser(), UpdateDeltaOp.class);
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithPassword(), UpdateDeltaOp.class);
        }
    }

    @Override
    public Schema getPartialSchema(LightweightObjectClassInfo... lightweightObjectClassInfos) {
        var filtered = new SchemaBuilder(this.getClass());
        var original = schema();
        for (LightweightObjectClassInfo requested : lightweightObjectClassInfos) {
            filtered.defineObjectClass(original.findObjectClassInfo(requested.getType()));
        }
        var originalOpts = original.getSupportedOptionsByOperation();
        var alreadyDefined = new HashSet<OperationOptionInfo>();
        if (originalOpts != null) {
            for (var opts : originalOpts.entrySet()) {
                for (var opt : opts.getValue()) {
                    if (!alreadyDefined.contains(opt)) {
                        filtered.defineOperationOption(opt);
                        alreadyDefined.add(opt);
                    }
                }
            }
        }
        return filtered.build();
    }

    @Override
    public LightweightObjectClassInfo[] getObjectClassInformation() {
        return schema().getObjectClassInfo().stream()
                .map(LightweightObjectClassInfo::new)
                .toList().toArray(new LightweightObjectClassInfo[0]);
    }
}
