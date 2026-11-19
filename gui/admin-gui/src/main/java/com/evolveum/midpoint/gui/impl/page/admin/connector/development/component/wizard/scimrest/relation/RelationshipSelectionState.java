/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;

/**
 * State of selecting one of the detected relationships.
 */
public class RelationshipSelectionState implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Relationships recommended for selection.
     */
    private final List<RelationshipDto> recommendedRelationships;

    /**
     * Other relationships available for selection.
     */
    private final List<RelationshipDto> otherRelationships;

    /**
     * Selected relationship, null when nothing is selected.
     */
    private RelationshipDto selectedRelationship;

    /**
     * Object class the other relationships are filtered by, null for all object classes.
     */
    private String objectClassFilter;

    /**
     * Text the names of the other relationships are filtered by.
     */
    private String nameFilter;

    public RelationshipSelectionState(List<RelationshipDto> recommendedRelationships, List<RelationshipDto> otherRelationships) {
        this.recommendedRelationships = recommendedRelationships;
        this.otherRelationships = otherRelationships;
    }

    public List<RelationshipDto> getRecommendedRelationships() {
        return recommendedRelationships;
    }

    public List<RelationshipDto> getOtherRelationships() {
        return otherRelationships;
    }

    public RelationshipDto getSelectedRelationship() {
        return selectedRelationship;
    }

    public void setSelectedRelationship(RelationshipDto selectedRelationship) {
        this.selectedRelationship = selectedRelationship;
    }

    public String getObjectClassFilter() {
        return objectClassFilter;
    }

    public void setObjectClassFilter(String objectClassFilter) {
        this.objectClassFilter = objectClassFilter;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    /**
     * Returns the object class names the other relationships can be filtered by.
     *
     * @return sorted object class names without duplicates
     */
    public List<String> getObjectClassFilterOptions() {
        return otherRelationships.stream()
                .flatMap(relationship -> Stream.of(
                        relationship.getSubject().getObjectClass().getName(),
                        relationship.getObject().getObjectClass().getName()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Returns the other relationships matching the object class filter and the name filter.
     *
     * @return filtered relationships
     */
    public List<RelationshipDto> getFilteredOtherRelationships() {
        return otherRelationships.stream()
                .filter(this::matchesObjectClassFilter)
                .filter(this::matchesNameFilter)
                .toList();
    }

    private boolean matchesObjectClassFilter(RelationshipDto relationship) {
        return objectClassFilter == null
                || objectClassFilter.equals(relationship.getSubject().getObjectClass().getName())
                || objectClassFilter.equals(relationship.getObject().getObjectClass().getName());
    }

    private boolean matchesNameFilter(RelationshipDto relationship) {
        return Strings.CI.contains(relationship.getDisplayName(), Objects.requireNonNullElse(nameFilter, ""));
    }
}
