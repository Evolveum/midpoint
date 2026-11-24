/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.util.Comparator;

/**
 * Orders relationships by how much of them is configured in the connector, fully configured first.
 */
public class RelationshipConfigurationComparator implements Comparator<RelationshipDto> {

    @Override
    public int compare(RelationshipDto relationship1, RelationshipDto relationship2) {
        return Comparator.comparing(RelationshipDto::hasConfiguredObjectClasses, Comparator.reverseOrder())
                .thenComparing(RelationshipDto::hasConfiguredAttributes, Comparator.reverseOrder())
                .compare(relationship1, relationship2);
    }
}
