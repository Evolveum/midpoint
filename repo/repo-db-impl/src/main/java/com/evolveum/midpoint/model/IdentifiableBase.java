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

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
 * An abstract implementation of {@link Identifiable}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class IdentifiableBase implements Identifiable, Serializable {

    public static final String code_id = "$Id$";
    /**
     * The ID.
     */
    private UUID oid;    

    /**
     * {@inheritDoc}
     */
    @Id
    @GenericGenerator(name = "IdGenerator", strategy = "com.evolveum.midpoint.model.UUIDGenerator")
    @GeneratedValue(generator = "IdGenerator")
    @Type(type = "com.evolveum.midpoint.hibernate.usertype.UUIDType")
    @Columns(columns = {@Column(name = "uuid", length = 36)})
    @Override
    public UUID getOid() {
        return oid;
    }

    /**
     * {@inheritDoc}
     */
    public void setOid(final UUID oid) {
        this.oid = oid;
    }

    @Override
    public String toString() {
        return "IdentifiableBase [" + "oid " + oid + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IdentifiableBase other = (IdentifiableBase) obj;
        if (this.oid != other.oid && (this.oid == null || !this.oid.equals(other.oid))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.oid != null ? this.oid.hashCode() : 0);
        return hash;
    }

}
