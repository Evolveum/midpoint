/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import javax.xml.namespace.QName;

/**
 * Link from AnyContainer to specific item in this container.
 *
 * @author mederly
 */
public class JpaAnyItemLinkDefinition extends JpaLinkDefinition<JpaDataNodeDefinition> {

    final private RObjectExtensionType ownerType;

    JpaAnyItemLinkDefinition(QName jaxbName, String jpaName, CollectionSpecification collectionSpecification,
            RObjectExtensionType ownerType, JpaDataNodeDefinition targetDefinition) {
        super(jaxbName, jpaName, collectionSpecification, false, targetDefinition);
        this.ownerType = ownerType;
    }

    public RObjectExtensionType getOwnerType() {
        return ownerType;
    }

    public QName getItemName() {
        return ((NameItemPathSegment) getItemPathSegment()).getName();
    }
}
