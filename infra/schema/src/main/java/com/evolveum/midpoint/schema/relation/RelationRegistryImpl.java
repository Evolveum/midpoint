/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.relation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

import static java.util.Collections.emptyList;

/**
 * @author semancik
 */
@Component
public class RelationRegistryImpl implements RelationRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(RelationRegistryImpl.class);

    @Autowired private PrismContext prismContext;

    @NotNull
    private IndexedRelationDefinitions indexedRelationDefinitions = createAndIndexRelationDefinitions(null);

    @Override
    public void applyRelationsConfiguration(SystemConfigurationType systemConfiguration) {
        RoleManagementConfigurationType roleManagement = systemConfiguration != null ? systemConfiguration.getRoleManagement() : null;
        RelationsDefinitionType relationsDef = roleManagement != null ? roleManagement.getRelations() : null;
        LOGGER.debug("Applying relation configuration ({} entries)", relationsDef != null ? relationsDef.getRelation().size() : 0);
        indexedRelationDefinitions = createAndIndexRelationDefinitions(relationsDef);
        prismContext.setDefaultRelation(indexedRelationDefinitions.getDefaultRelationFor(RelationKindType.MEMBER));
    }

    public List<RelationDefinitionType> getRelationDefinitions() {
        return indexedRelationDefinitions.getDefinitions();
    }

    @NotNull
    private IndexedRelationDefinitions createAndIndexRelationDefinitions(RelationsDefinitionType relationsDef) {
        List<RelationDefinitionType> configuredRelations = emptyList();
        boolean includeDefaultRelations = true;
        if (relationsDef != null) {
            configuredRelations = relationsDef.getRelation();
            if (BooleanUtils.isFalse(relationsDef.isIncludeDefaultRelations())) {
                includeDefaultRelations = false;
            }
        }
        List<RelationDefinitionType> relations = new ArrayList<>();
        for (RelationDefinitionType configuredRelation: configuredRelations) {
            relations.add(cloneAndNormalize(configuredRelation));
        }
        if (includeDefaultRelations) {
            addStaticallyDefinedRelations(relations);
        }
        return new IndexedRelationDefinitions(relations);
    }

    private RelationDefinitionType cloneAndNormalize(RelationDefinitionType definition) {
        RelationDefinitionType clone = definition.clone();
        if (clone.getDefaultFor() != null && !clone.getKind().contains(clone.getDefaultFor())) {
            clone.getKind().add(clone.getDefaultFor());
        }
        clone.setStaticallyDefined(false);
        return clone;
    }

    private void addStaticallyDefinedRelations(List<RelationDefinitionType> relations) {
        for (RelationTypes staticRelationDefinition : RelationTypes.values()) {
            if (ObjectTypeUtil.findRelationDefinition(relations, staticRelationDefinition.getRelation()) == null) {
                relations.add(createRelationDefinitionFromStaticDefinition(staticRelationDefinition));
            }
        }
    }

    @NotNull
    static RelationDefinitionType createRelationDefinitionFromStaticDefinition(RelationTypes defaultRelationDefinition) {
        RelationDefinitionType relationDef = new RelationDefinitionType();
        relationDef.setRef(defaultRelationDefinition.getRelation());
        DisplayType display = new DisplayType();
        display.setCssClass(defaultRelationDefinition.getDefaultCssClass());
        PolyStringType label = new PolyStringType(QNameUtil.getLocalPart(defaultRelationDefinition.getRelation()));
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey(defaultRelationDefinition.getLabelKey());
        label.setTranslation(translation);
        display.setLabel(label);
        if (StringUtils.isNotEmpty(defaultRelationDefinition.getDefaultIconStyle())){
            IconType icon = new IconType();
            icon.setCssClass(defaultRelationDefinition.getDefaultIconStyle());
            if (StringUtils.isNotEmpty(defaultRelationDefinition.getDefaultIconColor())){
                icon.setColor(defaultRelationDefinition.getDefaultIconColor());
            }
            display.setIcon(icon);
        }
        relationDef.setDisplay(display);
        relationDef.setDefaultFor(defaultRelationDefinition.getDefaultFor());
        relationDef.getKind().addAll(defaultRelationDefinition.getKinds());
        relationDef.getCategory().addAll(Arrays.asList(defaultRelationDefinition.getCategories()));
        relationDef.setStaticallyDefined(true);
        return relationDef;
    }

    //region =============================================================================================== query methods

    @Override
    public RelationDefinitionType getRelationDefinition(QName relation) {
        return indexedRelationDefinitions.getRelationDefinition(relation);
    }

    @Override
    public boolean isOfKind(QName relation, RelationKindType kind) {
        return indexedRelationDefinitions.isOfKind(relation, kind);
    }

    @Override
    public boolean isProcessedOnLogin(QName relation) {
        return indexedRelationDefinitions.isProcessedOnLogin(relation);
    }

    @Override
    public boolean isProcessedOnRecompute(QName relation) {
        return indexedRelationDefinitions.isProcessedOnRecompute(relation);
    }

    @Override
    public boolean isStoredIntoParentOrgRef(QName relation) {
        return indexedRelationDefinitions.isStoredIntoParentOrgRef(relation);
    }

    @Override
    public boolean isAutomaticallyMatched(QName relation) {
        return indexedRelationDefinitions.isAutomaticallyMatched(relation);
    }

    @Override
    public QName getDefaultRelationFor(RelationKindType kind) {
        return indexedRelationDefinitions.getDefaultRelationFor(kind);
    }

    @NotNull
    @Override
    public Collection<QName> getAllRelationsFor(RelationKindType kind) {
        return indexedRelationDefinitions.getAllRelationsFor(kind);
    }

    @Override
    public QName getDefaultRelation() {
        return getDefaultRelationFor(RelationKindType.MEMBER);
    }

    @NotNull
    @Override
    public QName normalizeRelation(QName relation) {
        return indexedRelationDefinitions.normalizeRelation(relation);
    }

    @Override
    public boolean isDefault(QName relation) {
        return prismContext.isDefaultRelation(relation);
    }

    @Override
    @NotNull
    public Collection<QName> getAliases(QName relation) {
        return indexedRelationDefinitions.getAliases(relation);
    }

    //endregion
}
