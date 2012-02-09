/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model;

import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    @Autowired(required = true)
    private transient TaskManager taskManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioning;

    public void executeChanges(Collection<ObjectDelta<?>> changes, OperationResult result) throws
            ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException {
        for (ObjectDelta<?> change : changes) {
            executeChange(change, result);
        }
    }

    public void executeChanges(SyncContext syncContext, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException {
        ObjectDelta<UserType> userDelta = syncContext.getUserDelta();
        if (userDelta != null) {
            LOGGER.trace("Executing USER change " + userDelta);
            executeChange(userDelta, result);

            // userDelta is composite, mixed from primary and secondary. The OID set into
            // it will be lost ... unless we explicitly save it
            syncContext.setUserOid(userDelta.getOid());
        } else {
            LOGGER.trace("Skipping change execute, because user delta is null");
        }

        for (AccountSyncContext accCtx : syncContext.getAccountContexts()) {
            ObjectDelta<AccountShadowType> accDelta = accCtx.getAccountDelta();
            if (accDelta == null) {
                LOGGER.trace("No change for account " + accCtx.getResourceAccountType());
                accCtx.setOid(getOidFromContext(accCtx));
                updateAccountLinks(syncContext.getUserNew(), accCtx, result);
                continue;
            }

            LOGGER.trace("Executing ACCOUNT change " + accDelta);
            executeChange(accDelta, result);
            // To make sure that the OID is set (e.g. after ADD operation)
            accCtx.setOid(accDelta.getOid());
            updateAccountLinks(syncContext.getUserNew(), accCtx, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Context after change execution:\n{}", syncContext.dump());
        }
    }

    private String getOidFromContext(AccountSyncContext context) {
        if (context.getAccountDelta() != null && context.getAccountDelta().getOid() != null) {
            return context.getAccountDelta().getOid();
        }

        if (context.getAccountOld() != null && context.getAccountOld().getOid() != null) {
            return context.getAccountOld().getOid();
        }

        if (context.getAccountSyncDelta() != null && context.getAccountSyncDelta().getOid() != null) {
            return context.getAccountSyncDelta().getOid();
        }

        return null;
    }

    /**
     * Make sure that the account is linked (or unlinked) as needed.
     */
    private void updateAccountLinks(PrismObject<UserType> userNew, AccountSyncContext accCtx,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        UserType userTypeNew = userNew.getOrParseObjectType();
        String accountOid = accCtx.getOid();
        if (accountOid == null) {
            throw new IllegalStateException("Account has null OID, this should not happen");
        }

        if (accCtx.getPolicyDecision() == PolicyDecision.UNLINK || accCtx.getPolicyDecision() == PolicyDecision.DELETE) {
            // Link should NOT exist
            for (ObjectReferenceType accountRef : userTypeNew.getAccountRef()) {
                if (accountRef.getOid().equals(accountOid)) {
                    // Linked, need to unlink
                    unlinkAccount(userTypeNew.getOid(), accountOid, result);
                }
            }
            // Not linked, that's OK

        } else {
            // Link should exist
            for (ObjectReferenceType accountRef : userTypeNew.getAccountRef()) {
                if (accountRef.getOid().equals(accountOid)) {
                    // Already linked, nothing to do
                    return;
                }
            }
            // Not linked, need to link
            linkAccount(userTypeNew.getOid(), accountOid, result);
        }
    }

    private void linkAccount(String userOid, String accountOid, OperationResult result) throws ObjectNotFoundException,
            SchemaException {

        LOGGER.trace("Linking account " + accountOid + " to user " + userOid);
        ObjectReferenceType accountRef = new ObjectReferenceType();
        accountRef.setOid(accountOid);
        accountRef.setType(ObjectTypes.ACCOUNT.getTypeQName());

        PropertyModificationType accountRefMod = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.add, null, SchemaConstants.I_ACCOUNT_REF, accountRef);
        ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid(userOid);
        objectChange.getPropertyModification().add(accountRefMod);
        cacheRepositoryService.modifyObject(UserType.class, objectChange, result);
    }

    private void unlinkAccount(String userOid, String accountOid, OperationResult result) throws
            ObjectNotFoundException, SchemaException {

        LOGGER.trace("Unlinking account " + accountOid + " to user " + userOid);
        ObjectReferenceType accountRef = new ObjectReferenceType();
        accountRef.setOid(accountOid);
        accountRef.setType(ObjectTypes.ACCOUNT.getTypeQName());

        PropertyModificationType accountRefMod = ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.delete, null, SchemaConstants.I_ACCOUNT_REF, accountRef);
        ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid(userOid);
        objectChange.getPropertyModification().add(accountRefMod);
        cacheRepositoryService.modifyObject(UserType.class, objectChange, result);
    }

    public void executeChange(ObjectDelta<?> change, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException {

        if (change == null) {
            throw new IllegalArgumentException("Null change");
        }

        if (change.getChangeType() == ChangeType.ADD) {
            executeAddition(change, result);

        } else if (change.getChangeType() == ChangeType.MODIFY) {
            executeModification(change, result);

        } else if (change.getChangeType() == ChangeType.DELETE) {
            executeDeletion(change, result);
        }
    }

    private void executeAddition(ObjectDelta<?> change, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException {

        PrismObject<?> mpObject = change.getObjectToAdd();

        if (change.getModifications() != null) {
            for (PropertyDelta delta : change.getModifications()) {
                delta.applyTo(mpObject);
            }
            change.getModifications().clear();
        }

        mpObject.setObjectType(null);
        ObjectType object = mpObject.getOrParseObjectType();

        String oid = null;
        if (object instanceof TaskType) {
            oid = addTask((TaskType) object, result);
        } else if (ObjectTypes.isManagedByProvisioning(object)) {
            oid = addProvisioningObject(object, result);
        } else {
            oid = cacheRepositoryService.addObject(object, result);
        }
        change.setOid(oid);

    }

    private void executeDeletion(ObjectDelta<? extends ObjectType> change, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {

        String oid = change.getOid();
        Class<? extends ObjectType> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            deleteProvisioningObject(objectTypeClass, oid, result);
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private void executeModification(ObjectDelta<?> change, OperationResult result) throws ObjectNotFoundException,
            SchemaException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        String oid = change.getOid();
        Class<? extends ObjectType> objectTypeClass = change.getObjectTypeClass();
        ObjectModificationType objectChange = change.toObjectModificationType();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(objectChange, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            modifyProvisioningObject(objectTypeClass, objectChange, result);
        } else {
            cacheRepositoryService.modifyObject(objectTypeClass, objectChange, result);
        }
    }

    private String addTask(TaskType task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException {
        try {
            return taskManager.addTask(task, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private String addProvisioningObject(ObjectType object, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException {

        if (object instanceof ResourceObjectShadowType) {
            ResourceObjectShadowType shadow = (ResourceObjectShadowType) object;
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
            ModelUtils.unresolveResourceObjectShadow(shadow);
        }

        try {
            ScriptsType scripts = getScripts(object, result);
            return provisioning.addObject(object, scripts, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (CommunicationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException {

        try {
            // TODO: scripts
            provisioning.deleteObject(objectTypeClass, oid, null, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private void modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass,
            ObjectModificationType objectChange, OperationResult result) throws ObjectNotFoundException {

        try {
            // TODO: scripts
            provisioning.modifyObject(objectTypeClass, objectChange, null, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private ScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException {
        ScriptsType scripts = null;
        if (object instanceof ResourceType) {
            ResourceType resource = (ResourceType) object;
            scripts = resource.getScripts();
        } else if (object instanceof ResourceObjectShadowType) {
            ResourceObjectShadowType resourceObject = (ResourceObjectShadowType) object;
            if (resourceObject.getResource() != null) {
                scripts = resourceObject.getResource().getScripts();
            } else {
                String resourceOid = ResourceObjectShadowUtil.getResourceOid(resourceObject);
                ResourceType resObject = provisioning.getObject(ResourceType.class, resourceOid,
                        new PropertyReferenceListType(), result);
                scripts = resObject.getScripts();
            }
        }

        if (scripts == null) {
            scripts = new ScriptsType();
        }

        return scripts;
    }

}
