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

package com.evolveum.midpoint.provisioning.exceptions;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * Parameter validation failed.
 *
 * This most likely happens before provisioning.
 *
 * @author semancik
 */
public class ValidationException extends Exception {

    /**
     * Creates a new instance of <code>ValidationException</code> without detail message.
     */
    public ValidationException() {
    }


    /**
     * Constructs an instance of <code>ValidationException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ValidationException(String msg) {
        super(msg);
        failedAttributes = new ArrayList<QName>();
    }

    protected List<QName> failedAttributes;

    /**
     * Get the value of failedAttributes
     *
     * @return the value of failedAttributes
     */
    public List<QName> getFailedAttributes() {
        return failedAttributes;
    }

    /**
     * Set the value of failedAttributes
     *
     * @param failedAttributes new value of failedAttributes
     */
    public void setFailedAttributes(List<QName> failedAttributes) {
        this.failedAttributes = failedAttributes;
    }

    public void addFailedAttribute(QName failedAttribute) {
        failedAttributes.add(failedAttribute);
    }

}
