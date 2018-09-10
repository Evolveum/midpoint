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

import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import javax.xml.namespace.QName;
import java.util.*;

import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * Relation definitions indexed for fast querying by the relation registry.
 * It is intentionally package-private. Should be used only for RelationRegistryImpl.
 *
 * @author mederly
 */
class IndexedRelationDefinitions {

	private static final Trace LOGGER = TraceManager.getTrace(IndexedRelationDefinitions.class);

	/**
	 * All valid definitions are here; duplicates and invalid ones are filtered out.
	 */
	@NotNull private final List<RelationDefinitionType> definitions;
	/**
	 * Looking up relation definition by its name.
	 *
	 * All variants of relation name are present in keySet, e.g. both 'org:approver' and 'approver' (in default configuration)
	 * There's also an entry with key=null, denoting the default relation.
	 */
	@NotNull private final Map<QName, RelationDefinitionType> relationDefinitionsByRelationName;
	/**
	 * Looking up relation kinds by its name. It is returned as HashSet to allow for quick determination of presence of specified kinds.
	 *
	 * Both qualified and unqualified forms of relation name are listed here, e.g. both 'org:approver' and 'approver'.
	 * Also an entry with key=null is present.
	 */
	@NotNull private final SetValuedMap<QName, RelationKindType> kindsByRelationName;
	/**
	 * Looking up relations by its kind.
	 *
	 * Each relation is listed only once in values; e.g. there's 'org:approver' but not 'approver' (in default configuration)
	 */
	@NotNull private final SetValuedMap<RelationKindType, QName> relationsByKind;
	/**
	 * Default relation for each kind.
	 * There should be exactly one for each kind.
	 */
	@NotNull private final Map<RelationKindType, QName> defaultRelationByKind;
	/**
	 * Relations to be processed on login. Each relation is listed here under all its names.
	 */
	@NotNull private final Set<QName> relationsToProcessOnLogin;
	/**
	 * Relations to be processed on recompute. Each relation is listed here under all its names.
	 */
	@NotNull private final Set<QName> relationsToProcessOnRecompute;
	/**
	 * Aliases for each normalized relation QName.
	 */
	@NotNull private final SetValuedMap<QName, QName> aliases;

	//region Initialization

	IndexedRelationDefinitions(@NotNull List<RelationDefinitionType> definitions) {
		List<RelationDefinitionType> validatedDefinitions = validateDefinitions(definitions);
		this.definitions = validatedDefinitions;
		relationDefinitionsByRelationName = initializeRelationDefinitionsByRelationName(validatedDefinitions);
		kindsByRelationName = computeKindsByRelationName();
		relationsByKind = computeRelationsByKind();
		defaultRelationByKind = computeDefaultRelationByKind();

		addDefaultRelationToMaps();

		relationsToProcessOnLogin = computeRelationsToProcessOnLogin();
		relationsToProcessOnRecompute = computeRelationsToProcessOnRecompute();

		aliases = computeAliases();
		logState();
	}

	private void addDefaultRelationToMaps() {
		QName defaultRelation = defaultRelationByKind.get(RelationKindType.MEMBERSHIP);
		if (defaultRelation != null) {
			relationDefinitionsByRelationName.put(null, relationDefinitionsByRelationName.get(defaultRelation));
			kindsByRelationName.putAll(null, kindsByRelationName.get(defaultRelation));
		}
	}

