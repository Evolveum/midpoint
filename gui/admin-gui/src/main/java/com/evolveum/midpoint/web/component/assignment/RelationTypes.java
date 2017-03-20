/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;

/**
 * Created by honchar.
 */
public enum RelationTypes {

    MEMBER(null, ""),
    MANAGER(SchemaConstants.ORG_MANAGER, "Manager"),
    OWNER(SchemaConstants.ORG_OWNER, "Owner"),
    APPROVER(SchemaConstants.ORG_APPROVER, "Approver");


    private QName relation;
    private String headerLabel;

    RelationTypes(QName relation, String headerLabel) {
        this.relation = relation;
        this.headerLabel = headerLabel;
    }

    public QName getRelation() {
        return relation;
    }

    public String getHeaderLabel() {
        return headerLabel;
    }

    public static RelationTypes getRelationType(QName relation) {
        if (ObjectTypeUtil.isDefaultRelation(relation)) {
            return RelationTypes.MEMBER;
        }
        for (RelationTypes relationTypes : RelationTypes.values()) {
            if (QNameUtil.match(relation, relationTypes.getRelation())) {
                return relationTypes;
            }
        }
        return RelationTypes.MEMBER;
    }
}
