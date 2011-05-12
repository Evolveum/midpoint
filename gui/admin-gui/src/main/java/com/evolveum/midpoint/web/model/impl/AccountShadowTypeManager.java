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

package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.diff.DiffException;
import com.evolveum.midpoint.util.Utils;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.AccountShadowManager;
import com.evolveum.midpoint.web.model.ObjectStage;
import com.evolveum.midpoint.web.model.PagingDto;
import com.evolveum.midpoint.web.model.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.PropertyChange;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End user entity.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class AccountShadowTypeManager implements AccountShadowManager, Serializable {

	private static final long serialVersionUID = 4540270042561861862L;
	private static final Trace TRACE = TraceManager.getTrace(AccountShadowTypeManager.class);
    private Class constructAccountShadowType;

    public AccountShadowTypeManager(Class constructAccountShadowType) {
        this.constructAccountShadowType = constructAccountShadowType;
    }

    @Autowired
    private transient ModelPortType port;
    
    @Override
    public Collection<AccountShadowDto> list() {

        try { // Call Web Service Operation
            // TODO: more reasonable handling of paging info
            PagingType paging = new PagingType();
            ObjectListType result = port.listObjects(Utils.getObjectType("AccountType"),paging);
            List<ObjectType> objects = result.getObject();
            Collection<AccountShadowDto> items = new ArrayList<AccountShadowDto>(objects.size());
            for (Object o : objects) {
                AccountShadowType accountType = (AccountShadowType) o;
                items.add((new AccountShadowDto(accountType)));
            }
            return items;
        } catch (Exception ex) {
            TRACE.error("List accounts failed");
            TRACE.error("Exception was: ", ex);
            return null;
        }

    }

    @Override
    public AccountShadowDto get(String oid, PropertyReferenceListType resolve) throws WebModelException {
        TRACE.info("oid = {}", new Object[]{oid});
        Validate.notNull(oid);
        try { // Call Web Service Operation
            ObjectContainerType result = port.getObject(oid, resolve);
            ObjectStage stage = new ObjectStage();
            stage.setObject(result.getObject());

            AccountShadowDto accountShadowDto = (AccountShadowDto) constructAccountShadowType.newInstance();
            accountShadowDto.setStage(stage);

            return accountShadowDto;
        } catch (FaultMessage ex) {
            TRACE.error("Account lookup for oid = {} failed", oid);
            TRACE.error("Exception was: ", ex);
            throw new WebModelException(ex.getMessage(), "Failed to get account with oid " + oid);
        } catch (InstantiationException ex) {
            TRACE.error("Instantiation failed: {}", ex);
            return null;
        } catch (IllegalAccessException ex) {
            TRACE.error("Class or its nullary constructor is not accessible: {}", ex);
            return null;
        }
    }

    @Override
    public String add(AccountShadowDto accountShadowDto) throws WebModelException {
        Validate.notNull(accountShadowDto);

        try { // Call Web Service Operation
            ObjectContainerType objectContainer = new ObjectContainerType();
            objectContainer.setObject(accountShadowDto.getXmlObject());
            String result = port.addObject(objectContainer);
            return result;
        } catch (FaultMessage ex) {
            throw new WebModelException(ex.getMessage(), "[Web Service Error] Add account failed");
        }

    }

    @Override
    public void delete(String oid) throws WebModelException {
        TRACE.info("oid = {}", new Object[]{oid});
        Validate.notNull(oid);

        try { // Call Web Service Operation
            port.deleteObject(oid);
        } catch (FaultMessage ex) {
            throw new WebModelException(ex.getMessage(), "[Web Service Error] Delete account failed.");
        }
    }

    @Override
    public UserType listOwner(String oid) throws WebModelException {
        Validate.notNull(oid);

        try {
            UserContainerType userContainerType = port.listAccountShadowOwner(oid);
            UserType userType = userContainerType.getUser();
            return userType;
        } catch (FaultMessage ex) {
            throw new WebModelException(ex.getMessage(), "[Web Service Error] List owner failed.");
        }
    }

    @Override
    public AccountShadowDto create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<PropertyChange> submit(AccountShadowDto changedObject) throws WebModelException {
        AccountShadowDto oldObject = get(changedObject.getOid(), Utils.getResolveResourceList());
        try {
            ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldObject.getXmlObject(), changedObject.getXmlObject());
            if (changes != null && changes.getOid() != null) {
                port.modifyObject(changes);
            }
        } catch (FaultMessage ex) {
            throw new WebModelException(ex.getMessage(), "[Web Service Error] Submit account failed (Model service call failed)");
        } catch (DiffException ex) {
            throw new WebModelException(ex.getMessage(), "Submit account failed (XML Diff failed)");
        }

        //TODO: convert changes to GUI changes
        return null;
    }

    @Override
    public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<UserDto> list(PagingDto pagingDto) throws WebModelException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
