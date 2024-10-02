/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import static com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef.extractRealValue;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.*;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisAttributeDefUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisAttributeDefUtils.class);

    public static RoleAnalysisAttributeDef getObjectNameDef() {
        return name;
    }

    public static RoleAnalysisAttributeDef name = new RoleAnalysisAttributeDef(
            FocusType.F_NAME,
            false,
            "name",
            ObjectType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(ObjectType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

//    public static RoleAnalysisAttributeDef getOrgAssignment() {
//        return orgAssignment;
//    }

//    public static RoleAnalysisAttributeDef orgAssignment = getRoleAnalysisItemDefAssignment(
//            FocusType.F_ASSIGNMENT, "org assignment", OrgType.COMPLEX_TYPE);
//    public static RoleAnalysisAttributeDef roleAssignment = getRoleAnalysisItemDefAssignment(
//            FocusType.F_ASSIGNMENT, "role assignment", RoleType.COMPLEX_TYPE);
//    public static RoleAnalysisAttributeDef serviceAssignment = getRoleAnalysisItemDefAssignment(
//            FocusType.F_ASSIGNMENT, "service assignment", ServiceType.COMPLEX_TYPE);
//    public static RoleAnalysisAttributeDef archetypeAssignment = getRoleAnalysisItemDefAssignment(
//            FocusType.F_ASSIGNMENT, "archetype assignment", ArchetypeType.COMPLEX_TYPE);
//    public static RoleAnalysisAttributeDef resourceAssignment = getRoleAnalysisItemDefAssignment(
//            FocusType.F_ASSIGNMENT, "resource assignment", ResourceType.COMPLEX_TYPE);
//
//    public static RoleAnalysisAttributeDef orgInducement = getRoleAnalysisItemDefAssignment(
//            OrgType.F_INDUCEMENT, "org inducement", OrgType.COMPLEX_TYPE);
//
//    public static RoleAnalysisAttributeDef roleInducement = getRoleAnalysisItemDefAssignment(
//            RoleType.F_INDUCEMENT, "role inducement", RoleType.COMPLEX_TYPE);
//
//    public static RoleAnalysisAttributeDef serviceInducement = getRoleAnalysisItemDefAssignment(
//            ServiceType.F_INDUCEMENT, "service inducement", ServiceType.COMPLEX_TYPE);
//
//    public static RoleAnalysisAttributeDef archetypeInducement = getRoleAnalysisItemDefAssignment(
//            ArchetypeType.F_INDUCEMENT, "archetype inducement", ArchetypeType.COMPLEX_TYPE);

//    public static RoleAnalysisAttributeDef getArchetypeRef() {
//        return archetypeRef;
//    }

//    public static RoleAnalysisAttributeDef archetypeRef = new RoleAnalysisAttributeDef(
//            ItemPath.create(FocusType.F_ARCHETYPE_REF),
//            false,
//            "archetype ref",
//            ArchetypeType.class,
//            RoleAnalysisAttributeDef.IdentifierType.OID) {
//
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(FocusType.class)
//                    .item(getPath()).ref(value)
//                    .build();
//        }
//
//        @Override
//        public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
//            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
//
//            if (property == null) {
//                return null;
//            }
//
//            Object realValue = property.getRealValue();
//            if (realValue instanceof ObjectReferenceType objectReference) {
//                return objectReference.getOid();
//            }
//            return null;
//        }
//    };

//    public static RoleAnalysisAttributeDef getTitle() {
//        return title;
//    }
//
//    public static RoleAnalysisAttributeDef title = new RoleAnalysisAttributeDef(
//            UserType.F_TITLE,
//            false,
//            "title",
//            UserType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(UserType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

//    public static RoleAnalysisAttributeDef locale = new RoleAnalysisAttributeDef(
//            UserType.F_LOCALE,
//            false,
//            "locale",
//            FocusType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(FocusType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

//    public static RoleAnalysisAttributeDef getLocality() {
//        return locality;
//    }
//
//    public static RoleAnalysisAttributeDef locality = new RoleAnalysisAttributeDef(
//            UserType.F_LOCALITY,
//            false,
//            "locality",
//            FocusType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(FocusType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

//    public static RoleAnalysisAttributeDef costCenter = new RoleAnalysisAttributeDef(
//            UserType.F_COST_CENTER,
//            false,
//            "costCenter",
//            FocusType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(FocusType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

//    public static RoleAnalysisAttributeDef lifecycleState = new RoleAnalysisAttributeDef(
//            UserType.F_LIFECYCLE_STATE,
//            false,
//            "lifecycleState",
//            FocusType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(FocusType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

//    public static RoleAnalysisAttributeDef riskLevel = new RoleAnalysisAttributeDef(
//            RoleType.F_RISK_LEVEL,
//            false,
//            "riskLevel",
//            RoleType.class,
//            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
//        @Override
//        public ObjectQuery getQuery(String value) {
//            return PrismContext.get().queryFor(RoleType.class)
//                    .item(getPath()).eq(value)
//                    .build();
//        }
//    };

    public static RoleAnalysisAttributeDef getAttributeByItemPath(ItemPath itemPath, AnalysisAttributeSettingType analysisAttributeSettings) {
        List<RoleAnalysisAttributeDef> attributeMap = createAttributeList(analysisAttributeSettings);
        for (RoleAnalysisAttributeDef attribute : attributeMap) {
            if (attribute.getPath().equivalent(itemPath)) {
                return attribute;
            }
        }
        return null;
//        return attributeMap.get(displayValue);
    }

    public static List<RoleAnalysisAttributeDef> createAttributeList(AnalysisAttributeSettingType analysisAttributeSetting) {
        List<RoleAnalysisAttributeDef> attributeDefs = new ArrayList<>();

        PrismObjectDefinition<UserType> userDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
//
//        PrismContainerDefinition<AssignmentType> assignmentDefinition = userDefinition.findContainerDefinition(F_ASSIGNMENT);
//        List<AnalysisAttributeRuleType> assignmentRules = analysisAttributeSetting.getAssignmentRule();
//        for (AnalysisAttributeRuleType rule : assignmentRules) {
//            RoleAnalysisAttributeDef attributeDef = new RoleAnalysisAssignmentAttributeDef(ItemPath.create(F_ASSIGNMENT, AssignmentType.F_TARGET_REF), assignmentDefinition, rule);
//            attributeDefs.add(attributeDef);
//        }

        List<ItemPathType> analysisAttributeRule = analysisAttributeSetting.getPath();

        if (analysisAttributeRule.isEmpty()) {
            return attributeDefs;
        }

        for (ItemPathType itemPathType : analysisAttributeRule) {
            if (itemPathType == null) {
                continue;
            }
            ItemPath path = itemPathType.getItemPath();
            ItemDefinition<?> itemDefinition = userDefinition.findItemDefinition(path);
            if (itemDefinition instanceof PrismContainerDefinition<?>) {
                LOGGER.debug("Skipping {} because container items are not supported for attribute analysis.", itemDefinition);
                continue;
            }
            //TODO reference vs. property
            RoleAnalysisAttributeDef attributeDef = new RoleAnalysisAttributeDef(path, itemDefinition);
            attributeDefs.add(attributeDef);
        }
        return attributeDefs;
    }



//    public static @NotNull List<RoleAnalysisAttributeDef> getAttributesForRoleAnalysis() {
//        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
////                lifecycleState,
////                locale,
//                locality,
//                costCenter,
//                riskLevel,
//                orgAssignment,
//                resourceAssignment,
//                roleAssignment,
//                serviceAssignment,
//                orgInducement,
//                roleInducement,
//                serviceInducement,
//                archetypeRef
//        ));
//        analysisAttributeDefs.addAll(loadRoleExtension());
//
//        return Collections.unmodifiableList(analysisAttributeDefs);
//    }

//    public static @NotNull List<RoleAnalysisAttributeDef> getAttributesForUserAnalysis() {
//        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
//                title,
////                locale,
//                locality,
//                costCenter,
////                lifecycleState,
////                archetypeAssignment,
//                orgAssignment,
//                resourceAssignment,
//                roleAssignment,
//                serviceAssignment,
//                archetypeRef
//        ));
//
//        analysisAttributeDefs.addAll(loadUserExtension());
//
//        return Collections.unmodifiableList(analysisAttributeDefs);
//    }

    @NotNull
    private static RoleAnalysisAttributeDef getRoleAnalysisItemDefAssignment(
            @NotNull ItemName itemName,
            @NotNull String displayValue,
            @NotNull QName targetType) {
        return new RoleAnalysisAttributeDef(
                ItemPath.create(itemName, AssignmentType.F_TARGET_REF),
                true,
                displayValue,
                null,
                RoleAnalysisAttributeDef.IdentifierType.OID) {
            @Override
            public @NotNull Set<String> resolveMultiValueItem(
                    @NotNull PrismObject<?> prismObject,
                    @NotNull ItemPath itemPath) {
                return resolveAssignment(prismObject, itemPath, targetType);
            }

            @Override
            public Class<? extends ObjectType> getTargetClassType() {
                Class<? extends ObjectType> objectClass = null;
                if (targetType.equals(OrgType.COMPLEX_TYPE)) {
                    objectClass = OrgType.class;
                } else if (targetType.equals(RoleType.COMPLEX_TYPE)) {
                    objectClass = RoleType.class;
                } else if (targetType.equals(ServiceType.COMPLEX_TYPE)) {
                    objectClass = ServiceType.class;
                } else if (targetType.equals(ArchetypeType.COMPLEX_TYPE)) {
                    objectClass = ArchetypeType.class;
                } else if (targetType.equals(ResourceType.COMPLEX_TYPE)) {
                    objectClass = ResourceType.class;
                }
                return objectClass;
            }

            @Override
            public ObjectQuery getQuery(String value) {

                Class<? extends ObjectType> objectClass = null;
                if (targetType.equals(OrgType.COMPLEX_TYPE)) {
                    objectClass = OrgType.class;
                } else if (targetType.equals(RoleType.COMPLEX_TYPE)) {
                    objectClass = RoleType.class;
                } else if (targetType.equals(ServiceType.COMPLEX_TYPE)) {
                    objectClass = ServiceType.class;
                } else if (targetType.equals(ArchetypeType.COMPLEX_TYPE)) {
                    objectClass = ArchetypeType.class;
                } else if (targetType.equals(ResourceType.COMPLEX_TYPE)) {
                    objectClass = ResourceType.class;
                }

                return PrismContext.get().queryFor(objectClass)
                        .item(getPath()).ref(value)
                        .build();
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
            boolean isMultiValue = !item.isSingleValue();

            if (isMultiValue) {
                Collection<?> realValues = item.getRealValues();
                for (Object realValue : realValues) {
                    if (realValue instanceof ObjectReferenceType objectReference) {
                        QName refTargetType = objectReference.getType();
                        if (refTargetType.equals(targetType)) {
                            resolvedValues.add(objectReference.getOid());
                        }
                    }
                }
            } else {
                Object realValue = item.getRealValue();
                if (realValue instanceof ObjectReferenceType objectReference) {
                    QName refTargetType = objectReference.getType();
                    if (refTargetType.equals(targetType)) {
                        resolvedValues.add(objectReference.getOid());
                    }
                }
            }
        }

        return resolvedValues;
    }

    @NotNull
    private static Set<String> resolveRefs(
            @NotNull PrismObject<?> prismObject,
            @NotNull ItemPath itemPath) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            boolean isMultiValue = !item.isSingleValue();

            if (isMultiValue) {
                Collection<?> realValues = item.getRealValues();
                for (Object realValue : realValues) {
                    if (realValue instanceof ObjectReferenceType objectReference) {
                        resolvedValues.add(objectReference.getOid());
                    }
                }
            } else {
                Object realValue = item.getRealValue();
                if (realValue instanceof ObjectReferenceType objectReference) {
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
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry()
                    .findItemDefinitionByFullPath(RoleType.class, PrismContainerDefinition.class, RoleType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismReferenceDefinition prismReferenceDefinition) {
                RoleAnalysisAttributeDef attribute = createRoleAttribute(
                        prismReferenceDefinition);
                attributes.add(attribute);
            }
            if (definition instanceof PrismPropertyDefinition<?> prismPropertyDefinition) {

                RoleAnalysisAttributeDef attribute = createRoleAttribute(
                        prismPropertyDefinition);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }

        return attributes;
    }

    private static @NotNull List<RoleAnalysisAttributeDef> loadUserExtension() {
        List<RoleAnalysisAttributeDef> attributes = new ArrayList<>();
        PrismContainerDefinition<?> itemDefinitionByFullPath;

        try {
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry()
                    .findItemDefinitionByFullPath(UserType.class, PrismContainerDefinition.class, UserType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismReferenceDefinition prismReferenceDefinition) {
                RoleAnalysisAttributeDef attribute = createUserAttribute(
                        prismReferenceDefinition);
                attributes.add(attribute);
            }
            if (definition instanceof PrismPropertyDefinition<?> prismPropertyDefinition) {

                RoleAnalysisAttributeDef attribute = createUserAttribute(
                        prismPropertyDefinition);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }

        return attributes;
    }

    private static @NotNull RoleAnalysisAttributeDef createUserAttribute(
            @NotNull PrismReferenceDefinition prismReferenceDefinition) {
        boolean isContainer = !prismReferenceDefinition.isSingleValue();

        ItemPath itemName = prismReferenceDefinition.getItemName();
        String attributeName = itemName + " extension";

        return new RoleAnalysisAttributeDef(
                ItemPath.create(UserType.F_EXTENSION, itemName),
                isContainer,
                attributeName,
                UserType.class,
                RoleAnalysisAttributeDef.IdentifierType.OID) {
            @Override
            public ObjectQuery getQuery(String value) {
                return PrismContext.get().queryFor(UserType.class)
                        .item(getPath()).ref(value)
                        .build();
            }

            @Override
            public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                return resolveRef(prismObject, itemPath);
            }

            @Override
            public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                return resolveRefs(prismObject, itemPath);
            }
        };

    }

    private static @Nullable RoleAnalysisAttributeDef createUserAttribute(
            @NotNull PrismPropertyDefinition<?> prismPropertyDefinition) {

        Class<?> typeClass = prismPropertyDefinition.getTypeClass();
        boolean isContainer = !prismPropertyDefinition.isSingleValue();

        ItemPath itemName = prismPropertyDefinition.getItemName();
        String attributeName = itemName + " extension";

        //TODO in some cases typeClass might be null, e.g. enumeration extension
        if (typeClass == null) {
            return null;
        }

        if (isSupportedPropertyType(typeClass)) {

            return new RoleAnalysisAttributeDef(
                    ItemPath.create(UserType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isContainer,
                    attributeName,
                    UserType.class,
                    RoleAnalysisAttributeDef.IdentifierType.FINAL) {

                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(UserType.class)
                            .item(getPath()).eq(value)
                            .build();
                }
            };
        } else if (typeClass.equals(ObjectReferenceType.class)) {
            return new RoleAnalysisAttributeDef(
                    ItemPath.create(UserType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isContainer,
                    attributeName,
                    UserType.class,
                    RoleAnalysisAttributeDef.IdentifierType.OID) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(UserType.class)
                            .item(getPath()).ref(value)
                            .build();
                }

                @Override
                public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRef(prismObject, itemPath);
                }

                @Override
                public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRefs(prismObject, itemPath);
                }
            };
        }
        return null;
    }

    private static @NotNull RoleAnalysisAttributeDef createRoleAttribute(
            @NotNull PrismReferenceDefinition prismReferenceDefinition) {
        boolean isContainer = !prismReferenceDefinition.isSingleValue();

        ItemPath itemName = prismReferenceDefinition.getItemName();
        String attributeName = itemName + " extension";

        return new RoleAnalysisAttributeDef(
                ItemPath.create(RoleType.F_EXTENSION, itemName),
                isContainer,
                attributeName,
                RoleType.class,
                RoleAnalysisAttributeDef.IdentifierType.OID) {
            @Override
            public ObjectQuery getQuery(String value) {
                return PrismContext.get().queryFor(RoleType.class)
                        .item(getPath()).ref(value)
                        .build();
            }

            @Override
            public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                return resolveRef(prismObject, itemPath);
            }

            @Override
            public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                return resolveRefs(prismObject, itemPath);
            }
        };

    }

    private static @Nullable RoleAnalysisAttributeDef createRoleAttribute(
            @NotNull PrismPropertyDefinition<?> prismPropertyDefinition) {

        Class<?> typeClass = prismPropertyDefinition.getTypeClass();
        boolean isContainer = !prismPropertyDefinition.isSingleValue();

        ItemPath itemName = prismPropertyDefinition.getItemName();
        String attributeName = itemName + " extension";

        //TODO in some cases typeClass might be null, e.g. enumeration extension
        if (typeClass == null) {
            return null;
        }

        if (isSupportedPropertyType(typeClass)) {
            return new RoleAnalysisAttributeDef(
                    ItemPath.create(RoleType.F_EXTENSION, itemName),
                    isContainer,
                    attributeName,
                    RoleType.class,
                    RoleAnalysisAttributeDef.IdentifierType.FINAL) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(RoleType.class)
                            .item(getPath()).eq(value)
                            .build();
                }
            };
        } else if (typeClass.equals(ObjectReferenceType.class)) {
            return new RoleAnalysisAttributeDef(
                    ItemPath.create(RoleType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isContainer,
                    attributeName,
                    RoleType.class,
                    RoleAnalysisAttributeDef.IdentifierType.OID) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(RoleType.class)
                            .item(getPath()).ref(value)
                            .build();
                }

                @Override
                public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRef(prismObject, itemPath);
                }

                @Override
                public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRefs(prismObject, itemPath);
                }
            };
        }
        return null;
    }

    public static boolean isSupportedPropertyType(@NotNull Class<?> typeClass) {
        return typeClass.equals(Integer.class) || typeClass.equals(Long.class) || typeClass.equals(Boolean.class)
                || typeClass.equals(Double.class) || typeClass.equals(String.class) || typeClass.equals(PolyString.class);
    }

    @Nullable
    private static String resolveRef(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
        if (property != null && property.isSingleValue()) {
            Object object = property.getRealValue();
            if (object instanceof ObjectReferenceType objectReference) {
                return objectReference.getOid();
            }
            return extractRealValue(object);
        }
        return null;
    }

