/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
