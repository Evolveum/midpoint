/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.schema;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.util.PrismSchemaTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;

/**
 * @author katka
 */
@Component
public class ResourceSchemaWrapperFactory
        extends SchemaDefinitionWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaWrapperFactory.class);

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }
        return parent != null && QNameUtil.match(parent.getTypeName(), XmlSchemaType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 200;
    }

    @Override
    public PrismContainerWrapper<PrismSchemaType> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> schemaDef, WrapperContext context) throws SchemaException {
        PrismContainerWrapper<PrismSchemaType> wrapper = super.createWrapper(parent, schemaDef, context);
        setReadOnlyRecursively(wrapper);
        wrapper.setReadOnly(true);
        return wrapper;
    }

    /**
     * Lifecycle state of the owner (e.g. resource) describes the lifecycle of the object itself,
     * not of the schema definitions
     */
    @Override
    protected PrismSchemaType createPrismSchemaBean(
            PrismContainerValueWrapper<?> parent, PrismProperty<SchemaDefinitionType> schema) throws SchemaException {
        return PrismSchemaTypeUtil.convertToPrismSchemaType(schema.getRealValue());
    }
}
