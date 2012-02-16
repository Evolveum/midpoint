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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "ext_complex")
public class RComplexValue extends RValue {

    private boolean dom;
    private String value;

    @Type(type = "org.hibernate.type.TextType")
    public String getValue() {
        return value;
    }

    public boolean isDom() {
        return dom;
    }

    public void setDom(boolean dom) {
        this.dom = dom;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Object toObject() throws DtoTranslationException {
        if (StringUtils.isEmpty(getValue())) {
            return null;
        }

        return DOMUtil.parseDocument(getValue()).getDocumentElement();
    }

    @Override
    public void insertValueFromElement(Element element) throws SchemaException {
        Validate.notNull(element, "Element must not be null.");
        setValue(DOMUtil.printDom(element).toString());
    }
}
