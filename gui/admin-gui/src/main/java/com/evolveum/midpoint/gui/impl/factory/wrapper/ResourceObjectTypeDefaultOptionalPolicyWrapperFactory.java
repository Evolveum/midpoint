package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceObjectTypeArchetypeValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceObjectTypeMarkPolicyValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class ResourceObjectTypeDefaultOptionalPolicyWrapperFactory<R extends Referencable> extends PrismReferenceWrapperFactory<R>{

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (!def.getItemName().equivalent(ResourceObjectTypeDefinitionType.F_DEFAULT_OPERATION_POLICY_REF)) {
            return false;
        }

        if (!parent.getPath().namedSegmentsOnly().equivalent(
                ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE))) {
            return false;
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected PrismReferenceWrapper<R> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status, WrapperContext ctx) {
        PrismReferenceWrapper<R> wrapper = super.createWrapperInternal(parent, item, status, ctx);
        ObjectFilter filter = PrismContext.get().queryFor(MarkType.class)
                .item(MarkType.F_ARCHETYPE_REF)
                .ref(SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value())
                .buildFilter();
        wrapper.setFilter(filter);
        return wrapper;
    }

    @Override
    public PrismReferenceValueWrapperImpl<R> createValueWrapper(PrismReferenceWrapper<R> parent, PrismReferenceValue value, ValueStatus status, WrapperContext context) {
        return new ResourceObjectTypeMarkPolicyValueWrapperImpl<>(parent, value, status);
    }
}
