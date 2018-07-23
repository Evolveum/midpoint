/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;

import javax.xml.namespace.QName;

/**
 * Built-in (hardcoded) relations.
 * 
 * Created by honchar.
 */
public enum RelationTypes {

	MEMBER(SchemaConstants.ORG_DEFAULT, "", AreaCategoryType.ADMINISTRATION, AreaCategoryType.ORGANIZATION, AreaCategoryType.SELF_SERVICE),
    MANAGER(SchemaConstants.ORG_MANAGER, "Manager", AreaCategoryType.ADMINISTRATION, AreaCategoryType.ORGANIZATION, AreaCategoryType.SELF_SERVICE),
    META(SchemaConstants.ORG_META, "Meta", AreaCategoryType.POLICY),
    DEPUTY(SchemaConstants.ORG_DEPUTY, "Deputy" /* no values */),
    APPROVER(SchemaConstants.ORG_APPROVER, "Approver", AreaCategoryType.ADMINISTRATION, AreaCategoryType.GOVERNANCE, AreaCategoryType.SELF_SERVICE),
    OWNER(SchemaConstants.ORG_OWNER, "Owner", AreaCategoryType.ADMINISTRATION, AreaCategoryType.GOVERNANCE, AreaCategoryType.SELF_SERVICE),
    CONSENT(SchemaConstants.ORG_CONSENT, "Consent", AreaCategoryType.DATA_PROTECTION),;


    private final QName relation;
    private final String headerLabel;
    private final AreaCategoryType[] categories;

    RelationTypes(QName relation, String headerLabel, AreaCategoryType... categories) {
        this.relation = relation;
        this.headerLabel = headerLabel;
        this.categories = categories;
    }

    public QName getRelation() {
        return relation;
    }

    public String getHeaderLabel() {
        return headerLabel;
    }
    
    public String getLabelKey() {
    	return RelationTypes.class.getSimpleName() + "." + getHeaderLabel();
    }
    
    public AreaCategoryType[] getCategories() {
		return categories;
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
