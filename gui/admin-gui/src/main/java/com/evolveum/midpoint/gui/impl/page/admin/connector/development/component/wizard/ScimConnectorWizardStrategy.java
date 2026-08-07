/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.BaseUrlConnectorStepPanel;
import com.evolveum.midpoint.prism.path.ItemName;

/**
 * SCIM is a specialization of REST for wizard purposes (same connection step list; only the
 * base-URL config field differs) — mirrors {@code ScimBackend extends RestBackend}.
 */
public class ScimConnectorWizardStrategy extends RestConnectorWizardStrategy {

    @Override
    public ItemName connectionUrlFieldName() {
        return BaseUrlConnectorStepPanel.SCIM_BASE_URL_ITEM_NAME;
    }
}
