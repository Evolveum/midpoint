package com.evolveum.midpoint.prism.deleg;

import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ObjectDefinitionDelegator<O extends Objectable> extends ContainerDefinitionDelegator<O>, PrismObjectDefinition<O> {

    @Override
    public PrismObjectDefinition<O> delegate();

    @Override
    default PrismContainerDefinition<?> getExtensionDefinition() {
        return delegate().getExtensionDefinition();
    }

    @Override
    default @NotNull PrismObject<O> instantiate() throws SchemaException {
        return delegate().instantiate();
    }

    @Override
    default @NotNull PrismObject<O> instantiate(QName name) throws SchemaException {
        return delegate().instantiate(name);
    }

    @Override
    default PrismObjectValue<O> createValue() {
        return delegate().createValue();
    }

    @Override
    PrismObjectDefinition<O> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition);

    @Override
    PrismObjectDefinition<O> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction);

}
