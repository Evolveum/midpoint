package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConnectorDevelopmentTypeResourceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceObjectTypeMarkPolicyValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class ConnectorDevelopmentTypeResourceWrapperFactory<R extends Referencable> extends PrismReferenceWrapperFactory<R>{

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (parent == null) {
            return false;
        }

        if (!def.getItemName().equivalent(ConnDevTestingType.F_TESTING_RESOURCE)) {
            return false;
        }

        if (!parent.getPath().namedSegmentsOnly().equivalent(ConnectorDevelopmentType.F_TESTING)) {
            return false;
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public PrismReferenceValueWrapperImpl<R> createValueWrapper(PrismReferenceWrapper<R> parent, PrismReferenceValue value, ValueStatus status, WrapperContext context) {
        return new ConnectorDevelopmentTypeResourceValueWrapperImpl<>(parent, value, status);
    }
}
