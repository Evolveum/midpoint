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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.ForeignKey;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Entity
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = Account.DDL_TABLE_ACCOUNT,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")})
@NamedQueries({
    @NamedQuery(name = Account.QUERY_ACCOUNT_FIND_ALL, query = "SELECT m FROM Account m"),
    @NamedQuery(name = Account.QUERY_ACCOUNT_FIND_BY_NAME, query = "SELECT m FROM Account m WHERE m.name = ?0")
})
public class Account extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_ACCOUNT = "Accounts";
    // queries
    public final static String QUERY_ACCOUNT_FIND_BY_NAME = "Account.findByUname";
    public final static String QUERY_ACCOUNT_FIND_ALL = "Account.findAll";
    private Resource resource;
    private User user;
    private Map<String, String> attributes = new HashMap<String, String>(0);

    //TODO: fix made during integration testing, before release 1.5
    //Well, that's not entirelly correct place for objectClass property... but OK for now.
    private String objectClass;

    @Column(table = Account.DDL_TABLE_ACCOUNT, name = "object_class", unique = false, nullable = false, length = 256)
    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    @Column(table = Account.DDL_TABLE_ACCOUNT, name = "name", unique = false, nullable = false, length = 250)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(table = Account.DDL_TABLE_ACCOUNT, name = "resource_uuid", nullable = false, updatable = false)
    @ForeignKey(name = "none") 
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(table = Account.DDL_TABLE_ACCOUNT, name = "user_uuid")
    @ForeignKey(name = "none") 
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @JoinTable(name = "AccountAttributes",
    joinColumns = @JoinColumn(name = "uuid"))
    @Column(name = "attrvalue", nullable = false)
    @org.hibernate.annotations.MapKey(columns = {
        @Column(name = "attrname")
    })
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(
            Map<String, String> resourceAttributes) {
        this.attributes = resourceAttributes;
    }

    @Override
    public String toString() {
        return "Account [" + "attributes " + attributes + " " + "resource " + resource + " " + "user " + user + " " + "name " + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Account other = (Account) obj;
        if (this.resource != other.resource && (this.resource == null || !this.resource.equals(other.resource))) {
            return false;
        }
        if (this.user != other.user && (this.user == null || !this.user.equals(other.user))) {
            return false;
        }
        if (this.attributes != other.attributes && (this.attributes == null || !this.attributes.equals(other.attributes))) {
            return false;
        }
        if ((this.objectClass == null) ? (other.objectClass != null) : !this.objectClass.equals(other.objectClass)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.resource != null ? this.resource.hashCode() : 0);
        hash = 29 * hash + (this.user != null ? this.user.hashCode() : 0);
        hash = 29 * hash + (this.attributes != null ? this.attributes.hashCode() : 0);
        hash = 29 * hash + (this.objectClass != null ? this.objectClass.hashCode() : 0);
        return hash;
    }

}
