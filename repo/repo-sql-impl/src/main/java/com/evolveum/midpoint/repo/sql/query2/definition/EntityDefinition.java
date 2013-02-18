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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class EntityDefinition extends Definition {

    /**
     * child definitions of this entity
     */
    private List<Definition> definitions;
    private boolean embedded;

    public EntityDefinition(QName jaxbName, QName jaxbType, String jpaName, Class jpaType) {
        super(jaxbName, jaxbType, jpaName, jpaType);
    }

    public boolean isEmbedded() {
        return embedded;
    }

    void setEmbedded(boolean embedded) {
        embedded = embedded;
    }

    public List<Definition> getDefinitions() {
        if (definitions == null) {
            definitions = new ArrayList<Definition>();
        }
        return definitions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityDefinition{");
        builder.append("jpaName=").append(getJpaName());
        builder.append(", jaxbName=").append(getJaxbName());
        builder.append(", embedded=").append(embedded);
        builder.append(", definitions=[\n");

        List<Definition> definitions = getDefinitions();
        for (Definition definition : definitions) {
            builder.append("   ").append(definition);
            if (definitions.indexOf(definition) != definitions.size() - 1) {
                builder.append('\n');
            }
        }
        builder.append("]}");

        return builder.toString();
    }
}
