/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import com.evolveum.midpoint.repo.common.activity.run.*;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

public final class ImportActivityRun
        extends AbstractImportActivityRun<ImportWorkDefinition, ImportActivityHandler> {

    ImportActivityRun(ActivityRunInstantiationContext<ImportWorkDefinition, ImportActivityHandler> context) {
        super(context, "Import");
    }

    @Override
    protected QName getSourceChannel() {
        return SchemaConstants.CHANNEL_IMPORT;
    }

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_IMPORT_URI;
    }
}
