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

package com.evolveum.midpoint.repo.sql.data.atest;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/7/12
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class ResourceShadow extends O {

    private Set<ExtensionValue> attributes;

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ExtensionValue> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<ExtensionValue> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Collection<IdentifiableContainer> getContainers(QName name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
