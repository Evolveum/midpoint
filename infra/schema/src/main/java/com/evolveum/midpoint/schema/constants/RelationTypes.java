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

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType.*;
import static java.util.Collections.singletonList;

/**
 * Built-in (hardcoded) relations.
 * 
 * Created by honchar.
 */
public enum RelationTypes {

	MEMBER(SchemaConstants.ORG_DEFAULT, "", "fe fe-action-assign", RelationKindType.MEMBER, null, ADMINISTRATION, ORGANIZATION, SELF_SERVICE),
    MANAGER(SchemaConstants.ORG_MANAGER, "Manager", "fe fe-manager-tie-object", RelationKindType.MANAGER, singletonList(RelationKindType.MEMBER), ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    META(SchemaConstants.ORG_META, "Meta", "", RelationKindType.META, null, POLICY),
    DEPUTY(SchemaConstants.ORG_DEPUTY, "Deputy", "", RelationKindType.DELEGATION, null /* no values */),
    APPROVER(SchemaConstants.ORG_APPROVER, "Approver", "fe fe-approver-object", RelationKindType.APPROVER, null, ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    OWNER(SchemaConstants.ORG_OWNER, "Owner", "fe fe-crown-object", RelationKindType.OWNER, null, ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    CONSENT(SchemaConstants.ORG_CONSENT, "Consent", "", RelationKindType.CONSENT, null, DATA_PROTECTION);

    private final QName relation;
    private final String headerLabel;
    private final RelationKindType defaultFor;
    @NotNull private final Collection<RelationKindType> kinds;
    private final AreaCategoryType[] categories;
    private final String defaultIconStyle;

    RelationTypes(QName relation, String headerLabel, String defaultIconStyle, RelationKindType defaultFor, Collection<RelationKindType> additionalKinds, AreaCategoryType... categories) {
        this.relation = relation;
        this.headerLabel = headerLabel;
        this.defaultIconStyle = defaultIconStyle;
        this.kinds = new ArrayList<>();
        if (defaultFor != null) {
            kinds.add(defaultFor);
        }
        if (additionalKinds != null) {
            kinds.addAll(additionalKinds);
        }
        this.defaultFor = defaultFor;
        this.categories = categories;
    }

    public QName getRelation() {
        return relation;
    }

    public String getHeaderLabel() {
        return headerLabel;
    }
    
    public String getLabelKey() {
    	return RelationTypes.class.getSimpleName() + "." + relation.getLocalPart();
    }

    public RelationKindType getDefaultFor() {
        return defaultFor;
    }

    @NotNull
    public Collection<RelationKindType> getKinds() {
        return kinds;
    }

    public AreaCategoryType[] getCategories() {
		return categories;
	}

    public String getDefaultIconStyle() {
        return defaultIconStyle;
    }

    public static RelationTypes getRelationTypeByRelationValue(QName relation){
        for (RelationTypes relationType : values()) {
            if (relationType.getRelation().equals(relation)) {
                return relationType;
            }
        }
        return null;
    }
}
