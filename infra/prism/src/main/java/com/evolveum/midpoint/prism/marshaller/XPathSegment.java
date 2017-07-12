/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.marshaller;

import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;

/**
 *
 * @author Radovan Semancik
 * @author Pavol Mederly
 */
public class XPathSegment {

    /*
     * These cases are possible:
     *
     * qName      variable    value       meaning
     * ===========================================
     * non-null   false       null        sub-element selector (e.g. .../c:givenName/...
     * non-null   true        null        variable name (e.g. $account/...)
     * null       false       non-null    ID value filter (e.g. .../c:accountConstruction[1])
     *
     * Forbidden combinations:
     * non-null   true        non-null
     * null       true        *
     * null       *           null
     * non-null   false       non-null    attribute value filter (e.g. ...[c:givenName='Peter']...) - PROBABLY WILL NOT BE IMPLEMENTED
     *
     */

    private QName qName;
    boolean variable;
    private String value;

    public XPathSegment(QName qName) {
        this.qName = qName;
        this.variable = false;
        this.value = null;
    }

    public XPathSegment(QName qName, boolean variable) {
        this.qName = qName;
        this.variable = variable;
        this.value = null;
    }

    public XPathSegment(String value) {
        this.qName = qName;
        this.variable = false;
        this.value = value;
    }

    public boolean isIdValueFilter() {
        return value != null;
    }

    /**
     * Get the value of qName
     *
     * @return the value of qName
     */
    public QName getQName() {
        return qName;
    }

    /**
     * Set the value of qName
     *
     * @param qName new value of qName
     */
    public void setQName(QName qName) {
        this.qName = qName;
    }

    public boolean isVariable() {
        return variable;
    }

    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (value != null) {
            sb.append("[");
            if (qName != null) {        // this is currently not used, anyway...
                sb.append(qName.toString());
                sb.append("='");
                sb.append(value);       // todo escape apostrophes in value
                sb.append("'");
            } else {
                sb.append(value);       // todo escape [, ], etc in value
            }
            sb.append("]");
        } else {
            if (variable) {
                sb.append("$");
            }
            sb.append(qName.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XPathSegment that = (XPathSegment) o;

        if (variable != that.variable) return false;
        if (qName != null ? !qName.equals(that.qName) : that.qName != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = qName != null ? qName.hashCode() : 0;
        result = 31 * result + (variable ? 1 : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    /**
     * Sets a given prefix to current QName (without changing NS URI).
     * It's a bit of hack.
     *
     * Precondition: there is no prefix set.
     * @param prefix
     */
    public void setQNamePrefix(String prefix) {
        if (qName == null) {
            throw new IllegalStateException("qName is null");
        }
        if (StringUtils.isNotEmpty(qName.getPrefix())) {
            throw new IllegalStateException("Prefix for qName is already set: " + qName.getPrefix());
        }
        qName = new QName(qName.getNamespaceURI(), qName.getLocalPart(), prefix);
    }
}
