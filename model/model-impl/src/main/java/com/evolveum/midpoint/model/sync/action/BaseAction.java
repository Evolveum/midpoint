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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vilo Repan
 */
public abstract class BaseAction implements Action {

    private UserSynchronizer synchronizer;
    private ChangeExecutor executor;
    @Deprecated
    private ModelController model;
    private List<Object> parameters;

    @Override
    public List<Object> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<Object>();
        }
        return parameters;
    }

    @Override
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    protected Element getParameterElement(QName qname) {
        Validate.notNull(qname, "QName must not be null.");

        List<Object> parameters = getParameters();
        Element element = null;
        for (Object object : parameters) {
            if (!(object instanceof Element)) {
                continue;
            }
            Element parameter = (Element) object;
            if (parameter.getLocalName().equals(qname.getLocalPart())
                    && qname.getNamespaceURI().equals(parameter.getNamespaceURI())) {
                element = parameter;
                break;
            }
        }

        return element;
    }

    protected UserType getUser(String oid, OperationResult result) throws SynchronizationException {
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        try {
            return model.getObject(UserType.class, oid, new PropertyReferenceListType(), result);
        } catch (ObjectNotFoundException ex) {
            // user was not found, we return null
        } catch (Exception ex) {
            throw new SynchronizationException("Can't get user with oid '" + oid
                    + "'. Unknown error occurred.", ex);
        }

        return null;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
                                 SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
                                 OperationResult result) throws SynchronizationException {
        Validate.notNull(change, "Resource object change description must not be null.");
        Validate.notNull(situation, "Synchronization situation must not be null.");
        Validate.notNull(shadowAfterChange, "Resource object shadow after change must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        return null;
    }

    @Deprecated
    public void setModel(ModelController model) {
        this.model = model;
    }

    @Deprecated
    protected ModelController getModel() {
        return model;
    }

    public UserSynchronizer getSynchronizer() {
        return synchronizer;
    }

    public void setSynchronizer(UserSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public ChangeExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ChangeExecutor executor) {
        this.executor = executor;
    }
}
