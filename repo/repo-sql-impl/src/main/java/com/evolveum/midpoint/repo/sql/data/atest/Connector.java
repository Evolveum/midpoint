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

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Connector extends O {

    private String someName;
    private ObjectReference connectorHost;

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ObjectReference getConnectorHost() {
        return connectorHost;
    }

    public String getSomeName() {
        return someName;
    }

    public void setConnectorHost(ObjectReference connectorHost) {
        this.connectorHost = connectorHost;
    }

    public void setSomeName(String someName) {
        this.someName = someName;
    }

    @Override
    public Collection<IdentifiableContainer> getContainers(QName name) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
}
