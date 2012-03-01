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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@Table(name = "object_reference")
public class RObjectReferenceType implements Serializable {

    private String owner;
    private long containerId;
    private String description;
    private String target;
    private String filter;
    private QName type;

    @Id
    @Column(length = 32, nullable = false)
    public String getOwner() {
        return owner;
    }

    @Id
    public long getContainerId() {
        return containerId;
    }

    @Id
    @Column(length = 32)
    public String getTarget() {
        return target;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getFilter() {
        return filter;
    }

    @Columns(columns = {
            @Column(name = "namespaceURI"),
            @Column(name = "localPart")
    })
    public QName getType() {
        return type;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContainerId(long containerId) {
        this.containerId = containerId;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public static void copyToJAXB(RObjectReferenceType repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        String filter = repo.getFilter();
        if (StringUtils.isNotEmpty(filter)) {
            Element element = DOMUtil.parseDocument(filter).getDocumentElement();
            jaxb.setFilter(element);
        }
        jaxb.setType(repo.getType());
        jaxb.setOid(repo.getTarget());
        jaxb.setDescription(repo.getDescription());
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RObjectReferenceType repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setTarget(jaxb.getOid());
        repo.setType(jaxb.getType());

        if (jaxb.getFilter() != null) {
            repo.setFilter(DOMUtil.printDom(jaxb.getFilter()).toString());
        }
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
