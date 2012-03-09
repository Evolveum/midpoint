/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.a0;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/6/12
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ExtensionValue implements Serializable {

    private String objectOid;
    private Long objectId;

    private Assignment object;

    @MapsId("object")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "object_owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "object_id", referencedColumnName = "id")
    })
    public Assignment getObject() {
        return object;
    }

    @Id
    @Column(name = "object_id")
    public Long getObjectId() {
        if (objectId == null && object != null) {
            objectId = object.getId();
        }
        return objectId;
    }

    @Id
    @Column(name = "object_owner_oid", length = 36, nullable = false, insertable = true, updatable = false)
    public String getObjectOid() {
        if (objectOid == null && object != null) {
            objectOid = object.getOwnerOid();
        }
        return objectOid;
    }

    public void setObject(Assignment object) {
        this.object = object;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public void setObjectOid(String objectOid) {
        this.objectOid = objectOid;
    }

    private String value;

    @Id
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExtensionValue that = (ExtensionValue) o;

        if (objectId != null ? !objectId.equals(that.objectId) : that.objectId != null) return false;
        if (objectOid != null ? !objectOid.equals(that.objectOid) : that.objectOid != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = objectOid != null ? objectOid.hashCode() : 0;
        result = 31 * result + (objectId != null ? objectId.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
