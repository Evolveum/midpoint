/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import java.util.*;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemWrapperComparator;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPasswordPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismReferencePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormRoleAnalysisAttributeSettingPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ProtectedStringTypeWrapperImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.security.MidPointApplication;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * @author katka
 */
public class WebPrismUtil {

    private static final Trace LOGGER = TraceManager.getTrace(WebPrismUtil.class);

    private static final String DOT_CLASS = WebPrismUtil.class.getName() + ".";
    private static final String OPERATION_CREATE_NEW_VALUE = DOT_CLASS + "createNewValue";

    public static final ItemName PRISM_SCHEMA = new ItemName(PrismSchemaType.F_COMPLEX_TYPE.getNamespaceURI(), "prismSchema");

    public static <ID extends ItemDefinition<I>, I extends Item<?, ?>> String getHelpText(ID def, Class<?> containerClass) {
        if (def == null) {
            return null;
        }

        String help = def.getHelp();
        if (StringUtils.isNotEmpty(help)) {
            String defaultValue = help.replaceAll("\\s{2,}", " ").trim();
            return PageBase.createStringResourceStatic(help, defaultValue).getString();
        }

        QName name = def.getItemName();

        if (name != null && containerClass != null) {
            String localizedHelp = getLocalizedHelpWithContainerClass(name, containerClass);
            if (StringUtils.isNotEmpty(localizedHelp)) {
                return localizedHelp;
            }
        }

        String doc = def.getDocumentation();
        if (StringUtils.isEmpty(doc)) {
            return null;
        }

        return doc.replaceAll("\\s{2,}", " ").trim();
    }

    private static String getLocalizedHelpWithContainerClass(@NotNull QName name, @NotNull Class<?> containerClass) {
        String displayName = name.getLocalPart();
        String containerName = containerClass.getSimpleName();

        String helpKey = containerName + "." + displayName + ".help";
        String localizedHelp = PageBase.createStringResourceStatic(helpKey).getString();
        if (!localizedHelp.equals(helpKey)) {
            return localizedHelp;
        }
        if (containerClass.getSuperclass() != null) {
            return getLocalizedHelpWithContainerClass(name, containerClass.getSuperclass());
        }
        return null;
    }

    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(IW itemWrapper, PV newValue, PageBase pageBase, AjaxRequestTarget target) {
        LOGGER.debug("Adding value to {}", itemWrapper);

        Task task = pageBase.createSimpleTask(OPERATION_CREATE_NEW_VALUE);
        OperationResult result = task.getResult();

        VW newValueWrapper = null;
        try {

            if (!(itemWrapper instanceof PrismContainerWrapper)) {
                itemWrapper.getItem().add(newValue);
            }

            WrapperContext context = new WrapperContext(task, result);
            context.setObjectStatus(itemWrapper.findObjectStatus());
            context.setShowEmpty(true);
            context.setCreateIfEmpty(true);

            newValueWrapper = pageBase.createValueWrapper(itemWrapper, newValue, ValueStatus.ADDED, context);
            itemWrapper.getValues().add(newValueWrapper);
            result.recordSuccess();

        } catch (SchemaException e) {
            LOGGER.error("Cannot create new value for {}", itemWrapper, e);
            result.recordFatalError(pageBase.createStringResource("WebPrismUtil.message.createNewValueWrapper.fatalError", newValue, e.getMessage()).getString(), e);
            target.add(pageBase.getFeedbackPanel());
        }

        return newValueWrapper;
    }

    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(
            IW itemWrapper, PV newValue, ModelServiceLocator modelServiceLocator) throws SchemaException {
        return createNewValueWrapper(itemWrapper, newValue, ValueStatus.ADDED, modelServiceLocator);
    }

    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(
            IW itemWrapper, PV newValue, ModelServiceLocator modelServiceLocator,
            WrapperContext wrapperContext) throws SchemaException {
        return createNewValueWrapper(itemWrapper, newValue, ValueStatus.ADDED, modelServiceLocator, wrapperContext);
    }

    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(
            IW itemWrapper, PV newValue, ValueStatus status, ModelServiceLocator modelServiceLocator) throws SchemaException {
        return createNewValueWrapper(itemWrapper, newValue, status, modelServiceLocator, null);
    }

    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(
            IW itemWrapper, PV newValue, ValueStatus status, ModelServiceLocator modelServiceLocator,
            WrapperContext context) throws SchemaException {
        LOGGER.debug("Adding value to {}", itemWrapper);

        Task task = modelServiceLocator.createSimpleTask(OPERATION_CREATE_NEW_VALUE);
        OperationResult result = task.getResult();

        if (context == null) {
            context = new WrapperContext(task, result);
            context.setObjectStatus(itemWrapper.findObjectStatus());
            context.setShowEmpty(true);
            context.setCreateIfEmpty(true);
        }

        VW newValueWrapper = modelServiceLocator.createValueWrapper(itemWrapper, newValue, status, context);
        result.recordSuccess();

        return newValueWrapper;
    }

