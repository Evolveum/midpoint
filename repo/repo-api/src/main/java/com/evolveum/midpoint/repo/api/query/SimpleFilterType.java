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

/**
 * @author lazyman
 */
public enum SimpleFilterType {

    EQ("equal"), GT("gt"), LT("lt"), GTEQ("gtEqual"), LTEQ("ltEqual"), NEQ("notEqual"),
    NULL("null"), NOT_NULL("notNull"), LIKE("like");

    private String elementName;

    private SimpleFilterType(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }
}