	private void logState() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("relation definitions = {}", definitions);
			LOGGER.trace("relationDefinitionsByRelationName = {}", relationDefinitionsByRelationName);
			LOGGER.trace("relationsByKind = {}", relationsByKind);
			LOGGER.trace("defaultRelationByKind = {}", defaultRelationByKind);
			LOGGER.trace("relationsToProcessOnLogin = {}", relationsToProcessOnLogin);
			LOGGER.trace("relationsToProcessOnRecompute = {}", relationsToProcessOnRecompute);
			LOGGER.trace("aliases = {}", aliases);
		}
	}

	private List<RelationDefinitionType> validateDefinitions(@NotNull List<RelationDefinitionType> definitions) {
		List<RelationDefinitionType> validatedDefinitions = new ArrayList<>(definitions.size());
		boolean membershipRelationExists = false;
		for (RelationDefinitionType definition : definitions) {
			if (definition.getRef() == null) {
				LOGGER.error("Relation definition with null ref; ignoring: {}", definition);
			} else {
				if (QNameUtil.isUnqualified(definition.getRef())) {
					LOGGER.warn("Unqualified relation name '{}'; please fix it as soon as possible; in {}", definition.getRef(), definition);
				}
				validatedDefinitions.add(definition);
				if (!membershipRelationExists && definition.getKind().contains(RelationKindType.MEMBERSHIP)) {
					membershipRelationExists = true;
				}
			}
		}
		if (!membershipRelationExists) {
			LOGGER.error("No 'member' relation was defined. This would be a fatal condition, so we define one.");
			validatedDefinitions.add(RelationRegistryImpl.createRelationDefinitionFromStaticDefinition(RelationTypes.MEMBER));
		}
		return validatedDefinitions;
	}

	/**
	 * Removes duplicate definitions as well.
	 */
	@NotNull
	private Map<QName, RelationDefinitionType> initializeRelationDefinitionsByRelationName(List<RelationDefinitionType> definitions) {
		Map<QName, RelationDefinitionType> map = new HashMap<>();
		ListValuedMap<String, QName> expansions = new ArrayListValuedHashMap<>();
		for (Iterator<RelationDefinitionType> iterator = definitions.iterator(); iterator.hasNext(); ) {
			RelationDefinitionType definition = iterator.next();
			if (map.containsKey(definition.getRef())) {
				LOGGER.error("Duplicate relation definition for '{}'; ignoring: {}", definition.getRef(), definition);
				iterator.remove();
			} else {
				map.put(definition.getRef(), definition);
				expansions.put(definition.getRef().getLocalPart(), definition.getRef());
			}
		}
		// add entries for unqualified versions of the relation names
		for (String unqualified : expansions.keySet()) {
			List<QName> names = expansions.get(unqualified);
			if (names.contains(new QName(unqualified))) {
				continue;       // cannot expand unqualified if the expanded value is also unqualified
			}
			assert !names.isEmpty();
			assert names.stream().allMatch(QNameUtil::isQualified);
			@NotNull QName chosenExpansion;
			if (names.size() == 1) {
				chosenExpansion = names.get(0);
			} else {
				QName nameInOrgNamespace = names.stream()
						.filter(n -> SchemaConstants.NS_ORG.equals(n.getNamespaceURI()))
						.findFirst().orElse(null);
				if (nameInOrgNamespace != null) {
					// org:xxx expansion will be the default one
					chosenExpansion = nameInOrgNamespace;
				} else {
					chosenExpansion = names.get(0);
					LOGGER.warn("Multiple resolutions of unqualified relation name '{}' ({}); "
							+ "using the first one as default: '{}'. Please reconsider this as it could lead to "
							+ "unpredictable behavior.", unqualified, names, chosenExpansion);
				}
			}
			assert QNameUtil.isQualified(chosenExpansion);
			map.put(new QName(unqualified), map.get(chosenExpansion));
		}
		return map;
	}

	private SetValuedMap<QName, RelationKindType> computeKindsByRelationName() {
		SetValuedMap<QName, RelationKindType> rv = new HashSetValuedHashMap<>();
		for (Map.Entry<QName, RelationDefinitionType> entry : relationDefinitionsByRelationName.entrySet()) {
			for (RelationKindType kind : entry.getValue().getKind()) {
				rv.put(entry.getKey(), kind);
			}
		}
		return rv;
	}

	private SetValuedMap<RelationKindType, QName> computeRelationsByKind() {
		SetValuedMap<RelationKindType, QName> rv = new HashSetValuedHashMap<>();
		for (RelationDefinitionType definition : definitions) {
			for (RelationKindType kind : definition.getKind()) {
				rv.put(kind, definition.getRef());
			}
		}
		return rv;
	}

	// not optimized for speed
	@NotNull
	private Collection<QName> getAllRelationNamesFor(RelationKindType kind) {
		Set<QName> rv = new HashSet<>();
		for (Map.Entry<QName, RelationDefinitionType> entry : relationDefinitionsByRelationName.entrySet()) {
			if (entry.getValue().getKind().contains(kind)) {
				rv.add(entry.getKey());
			}
		}
		return rv;
	}

	private Map<RelationKindType, QName> computeDefaultRelationByKind() {
		Map<RelationKindType, QName> rv = new HashMap<>();
		for (RelationKindType kind : RelationKindType.values()) {
			Set<QName> relationNames = relationsByKind.get(kind);
			if (relationNames.isEmpty()) {
				LOGGER.warn("No relations defined for kind {}, please define at least one", kind);
				continue;
			}
			Set<QName> defaultRelationNames = new HashSet<>();
			for (QName relationName : relationNames) {
				RelationDefinitionType definition = relationDefinitionsByRelationName.get(relationName);
				assert definition != null;
				if (definition.getDefaultFor() == kind) {
					defaultRelationNames.add(relationName);
				}
			}
			QName chosen;
			if (defaultRelationNames.size() > 1) {
				chosen = defaultRelationNames.iterator().next();
				LOGGER.error("More than one default relation set up for kind '{}': {}. Please choose one! Temporarily selecting '{}'",
						kind, defaultRelationNames, chosen);
			} else if (defaultRelationNames.size() == 1) {
				chosen = defaultRelationNames.iterator().next();
			} else {
				chosen = relationNames.iterator().next();       // maybe we could select org:xxx here but let's not bother now
				LOGGER.warn("No default relation set up for kind '{}'. Please choose one! Temporarily selecting '{}'", kind, chosen);
			}
			rv.put(kind, chosen);
		}
		return rv;
	}

	// We want to make this configurable in the future MID-3581
	private Set<QName> computeRelationsToProcessOnLogin() {
		HashSet<QName> rv = new HashSet<>();
		rv.addAll(getAllRelationNamesFor(RelationKindType.MEMBERSHIP));
		rv.addAll(getAllRelationNamesFor(RelationKindType.DELEGATION));
		return rv;
	}

	// We want to make this configurable in the future MID-3581
	private Set<QName> computeRelationsToProcessOnRecompute() {
		HashSet<QName> rv = new HashSet<>();
		rv.addAll(getAllRelationsFor(RelationKindType.MEMBERSHIP));
		rv.addAll(getAllRelationsFor(RelationKindType.MANAGER));
		rv.addAll(getAllRelationsFor(RelationKindType.DELEGATION));
		return rv;
	}

	private SetValuedMap<QName, QName> computeAliases() {
		SetValuedMap<QName, QName> rv = new HashSetValuedHashMap<>();
		for (Map.Entry<QName, RelationDefinitionType> entry : relationDefinitionsByRelationName.entrySet()) {
			rv.put(entry.getValue().getRef(), entry.getKey());
		}
		return rv;
	}

	//endregion

	//region Querying
	@NotNull
	List<RelationDefinitionType> getDefinitions() {
		return definitions;
	}

	RelationDefinitionType getRelationDefinition(QName relation) {
		return relationDefinitionsByRelationName.get(relation);
	}

	boolean isOfKind(QName relation, RelationKindType kind) {
		Set<RelationKindType> relationKinds = kindsByRelationName.get(relation);
		return relationKinds != null && relationKinds.contains(kind);
	}

	boolean processRelationOnLogin(QName relation) {
		return relationsToProcessOnLogin.contains(relation);
	}

	boolean processRelationOnRecompute(QName relation) {
		return relationsToProcessOnRecompute.contains(relation);
	}

	// We want to make this configurable in the future MID-3581
	public boolean includeIntoParentOrgRef(QName relation) {
		return isOfKind(relation, RelationKindType.MEMBERSHIP) && !isOfKind(relation, RelationKindType.META);
	}

	QName getDefaultRelationFor(RelationKindType kind) {
		return defaultRelationByKind.get(kind);
	}

	@NotNull
	Collection<QName> getAllRelationsFor(RelationKindType kind) {
		return emptyIfNull(relationsByKind.get(kind));
	}

	@NotNull
	QName normalizeRelation(QName relation) {
		RelationDefinitionType definition = getRelationDefinition(relation);
		assert !(relation == null && definition == null);       // there is always a default definition
		return definition != null ? definition.getRef() : relation;
	}

	@NotNull
	Collection<QName> getAliases(QName relation) {
		Set<QName> aliases = this.aliases.get(normalizeRelation(relation));
		if (!CollectionUtils.isEmpty(aliases)) {
			return aliases;
		} else {
			// for unknown relations we would like to return at least the provided name
			return singleton(relation);
		}
	}
	//endregion
}
