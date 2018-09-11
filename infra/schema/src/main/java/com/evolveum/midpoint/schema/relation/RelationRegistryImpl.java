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

package com.evolveum.midpoint.schema.relation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author mederly
 * @author semancik
 */
@Component
public class RelationRegistryImpl implements RelationRegistry {

	private static final Trace LOGGER = TraceManager.getTrace(RelationRegistryImpl.class);

	@Autowired private PrismContext prismContext;

	@NotNull
	private IndexedRelationDefinitions indexedRelationDefinitions = createAndIndexRelationDefinitions(null);

	@Override
	public void applyRelationConfiguration(SystemConfigurationType systemConfiguration) {
		RoleManagementConfigurationType roleManagement = systemConfiguration != null ? systemConfiguration.getRoleManagement() : null;
		RelationsDefinitionType relationsDef = roleManagement != null ? roleManagement.getRelations() : null;
		LOGGER.info("Applying relation configuration ({} entries)", relationsDef != null ? relationsDef.getRelation().size() : 0);
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
			addDefaultRelations(relations);
		}
		return new IndexedRelationDefinitions(relations);
	}

	private RelationDefinitionType cloneAndNormalize(RelationDefinitionType definition) {
		RelationDefinitionType clone = definition.clone();
		if (clone.getDefaultFor() != null && !clone.getKind().contains(clone.getDefaultFor())) {
			clone.getKind().add(clone.getDefaultFor());
		}
		return clone;
	}

	private void addDefaultRelations(List<RelationDefinitionType> relations) {
		for (RelationTypes defaultRelationDefinition : RelationTypes.values()) {
			if (ObjectTypeUtil.findRelationDefinition(relations, defaultRelationDefinition.getRelation()) == null) {
				relations.add(createRelationDefinitionFromStaticDefinition(defaultRelationDefinition));
			}
		}
	}

	@NotNull
	static RelationDefinitionType createRelationDefinitionFromStaticDefinition(RelationTypes defaultRelationDefinition) {
		RelationDefinitionType relationDef = new RelationDefinitionType();
		relationDef.setRef(defaultRelationDefinition.getRelation());
		DisplayType display = new DisplayType();
		display.setLabel(defaultRelationDefinition.getLabelKey());
		relationDef.setDisplay(display);
		relationDef.setDefaultFor(defaultRelationDefinition.getDefaultFor());
		relationDef.getKind().addAll(defaultRelationDefinition.getKinds());
		relationDef.getCategory().addAll(Arrays.asList(defaultRelationDefinition.getCategories()));
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
	public boolean processRelationOnLogin(QName relation) {
		return indexedRelationDefinitions.processRelationOnLogin(relation);
	}

	@Override
	public boolean processRelationOnRecompute(QName relation) {
		return indexedRelationDefinitions.processRelationOnRecompute(relation);
	}

	@Override
	public boolean includeIntoParentOrgRef(QName relation) {
		return indexedRelationDefinitions.includeIntoParentOrgRef(relation);
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
