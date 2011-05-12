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

package com.evolveum.midpoint.provisioning.schema;

import javax.xml.namespace.QName;

/**
 * <i>Operation</i> is meta data responsible for describing the specific
 * {@link ResourceAttribute} behavior.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class Operation {

    public static final String code_id = "$Id$";
    /**
     * Possible Resource Object Operations
     */
    public static final String OP_CREATE = "CREATE";
    public static final String OP_READ = "READ";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
    public static final String OP_DISABLE = "DISABLE";
    public static final String OP_ENABLE = "ENABLE";
    public static final String OP_TEST = "TEST";
    public static final String OP_SEARCH = "SEARCH";
    //
    private String name;
    private Flags action = Flags.EXECUTE;
    /**
     * The operation use this attribute to read some information
     * 
     */
    private QName readAttribute = null;
    /**
     * The operation use this attribute to write some information
     * Example: LDAP systems sometimes use different attribute to
     * enable or disable accounts
     *
     */
    private QName writeAttribute = null;
    /**
     * Formula to calculate the value of the writeAttribute
     * when this action is executed. Usefull at
     * Activation/Deactivation or Enable/Disable
     * 
     */
    private String formula = null;

    public static enum Flags {

        /**
         * The operation execute normaly
         */
        EXECUTE,
        /**
         * The operation is not executes and there is not response of it
         */
        IGNORE_SILENTLY,
        /**
         * The operation is not execute and an exception is raised
         */
        IGNORE_EXCEPTION,
        /**
         * The peration is executed but the invvocation is redirected to an
         * injected test system
         */
        TEST
    }

    Operation(String name) {
        this.name = name;
    }

    public Flags getAction() {
        return action;
    }

    public void setAction(Flags action) {
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QName getReadAttribute() {
        return readAttribute;
    }

    public void setReadAttribute(QName readAttribute) {
        this.readAttribute = readAttribute;
    }

    public QName getWriteAttribute() {
        return writeAttribute;
    }

    public void setWriteAttribute(QName writeAttribute) {
        this.writeAttribute = writeAttribute;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }
}
