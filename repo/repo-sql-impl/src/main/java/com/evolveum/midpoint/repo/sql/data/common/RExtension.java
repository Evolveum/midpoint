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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.Identifiable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;

/**
 * @author lazyman
 */
@Entity
@Table(name = "extension")
public class RExtension implements Identifiable {
    
    private long id;
    private List<String> objects;

    //todo what is this?
    @Id
    @GeneratedValue
    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    //todo what mapping here? are we sure we want List<String> in here?
    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "extension_object", joinColumns =
            {@JoinColumn(name = "extensionId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public List<String> getObjects() {
        return objects;
    }

    public void setObjects(List<String> objects) {
        this.objects = objects;
    }

    public static void copyToJAXB(RExtension repo, Extension jaxb) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        //todo if it's XExtension also copy identifier (id)

//        jaxb.setExtension(repo.getExtension());
    }

    public static void copyFromJAXB(Extension jaxb, RExtension repo) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        //todo if it's XExtension also copy identifier (id)

//        repo.setExtension(jaxb.getExtension());
    }
}
