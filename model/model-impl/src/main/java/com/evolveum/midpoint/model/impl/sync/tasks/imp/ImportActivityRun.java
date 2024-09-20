/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
