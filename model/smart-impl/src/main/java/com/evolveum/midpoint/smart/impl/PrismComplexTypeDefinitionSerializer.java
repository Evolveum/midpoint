package com.evolveum.midpoint.smart.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Serializes {@link PrismObjectDefinition} into {@link SiObjectSchemaType}. */
class PrismComplexTypeDefinitionSerializer extends SchemaSerializer {

    private final ComplexTypeDefinition complexTypeDefinition;
    private final PathSet ignoredPaths;
    private final DescriptiveItemPath prefix;
    private final Set<QName> typesSeen;

    private PrismComplexTypeDefinitionSerializer(
            ComplexTypeDefinition complexTypeDefinition, PathSet ignoredPaths, DescriptiveItemPath prefix, Set<QName> typesSeen) {
        this.complexTypeDefinition = complexTypeDefinition;
        this.ignoredPaths = ignoredPaths;
        this.prefix = prefix;
        this.typesSeen = typesSeen;
    }

    private PrismComplexTypeDefinitionSerializer(
            ComplexTypeDefinition complexTypeDefinition, PathSet ignoredPaths, DescriptiveItemPath prefix, Set<QName> typesSeen, Map<String, ItemPath> descriptiveToItemPath) {
        this(complexTypeDefinition, ignoredPaths, prefix, typesSeen);
        this.descriptiveToItemPath = descriptiveToItemPath;
    }

    static SiObjectSchemaType serialize(PrismObjectDefinition<?> objectDef) {
        return create(objectDef).serialize();
    }

    SiObjectSchemaType serialize() {
        var schema = createSchema();
        addItemDefinitions(schema);
        return schema;
    }

    static @NotNull PrismComplexTypeDefinitionSerializer create(PrismObjectDefinition<?> objectDef) {
        return new PrismComplexTypeDefinitionSerializer(
                objectDef.getComplexTypeDefinition(),
                PathSet.of(
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
                DescriptiveItemPath.empty(),
                Set.of());
    }

    private SiObjectSchemaType createSchema() {
        return new SiObjectSchemaType()
                .name(this.complexTypeDefinition.getTypeName())
                .description(this.complexTypeDefinition.getDocumentation());
    }

    private void addItemDefinitions(SiObjectSchemaType schema) {
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
            var itemPath = prefix.append(itemName, itemDef.isMultiValue());
            var pathString = itemPath.asString();
            registerPathMapping(pathString, itemPath.getItemPath());
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(pathString)
                            .type(fixTypeName(itemDef.getTypeName()))
                            .description(itemDef.getDocumentation())
                            .minOccurs(itemDef.getMinOccurs())
                            .maxOccurs(itemDef.getMaxOccurs()));
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
                            newTypesSeen,
                            this.descriptiveToItemPath)
                            .addItemDefinitions(schema);
                }
            }
        }
    }
}
