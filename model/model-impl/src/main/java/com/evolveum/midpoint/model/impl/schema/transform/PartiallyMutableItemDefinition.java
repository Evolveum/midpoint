package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.MutablePrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.SchemaMigration;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.schema.processor.MutableRawResourceAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;

import java.util.Collection;

public interface PartiallyMutableItemDefinition<I extends Item<?,?>> extends MutableItemDefinition<I> {

    @Override
    default void setProcessing(ItemProcessing processing) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDeprecated(boolean deprecated) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setRemoved(boolean removed) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setRemovedSince(String removedSince) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setExperimental(boolean experimental) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setEmphasized(boolean emphasized) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDisplayName(String displayName) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDisplayOrder(Integer displayOrder) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setHelp(String help) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setRuntimeSchema(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setTypeName(QName typeName) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDocumentation(String value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void addSchemaMigration(SchemaMigration schemaMigration) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void addDiagram(ItemDiagramSpecification diagram) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setMinOccurs(int value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setMaxOccurs(int value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setCanRead(boolean val) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setCanModify(boolean val) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setCanAdd(boolean val) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setOperational(boolean operational) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDynamic(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setItemName(QName name) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setReadOnly() {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setDeprecatedSince(String value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setPlannedRemoval(String value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setElaborate(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setHeterogeneousListItem(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setSubstitutionHead(QName value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setIndexOnly(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void setInherited(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    public interface Container<C extends Containerable> extends PartiallyMutableItemDefinition<PrismContainer<C>>, MutablePrismContainerDefinition<C> {

        @Override
        default void setCompileTimeClass(Class<C> compileTimeClass) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default MutablePrismPropertyDefinition<?> createPropertyDefinition(String localName, QName propType) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default MutablePrismContainerDefinition<?> createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition ctd, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }

    public interface Reference extends MutablePrismReferenceDefinition, PartiallyMutableItemDefinition<PrismReference> {

        @Override
        default void setTargetTypeName(QName typeName) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setComposite(boolean value) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }

    public interface Property<T> extends MutablePrismPropertyDefinition<T>, PartiallyMutableItemDefinition<PrismProperty<T>> {

        @Override
        default void setIndexed(Boolean value) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setMatchingRuleQName(QName matchingRuleQName) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setInherited(boolean value) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setAllowedValues(Collection<? extends DisplayableValue<T>> allowedValues){
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setSuggestedValues(Collection<? extends DisplayableValue<T>> suggestedValues) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }

    public interface Attribute<T> extends MutableRawResourceAttributeDefinition<T>, Property<T> {

        @Override
        default void setReturnedByDefault(Boolean returnedByDefault) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setNativeAttributeName(String nativeAttributeName) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setFrameworkAttributeName(String frameworkAttributeName) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }
}
