/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import java.util.*;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    //TODO quick hack ... use for it wrappers
    public static <C extends Containerable> boolean isUseAsEmptyValue(PrismContainerValue<C> valueAfter) {
        return valueAfter != null && isUseAsEmptyValue(valueAfter.getRealClass());
    }

    private static <C extends Containerable> boolean isUseAsEmptyValue(Item item) {
        return item != null && isUseAsEmptyValue(item.getDefinition().getTypeClass());
    }

    private static <C extends Containerable> boolean isUseAsEmptyValue(Class<?> typeClass) {
        return typeClass != null &&
                (AbstractSynchronizationActionType.class.isAssignableFrom(typeClass)
                        || ActivitySimulationResultDefinitionType.class.isAssignableFrom(typeClass));
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

        if (!isUseAsEmptyValue(value) && (value.getItems() == null || value.getItems().isEmpty())) {
            return null;
        }

        return value;
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
                }
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
                }
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
                acquisitionType.setActorRef(ObjectTypeUtil.createObjectRef(focus, app.getPrismContext()));
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

    public static List<ResourceAttributeDefinition> searchAttributeDefinitions(
            ResourceSchema schema, ResourceObjectTypeDefinitionType objectType) {
        List<ResourceAttributeDefinition> allAttributes = new ArrayList<>();
        if (objectType != null) {
            @Nullable ResourceObjectTypeDefinition objectTypeDef = null;
            if (objectType.getKind() != null && objectType.getIntent() != null) {

                @NotNull ResourceObjectTypeIdentification identifier =
                        ResourceObjectTypeIdentification.of(objectType.getKind(), objectType.getIntent());
                objectTypeDef = schema.getObjectTypeDefinition(identifier);

                if (objectTypeDef != null) {
                    objectTypeDef.getAttributeDefinitions()
                            .forEach(attr -> allAttributes.add(attr));
                }
            }
            if (objectTypeDef == null && objectType.getDelineation() != null && objectType.getDelineation().getObjectClass() != null) {

                @NotNull Collection<ResourceObjectClassDefinition> defs = schema.getObjectClassDefinitions();
                Optional<ResourceObjectClassDefinition> objectClassDef = defs.stream()
                        .filter(d -> QNameUtil.match(d.getTypeName(), objectType.getDelineation().getObjectClass()))
                        .findFirst();

                if (!objectClassDef.isEmpty()) {
                    objectClassDef.get().getAttributeDefinitions().forEach(attr -> allAttributes.add(attr));
                    defs.stream()
                            .filter(d -> {
                                for (QName auxClass : objectType.getDelineation().getAuxiliaryObjectClass()) {
                                    if (QNameUtil.match(d.getTypeName(), auxClass)) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .forEach(d -> d.getAttributeDefinitions()
                                    .forEach(attr -> allAttributes.add(attr)));
                }
            }
        }
        return allAttributes;
    }
}
