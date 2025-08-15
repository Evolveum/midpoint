package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ItemDefinition.ItemDefinitionMutator;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.schema.processor.MutableRawResourceAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface PartiallyMutableItemDefinition<I extends Item<?,?>> extends ItemDefinition<I>, ItemDefinitionMutator {

    @Override
    default void setOptionalCleanup(boolean optionalCleanup) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

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
    default void setDocumentation(String value) {
        throw new IllegalStateException("Item Definition is not modifiable");

    }

    @Override
    default void addSchemaMigration(SchemaMigration schemaMigration) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setSchemaMigrations(List<SchemaMigration> value) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setDiagrams(List<ItemDiagramSpecification> value) {
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
    default void setIndexed(Boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    @Override
    default void setInherited(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    default void setSearchable(boolean value) {
        throw new IllegalStateException("Item Definition is not modifiable");
    }

    interface Container<C extends Containerable> extends PartiallyMutableItemDefinition<PrismContainer<C>>, PrismContainerDefinition.PrismContainerDefinitionMutator<C> {

        @Override
        default void setCompileTimeClass(Class<C> compileTimeClass) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default PrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default PrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default PrismPropertyDefinition<?> createPropertyDefinition(String localName, QName propType) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default PrismContainerDefinition<?> createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default PrismContainerDefinition<?> createContainerDefinition(@NotNull QName name, @NotNull ComplexTypeDefinition ctd, int minOccurs, int maxOccurs) {
            throw new IllegalStateException("Item Definition is not modifiable");

        }

        @Override
        default void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }

    interface Reference extends PrismReferenceDefinition.PrismReferenceDefinitionMutator, PartiallyMutableItemDefinition<PrismReference> {

        @Override
        default void setTargetTypeName(QName typeName) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setTargetObjectDefinition(PrismObjectDefinition<?> definition) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setComposite(boolean value) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }

    interface Property<T> extends PrismPropertyDefinition.PrismPropertyDefinitionMutator<T>, PartiallyMutableItemDefinition<PrismProperty<T>> {

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

        @Override
        default void setDefaultValue(T value) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }

        @Override
        default void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
            PartiallyMutableItemDefinition.super.setValueEnumerationRef(valueEnumerationRef);
        }

        @Override
        @NotNull Property<T> clone();
    }

    interface Attribute<T> extends MutableRawResourceAttributeDefinition<T>, Property<T> {

        // FIXME not sure why these have to be here
        @Override
        default void setCanRead(boolean val) {
            Property.super.setCanRead(val);
        }

        @Override
        default void setCanModify(boolean val) {
            Property.super.setCanModify(val);
        }

        @Override
        default void setCanAdd(boolean val) {
            Property.super.setCanAdd(val);
        }

        @Override
        default void setMinOccurs(int value) {
            Property.super.setMinOccurs(value);
        }

        @Override
        default void setMaxOccurs(int value) {
            Property.super.setMaxOccurs(value);
        }

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

        @Override
        default void setNativeDescription(String nativeDescription) {
            throw new IllegalStateException("Item Definition is not modifiable");
        }
    }
}
