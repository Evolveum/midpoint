/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.metadata.MetadataMappingBuilder;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Radovan Semancik
 *
 */
public class MappingFactory {

    /**
     * Beans commonly needed for model-common module. We use these to avoid massive
     * copying of various beans. The current situation is that there is always a single
     * implementation for any given bean. In case of exceptions we can introduce specific
     * beans fields here.
     *
     * (Note that when constructing the factory in model-common tests we need not fill-in
     * all the beans. Only selected ones are necessary for basic mapping functionality.)
     */
    private ModelCommonBeans beans;

    private boolean profiling = false;

    public ExpressionFactory getExpressionFactory() {
        return beans.expressionFactory;
    }

    public ObjectResolver getObjectResolver() {
        return beans.objectResolver;
    }

    public void setBeans(ModelCommonBeans beans) {
        this.beans = beans;
    }

    public boolean isProfiling() {
        return profiling;
    }

    public void setProfiling(boolean profiling) {
        this.profiling = profiling;
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> MappingBuilder<V, D> createMappingBuilder() {
        return initializeMappingBuilder(new MappingBuilder<>());
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> MetadataMappingBuilder<V, D> createMetadataMappingBuilder() {
        return initializeMappingBuilder(new MetadataMappingBuilder<>());
    }

    private <AMB extends AbstractMappingBuilder<?, ?, ?, AMB>> AMB initializeMappingBuilder(AMB abstractMappingBuilder) {
        return abstractMappingBuilder
                .beans(beans)
                .profiling(profiling);
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> MappingBuilder<V, D> createMappingBuilder(
            @NotNull ConfigurationItem<MappingType> mappingCI, String shortDesc) {
        return createMappingBuilder(mappingCI.value(), mappingCI.origin(), shortDesc);
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> MappingBuilder<V, D> createMappingBuilder(
            @Nullable MappingType mappingBean, @NotNull ConfigurationItemOrigin context, String shortDesc) {
        return this.<V,D>createMappingBuilder()
                .mappingBean(mappingBean, context)
                .contextDescription(shortDesc);
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> MetadataMappingBuilder<V, D> createMappingBuilder(
            @Nullable MetadataMappingType mappingBean, @NotNull ConfigurationItemOrigin context, String shortDesc) {
        return this.<V,D>createMetadataMappingBuilder()
                .mappingBean(mappingBean, context)
                .contextDescription(shortDesc);
    }
}