//    public static @NotNull List<ItemPath> createAnalysisAttributeChoiceSet() {

//        List<AnalysisAttributeRuleType> result = new ArrayList<>();
//        List<RoleAnalysisAttributeDef> roleAttributesForRoleAnalysis = new ArrayList<>(
//                RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis());
//        List<RoleAnalysisAttributeDef> userAttributesForUserAnalysis = new ArrayList<>(
//                RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis());
//
//        addAttributesToResult(roleAttributesForRoleAnalysis, result, RoleType.COMPLEX_TYPE);
//        addAttributesToResult(userAttributesForUserAnalysis, result, UserType.COMPLEX_TYPE);
//    }


//    private static void addAttributesToResult(
//            @NotNull List<RoleAnalysisAttributeDef> attributeDef,
//            @NotNull List<AnalysisAttributeRuleType> result,
//            @NotNull QName complexType) {
//        for (RoleAnalysisAttributeDef def : attributeDef) {
//            result.add(createAnalysisAttributeRule(def, complexType));
//        }
//    }
//
//    private static @NotNull AnalysisAttributeRuleType createAnalysisAttributeRule(
//            @NotNull RoleAnalysisAttributeDef def,
//            @NotNull QName complexType) {
//        AnalysisAttributeRuleType rule = new AnalysisAttributeRuleType();
////        rule.setsetAttributeIdentifier(def.getDisplayValue());
////        rule.setPropertyType(complexType);
//        return rule;
//    }

