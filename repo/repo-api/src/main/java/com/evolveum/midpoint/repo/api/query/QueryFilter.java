/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.query;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * Common interface for all types of query filters.
 *
 * @author lazyman
 */
public interface QueryFilter {

    /**
     * @return prism container qname type
     */
    QName getType();

    void setType(QName type);

    /**
     * This method is used for query serialization.
     *
     * @param parent
     */
    void toDOM(Element parent);
}
