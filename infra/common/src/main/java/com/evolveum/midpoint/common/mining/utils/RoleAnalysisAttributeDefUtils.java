package com.evolveum.midpoint.common.mining.utils;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.xml.namespace.QName;
import java.util.*;

public class RoleAnalysisAttributeDefUtils {

    public static final ItemName F_NAME = new ItemName(ObjectFactory.NAMESPACE, "name");
    public static final ItemName F_TITLE = new ItemName(ObjectFactory.NAMESPACE, "title");
    public static final ItemName F_ORGANIZATION = new ItemName(ObjectFactory.NAMESPACE, "organization");
    public static final ItemName F_ORGANIZATIONAL_UNIT = new ItemName(ObjectFactory.NAMESPACE, "organizationalUnit");
    public static final ItemName F_LOCALE = new ItemName(ObjectFactory.NAMESPACE, "locale");
    public static final ItemName F_LOCALITY = new ItemName(ObjectFactory.NAMESPACE, "locality");
    public static final ItemName F_COST_CENTER = new ItemName(ObjectFactory.NAMESPACE, "costCenter");
    public static final ItemName F_LIFECYCLE_STATE = new ItemName(ObjectFactory.NAMESPACE, "lifecycleState");
    public static final ItemName F_RISK_LEVEL = new ItemName(ObjectFactory.NAMESPACE, "riskLevel");

    public static RoleAnalysisAttributeDef orgAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "org assignment", OrgType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef roleAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "role assignment", RoleType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef serviceAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "service assignment", ServiceType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef archetypeAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "archetype assignment", ArchetypeType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef resourceAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "resource assignment", ResourceType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef orgInducement = getRoleAnalysisItemDefAssignment(
            OrgType.F_INDUCEMENT, "org inducement", OrgType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef roleInducement = getRoleAnalysisItemDefAssignment(
            RoleType.F_INDUCEMENT, "role inducement", RoleType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef serviceInducement = getRoleAnalysisItemDefAssignment(
            ServiceType.F_INDUCEMENT, "service inducement", ServiceType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef archetypeInducement = getRoleAnalysisItemDefAssignment(
            ArchetypeType.F_INDUCEMENT, "archetype inducement", ArchetypeType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef archetypeRef = new RoleAnalysisAttributeDef(
            ItemPath.create(FocusType.F_ARCHETYPE_REF),
            false,
            "archetypeRef") {
        @Override
        public String resolveSingleValueItem(PrismObject<?> prismObject, ItemPath itemPath) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);

            if (property == null) {
                return null;
            }

            Object realValue = property.getRealValue();
            if (realValue instanceof ObjectReferenceType objectReference) {
                return objectReference.getOid();
            }
            return null;
        }
    };

    public static RoleAnalysisAttributeDef title = new RoleAnalysisAttributeDef(
            F_TITLE,
            false,
            "title");

    public static RoleAnalysisAttributeDef locale = new RoleAnalysisAttributeDef(
            F_LOCALE,
            false,
            "locale");

    public static RoleAnalysisAttributeDef locality = new RoleAnalysisAttributeDef(
            F_LOCALITY,
            false,
            "locality");

    public static RoleAnalysisAttributeDef costCenter = new RoleAnalysisAttributeDef(
            F_COST_CENTER,
            false,
            "costCenter");

    public static RoleAnalysisAttributeDef lifecycleState = new RoleAnalysisAttributeDef(
            F_LIFECYCLE_STATE,
            false,
            "lifecycleState");

    public static RoleAnalysisAttributeDef riskLevel = new RoleAnalysisAttributeDef(
            F_RISK_LEVEL,
            false,
            "riskLevel");

    @Contract(pure = true)
    public static @Unmodifiable @NotNull List<RoleAnalysisAttributeDef> getAttributesForRoleAnalysis() {

        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
                lifecycleState,
                locale,
                locality,
                costCenter,
                riskLevel,
//                archetypeAssignment,
                orgAssignment,
                resourceAssignment,
                roleAssignment,
                serviceAssignment,
                orgInducement,
                roleInducement,
                serviceInducement,
//                archetypeInducement,
                archetypeRef
        ));
        analysisAttributeDefs.addAll(loadRoleExtension());

        return analysisAttributeDefs;
    }

