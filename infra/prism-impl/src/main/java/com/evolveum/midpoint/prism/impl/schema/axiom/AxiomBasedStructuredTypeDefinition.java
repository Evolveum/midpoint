package com.evolveum.midpoint.prism.impl.schema.axiom;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SchemaMigration;
import com.evolveum.midpoint.prism.SmartVisitation;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;

public class AxiomBasedStructuredTypeDefinition implements ComplexTypeDefinition {

    @Override
    public @Nullable Class<?> getCompileTimeClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable QName getSuperType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @NotNull Collection<TypeDefinition> getStaticSubTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getInstantiationOrder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public @NotNull QName getTypeName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRuntimeSchema() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIgnored() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ItemProcessing getProcessing() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAbstract() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDeprecated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isExperimental() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getPlannedRemoval() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isElaborate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getDeprecatedSince() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmphasized() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getDisplayOrder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDocumentation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDocumentationPreview() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getTypeClassIfKnown() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getTypeClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrismContext getPrismContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String debugDump(int indent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isImmutable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void freeze() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        // TODO Auto-generated method stub

    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @NotNull List<? extends ItemDefinition> getDefinitions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isShared() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public @Nullable QName getExtensionForType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReferenceMarker() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isContainerMarker() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isObjectMarker() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isXsdAnyMarker() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isListMarker() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public @Nullable String getDefaultNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @NotNull List<String> getIgnoredNamespaces() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        // TODO Auto-generated method stub

    }

    @Override
    public void revive(PrismContext prismContext) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public @NotNull ComplexTypeDefinition clone() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @NotNull ComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        // TODO Auto-generated method stub

    }

    @Override
    public MutableComplexTypeDefinition toMutable() {
        // TODO Auto-generated method stub
        return null;
    }

}
