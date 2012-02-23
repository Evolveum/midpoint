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
package com.evolveum.midpoint.web.controller.config;

import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Controller("encryption")
@Scope("session")
public class EncryptionController implements Serializable {

    public static final String PAGE_NAVIGATION = "/admin/config/encryption?faces-redirect=true";
    private static final long serialVersionUID = 4415668346210408646L;
    private static final String OPTION_DECRYPT = "decrypt";
    private static final String OPTION_ENCRYPT = "encrypt";
    @Autowired(required = true)
    private transient Protector protector;
    @Autowired(required = true)
    private transient PrismContext prismContext;
    private String value;
    private String encrypt = OPTION_ENCRYPT;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(String encrypt) {
        this.encrypt = encrypt;
    }

    public boolean isEncrypting() {
        return OPTION_ENCRYPT.equals(getEncrypt());
    }

    public String init() {
        encrypt = OPTION_DECRYPT;
        value = null;

        return PAGE_NAVIGATION;
    }

    @SuppressWarnings("unchecked")
    public void runPerformed() {
        if (StringUtils.isEmpty(value)) {
            FacesUtils.addWarnMessage("Value must not be empty.");
        }

        try {
            PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
            if (isEncrypting()) {
                ProtectedStringType protectedString = protector.encryptString(value);
                value = jaxbProcessor.marshalElementToString(protectedString, SchemaConstants.R_PROTECTED_STRING);
            } else {
                JAXBElement<ProtectedStringType> object = jaxbProcessor.unmarshalElement(value, ProtectedStringType.class);
                ProtectedStringType protectedString = (ProtectedStringType) object.getValue();
                value = protector.decryptString(protectedString);
            }
        } catch (Exception ex) {
            String encrypt = isEncrypting() ? "encrypt" : "decrypt";
            FacesUtils.addErrorMessage("Couldn't " + encrypt + " value.", ex);
        }
    }
}
