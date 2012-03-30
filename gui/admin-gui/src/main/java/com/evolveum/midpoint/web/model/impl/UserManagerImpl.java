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
 */
package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.*;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class UserManagerImpl extends ObjectManagerImpl<UserType, GuiUserDto> implements UserManager {

    private static final Trace LOGGER = TraceManager.getTrace(UserManagerImpl.class);
    private static final long serialVersionUID = -3457278299468312767L;
    @Autowired(required = true)
    private transient ObjectTypeCatalog objectTypeCatalog;
    @Autowired(required = true)
    private transient Protector protector;
    @Autowired(required = true)
    private PrismContext prismContext;

    @Override
    protected Class<? extends ObjectType> getSupportedObjectClass() {
        return UserType.class;
    }

    @Override
    protected GuiUserDto createObject(UserType objectType) {
        return new GuiUserDto(objectType, getModel());
    }

    @Override
    public Collection<GuiUserDto> list(PagingType paging) {
        return list(paging, ObjectTypes.USER);
    }

    @Override
    public Set<PropertyChange> submit(GuiUserDto changedObject, Task task, OperationResult parentResult) {
        Validate.notNull(changedObject, "User object must not be null.");

        Set<PropertyChange> set = null;
        UserDto oldUser = get(changedObject.getOid(), new PropertyReferenceListType());

        // we don't want user to have resolved all account
        // only those which were added are needed to create appropriate link
        // between user and account and to add account to the resource and
        // repository
        // in the case of delete account, in this step only unlink should be
        // made..accounts are deleted in UserDetailsController using account
        // manager
        // in the case of modifying account, it is not needed to detect changes
        // using user, this is also made in UserDetailsController using account
        // manager
        

        OperationResult result = parentResult.createSubresult(UserManager.SUBMIT);
        try {
        	changedObject = unresolveNotAddedAccounts(changedObject);
            changedObject.encryptCredentials(protector);

            PropertyModificationType passwordChange = null;
            //detect if the password was changed
            if (changedObject.encryptCredentials(protector)) {
                PasswordType password = changedObject.getXmlObject().getCredentials().getPassword();
                // if password was changed, create modification change
                List<XPathSegment> segments = new ArrayList<XPathSegment>();
                segments.add(new XPathSegment(SchemaConstants.I_CREDENTIALS));
                segments.add(new XPathSegment(SchemaConstants.I_PASSWORD));
                XPathHolder xpath = new XPathHolder(segments);
                passwordChange = ObjectTypeUtil.createPropertyModificationType(
                        PropertyModificationTypeType.replace, xpath, SchemaConstants.R_PROTECTED_STRING,
                        password.getProtectedString());
                // now when modification change of password was made, clear
                // credentials from changed user and also from old user to be not used by diff..
               

            }
            changedObject.getXmlObject().setCredentials(null);
            oldUser.getXmlObject().setCredentials(null);

            //detect other changes
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("USER FORM old:\n{}", oldUser.toString());
                LOGGER.trace("USER FORM changed:\n{}", oldUser.toString());
            }

//            prismContext.adopt(changedObject.getXmlObject());
            ObjectDelta<UserType> userDelta = DiffUtil.diff(
                    oldUser.getXmlObject(), changedObject.getXmlObject(),
                    UserType.class, prismContext);
            
//            PrismObject<UserType> oldPrism = oldUser.getXmlObject().asPrismObject();
//            PrismObject<UserType> changedPrism = changedObject.getXmlObject().asPrismObject();
//            ObjectDelta<UserType> userDelta = oldPrism.diff(changedPrism);

            LOGGER.trace("USER FORM delta:\n{}", userDelta.dump());

            if (!userDelta.isEmpty()) {
                getModel().modifyObject(UserType.class, userDelta.getOid(),  userDelta.getModifications(), task, result);
            }
            //todo this magic was commented out during refactor, will be fixed later by somebody :)

//            //todo XXX: MEGA HACK
//
//            // Account add and account delete are doing "together" with user modify now, so they
//            // will be properly linked/unlinked "inside" the operation. FIXME: is this correct?
//
//            List<AccountShadowDto> newAccounts = changedObject.getAccount();
//            if (newAccounts != null) {
//                for (AccountShadowDto account : newAccounts) {
//                    if (account.getOid() == null) {
//                        // This has to mean that the account is being created
//                        accontAdd(changes, account.getXmlObject());
//                    } else if (hasAccount(oldUser.getXmlObject(), account.getOid())) {
//                        // modification?
//
//                    } else {
//                        // also creating an account
//                        accontAdd(changes, account.getXmlObject());
//                    }
//                }
//            }
//
//            List<AccountShadowDto> oldAccounts = oldUser.getAccount();
//            if (oldAccounts != null) {
//                for (AccountShadowDto account : oldAccounts) {
//                    if (account.getOid() == null) {
//                        // This has to mean that the account is being created
//                        accontDelete(changes, account.getXmlObject());
//                    } else if (hasAccount(changedObject.getXmlObject(), account.getOid())) {
//                        // no change in account links
//
//                    } else {
//                        // also creating an account
//                        accontDelete(changes, account.getXmlObject());
//                    }
//                }
//            }
//
//            //todo XXX: MEGA HACK END
//
////            ObjectModificationType changes1 = CalculateXmlDiff.calculateChanges(oldUser.getXmlObject(),
////                    changedObject.getXmlObject());
//
//
//            //process user changes
//            if (changes != null) {
//                if (passwordChange != null) {
//                    if (changes.getOid() == null) {
//                        changes.setOid(changedObject.getXmlObject().getOid());
//                    }
//                    changes.getPropertyModification().add(passwordChange);
//                }
//                if (changes.getOid() != null && changes.getPropertyModification().size() > 0) {
//                    getModel().modifyObject(UserType.class, changes, task, result);
//                }
//
//            }
//
//            //TODO: probably, this is not needed more, test and remove this if isn't needed
//            if (null != changes) {
//                set = new HashSet<PropertyChange>();
//                // TODO: finish this
//                List<PropertyModificationType> modifications = changes.getPropertyModification();
//                for (PropertyModificationType modification : modifications) {
//                    Set<Object> values = new HashSet<Object>();
//                    if (modification.getValue() != null) {
//                        values.addAll(modification.getValue().getAny());
//                    }
//                    set.add(new PropertyChange(createQName(modification.getPath()),
//                            getChangeType(modification.getModificationType()), values));
//                }
//            }

            result.recordSuccess();

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't submit user {}", ex,
                    new Object[]{changedObject.getName()});
            result.recordFatalError(ex);
        }

        result.computeStatus();

