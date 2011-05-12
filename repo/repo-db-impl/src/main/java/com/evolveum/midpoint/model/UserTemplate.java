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

package com.evolveum.midpoint.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
@Entity
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = UserTemplate.DDL_TABLE_USER_TEMPLATE,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")})
@NamedQueries(value = {
    @NamedQuery(name = UserTemplate.QUERY_USER_TEMPLATE_FIND_ALL, query = "SELECT m FROM UserTemplate m"),
    @NamedQuery(name = UserTemplate.QUERY_USER_TEMPLATE_FIND_BY_NAME, query = "SELECT m FROM UserTemplate m WHERE m.name = ?0")
})
public class UserTemplate extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_USER_TEMPLATE = "UserTemplates";
    // queries
    public final static String QUERY_USER_TEMPLATE_FIND_BY_NAME = "UserTemplate.findByUname";
    public final static String QUERY_USER_TEMPLATE_FIND_ALL = "UserTemplate.findAll";
    private String template;

    public UserTemplate() {
    }

    @Column(table = UserTemplate.DDL_TABLE_USER_TEMPLATE, name = "template", unique = false, nullable = false, length = 32000)
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @Column(table = UserTemplate.DDL_TABLE_USER_TEMPLATE, name = "name", unique = false, nullable = false, length = 128)
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "UserTemplate [" + "template " + template + " " + "name " + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserTemplate other = (UserTemplate) obj;
        if ((this.template == null) ? (other.template != null) : !this.template.equals(other.template)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.template != null ? this.template.hashCode() : 0);
        return hash;
    }
}
