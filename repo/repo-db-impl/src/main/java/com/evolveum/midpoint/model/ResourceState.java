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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import org.hibernate.annotations.ForeignKey;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
@Entity
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = ResourceState.DDL_TABLE_RESOURCE_STATE,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")})
@NamedQueries({
    @NamedQuery(name = ResourceState.QUERY_RESOURCE_STATE_FIND_ALL, query = "SELECT m FROM ResourceState m")
})
public class ResourceState extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_RESOURCE_STATE = "ResourcesStates";
    // queries
    public final static String QUERY_RESOURCE_STATE_FIND_ALL = "ResourceState.findAll";
    private String state;
    private Resource resource;

    @Column(table = ResourceState.DDL_TABLE_RESOURCE_STATE, name = "state", unique = false, nullable = true, length = 32000)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(table = ResourceState.DDL_TABLE_RESOURCE_STATE, name = "resource_uuid", nullable = false, updatable = false)
    @ForeignKey(name = "none") 
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Column(table = ResourceState.DDL_TABLE_RESOURCE_STATE, name = "name", unique = false, nullable = false, length = 128)
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ResourceState [" + "resource " + resource + " " + "state " + state + " " + "name " + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceState other = (ResourceState) obj;
        if ((this.state == null) ? (other.state != null) : !this.state.equals(other.state)) {
            return false;
        }
        if (this.resource != other.resource && (this.resource == null || !this.resource.equals(other.resource))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.state != null ? this.state.hashCode() : 0);
        hash = 67 * hash + (this.resource != null ? this.resource.hashCode() : 0);
        return hash;
    }
}