//		ControllerUtil.printResults(LOGGER, result);

        return set;
    }


    private boolean hasAccount(UserType user, String oid) {
        for (AccountShadowType account : user.getAccount()) {
            if (oid.equals(account.getOid())) {
                return true;
            }
        }
        for (ObjectReferenceType ref : user.getAccountRef()) {
            if (oid.equals(ref.getOid())) {
                return true;
            }
        }
        return false;
    }

//    private void accontAdd(ObjectModificationType changes, AccountShadowType xmlObject) throws JAXBException {
//        Element element = JAXBUtil.jaxbToDom(xmlObject, SchemaConstants.I_ACCOUNT,
//                DOMUtil.getDocument());
//
//        PropertyModificationType propertyModification = ObjectTypeUtil.createPropertyModificationType(
//                PropertyModificationTypeType.add, null, element);
//        changes.getPropertyModification().add(propertyModification);
//
//    }
//
//    private void accontDelete(ObjectModificationType changes, AccountShadowType xmlObject) throws JAXBException {
//        Element element = JAXBUtil.jaxbToDom(xmlObject, SchemaConstants.I_ACCOUNT,
//                DOMUtil.getDocument());
//
//        PropertyModificationType propertyModification = ObjectTypeUtil.createPropertyModificationType(
//                PropertyModificationTypeType.delete, null, element);
//        changes.getPropertyModification().add(propertyModification);
//
//    }

    @Override
    public String add(GuiUserDto object) {
        Validate.notNull(object);
        try {
            object.encryptCredentials(protector);
        } catch (EncryptionException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't encrypt credentials for user {}", ex,
                    object.getName());
        }
        return super.add(object);
    }

    @Override
    public AccountShadowDto addAccount(UserDto userDto, String resourceOid) {
        AccountShadowDto accountShadowDto = new AccountShadowDto();
        AccountShadowType accountShadowType = new AccountShadowType();
        accountShadowType.setAttributes(new ResourceObjectShadowAttributesType());

        ResourceManager manager = (ResourceManager) objectTypeCatalog.getObjectManager(ResourceType.class,
                GuiResourceDto.class);
        ResourceDto resourceDto = null;
        try {
            resourceDto = manager.get(resourceOid, new PropertyReferenceListType());
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("User add account failed, reason: " + ex.getMessage(), ex);
            return null;
        }
        accountShadowType.setResource((ResourceType) resourceDto.getXmlObject());
        accountShadowDto.setXmlObject(accountShadowType);

        // TODO: account is set to user not here, but in method where we are
        // going to persist it from GUI, because actual account is retrieved
        // from form generator userDto.getAccount().add(accountShadowDto);

        return accountShadowDto;
    }

    @Override
    public List<UserDto> search(QueryType search, PagingType paging) {
        Validate.notNull(search, "Query must not be null.");

        OperationResult result = new OperationResult(SEARCH);
        List<UserDto> users = new ArrayList<UserDto>();
        try {
            List<PrismObject<UserType>> list = getModel().searchObjects(UserType.class, search, paging, result);
            for (PrismObject<UserType> object : list) {
                UserDto userDto = createObject(object.asObjectable());
                users.add(userDto);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't search users", ex);
            FacesUtils.addErrorMessage("Couldn't search users.", ex);
        }

        return users;
    }

    private QName createQName(Element element) {
        String namespace = element.getNamespaceURI();
        if (namespace == null) {
            namespace = element.getBaseURI();
        }
        return new QName(namespace, element.getLocalName(), element.getPrefix());
    }

    private PropertyChange.ChangeType getChangeType(PropertyModificationTypeType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case add:
                return PropertyChange.ChangeType.ADD;
            case delete:
                return PropertyChange.ChangeType.DELETE;
            case replace:
                return PropertyChange.ChangeType.REPLACE;
            default:
                throw new IllegalArgumentException("Unknown change type '" + type + "'.");
        }
    }

    private GuiUserDto unresolveNotAddedAccounts(GuiUserDto changedObject) throws SchemaException {
        changedObject.getXmlObject().getAccountRef().clear();

        for (Iterator<AccountShadowDto> i = changedObject.getAccount().iterator(); i.hasNext(); ) {
            AccountShadowDto account = i.next();
            if (account.isAdded()) {
                changedObject.getXmlObject().getAccount().add(account.getXmlObject());
            } else {
                changedObject.getXmlObject().getAccount().remove(account.getXmlObject());
                ObjectReferenceType ort = new ObjectReferenceType();
                ort.setOid(account.getOid());
                changedObject.getXmlObject().getAccountRef().add(ort);
            }
        }
        return changedObject;
    }

}
