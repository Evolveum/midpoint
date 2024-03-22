/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.Application;
import org.apache.wicket.model.IModel;

public class ProtectedStringClearPasswordModel implements IModel<String> {
    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ProtectedStringClearPasswordModel.class);

    IModel<ProtectedStringType> psModel;

    public ProtectedStringClearPasswordModel(IModel<ProtectedStringType> psModel) {
        this.psModel = psModel;
    }

    @Override
    public void detach() {
        // Nothing to do
    }

    private Protector getProtector() {
        return ((MidPointApplication) Application.get()).getProtector();
    }

    @Override
    public String getObject() {
        ProtectedStringType actualPs = psModel.getObject();
        if (actualPs == null) {
            return null;
        }

        ProtectedStringType ps = actualPs.clone();
        try {
            ps.setExternalData(null);
            return getProtector().decryptString(ps);
        } catch (EncryptionException e) {
            LOGGER.debug("Couldn't get the object of the protected string model", e);
            return null;
        }
    }

    @Override
    public void setObject(String object) {
        if (object == null) {
            ExternalDataType externalData = null;
            if (psModel.getObject() != null) {
                externalData = psModel.getObject().getExternalData();
            }
            var emptyValue = new ProtectedStringType();
            emptyValue.setExternalData(externalData);
            psModel.setObject(emptyValue);
        } else {
            if (psModel.getObject() == null) {
                psModel.setObject(new ProtectedStringType());
            } else {
                psModel.getObject().clear();
            }
            psModel.getObject().setClearValue(object);
            try {
                getProtector().encrypt(psModel.getObject());
            } catch (EncryptionException e) {
                throw new SystemException(e.getMessage(), e);   // todo handle somewhat better
            }
        }
    }

}
