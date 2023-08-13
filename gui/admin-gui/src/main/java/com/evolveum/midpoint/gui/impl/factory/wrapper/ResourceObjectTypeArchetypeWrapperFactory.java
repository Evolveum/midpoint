package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class ResourceObjectTypeArchetypeWrapperFactory<R extends Referencable> extends PrismReferenceWrapperFactory<R>{

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeArchetypeWrapperFactory.class);

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (!def.getItemName().equivalent(ResourceObjectFocusSpecificationType.F_ARCHETYPE_REF)) {
            return false;
        }

        if (!parent.getPath().namedSegmentsOnly().equivalent(
                ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_FOCUS))) {
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
        wrapper.setFilter((BiFunction<PrismReferenceWrapper, PageBase, ObjectFilter> & Serializable) (referenceWrapper, pageBase) -> {
            PrismContainerValueWrapper resourceFocusValue = referenceWrapper.getParent();
            if (resourceFocusValue != null) {
                try {
                    PrismPropertyWrapper focusType = resourceFocusValue.findProperty(
                            ResourceObjectFocusSpecificationType.F_TYPE);
                    if (focusType != null) {
                        PrismValueWrapper focusTypeValue = focusType.getValue();
                        if (focusTypeValue != null) {
                            QName focusTypeBean = (QName) focusTypeValue.getRealValue();
                            if (focusTypeBean != null) {
                                Class<? extends AssignmentHolderType> holderType = WebComponentUtil.qnameToClass(
                                        PrismContext.get(), focusTypeBean, AssignmentHolderType.class);

                                if (holderType != null) {
                                    List<String> archetypeOidsList =
                                            WebComponentUtil.getArchetypeOidsListByHolderType(holderType, pageBase);

                                    if (!archetypeOidsList.isEmpty()) {
                                        return PrismContext.get().queryFor(ArchetypeType.class)
                                                .id(archetypeOidsList.toArray(new String[0]))
                                                .buildFilter();
                                    }
                                }
                            }
                        }
                    }
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't find type in " + resourceFocusValue);
                }
            }
            return null;
        });
        return wrapper;
    }
}
