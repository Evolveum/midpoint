/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

/**
 * Represents possible sources of the test object for the GET operation.
 */
public enum SearchByIdTestObjectSource {

    /**
     * Object picked from the results of the search operation.
     */
    SEARCH_RESULTS,

    /**
     * Object found by the primary identifier typed by the user.
     */
    MANUAL_IDENTIFIER
}