//
//    @NotNull
//    public static List<RoleAnalysisAttributeDef> createSimpleUserAttributeChoiceSet() {
//        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = new ArrayList<>(getAttributesForUserAnalysis());
//        attributesForUserAnalysis.removeIf(RoleAnalysisAttributeDef::isContainer);
//
//        RoleAnalysisAttributeDef objectNameDef = getObjectNameDef();
//        attributesForUserAnalysis.add(0, objectNameDef);
//        return attributesForUserAnalysis;
//    }
//
//    @NotNull
//    public static List<RoleAnalysisAttributeDef> createSimpleRoleAttributeChoiceSet() {
//        List<RoleAnalysisAttributeDef> attributesForRoleAnalysis = new ArrayList<>(getAttributesForRoleAnalysis());
//        attributesForRoleAnalysis.removeIf(RoleAnalysisAttributeDef::isContainer);
//
//        RoleAnalysisAttributeDef objectNameDef = getObjectNameDef();
//        attributesForRoleAnalysis.add(0, objectNameDef);
//        return attributesForRoleAnalysis;
//    }

//    public static @NotNull List<ClusteringAttributeRuleType> createClusteringAttributeChoiceSet(
//            @NotNull RoleAnalysisProcessModeType processModeType) {
//        List<RoleAnalysisAttributeDef> attributesForUserAnalysis;
//        if (processModeType.equals(RoleAnalysisProcessModeType.USER)) {
//            attributesForUserAnalysis = RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis();
//        } else {
//            attributesForUserAnalysis = RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis();
//        }
//
//        List<ClusteringAttributeRuleType> result = new ArrayList<>();
//        for (RoleAnalysisAttributeDef def : attributesForUserAnalysis) {
//            ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType();
//            rule.setPath(def.getPath().toBean());
//            rule.setSimilarity(100.0);
//            rule.setWeight(1.0);
//            rule.setIsMultiValue(def.isContainer());
//            result.add(rule);
//        }
//        return result;
//    }

}
