/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    MEMBER(SchemaConstants.ORG_DEFAULT, "", "", "", "", RelationKindType.MEMBER, null, ADMINISTRATION, ORGANIZATION, SELF_SERVICE),
    MANAGER(SchemaConstants.ORG_MANAGER, "Manager", "bg-info text-light", "fe fe-manager-tie-object", "darkblue", RelationKindType.MANAGER, singletonList(RelationKindType.MEMBER), ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    META(SchemaConstants.ORG_META, "Meta", "", "", "", RelationKindType.META, null, POLICY),
    DEPUTY(SchemaConstants.ORG_DEPUTY, "Deputy", "", "", "", RelationKindType.DELEGATION, null /* no values */),
    APPROVER(SchemaConstants.ORG_APPROVER, "Approver", "bg-success text-light", "fe fe-approver-object", "green", RelationKindType.APPROVER, null, ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    OWNER(SchemaConstants.ORG_OWNER, "Owner", "bg-warning text-dark", "fe fe-crown-object", "darkorange", RelationKindType.OWNER, null, ADMINISTRATION, GOVERNANCE, ORGANIZATION, SELF_SERVICE),
    CONSENT(SchemaConstants.ORG_CONSENT, "Consent", "", "", "", RelationKindType.CONSENT, null, DATA_PROTECTION),
    RELATED(SchemaConstants.ORG_RELATED, "Related", "", "", "", RelationKindType.RELATED, null);

    private final QName relation;
    private final String headerLabel;
    private final String defaultCssClass;
    private final RelationKindType defaultFor;
    @NotNull private final Collection<RelationKindType> kinds;
    private final AreaCategoryType[] categories;
    private final String defaultIconStyle;
    private final String defaultIconColor;

    RelationTypes(QName relation, String headerLabel, String defaultCssClass, String defaultIconStyle, String defaultIconColor, RelationKindType defaultFor, Collection<RelationKindType> additionalKinds, AreaCategoryType... categories) {
        this.relation = relation;
        this.headerLabel = headerLabel;
        this.defaultCssClass = defaultCssClass;
        this.defaultIconStyle = defaultIconStyle;
        this.defaultIconColor = defaultIconColor;
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

    public String getDefaultCssClass() {
        return defaultCssClass;
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

    public String getDefaultIconColor() {
        return defaultIconColor;
    }

    public static RelationTypes getRelationTypeByRelationValue(QName relation){
        for (RelationTypes relationType : values()) {
            if (QNameUtil.match(relationType.getRelation(), relation)) {
                return relationType;
            }
        }
        return null;
    }
}
