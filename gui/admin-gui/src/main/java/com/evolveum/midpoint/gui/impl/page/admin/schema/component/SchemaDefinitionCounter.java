/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

public class SchemaDefinitionCounter extends SimpleCounter<AssignmentHolderDetailsModel<SchemaType>, SchemaType> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaDefinitionCounter.class);

    public SchemaDefinitionCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SchemaType> objectDetailsModels, PageBase pageBase) {
        PrismObjectWrapper<SchemaType> schemaWrapper = objectDetailsModels.getObjectWrapperModel().getObject();
        int count = 0;
        try {
            PrismContainerWrapper<PrismSchemaType> prismSchema = schemaWrapper.findContainer(WebPrismUtil.PRISM_SCHEMA);
            PrismSchemaType prismSchemaBean = prismSchema.getValue().getRealValue();
            count = prismSchemaBean.getComplexType().size();
            count += prismSchemaBean.getEnumerationType().size();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find container for schema definition in " + schemaWrapper, e);
        }
        return count;
    }
}