    public static <IW extends ItemWrapper> IW findItemWrapper(ItemWrapper<?, ?> child, ItemPath absoluthPathToFind, Class<IW> wrapperClass) {
        PrismObjectWrapper<?> taskWrapper = child.findObjectWrapper();
        try {
            return taskWrapper.findItem(ItemPath.create(absoluthPathToFind), wrapperClass);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get object reference value, {}", e, e.getMessage());
            return null;
        }
    }

    public static <R extends Referencable> PrismReferenceWrapper<R> findReferenceWrapper(ItemWrapper<?, ?> child, ItemPath pathToFind) {
        return findItemWrapper(child, pathToFind, PrismReferenceWrapper.class);
    }

    public static <T> PrismPropertyWrapper<T> findPropertyWrapper(ItemWrapper<?, ?> child, ItemPath pathToFind) {
        return findItemWrapper(child, pathToFind, PrismPropertyWrapper.class);
    }

    public static <R extends Referencable> PrismReferenceValue findSingleReferenceValue(ItemWrapper<?, ?> child, ItemPath pathToFind) {
        PrismReferenceWrapper<R> objectRefWrapper = findReferenceWrapper(child, pathToFind);
        if (objectRefWrapper == null) {
            return null;
        }

        try {
            return objectRefWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get object reference value, {}", e, e.getMessage());
            return null;
        }
    }

    public static <T> PrismPropertyValue<T> findSinglePropertyValue(ItemWrapper<?, ?> child, ItemPath pathToFind) {
        PrismPropertyWrapper<T> propertyWrapper = findPropertyWrapper(child, pathToFind);
        if (propertyWrapper == null) {
            return null;
        }

        try {
            return propertyWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get object reference value, {}", e, e.getMessage());
            return null;
        }
    }

    public static <C extends Containerable> void cleanupEmptyContainers(PrismContainer<C> container) {
        List<PrismContainerValue<C>> values = container.getValues();
        Iterator<PrismContainerValue<C>> valueIterator = values.iterator();
        while (valueIterator.hasNext()) {
            PrismContainerValue<C> value = valueIterator.next();

            PrismContainerValue<C> valueAfter = cleanupEmptyContainerValue(value);
            if (isUseAsEmptyValue(valueAfter)) {
                continue;
            }
            if (valueAfter == null || valueAfter.isIdOnly() || valueAfter.isEmpty()) {
                valueIterator.remove();
            }
        }
    }

    public static <C extends Containerable> boolean isEmptyContainer(PrismContainer<C> container) {
        PrismContainer<C> clone = container.clone();
        cleanupEmptyContainers(clone);
        return clone.isEmpty();
    }

    //TODO quick hack ... use for it wrappers
    public static <C extends Containerable> boolean isUseAsEmptyValue(PrismContainerValue<C> valueAfter) {
        return valueAfter != null && isUseAsEmptyValue(valueAfter.getRealClass());
    }

    private static <C extends Containerable> boolean isUseAsEmptyValue(Item item) {
        return item != null && item.getDefinition() != null && isUseAsEmptyValue(item.getDefinition().getTypeClass());
    }

    private static <C extends Containerable> boolean isUseAsEmptyValue(Class<?> typeClass) {
        return typeClass != null
                && (AbstractSynchronizationActionType.class.isAssignableFrom(typeClass)
                || AssociationSynchronizationExpressionEvaluatorType.class.isAssignableFrom(typeClass)
                || AssociationConstructionExpressionEvaluatorType.class.isAssignableFrom(typeClass));
    }

