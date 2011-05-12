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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Entity
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = Resource.DDL_TABLE_RESOURCE,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")})
@NamedQueries({
    @NamedQuery(name = Resource.QUERY_RESOURCE_FIND_ALL, query = "SELECT m FROM Resource m"),
    @NamedQuery(name = Resource.QUERY_RESOURCE_FIND_BY_NAME, query = "SELECT m FROM Resource m WHERE m.name = ?0")
})
public class Resource extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_RESOURCE = "Resources";
    // queries
    public final static String QUERY_RESOURCE_FIND_BY_NAME = "Resource.findByName";
    public final static String QUERY_RESOURCE_FIND_ALL = "Resource.findAll";

    @Column(table=Resource.DDL_TABLE_RESOURCE, name = "name", unique = false, nullable = false, length = 128)
    public String getName() {
        return name;
    }

    private String type;
    private String namespace;

    @Column(table=Resource.DDL_TABLE_RESOURCE, name = "type", unique = false, nullable = false, length = 128)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(table=Resource.DDL_TABLE_RESOURCE, name = "namespace", unique = false, nullable = false, length = 128)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return "Resource [" + "namespace " + namespace + " " + "type " + type + " " + "name " + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Resource other = (Resource) obj;
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.namespace == null) ? (other.namespace != null) : !this.namespace.equals(other.namespace)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 29 * hash + (this.namespace != null ? this.namespace.hashCode() : 0);
        return hash;
    }

}
