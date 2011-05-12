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

package com.evolveum.midpoint.exception;

/**
 * Illegal Request Exception.
 *
 * This is a reusable exception type that indicates that the request was
 * ill-formed. It may have illegal format, may miss required attribute or it
 * is violating interface contract in some other way.
 *
 * The reaction to this exception should be IllegalArgumentFaultType in WSDL
 * interface.
 *
 * This is different from IllegalArgumentException in that this is a checked
 * exception. IllegalArgumentException indicates the programmer's error in
 * the java code. This exception does not indicate programmer's erorro, or at
 * least not a programmer error on "our" side. This exception indicates that we
 * have been called with wrong parameters (and we have somehow expected that).
 *
 * @author semancik
 */
public class IllegalRequestException extends Exception {

    /**
     * Creates a new instance of <code>IllegalRequestException</code> without detail message.
     */
    public IllegalRequestException() {
    }


    /**
     * Constructs an instance of <code>IllegalRequestException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public IllegalRequestException(String msg) {
        super(msg);
    }
}
