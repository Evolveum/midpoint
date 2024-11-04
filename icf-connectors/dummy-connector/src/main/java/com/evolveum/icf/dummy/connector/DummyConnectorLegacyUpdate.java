/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import com.evolveum.icf.dummy.resource.*;

/**
 * Connector for the Dummy Resource, Legacy version.
 * This version supports UpdateAttributeValuesOp.
 *
 * @see DummyResource
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_LEGACY_NAME", configurationClass = DummyConfiguration.class)
public class DummyConnectorLegacyUpdate extends AbstractObjectDummyConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<Filter>, SyncOp, TestOp, UpdateAttributeValuesOp {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(DummyConnectorLegacyUpdate.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        LOG.info("update::begin");
        validate(objectClass);
        validate(uid);

        var objectClassName = fromConnIdObjectClass(objectClass);

        try {

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
                applyModifyMetadata(account, options);

                // we do this before setting attribute values, in case when description itself would be changed
                resource.changeDescriptionIfNeeded(account);

                for (Attribute attr : replaceAttributes) {
                    if (attr.is(Name.NAME)) {
                        String newName = (String) attr.getValue().get(0);
                        try {
                            resource.renameAccount(account.getId(), account.getName(), newName);
                        } catch (ObjectDoesNotExistException e) {
                            throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                        } catch (ObjectAlreadyExistsException e) {
                            throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                        } catch (SchemaViolationException e) {
                            throw new org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException("Schema exception: " + e.getMessage(), e);
                        }
                        // We need to change the returned uid here (only if the mode is not set to NAME)
                        if (configuration.isUidBoundToName()) {
                            uid = new Uid(newName);
                        }
                    } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                        changePassword(account, attr);

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        account.setEnabled(getBoolean(attr));

                    } else if (attr.is(OperationalAttributes.ENABLE_DATE_NAME)) {
                        account.setValidFrom(getDate(attr));

                    } else if (attr.is(OperationalAttributes.DISABLE_DATE_NAME)) {
                        account.setValidTo(getDate(attr));

                    } else if (attr.is(PredefinedAttributes.LAST_LOGIN_DATE_NAME)) {
                        account.setLastLoginDate(getDate(attr));

                    } else if (attr.is(OperationalAttributes.LOCK_OUT_NAME)) {
                        account.setLockoutStatus(getBooleanMandatory(attr));

                    } else if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equalsIgnoreCase(attr.getName())) {
                        account.replaceAuxiliaryObjectClassNames(attr.getValue());

                    } else {
                        String name = attr.getName();
                        try {
                            account.replaceAttributeValues(name, attr.getValue());
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
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
                    throw new UnknownUidException("Group with UID " + uid + " does not exist on resource");
                }
                applyModifyMetadata(group, options);

                for (Attribute attr : replaceAttributes) {
                    if (attr.is(Name.NAME)) {
                        String newName = (String) attr.getValue().get(0);
                        try {
                            resource.renameGroup(group.getId(), group.getName(), newName);
                        } catch (ObjectDoesNotExistException e) {
                            throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                        } catch (ObjectAlreadyExistsException e) {
                            throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                        }
                        // We need to change the returned uid here (only if the mode is not set to NAME)
                        if (configuration.isUidBoundToName()) {
                            uid = new Uid(newName);
                        }
                    } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to change password on group");

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        group.setEnabled(getBooleanMandatory(attr));

                    } else {
                        String name = attr.getName();
                        List<Object> values = attr.getValue();
                        if (attr.is(DummyGroup.ATTR_MEMBERS_NAME) && values != null && configuration.getUpCaseName()) {
                            List<Object> newValues = new ArrayList<>(values.size());
                            for (Object val : values) {
                                newValues.add(StringUtils.upperCase((String) val));
                            }
                            values = newValues;
                        }
                        try {
                            group.replaceAttributeValues(name, values);
                        } catch (SchemaViolationException e) {
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }

            } else {

                DummyObject dummyObject = findObjectByUidRequired(objectClassName, uid, false);
                applyModifyMetadata(dummyObject, options);

                for (Attribute attr : replaceAttributes) {
                    if (attr.is(Name.NAME)) {
                        String newName = (String) attr.getValue().get(0);
                        try {
                            resource.renameObject(objectClassName, dummyObject.getId(), dummyObject.getName(), newName);
                        } catch (ObjectDoesNotExistException e) {
                            throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
                        } catch (ObjectAlreadyExistsException e) {
                            throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
                        }
                        // We need to change the returned uid here (only if the mode is not set to NAME)
                        if (configuration.isUidBoundToName()) {
                            uid = new Uid(newName);
                        }
                    } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to change password on " + objectClassName);

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to change enable on " + objectClassName);

                    } else {
                        String name = attr.getName();
                        try {
                            dummyObject.replaceAttributeValues(name, attr.getValue());
                        } catch (SchemaViolationException e) {
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }
            }

        } catch (ConnectException e) {
            LOG.info("update::exception " + e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("update::exception " + e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("update::exception " + e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("update::exception " + e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("update::exception " + e);
            throw new OperationTimeoutException(e);
        }

        LOG.info("update::end");
        return uid;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        validate(objectClass);
        validate(uid);

        var objectClassName = fromConnIdObjectClass(objectClass);

        try {

            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

                DummyAccount account;
                if (configuration.isUidBoundToName()) {
                    account = resource.getAccountByName(uid.getUidValue());
                } else {
                    account = resource.getAccountById(uid.getUidValue());
                }
                if (account == null) {
                    throw new UnknownUidException("Account with UID " + uid + " does not exist on resource");
                }
                applyModifyMetadata(account, options);

                // we could change the description here, but don't do that not to collide with ADD operation
                // TODO add the functionality if needed

                for (Attribute attr : valuesToAdd) {

                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        if (account.getPassword() != null) {
                            throw new InvalidAttributeValueException("Attempt to add value for password while password is already set");
                        }
                        changePassword(account, attr);

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to add value for enable attribute");

                    } else if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equalsIgnoreCase(attr.getName())) {
                        account.addAuxiliaryObjectClassNames(attr.getValue());

                    } else {
                        String name = attr.getName();
                        try {
                            account.addAttributeValues(name, attr.getValue());
                            LOG.ok("Added attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), account, account.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }

            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {

                DummyGroup group;
                if (configuration.isUidBoundToName()) {
                    group = resource.getGroupByName(uid.getUidValue());
                } else {
                    group = resource.getGroupById(uid.getUidValue());
                }
                if (group == null) {
                    throw new UnknownUidException("Group with UID " + uid + " does not exist on resource");
                }
                applyModifyMetadata(group, options);

                for (Attribute attr : valuesToAdd) {

                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        throw new InvalidAttributeValueException("Attempt to change password on group");

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to add value for enable attribute");

                    } else {
                        String name = attr.getName();
                        List<Object> values = attr.getValue();
                        if (attr.is(DummyGroup.ATTR_MEMBERS_NAME) && values != null && configuration.getUpCaseName()) {
                            List<Object> newValues = new ArrayList<>(values.size());
                            for (Object val : values) {
                                newValues.add(StringUtils.upperCase((String) val));
                            }
                            values = newValues;
                        }
                        try {
                            group.addAttributeValues(name, values);
                            LOG.ok("Added attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), group, group.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }

            } else {

                DummyObject dummyObject;
                if (configuration.isUidBoundToName()) {
                    dummyObject = resource.getObjectByName(objectClassName, uid.getUidValue());
                } else {
                    dummyObject = resource.getObjectById(uid.getUidValue());
                }
                if (dummyObject == null) {
                    throw getUnknownUidException(objectClassName, uid);
                }
                applyModifyMetadata(dummyObject, options);

                for (Attribute attr : valuesToAdd) {

                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        throw new InvalidAttributeValueException("Attempt to change password on " + objectClassName);

                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to add value for enable attribute");

                    } else {
                        String name = attr.getName();
                        try {
                            dummyObject.addAttributeValues(name, attr.getValue());
                            LOG.ok("Added attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), dummyObject, dummyObject.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }
            }

        } catch (ConnectException e) {
            LOG.info("addAttributeValues::exception " + e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("addAttributeValues::exception " + e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("addAttributeValues::exception " + e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("addAttributeValues::exception " + e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("addAttributeValues::exception " + e);
            throw new OperationTimeoutException(e);
        }

        return uid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        validate(objectClass);
        validate(uid);

        var objectClassName = fromConnIdObjectClass(objectClass);

        try {

            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

                DummyAccount account;
                if (configuration.isUidBoundToName()) {
                    account = resource.getAccountByName(uid.getUidValue());
                } else {
                    account = resource.getAccountById(uid.getUidValue());
                }
                if (account == null) {
                    throw new UnknownUidException("Account with UID " + uid + " does not exist on resource");
                }
                applyModifyMetadata(account, options);

                // we could change the description here, but don't do that not to collide with REMOVE operation
                // TODO add the functionality if needed

                for (Attribute attr : valuesToRemove) {
                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        throw new UnsupportedOperationException("Removing password value is not supported");
                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to remove value from enable attribute");
                    } else if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equalsIgnoreCase(attr.getName())) {
                        account.deleteAuxiliaryObjectClassNames(attr.getValue());
                    } else {
                        String name = attr.getName();
                        try {
                            account.removeAttributeValues(name, attr.getValue());
                            LOG.ok("Removed attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), account, account.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }

            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {

                DummyGroup group;
                if (configuration.isUidBoundToName()) {
                    group = resource.getGroupByName(uid.getUidValue());
                } else {
                    group = resource.getGroupById(uid.getUidValue());
                }
                if (group == null) {
                    throw new UnknownUidException("Group with UID " + uid + " does not exist on resource");
                }
                applyModifyMetadata(group, options);

                for (Attribute attr : valuesToRemove) {
                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        throw new InvalidAttributeValueException("Attempt to change password on group");
                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to remove value from enable attribute");
                    } else {
                        String name = attr.getName();
                        List<Object> values = attr.getValue();
                        if (attr.is(DummyGroup.ATTR_MEMBERS_NAME) && values != null && configuration.getUpCaseName()) {
                            List<Object> newValues = new ArrayList<>(values.size());
                            for (Object val : values) {
                                newValues.add(StringUtils.upperCase((String) val));
                            }
                            values = newValues;
                        }
                        try {
                            group.removeAttributeValues(name, values);
                            LOG.ok("Removed attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), group, group.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }

            } else {

                DummyObject dummyObject = findObjectByUidRequired(objectClassName, uid, false);
                applyModifyMetadata(dummyObject, options);

                for (Attribute attr : valuesToRemove) {
                    if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                        throw new InvalidAttributeValueException("Attempt to change password");
                    } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
                        throw new InvalidAttributeValueException("Attempt to remove value from enable attribute");
                    } else {
                        String name = attr.getName();
                        try {
                            dummyObject.removeAttributeValues(name, attr.getValue());
                            LOG.ok("Removed attribute {0} values {1} from {2}, resulting values: {3}",
                                    name, attr.getValue(), dummyObject, dummyObject.getAttributeValues(name, Object.class));
                        } catch (SchemaViolationException e) {
                            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                            // The framework should deal with it ... somehow
                            throw new InvalidAttributeValueException(e.getMessage(), e);
                        }
                    }
                }
            }

        } catch (ConnectException e) {
            LOG.info("removeAttributeValues::exception " + e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("removeAttributeValues::exception " + e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("removeAttributeValues::exception " + e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("removeAttributeValues::exception " + e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("removeAttributeValues::exception " + e);
            throw new OperationTimeoutException(e);
        }

        return uid;
    }

    @Override
    protected void extendSchema(SchemaBuilder builder) {
        super.extendSchema(builder);

        if (configuration.getSupportRunAs()) {
            LOG.ok("Adding runAs options to schema");
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithUser(), UpdateAttributeValuesOp.class);
            builder.defineOperationOption(OperationOptionInfoBuilder.buildRunWithPassword(), UpdateAttributeValuesOp.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {

        try {
            return resource.runScript(request.getScriptLanguage(), request.getScriptText(), request.getScriptArguments());
        } catch (IllegalArgumentException e) {
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        }
    }

}
