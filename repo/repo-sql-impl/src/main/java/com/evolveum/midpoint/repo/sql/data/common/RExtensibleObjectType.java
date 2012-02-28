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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import org.hibernate.annotations.Cascade;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "extensible_object")
public abstract class RExtensibleObjectType extends RObjectType {

    private RExtension extension;

    @ManyToOne(optional = true)
//    @JoinTable(name = "extensible_object_extension", joinColumns = @JoinColumn(name = "extensibleOid"),
//            inverseJoinColumns = @JoinColumn(name = "extensionId"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RExtension getExtension() {
        return extension;
    }

    public void setExtension(RExtension extension) {
        this.extension = extension;
    }

    public static void copyToJAXB(RExtensibleObjectType repo, ExtensibleObjectType jaxb,
            PrismContext prismContext) throws DtoTranslationException {
        RObjectType.copyToJAXB(repo, jaxb, prismContext);

        if (repo.getExtension() != null) {
            Extension extension = new Extension();
            jaxb.setExtension(extension);

            RExtension.copyToJAXB(repo.getExtension(), extension, prismContext);
        }
    }

    public static void copyFromJAXB(ExtensibleObjectType jaxb, RExtensibleObjectType repo,
            PrismContext prismContext) throws DtoTranslationException {
        RObjectType.copyFromJAXB(jaxb, repo, prismContext);

        if (jaxb.getExtension() != null) {
            RExtension extension = new RExtension();
            repo.setExtension(extension);

            RExtension.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
        }
    }
}
