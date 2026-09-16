/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public class AssociationMappingEvaluatorModelBuilder {

    private final PageBase pageBase;
    private final WrapperContext wrapperContext;

    public AssociationMappingEvaluatorModelBuilder(
            @NotNull PageBase pageBase,
            @NotNull WrapperContext wrapperContext) {
        this.pageBase = pageBase;
        this.wrapperContext = wrapperContext;
    }

    @NotNull
    public <C extends Containerable> IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> build(
            @NotNull IModel<PrismContainerValueWrapper<C>> valueWrapper,
            @NotNull ItemPath containerPath) {

        boolean inbound = ShadowAssociationDefinitionType.F_INBOUND.equivalent(containerPath);

        try {
            IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> subjectWrapperModel =
                    PrismContainerValueWrapperModel
                            .fromContainerValueWrapper(
                                    valueWrapper,
                                    ItemPath.create(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));

            PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subjectWrapper = subjectWrapperModel.getObject();

            PrismContainerValueWrapper<MappingType> mappingWrapper =
                    getOrCreateMappingWrapper(subjectWrapper, containerPath, inbound);

            return PrismContainerValueWrapperModel.fromContainerValueWrapper(
                    () -> mappingWrapper,
                    evaluatorPath(inbound));

        } catch (SchemaException e) {
            throw new RuntimeException(
                    "Cannot load " + (inbound ? "inbound" : "outbound") + " association evaluator", e);
        }
    }

    private PrismContainerValueWrapper<MappingType> getOrCreateMappingWrapper(
            @NotNull PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subjectWrapper,
            ItemPath containerPath,
            boolean inbound) throws SchemaException {

        PrismContainerWrapper<MappingType> container = subjectWrapper.findContainer(containerPath);

        return container.getValues().isEmpty()
                ? createMappingWrapper(container)
                : container.getValues().get(0);
    }

    private @NotNull PrismContainerValueWrapper<MappingType> createMappingWrapper(
            @NotNull PrismContainerWrapper<MappingType> container)
            throws SchemaException {

        PrismContainerValue<MappingType> newValue =
                container.getItem().createNewValue();

        PrismContainerValueWrapper<MappingType> wrapper =
                WebPrismUtil.createNewValueWrapper(
                        container, newValue, pageBase, wrapperContext);

        container.getValues().add(wrapper);
        return wrapper;
    }

    private @NotNull ItemPath evaluatorPath(boolean inbound) {
        return ItemPath.create(
                inbound
                        ? SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION
                        : SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION);
    }
}