    @Contract(pure = true)
    public static @Unmodifiable @NotNull List<RoleAnalysisAttributeDef> getAttributesForUserAnalysis() {
        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
                title,
                locale,
                locality,
                costCenter,
                lifecycleState,
//                archetypeAssignment,
                orgAssignment,
                resourceAssignment,
                roleAssignment,
                serviceAssignment,
                archetypeRef
        ));

        analysisAttributeDefs.addAll(loadUserExtension());
        return analysisAttributeDefs;
    }

    @NotNull
    private static RoleAnalysisAttributeDef getRoleAnalysisItemDefAssignment(ItemName itemName, String displayValue, QName targetType) {
        return new RoleAnalysisAttributeDef(
                ItemPath.create(itemName, AssignmentType.F_TARGET_REF),
                true,
                displayValue) {
            @Override
            public @NotNull Set<String> resolveMultiValueItem(
                    @NotNull PrismObject<?> prismObject,
                    @NotNull ItemPath itemPath) {
                return resolveAssignment(prismObject, itemPath, targetType);
            }
        };
    }

    @NotNull
    private static Set<String> resolveAssignment(
            @NotNull PrismObject<?> prismObject,
            @NotNull ItemPath itemPath,
            @NotNull QName targetType) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            Object realValue = item.getRealValue();
            if (realValue instanceof ObjectReferenceType objectReference) {
                QName refTargetType = objectReference.getType();
                if (refTargetType.equals(targetType)) {
                    resolvedValues.add(objectReference.getOid());
                }
            }
        }

        return resolvedValues;
    }

    private static @NotNull List<RoleAnalysisAttributeDef> loadRoleExtension() {
        List<RoleAnalysisAttributeDef> attributes = new ArrayList<>();
        PrismContainerDefinition<?> itemDefinitionByFullPath;
        try {
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry().findItemDefinitionByFullPath(RoleType.class, PrismContainerDefinition.class, UserType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismPropertyDefinition<?> containerDefinition) {
                //TODO resolve if string polyString targetRef complexObject etc...
                Class<?> typeClass = containerDefinition.getTypeClass();
                ItemPath itemName = containerDefinition.getItemName();

                RoleAnalysisAttributeDef attribute = new RoleAnalysisAttributeDef(
                        ItemPath.create(RoleType.F_EXTENSION, itemName),
                        false, itemName + " extension");
                attributes.add(attribute);

            }
        }
        return attributes;
    }

    private static @NotNull List<RoleAnalysisAttributeDef> loadUserExtension() {

        List<RoleAnalysisAttributeDef> attributes = new ArrayList<>();
        PrismContainerDefinition<?> itemDefinitionByFullPath;
        try {
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry().findItemDefinitionByFullPath(UserType.class, PrismContainerDefinition.class, UserType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismPropertyDefinition<?> containerDefinition) {
                //TODO resolve if string polyString targetRef complexObject etc...
                Class<?> typeClass = containerDefinition.getTypeClass();
                ItemPath itemName = containerDefinition.getItemName();

                RoleAnalysisAttributeDef attribute = new RoleAnalysisAttributeDef(
                        ItemPath.create(UserType.F_EXTENSION, itemName),
                        false,
                        itemName + " extension");
                attributes.add(attribute);

            }
        }
        return attributes;
    }

    private static boolean isPolyStringOrString(@NotNull ItemDefinition<?> def) {
        return def.getTypeName().getLocalPart().equals(PolyStringType.COMPLEX_TYPE.getLocalPart())
                || def.getTypeName().getLocalPart().equals("string")
                || def.getTypeName().getLocalPart().equals("PolyString");
    }
}
