/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.LocalizableMessage;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.NotificationFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Formats prism items and their values for notification purposes.
 */
@Component
public class ValueFormatter {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired protected NotificationFunctions functions;
    @Autowired private LocalizationService localizationService;

    private static final Trace LOGGER = TraceManager.getTrace(ValueFormatter.class);

    // todo - should each hiddenAttribute be prefixed with something like F_ATTRIBUTE? Currently it should not be.
    public String formatAccountAttributes(ShadowType shadowType, Collection<ItemPath> hiddenAttributes, boolean showOperationalAttributes) {
        Validate.notNull(shadowType, "shadowType is null");

        StringBuilder retval = new StringBuilder();
        if (shadowType.getAttributes() != null) {
            formatContainerValue(retval, "", shadowType.getAttributes().asPrismContainerValue(), false, hiddenAttributes, showOperationalAttributes);
        }
        if (shadowType.getCredentials() != null) {
            formatContainerValue(retval, "", shadowType.getCredentials().asPrismContainerValue(), false, hiddenAttributes, showOperationalAttributes);
        }
        if (shadowType.getActivation() != null) {
            formatContainerValue(retval, "", shadowType.getActivation().asPrismContainerValue(), false, hiddenAttributes, showOperationalAttributes);
        }
        if (shadowType.getAssociation() != null) {
            boolean first = true;
            for (ShadowAssociationType shadowAssociationType : shadowType.getAssociation()) {
                if (first) {
                    first = false;
                    retval.append("\n");
                }
                retval.append("Association:\n");
                formatContainerValue(retval, "  ", shadowAssociationType.asPrismContainerValue(), false, hiddenAttributes, showOperationalAttributes);
                retval.append("\n");
            }
        }

        return retval.toString();
    }

    String formatObject(@NotNull PrismObject<?> object, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        StringBuilder retval = new StringBuilder();
        formatContainerValue(retval, "", object.getValue(), false, hiddenPaths, showOperationalAttributes);
        return retval.toString();
    }

    void formatPrismValue(StringBuilder sb, String prefix, PrismValue prismValue, boolean mightBeRemoved, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (prismValue instanceof PrismPropertyValue) {
            sb.append(toStringValue((PrismPropertyValue<?>) prismValue));
        } else if (prismValue instanceof PrismReferenceValue) {
            sb.append(formatReferenceValue((PrismReferenceValue) prismValue, mightBeRemoved));
        } else if (prismValue instanceof PrismContainerValue) {
            sb.append("\n");
            formatContainerValue(sb, prefix, (PrismContainerValue<?>) prismValue, mightBeRemoved, hiddenPaths, showOperationalAttributes);
        } else {
            sb.append("Unexpected PrismValue type: ");
            sb.append(prismValue);
            LOGGER.error("Unexpected PrismValue type: " + prismValue.getClass() + ": " + prismValue);
        }
    }

    void formatContainerValue(StringBuilder sb, String prefix, PrismContainerValue<?> containerValue, boolean mightBeRemoved, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        List<Item<?, ?>> visibleItems = filterAndOrderItems(containerValue.getItems(), hiddenPaths, showOperationalAttributes);

        for (Item<?, ?> item : visibleItems) {
            if (item instanceof PrismProperty) {
                formatPrismProperty(sb, prefix, item);
            } else if (item instanceof PrismReference) {
                formatPrismReference(sb, prefix, item, mightBeRemoved);
            } else if (item instanceof PrismContainer) {
                formatPrismContainer(sb, prefix, item, mightBeRemoved, hiddenPaths, showOperationalAttributes);
            } else {
                sb.append("Unexpected Item type: ");
                sb.append(item);
                sb.append("\n");
                LOGGER.error("Unexpected Item type: " + item.getClass() + ": " + item);
            }
        }
    }

    void formatPrismContainer(StringBuilder sb, String prefix, Item<?, ?> item, boolean mightBeRemoved, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        for (PrismContainerValue<?> subContainerValue : ((PrismContainer<? extends Containerable>) item).getValues()) {
            String prefixSubContainer = prefix + "   ";
            StringBuilder valueSb = new StringBuilder();
            formatContainerValue(valueSb, prefixSubContainer, subContainerValue, mightBeRemoved, hiddenPaths, showOperationalAttributes);
            if (valueSb.length() > 0) {
                sb.append(prefix);
                sb.append(" - ");
                sb.append(getItemLabel(item));
                if (subContainerValue.getId() != null) {
                    sb.append(" #").append(subContainerValue.getId());
                }
                sb.append(":\n");
                sb.append(valueSb);
            }
        }
    }

    void formatPrismReference(StringBuilder sb, String prefix, Item<?, ?> item, boolean mightBeRemoved) {
        sb.append(prefix);
        sb.append(" - ");
        sb.append(getItemLabel(item));
        sb.append(": ");
        if (item.size() > 1) {
            for (PrismReferenceValue referenceValue : ((PrismReference) item).getValues()) {
                sb.append("\n");
                sb.append(prefix).append("   - ");
                sb.append(formatReferenceValue(referenceValue, mightBeRemoved));
            }
        } else if (item.size() == 1) {
            sb.append(formatReferenceValue(((PrismReference) item).getAnyValue(), mightBeRemoved));
        }
        sb.append("\n");
    }

    void formatPrismProperty(StringBuilder sb, String prefix, Item<?, ?> item) {
        sb.append(prefix);
        sb.append(" - ");
        sb.append(getItemLabel(item));
        sb.append(": ");
        if (item.size() > 1) {
            for (PrismPropertyValue<?> propertyValue : ((PrismProperty<?>) item).getValues()) {
                sb.append("\n");
                sb.append(prefix).append("   - ");
                sb.append(toStringValue(propertyValue));
            }
        } else if (item.size() == 1) {
            sb.append(toStringValue(((PrismProperty<?>) item).getAnyValue()));
        }
        sb.append("\n");
    }

