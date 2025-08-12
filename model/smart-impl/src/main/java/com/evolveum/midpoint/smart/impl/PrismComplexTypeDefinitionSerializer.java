package com.evolveum.midpoint.smart.impl;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/** Serializes {@link PrismObjectDefinition} into {@link SiObjectSchemaType}. */
class PrismComplexTypeDefinitionSerializer extends SchemaSerializer {

    private final ComplexTypeDefinition complexTypeDefinition;
    private final PathSet ignoredPaths;
    private final ItemPath prefix;
    private final Set<QName> typesSeen;

    private PrismComplexTypeDefinitionSerializer(
            ComplexTypeDefinition complexTypeDefinition, PathSet ignoredPaths, ItemPath prefix, Set<QName> typesSeen) {
        this.complexTypeDefinition = complexTypeDefinition;
        this.ignoredPaths = ignoredPaths;
        this.prefix = prefix;
        this.typesSeen = typesSeen;
    }

    static SiObjectSchemaType serialize(PrismObjectDefinition<?> objectDef) {
        var serializer = new PrismComplexTypeDefinitionSerializer(
                objectDef.getComplexTypeDefinition(),
                PathSet.of(
                        FocusType.F_EXTENSION, // FIXME temporary hack, to avoid serializing custom namespaces (this breaks the simplistic JSON format used)
                        FocusType.F_ASSIGNMENT,
                        FocusType.F_IDENTITIES,
                        FocusType.F_LENS_CONTEXT,
                        FocusType.F_POLICY_EXCEPTION,
                        FocusType.F_POLICY_STATEMENT,
                        FocusType.F_DIAGNOSTIC_INFORMATION,
                        FocusType.F_INDESTRUCTIBLE,
                        FocusType.F_EFFECTIVE_MARK_REF,
                        FocusType.F_EFFECTIVE_OPERATION_POLICY,
                        FocusType.F_LINK_REF,
                        FocusType.F_PERSONA_REF,
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_PASSWORD).append(PasswordType.F_NAME),
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_PASSWORD).append(PasswordType.F_FORCE_CHANGE),
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_PASSWORD).append(PasswordType.F_HINT),
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_NONCE),
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_SECURITY_QUESTIONS),
                        FocusType.F_CREDENTIALS.append(CredentialsType.F_ATTRIBUTE_VERIFICATION),
                        UserType.F_ADMIN_GUI_CONFIGURATION),
                ItemPath.EMPTY_PATH,
                Set.of());
        var schema = serializer.createSchema();
        serializer.addItemDefinitions(schema);
        return schema;
    }

    private SiObjectSchemaType createSchema() {
        return new SiObjectSchemaType()
                .name(this.complexTypeDefinition.getTypeName())
                .description(this.complexTypeDefinition.getDocumentation());
    }

    public void addItemDefinitions(SiObjectSchemaType schema) {
        for (var itemDef : complexTypeDefinition.getDefinitions()) {
            if (itemDef.isOperational() || itemDef.isDeprecated()) {
                // Operational items should not take part in mappings
                // Deprecated items should not be suggested for mapping
                continue;
            }
            var itemName = itemDef.getItemName();
            if (ignoredPaths.contains(itemName)) {
                continue;
            }
            var itemPath = prefix.append(itemName);
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(new ItemPathType(itemPath))
                            .type(fixTypeName(itemDef.getTypeName()))
                            .description(itemDef.getDocumentation()));
            if (itemDef instanceof PrismContainerDefinition<?> pcd) {
                ComplexTypeDefinition ctd = pcd.getComplexTypeDefinition();
                if (typesSeen.contains(ctd.getTypeName())) {
                    System.out.println("Ignoring CTD seen: " + ctd.getTypeName() + " @ " + itemPath);
                } else {
                    var newTypesSeen = new HashSet<>(typesSeen);
                    newTypesSeen.add(ctd.getTypeName());
                    new PrismComplexTypeDefinitionSerializer(
                            ctd,
                            ignoredPaths.remainder(itemName),
                            itemPath,
                            newTypesSeen)
                            .addItemDefinitions(schema);
                }
            }
        }
    }
}
