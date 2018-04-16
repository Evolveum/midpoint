/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.*;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * @author mederly
 */
@Component
public class TextFormatter {

    @Autowired @Qualifier("cacheRepositoryService") private transient RepositoryService cacheRepositoryService;
    @Autowired protected NotificationFunctionsImpl functions;

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
			SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH);

    private static final Trace LOGGER = TraceManager.getTrace(TextFormatter.class);

    @SuppressWarnings("unused")
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        return formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, null, null);
    }

    // objectOld and objectNew are used for explaining changed container values, e.g. assignment[1]/tenantRef (see MID-2047)
    // if null, they are ignored
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths, boolean showOperationalAttributes,
                                                PrismObject objectOld, PrismObject objectNew) {
        Validate.notNull(objectDelta, "objectDelta is null");
        Validate.isTrue(objectDelta.isModify(), "objectDelta is not a modification delta");

        PrismObjectDefinition objectDefinition;
        if (objectNew != null && objectNew.getDefinition() != null) {
            objectDefinition = objectNew.getDefinition();
        } else if (objectOld != null && objectOld.getDefinition() != null) {
            objectDefinition = objectOld.getDefinition();
        } else {
            objectDefinition = null;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("formatObjectModificationDelta: objectDelta = " + objectDelta.debugDump() + ", hiddenPaths = " + PrettyPrinter.prettyPrint(hiddenPaths));
        }

        StringBuilder retval = new StringBuilder();

        List<ItemDelta> toBeDisplayed = filterAndOrderItemDeltas(objectDelta, hiddenPaths, showOperationalAttributes);
        for (ItemDelta itemDelta : toBeDisplayed) {
            retval.append(" - ");
            retval.append(getItemDeltaLabel(itemDelta, objectDefinition));
            retval.append(":\n");
            formatItemDeltaContent(retval, itemDelta, objectOld, hiddenPaths, showOperationalAttributes);
        }

        explainPaths(retval, toBeDisplayed, objectDefinition, objectOld, objectNew, hiddenPaths, showOperationalAttributes);

        return retval.toString();
    }

    private void explainPaths(StringBuilder sb, List<ItemDelta> deltas, PrismObjectDefinition objectDefinition, PrismObject objectOld, PrismObject objectNew, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (objectOld == null && objectNew == null) {
            return; // no data - no point in trying
        }
        boolean first = true;
        List<ItemPath> alreadyExplained = new ArrayList<>();
        for (ItemDelta itemDelta : deltas) {
            ItemPath pathToExplain = getPathToExplain(itemDelta);
            if (pathToExplain == null || ItemPath.containsSubpathOrEquivalent(alreadyExplained, pathToExplain)) {
                continue;       // null or already processed
            }
            PrismObject source = null;
            Object item = null;
            if (objectNew != null) {
                item = objectNew.find(pathToExplain);
                source = objectNew;
            }
            if (item == null && objectOld != null) {
                item = objectOld.find(pathToExplain);
                source = objectOld;
            }
            if (item == null) {
                LOGGER.warn("Couldn't find {} in {} nor {}, no explanation could be created.", pathToExplain, objectNew, objectOld);
                continue;
            }
            if (first) {
                sb.append("\nNotes:\n");
                first = false;
            }
            String label = getItemPathLabel(pathToExplain, itemDelta.getDefinition(), objectDefinition);
            // the item should be a PrismContainerValue
            if (item instanceof PrismContainerValue) {
                sb.append(" - ").append(label).append(":\n");
                formatContainerValue(sb, "   ", (PrismContainerValue) item, false, hiddenPaths, showOperationalAttributes);
            } else {
                LOGGER.warn("{} in {} was expected to be a PrismContainerValue; it is {} instead", pathToExplain, source, item.getClass());
                if (item instanceof PrismContainer) {
                    formatPrismContainer(sb, "   ", (PrismContainer) item, false, hiddenPaths, showOperationalAttributes);
                } else if (item instanceof PrismReference) {
                    formatPrismReference(sb, "   ", (PrismReference) item, false);
                } else if (item instanceof PrismProperty) {
                    formatPrismProperty(sb, "   ", (PrismProperty) item);
                } else {
                    sb.append("Unexpected item: ").append(item).append("\n");
                }
            }
            alreadyExplained.add(pathToExplain);
        }
    }

    private void formatItemDeltaContent(StringBuilder sb, ItemDelta itemDelta, PrismObject objectOld,
            List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        formatItemDeltaValues(sb, "ADD", itemDelta.getValuesToAdd(), false, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
        formatItemDeltaValues(sb, "DELETE", itemDelta.getValuesToDelete(), true, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
        formatItemDeltaValues(sb, "REPLACE", itemDelta.getValuesToReplace(), false, itemDelta.getPath(), objectOld, hiddenPaths, showOperationalAttributes);
    }

    private void formatItemDeltaValues(StringBuilder sb, String type, Collection<? extends PrismValue> values,
            boolean isDelete, ItemPath path, PrismObject objectOld,
            List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (values != null) {
            for (PrismValue prismValue : values) {
                sb.append("   - ").append(type).append(": ");
                String prefix = "     ";
                if (isDelete && prismValue instanceof PrismContainerValue) {
                    prismValue = fixEmptyContainerValue((PrismContainerValue) prismValue, path, objectOld);
                }
                formatPrismValue(sb, prefix, prismValue, isDelete, hiddenPaths, showOperationalAttributes);
                if (!(prismValue instanceof PrismContainerValue)) {         // container values already end with newline
                    sb.append("\n");
                }
            }
        }
    }

    private PrismValue fixEmptyContainerValue(PrismContainerValue pcv, ItemPath path, PrismObject objectOld) {
        if (pcv.getId() == null || CollectionUtils.isNotEmpty(pcv.getItems())) {
            return pcv;
        }
        PrismContainer oldContainer = objectOld.findContainer(path);
        if (oldContainer == null) {
            return pcv;
        }
        PrismContainerValue oldValue = oldContainer.getValue(pcv.getId());
        return oldValue != null ? oldValue : pcv;
    }

    // todo - should each hiddenAttribute be prefixed with something like F_ATTRIBUTE? Currently it should not be.
    public String formatAccountAttributes(ShadowType shadowType, List<ItemPath> hiddenAttributes, boolean showOperationalAttributes) {
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

    public String formatObject(PrismObject object, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {

        Validate.notNull(object, "object is null");

        StringBuilder retval = new StringBuilder();
        formatContainerValue(retval, "", object.getValue(), false, hiddenPaths, showOperationalAttributes);
        return retval.toString();
    }

    private void formatPrismValue(StringBuilder sb, String prefix, PrismValue prismValue, boolean mightBeRemoved, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (prismValue instanceof PrismPropertyValue) {
            sb.append(ValueDisplayUtil.toStringValue((PrismPropertyValue) prismValue));
        } else if (prismValue instanceof PrismReferenceValue) {
            sb.append(formatReferenceValue((PrismReferenceValue) prismValue, mightBeRemoved));
        } else if (prismValue instanceof PrismContainerValue) {
            sb.append("\n");
            formatContainerValue(sb, prefix, (PrismContainerValue) prismValue, mightBeRemoved, hiddenPaths, showOperationalAttributes);
        } else {
            sb.append("Unexpected PrismValue type: ");
            sb.append(prismValue);
            LOGGER.error("Unexpected PrismValue type: " + prismValue.getClass() + ": " + prismValue);
        }
    }

    private void formatContainerValue(StringBuilder sb, String prefix, PrismContainerValue containerValue, boolean mightBeRemoved, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
//        sb.append("Container of type " + containerValue.getParent().getDefinition().getTypeName());
//        sb.append("\n");

        List<Item> toBeDisplayed = filterAndOrderItems(containerValue.getItems(), hiddenPaths, showOperationalAttributes);

        for (Item item : toBeDisplayed) {
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

    private void formatPrismContainer(StringBuilder sb, String prefix, Item item, boolean mightBeRemoved, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        for (PrismContainerValue subContainerValue : ((PrismContainer<? extends Containerable>) item).getValues()) {
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
                sb.append(valueSb.toString());
            }
        }
    }

    private void formatPrismReference(StringBuilder sb, String prefix, Item item, boolean mightBeRemoved) {
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
            sb.append(formatReferenceValue(((PrismReference) item).getValue(0), mightBeRemoved));
        }
        sb.append("\n");
    }

    private void formatPrismProperty(StringBuilder sb, String prefix, Item item) {
        sb.append(prefix);
        sb.append(" - ");
        sb.append(getItemLabel(item));
        sb.append(": ");
        if (item.size() > 1) {
            for (PrismPropertyValue propertyValue : ((PrismProperty<?>) item).getValues()) {
                sb.append("\n");
                sb.append(prefix).append("   - ");
                sb.append(ValueDisplayUtil.toStringValue(propertyValue));
            }
        } else if (item.size() == 1) {
            sb.append(ValueDisplayUtil.toStringValue(((PrismProperty<?>) item).getValue(0)));
        }
        sb.append("\n");
    }

    private String formatReferenceValue(PrismReferenceValue value, boolean mightBeRemoved) {

        OperationResult result = new OperationResult("dummy");

        PrismObject<? extends ObjectType> object = value.getObject();

        if (object == null) {
            object = getPrismObject(value.getOid(), mightBeRemoved, result);
        }

        String qualifier = "";
        if (object != null && object.asObjectable() instanceof ShadowType) {
            ShadowType shadowType = (ShadowType) object.asObjectable();
            ResourceType resourceType = shadowType.getResource();
            if (resourceType == null) {
                PrismObject<? extends ObjectType> resource = getPrismObject(shadowType.getResourceRef().getOid(), false, result);
                if (resource != null) {
                    resourceType = (ResourceType) resource.asObjectable();
                }
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

    private PrismObject<? extends ObjectType> getPrismObject(String oid, boolean mightBeRemoved, OperationResult result) {
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
			return cacheRepositoryService.getObject(ObjectType.class, oid, options, result);
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

    // we call this on filtered list of item deltas - all of they have definition set
    private String getItemDeltaLabel(ItemDelta itemDelta, PrismObjectDefinition objectDefinition) {
        return getItemPathLabel(itemDelta.getPath(), itemDelta.getDefinition(), objectDefinition);
    }

    private String getItemPathLabel(ItemPath path, Definition deltaDefinition, PrismObjectDefinition objectDefinition) {

        NameItemPathSegment lastNamedSegment = path.lastNamed();

        StringBuilder sb = new StringBuilder();
        for (ItemPathSegment segment : path.getSegments()) {
            if (segment instanceof NameItemPathSegment) {
                if (sb.length() > 0) {
                    sb.append("/");
                }
                Definition itemDefinition;
                if (objectDefinition == null) {
                    if (segment == lastNamedSegment) {  // definition for last segment is the definition taken from delta
                        itemDefinition = deltaDefinition;    // this may be null but we don't care
                    } else {
                        itemDefinition = null;          // definitions for previous segments are unknown
                    }
                } else {
                    // todo we could make this iterative (resolving definitions while walking down the path); but this is definitely simpler to implement and debug :)
                    itemDefinition = objectDefinition.findItemDefinition(path.allUpToIncluding(segment));
                }
                if (itemDefinition != null && itemDefinition.getDisplayName() != null) {
                    sb.append(resolve(itemDefinition.getDisplayName()));
                } else {
                    sb.append(((NameItemPathSegment) segment).getName().getLocalPart());
                }
            } else if (segment instanceof IdItemPathSegment) {
                sb.append("[").append(((IdItemPathSegment) segment).getId()).append("]");
            }
        }
        return sb.toString();
    }

	private String resolve(String key) {
		if (key != null && RESOURCE_BUNDLE.containsKey(key)) {
			return RESOURCE_BUNDLE.getString(key);
		} else {
			return key;
		}
	}

	// we call this on filtered list of item deltas - all of they have definition set
    private ItemPath getPathToExplain(ItemDelta itemDelta) {
        ItemPath path = itemDelta.getPath();

        for (int i = 0; i < path.size(); i++) {
            ItemPathSegment segment = path.getSegments().get(i);
            if (segment instanceof IdItemPathSegment) {
                if (i < path.size()-1 || itemDelta.isDelete()) {
                    return path.allUpToIncluding(i);
                } else {
                    // this means that the path ends with [id] segment *and* the value(s) are
                    // only added and deleted, i.e. they are shown in the delta anyway
                    // (actually it is questionable whether path in delta can end with [id] segment,
                    // but we test for this case just to be sure)
                    return null;
                }
            }
        }
        return null;
    }

    private List<ItemDelta> filterAndOrderItemDeltas(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        List<ItemDelta> toBeDisplayed = new ArrayList<>(objectDelta.getModifications().size());
        List<QName> noDefinition = new ArrayList<>();
        for (ItemDelta itemDelta: objectDelta.getModifications()) {
            if (itemDelta.getDefinition() != null) {
                if ((showOperationalAttributes || !itemDelta.getDefinition().isOperational()) && !NotificationFunctionsImpl
						.isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
                    toBeDisplayed.add(itemDelta);
                }
            } else {
                noDefinition.add(itemDelta.getElementName());
            }
        }
		if (!noDefinition.isEmpty()) {
			LOGGER.error("ItemDeltas for {} without definition - WILL NOT BE INCLUDED IN NOTIFICATION. Containing object delta:\n{}",
					noDefinition, objectDelta.debugDump());
		}
        toBeDisplayed.sort((delta1, delta2) -> {
            Integer order1 = delta1.getDefinition().getDisplayOrder();
            Integer order2 = delta2.getDefinition().getDisplayOrder();
            if (order1 != null && order2 != null) {
                return order1 - order2;
            } else if (order1 == null && order2 == null) {
                return 0;
            } else if (order1 == null) {
                return 1;
            } else {
                return -1;
            }
        });
        return toBeDisplayed;
    }

    // we call this on filtered list of items - all of them have definition set
    private String getItemLabel(Item item) {
        return item.getDefinition().getDisplayName() != null ?
                resolve(item.getDefinition().getDisplayName()) : item.getElementName().getLocalPart();
    }

    private List<Item> filterAndOrderItems(List<Item> items, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<Item> toBeDisplayed = new ArrayList<>(items.size());
        List<QName> noDefinition = new ArrayList<>();
        for (Item item : items) {
            if (item.getDefinition() != null) {
                boolean isHidden = NotificationFunctionsImpl.isAmongHiddenPaths(item.getPath(), hiddenPaths);
                if (!isHidden && (showOperationalAttributes || !item.getDefinition().isOperational()) && !item.isEmpty()) {
                    toBeDisplayed.add(item);
                }
            } else {
				noDefinition.add(item.getElementName());
            }
        }
		if (!noDefinition.isEmpty()) {
			LOGGER.error("Items {} without definition - THEY WILL NOT BE INCLUDED IN NOTIFICATION.\nAll items:\n{}",
					noDefinition, DebugUtil.debugDump(items));
		}
        toBeDisplayed.sort((item1, item2) -> {
            Integer order1 = item1.getDefinition().getDisplayOrder();
            Integer order2 = item2.getDefinition().getDisplayOrder();
            if (order1 != null && order2 != null) {
                return order1 - order2;
            } else if (order1 == null && order2 == null) {
                return 0;
            } else if (order1 == null) {
                return 1;
            } else {
                return -1;
            }
        });
        return toBeDisplayed;
    }

    public String formatUserName(SimpleObjectRef ref, OperationResult result) {
        return formatUserName((UserType) ref.resolveObjectType(result, true), ref.getOid());
    }

    public String formatUserName(ObjectReferenceType ref, OperationResult result) {
        UserType user = (UserType) functions.getObjectType(ref, true, result);
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
