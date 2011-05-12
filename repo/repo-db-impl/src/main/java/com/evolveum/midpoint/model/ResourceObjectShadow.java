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
@SecondaryTable(catalog = SimpleDomainObject.DDL_CATALOG, name = ResourceObjectShadow.DDL_TABLE_RESOURCEOBJECTSHADOW,
pkJoinColumns = {@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")},
uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@NamedQueries({
    @NamedQuery(name = ResourceObjectShadow.QUERY_RESOURCEOBJECTSHADOW_FIND_ALL, query = "SELECT m FROM ResourceObjectShadow m"),
    @NamedQuery(name = ResourceObjectShadow.QUERY_RESOURCEOBJECTSHADOW_FIND_BY_NAME, query = "SELECT m FROM ResourceObjectShadow m WHERE m.name = ?0")
})
public class ResourceObjectShadow extends SimpleDomainObject {

    public static final String code_id = "$Id$";
    public static final String DDL_TABLE_RESOURCEOBJECTSHADOW = "ResourceObjectShadows";
    // queries
    public final static String QUERY_RESOURCEOBJECTSHADOW_FIND_BY_NAME = "ResourceObjectShadow.findByUname";
    public final static String QUERY_RESOURCEOBJECTSHADOW_FIND_ALL = "ResourceObjectShadow.findAll";
    private Resource resource;
    private User user;
    private Map<String, String> _attributes = new HashMap<String, String>(0);
    //TODO: fix made during integration testing, before release 1.5
    //Well, that's not entirelly correct place for objectClass property... but OK for now.
    private String objectClass;

    @Column(table = ResourceObjectShadow.DDL_TABLE_RESOURCEOBJECTSHADOW, name = "object_class", unique = false, nullable = false, length = 256)
    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    @Column(table = ResourceObjectShadow.DDL_TABLE_RESOURCEOBJECTSHADOW, name = "name", unique = true, nullable = false, length = 250)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(table = ResourceObjectShadow.DDL_TABLE_RESOURCEOBJECTSHADOW, name = "resource_uuid", nullable = false, updatable = false)
    @ForeignKey(name = "none") 
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }


    @CollectionOfElements(fetch = FetchType.EAGER)
    @JoinTable(name = "ResourceObjectAttributes",
    joinColumns = @JoinColumn(name = "uuid"))
    @Column(name = "attrvalue", nullable = false)
    @org.hibernate.annotations.MapKey(columns = {
        @Column(name = "attrname")
    })
    public Map<String, String> getAttributes() {
        return this._attributes;
    }

    public void setAttributes(
            Map<String, String> resourceAttributes) {
        this._attributes = resourceAttributes;
    }
}
