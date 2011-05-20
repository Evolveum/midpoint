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

package com.evolveum.midpoint.validator;

/**
 *
 * @author semancik
 */
public class ValidationMessage {
    
    public enum Type { WARNING, ERROR };

    public ValidationMessage() {
    }

    public ValidationMessage(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public ValidationMessage(Type type, String message, String oid) {
        this.type = type;
        this.message = message;
        this.oid = oid;
    }

    public ValidationMessage(Type type, String message, String oid, String property) {
        this.type = type;
        this.message = message;
        this.oid = oid;
        this.property = property;
    }

    public Type type;

    /**
     * Get the value of type
     *
     * @return the value of type
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the value of type
     *
     * @param type new value of type
     */
    public void setType(Type type) {
        this.type = type;
    }


    public String message;

    /**
     * Get the value of message
     *
     * @return the value of message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the value of message
     *
     * @param message new value of message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public String oid;

    /**
     * Get the value of oid
     *
     * @return the value of oid
     */
    public String getOid() {
        return oid;
    }

    /**
     * Set the value of oid
     *
     * @param oid new value of oid
     */
    public void setOid(String oid) {
        this.oid = oid;
    }

    public String property;

    /**
     * Get the value of property
     *
     * @return the value of property
     */
    public String getProperty() {
        return property;
    }

    /**
     * Set the value of property
     *
     * @param property new value of property
     */
    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (Type.ERROR.equals(getType())) {
            sb.append("ERROR: ");
        } else if (Type.WARNING.equals(getType())) {
            sb.append("WARNING: ");
        }
        sb.append(message);
        if (getOid()!=null || getProperty()!=null) {
            sb.append(" (");
            if (getOid()!=null) {
                sb.append("OID: ");
                sb.append(getOid());
            }
            if (getProperty()!=null) {
                sb.append(", property: ");
                sb.append(getProperty());
            }
            sb.append(")");
        }

        return sb.toString();
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ValidationMessage other = (ValidationMessage) obj;
        if (this.type != other.type) {
            return false;
        }
        if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message)) {
            return false;
        }
        if ((this.oid == null) ? (other.oid != null) : !this.oid.equals(other.oid)) {
            return false;
        }
        if ((this.property == null) ? (other.property != null) : !this.property.equals(other.property)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.type.hashCode();
        hash = 71 * hash + (this.message != null ? this.message.hashCode() : 0);
        hash = 71 * hash + (this.oid != null ? this.oid.hashCode() : 0);
        hash = 71 * hash + (this.property != null ? this.property.hashCode() : 0);
        return hash;
    }

}