    public static <C extends Containerable> PrismContainerValue<C> cleanupEmptyContainerValue(PrismContainerValue<C> value) {
        Collection<Item<?, ?>> items = value.getItems();

        if (items != null) {
            Iterator<Item<?, ?>> iterator = items.iterator();
            while (iterator.hasNext()) {
                Item<?, ?> item = iterator.next();

                cleanupEmptyValues(item);
                if (!isUseAsEmptyValue(item) && item.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        cleanupValueMetadata(value);

        if (!isUseAsEmptyValue(value) && (value.getItems() == null || value.getItems().isEmpty())) {
            return null;
        }

        return value;
    }

    public static void cleanupValueMetadata(PrismValue value) {
        if (value.hasValueMetadata()) {
            cleanupEmptyValues(value.getValueMetadata());
        }
    }

    private static <T> void cleanupEmptyValues(Item item) {
        if (item instanceof PrismContainer) {
            cleanupEmptyContainers((PrismContainer) item);
        }

        if (item instanceof PrismProperty) {
            PrismProperty<T> property = (PrismProperty) item;
            List<PrismPropertyValue<T>> pVals = property.getValues();
            if (pVals == null || pVals.isEmpty()) {
                return;
            }

            Iterator<PrismPropertyValue<T>> iterator = pVals.iterator();
            while (iterator.hasNext()) {
                PrismPropertyValue<T> pVal = iterator.next();
                if (pVal == null) {
                    iterator.remove();
                    continue;
                }
                if (pVal.getRealValue() instanceof ExpressionType && ExpressionUtil.isEmpty((ExpressionType) pVal.getRealValue())) {
                    iterator.remove();
                    continue;
                }
                if (pVal.isEmpty() || pVal.getRealValue() == null) {
                    iterator.remove();
                    continue;
                }

                cleanupValueMetadata(pVal);
            }
        }

        if (item instanceof PrismReference) {
            PrismReference ref = (PrismReference) item;
            List<PrismReferenceValue> values = ref.getValues();
            if (values == null || values.isEmpty()) {
                return;
            }

            Iterator<PrismReferenceValue> iterator = values.iterator();
            while (iterator.hasNext()) {
                PrismReferenceValue rVal = iterator.next();
                if (rVal == null || rVal.isEmpty()) {
                    iterator.remove();
                    continue;
                }

                cleanupValueMetadata(rVal);
            }
        }
    }

    //TODO find better place
    public static PrismContainerValue<ValueMetadataType> getNewYieldValue() {
        MidPointApplication app = MidPointApplication.get();
        ProvenanceMetadataType provenanceMetadataType = new ProvenanceMetadataType(app.getPrismContext()).acquisition(WebPrismUtil.createAcquition());
        ValueMetadataType valueMetadataType = new ValueMetadataType(app.getPrismContext()).provenance(provenanceMetadataType);
        return valueMetadataType.asPrismContainerValue();

    }

    public static ProvenanceAcquisitionType createAcquition() {
        MidPointApplication app = MidPointApplication.get();
        ProvenanceAcquisitionType acquisitionType = new ProvenanceAcquisitionType(app.getPrismContext());
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null) {
            FocusType focus = principal.getFocus();
            if (focus != null) {
                acquisitionType.setActorRef(ObjectTypeUtil.createObjectRef(focus));
            }
        }
        acquisitionType.setChannel(GuiChannel.USER.getUri());
        acquisitionType.setTimestamp(app.getClock().currentTimeXMLGregorianCalendar());
        return acquisitionType;
    }

    public static boolean isValueFromResourceTemplate(PrismValue valueFromDelta, PrismContainer parent) {
        if (valueFromDelta instanceof PrismObjectValue) {
            return false;
        }

        if (hasValueTemplateMetadata(valueFromDelta)) {
            return true;
        }
        Item<PrismValue, ItemDefinition<?>> item = parent.findItem(valueFromDelta.getParent().getPath());
        PrismContainerValue<?> value = item.getParent();
        while (!(value instanceof PrismObjectValue)) {
            if (hasValueTemplateMetadata(value)) {
                return true;
            }
            value = value.getParentContainerValue();
        }
        return false;
    }

    public static boolean hasValueTemplateMetadata(PrismValue value) {
        if (value == null) {
            return false;
        }

        if (value.hasValueMetadata()) {
            List<PrismContainerValue<Containerable>> metadataValues = value.getValueMetadata().getValues();

            if (metadataValues.size() == 1) {
                ProvenanceMetadataType provenance = ((ValueMetadataType) metadataValues.get(0).asContainerable()).getProvenance();
                if (provenance != null) {
                    List<ProvenanceAcquisitionType> acquisitionValues = provenance.getAcquisition();
                    if (acquisitionValues.size() == 1) {
                        ObjectReferenceType originRef = acquisitionValues.get(0).getOriginRef();
                        return originRef != null && StringUtils.isNotEmpty(originRef.getOid());
                    }
                }
            }
        }
        return false;
    }

    public static List<ShadowAttributeDefinition> searchAttributeDefinitions(
            ResourceSchema schema, ResourceObjectTypeDefinitionType objectType) {
        List<ShadowAttributeDefinition> allAttributes = new ArrayList<>();
        if (objectType != null) {
            @Nullable ResourceObjectTypeDefinition objectTypeDef = null;
            if (objectType.getKind() != null && objectType.getIntent() != null) {

                @NotNull ResourceObjectTypeIdentification identifier =
                        ResourceObjectTypeIdentification.of(objectType.getKind(), objectType.getIntent());
                objectTypeDef = schema.getObjectTypeDefinition(identifier);

                if (objectTypeDef != null) {
                    objectTypeDef.getSimpleAttributeDefinitions()
                            .forEach(attr -> allAttributes.add(attr));
                }
            }
            if (objectTypeDef == null && objectType.getDelineation() != null && objectType.getDelineation().getObjectClass() != null) {

                @NotNull Collection<ResourceObjectClassDefinition> defs = schema.getObjectClassDefinitions();
                Optional<ResourceObjectClassDefinition> objectClassDef = defs.stream()
                        .filter(d -> QNameUtil.match(d.getTypeName(), objectType.getDelineation().getObjectClass()))
                        .findFirst();

                if (!objectClassDef.isEmpty()) {
                    objectClassDef.get().getSimpleAttributeDefinitions().forEach(attr -> allAttributes.add(attr));
                    defs.stream()
                            .filter(d -> {
                                for (QName auxClass : objectType.getDelineation().getAuxiliaryObjectClass()) {
                                    if (QNameUtil.match(d.getTypeName(), auxClass)) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .forEach(d -> d.getSimpleAttributeDefinitions()
                                    .forEach(attr -> allAttributes.add(attr)));
                }
            }
        }
        return allAttributes;
    }

    public static ItemPanel createVerticalPropertyPanel(String id, IModel<? extends ItemWrapper<?, ?>> model, ItemPanelSettings origSettings) {
        ItemPanel propertyPanel;
        ItemPanelSettings settings = origSettings != null ? origSettings.copy() : null;
        if (model.getObject().getParent() != null && AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING.equivalent(model.getObject().getParent().getDefinition().getItemName())) {
            propertyPanel = new VerticalFormRoleAnalysisAttributeSettingPanel(id, (IModel<PrismPropertyWrapper<ItemPathType>>) model, settings);
        } else if (model.getObject() instanceof ProtectedStringTypeWrapperImpl) {
            propertyPanel = new VerticalFormPasswordPropertyPanel(
                    id, (IModel<PrismPropertyWrapper<ProtectedStringType>>) model, settings);
        } else if (model.getObject() instanceof PrismPropertyWrapper) {
            propertyPanel = new VerticalFormPrismPropertyPanel(id, model, settings);
        } else {
            propertyPanel = new VerticalFormPrismReferencePanel(id, model, settings);
        }
        propertyPanel.setOutputMarkupId(true);
        return propertyPanel;
    }

    public static void sortContainers(List<PrismContainerWrapper<? extends Containerable>> containers) {
        ItemWrapperComparator<?> comparator = new ItemWrapperComparator<>(WebComponentUtil.getCollator(), false);
        if (CollectionUtils.isNotEmpty(containers)) {
            containers.sort((Comparator) comparator);
        }
    }

    public static String getLocalizedDisplayName(Item item) {
        Validate.notNull(item, "Item must not be null.");

        String displayName = item.getDisplayName();
        if (!StringUtils.isEmpty(displayName)) {
            return localizeName(displayName, displayName);
        }

        QName name = item.getElementName();
        if (name != null) {
            displayName = name.getLocalPart();

            PrismContainerValue<?> val = item.getParent();
            if (!(item instanceof ShadowAttributesContainer)
                    && val != null
                    && val.getDefinition() != null
                    && val.getDefinition().isRuntimeSchema()) {
                return localizeName(displayName, displayName);
            }

            if (val != null) {
                if (val.getRealClass() != null) {
                    displayName = val.getRealClass().getSimpleName() + "." + displayName;
                    String localizedName = localizeName(displayName, displayName);
                    //try to find by super class name + item name
                    if (localizedName.equals(displayName) && val.getRealClass().getSuperclass() != null) {
                        return getItemDisplayNameFromSuperClassName(val.getRealClass().getSuperclass(), name.getLocalPart());
                    }
                } else if (val.getTypeName() != null) {
                    displayName = val.getTypeName().getLocalPart() + "." + displayName;
                }
            }
        } else {
            displayName = item.getDefinition().getTypeName().getLocalPart();
        }

        return localizeName(displayName, name.getLocalPart());
    }

    private static String getItemDisplayNameFromSuperClassName(Class superClass, String itemName) {
        if (superClass == null) {
            return "";
        }
        String displayNameParentClass = superClass.getSimpleName() + "." + itemName;
        String localizedName = localizeName(displayNameParentClass, displayNameParentClass);
        if (localizedName.equals(displayNameParentClass) && superClass.getSuperclass() != null) {
            return getItemDisplayNameFromSuperClassName(superClass.getSuperclass(), itemName);
        }
        if (!localizedName.equals(displayNameParentClass)) {
            return localizedName;
        } else {
            return itemName;
        }
    }

    private static String localizeName(String nameKey, String defaultString) {
        Validate.notNull(nameKey, "Null localization key");
        return ColumnUtils.createStringResource(nameKey, defaultString).getString();
    }

    public static void collectWrappers(ItemWrapper iw, List<ItemWrapper> iws) {
        iws.add(iw);

        if (!(iw instanceof PrismContainerWrapper)) {
            return;
        }

        PrismContainerWrapper pcw = (PrismContainerWrapper) iw;
        List<PrismContainerValueWrapper> pcvws = pcw.getValues();
        if (pcvws == null) {
            return;
        }

        pcvws.forEach(pcvw -> {
            pcvw.getItems().forEach(childIW -> {
                collectWrappers((ItemWrapper) childIW, iws);
            });
        });
    }

    public static PrismContainerValue findContainerValueParent(@NotNull PrismContainerValue child, Class<? extends Containerable> clazz) {
        PrismContainerable parent = child.getParent();
        if (parent == null || !(parent instanceof Item parentItem)) {
            return null;
        }
        return findContainerValueParent(parentItem, clazz);
    }

    public static PrismContainerValue findContainerValueParent(@NotNull Item child, Class<? extends Containerable> clazz) {
        @Nullable PrismContainerValue parent = child.getParent();
        if (parent == null) {
            return null;
        }
        if (clazz.equals(parent.getDefinition().getTypeClass())) {
            return parent;
        }

        if (parent.getParent() == null || !(parent.getParent() instanceof Item parentItem)) {
            return null;
        }

        return findContainerValueParent(parentItem, clazz);
    }

    public static String createMappingTypeDescription(MappingType mapping) {
        return createMappingTypeDescription(mapping, true);
    }

    public static String createMappingTypeDescription(MappingType mapping, boolean showExpression) {
        if (StringUtils.isNotEmpty(mapping.getDescription())) {
            return mapping.getDescription();
        }
        String strength = translateStrength(mapping);

        ExpressionType expressionBean = mapping.getExpression();
        String description = LocalizationUtil.translate(
                "AbstractSpecificMappingTileTable.tile.description.prefix",
                new Object[] { strength });

        if (showExpression) {
            ExpressionUtil.ExpressionEvaluatorType evaluatorType = null;
            if (expressionBean != null) {
                String expression = ExpressionUtil.loadExpression(expressionBean, PrismContext.get(), LOGGER);
                evaluatorType = ExpressionUtil.getExpressionType(expression);

            }

            if (evaluatorType == null) {
                evaluatorType = ExpressionUtil.ExpressionEvaluatorType.AS_IS;
            }

            String evaluator = PageBase.createStringResourceStatic(null, evaluatorType).getString();

            description += " " + LocalizationUtil.translate(
                    "AbstractSpecificMappingTileTable.tile.description.suffix",
                    new Object[] { evaluator });
        }
        return description;
    }

    public static String createMappingTypeStrengthHelp(MappingType mapping) {
        String strength = translateStrength(mapping);
        return LocalizationUtil.translate("AbstractSpecificMappingTileTable.tile.help", new Object[] { strength });
    }

    private static String translateStrength(MappingType mapping) {
        MappingStrengthType strengthBean = mapping.getStrength();
        if (strengthBean == null) {
            strengthBean = MappingStrengthType.NORMAL;
        }
        return PageBase.createStringResourceStatic(null, strengthBean).getString().toLowerCase();
    }

    public static QName convertStringWithPrefixToQName(String object) {
        if (StringUtils.isEmpty(object)) {
            return null;
        }

        if (object.contains(":")) {
            int index = object.indexOf(":");
            return new QName(null, object.substring(index + 1), object.substring(0, index));
        }
        return new QName(object);
    }

    public static int getNumberOfSameMappingNames(PrismContainerValueWrapper containerValue, String value) {
        int numberOfSameRef = 0;

        if (AbstractMappingType.class.isAssignableFrom(containerValue.getDefinition().getTypeClass())) {
            try {
                PrismPropertyWrapper<String> nameProperty = containerValue.findProperty(AbstractMappingType.F_NAME);
                String name = nameProperty.getValue().getRealValue();

                if (StringUtils.equals(value, name)) {
                    numberOfSameRef++;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find name attribute in objectType " + containerValue, e);
            }
        }

        List<ItemWrapper> containers = containerValue.getItems();
        for (ItemWrapper item : containers) {
            if (item instanceof PrismContainerWrapper<?> container) {
                for (PrismContainerValueWrapper childContainerValue : container.getValues()) {
                    numberOfSameRef = numberOfSameRef + getNumberOfSameMappingNames(childContainerValue, value);
                }
            }
        }

        return numberOfSameRef;
    }

    public static int getNumberOfSameAssociationNames(PrismContainerValueWrapper containerValue, QName value) {
        if (containerValue == null) {
            return 0;
        }
        if (containerValue.getDefinition() != null
                && QNameUtil.match(ShadowAssociationTypeDefinitionType.COMPLEX_TYPE, containerValue.getDefinition().getTypeName())) {
            try {
                PrismPropertyWrapper<QName> nameProperty = containerValue.findProperty(ShadowAssociationTypeDefinitionType.F_NAME);
                QName name = nameProperty.getValue().getRealValue();

                if (name != null && QNameUtil.match(value, name)) {
                    return 1;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find association name property in " + containerValue, e);
            }
        }

        int numberOfSameRef = 0;
        List<? extends ItemWrapper<?, ?>> containers = containerValue.getItems();
        for (ItemWrapper<?, ?> item : containers) {
            if (item instanceof PrismContainerWrapper<?> container) {
                for (PrismContainerValueWrapper childContainerValue : container.getValues()) {
                    numberOfSameRef = numberOfSameRef + getNumberOfSameAssociationNames(childContainerValue, value);
                }
            }
        }
        return numberOfSameRef;
    }

    /**
     * Set read-only all items in the container and its sub-containers.
     * Force to use LabelPanelFactory for all items.
     * @param wrapper container value to be set read-only
     */
    public static void setReadOnlyRecursively(@NotNull PrismContainerValueWrapper<?> wrapper) {
        wrapper.getItems().forEach(item -> {
            item.setReadOnly(true);
            if (item instanceof PrismContainerWrapper<?> containerWrapper) {
                setReadOnlyRecursively(containerWrapper);
            }
        });
    }

    /**
     * Set read-only all items in the container and its sub-containers.
     * Force to use LabelPanelFactory for all items.
     * @param wrapper container to be set read-only
     */
    public static void setReadOnlyRecursively(@NotNull PrismContainerWrapper<?> wrapper) {
        wrapper.getValues().forEach(WebPrismUtil::setReadOnlyRecursively);
    }

}