    private String toStringValue(PrismPropertyValue value) {
        LocalizableMessage msg = ValueDisplayUtil.toStringValue(value);
        return msg != null ? msg.getFallbackMessage() : null;
    }

    private String formatReferenceValue(PrismReferenceValue value, boolean mightBeRemoved) {

        OperationResult result = new OperationResult("dummy");

        //noinspection unchecked
        PrismObject<? extends ObjectType> object = value.getObject();

        if (object == null) {
            object = getPrismObject(value.getOid(), mightBeRemoved, result);
        }

        String qualifier = "";
        if (object != null && object.asObjectable() instanceof ShadowType) {
            ShadowType shadowType = (ShadowType) object.asObjectable();
            ObjectReferenceType resourceRef = shadowType.getResourceRef();
            //noinspection unchecked
            PrismObject<ResourceType> resource = resourceRef.asReferenceValue().getObject();
            ResourceType resourceType = null;
            if (resource == null) {
                resource = getPrismObject(resourceRef.getOid(), false, result);
                if (resource != null) {
                    resourceType = resource.asObjectable();
                }
            } else {
                resourceType = resource.asObjectable();
            }
            if (resourceType != null) {
                qualifier = " on " + resourceType.getName();
            } else {
                qualifier = " on resource " + shadowType.getResourceRef().getOid();
            }
        }

        String referredObjectIdentification;
        if (object != null) {
            referredObjectIdentification = PolyString.getOrig(object.asObjectable().getName()) +
                    " (" + object.toDebugType() + ")" +
                    qualifier;
        } else {
            String nameOrOid = value.getTargetName() != null ? value.getTargetName().getOrig() : value.getOid();
            if (mightBeRemoved) {
                referredObjectIdentification = "(cannot display the actual name of " + localPart(value.getTargetType()) + ":" + nameOrOid + ", as it might be already removed)";
            } else {
                referredObjectIdentification = localPart(value.getTargetType()) + ":" + nameOrOid;
            }
        }

        return value.getRelation() != null ?
                referredObjectIdentification + " [" + value.getRelation().getLocalPart() + "]"
                : referredObjectIdentification;
    }

    private <O extends ObjectType> PrismObject<O> getPrismObject(String oid, boolean mightBeRemoved, OperationResult result) {
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
            //noinspection unchecked
            return (PrismObject<O>) cacheRepositoryService.getObject(ObjectType.class, oid, options, result);
        } catch (ObjectNotFoundException e) {
            if (!mightBeRemoved) {
                LoggingUtils.logException(LOGGER, "Couldn't resolve reference when displaying object name within a notification (it might be already removed)", e);
            } else {
                // ok, accepted
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't resolve reference when displaying object name within a notification", e);
        }
        return null;
    }

    private String localPart(QName qname) {
        return qname == null ? null : qname.getLocalPart();
    }

    private String resolve(String key) {
        if (key != null) {
            return localizationService.translate(key, null, Locale.getDefault(), key);
        } else {
            return null;
        }
    }

    // we call this on filtered list of items - all of them have definition set
    private String getItemLabel(Item<?, ?> item) {
        return item.getDefinition().getDisplayName() != null ?
                resolve(item.getDefinition().getDisplayName()) : item.getElementName().getLocalPart();
    }

    private List<Item<?, ?>> filterAndOrderItems(Collection<Item<?, ?>> items, Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<Item<?, ?>> visibleItems = getVisibleItems(items, hiddenPaths, showOperationalAttributes);
        visibleItems.sort((item1, item2) -> DeltaFormatter.compareDisplayOrders(item1.getDefinition(), item2.getDefinition()));
        return visibleItems;
    }

    @NotNull
    private List<Item<?, ?>> getVisibleItems(Collection<Item<?, ?>> items, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes) {
        List<Item<?, ?>> visibleItems = new ArrayList<>(items.size());
        List<QName> noDefinition = new ArrayList<>();
        for (Item<?, ?> item : items) {
            if (item.getDefinition() != null) {
                boolean isHidden = TextFormatter.isAmongHiddenPaths(item.getPath(), hiddenPaths);
                if (!isHidden && (showOperationalAttributes || !item.getDefinition().isOperational()) && !item.isEmpty()) {
                    visibleItems.add(item);
                }
            } else {
                noDefinition.add(item.getElementName());
            }
        }
        if (!noDefinition.isEmpty()) {
            LOGGER.error("Items {} without definition - THEY WILL NOT BE INCLUDED IN NOTIFICATION.\nAll items:\n{}",
                    noDefinition, DebugUtil.debugDump(items));
        }
        return visibleItems;
    }

    public String formatUserName(SimpleObjectRef ref, OperationResult result) {
        return formatUserName((UserType) ref.resolveObjectType(result, true), ref.getOid());
    }

    public String formatUserName(ObjectReferenceType ref, OperationResult result) {
        UserType user = (UserType) functions.getObject(ref, true, result);
        return formatUserName(user, ref.getOid());
    }

    public String formatUserName(UserType user, String oid) {
        if (user == null || (user.getName() == null && user.getFullName() == null)) {
            return oid;
        }
        if (user.getFullName() != null) {
            return getOrig(user.getFullName()) + " (" + getOrig(user.getName()) + ")";
        } else {
            return getOrig(user.getName());
        }
    }

    // TODO implement seriously
    public String formatDateTime(XMLGregorianCalendar timestamp) {
        //DateFormatUtils.format(timestamp.toGregorianCalendar(), DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern());
        return String.valueOf(XmlTypeConverter.toDate(timestamp));
    }
}
