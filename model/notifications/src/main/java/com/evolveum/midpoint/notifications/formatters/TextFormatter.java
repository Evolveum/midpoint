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

package com.evolveum.midpoint.notifications.formatters;

import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
@Component
public class TextFormatter {

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    private static final Trace LOGGER = TraceManager.getTrace(TextFormatter.class);

    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths) {

        Validate.notNull(objectDelta, "objectDelta is null");
        Validate.isTrue(objectDelta.isModify(), "objectDelta is not a modification delta");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("formatObjectModificationDelta: objectDelta = " + objectDelta.debugDump() + ", hiddenPaths = " + PrettyPrinter.prettyPrint(hiddenPaths));
        }

        StringBuilder retval = new StringBuilder();

        List<ItemDelta> toBeDisplayed = filterAndOrderItemDeltas(objectDelta, hiddenPaths);

        for (ItemDelta itemDelta : toBeDisplayed) {
            retval.append(" - ");
            retval.append(getItemDeltaLabel(itemDelta));
            retval.append(":\n");
            formatItemDeltaContent(retval, itemDelta);
        }

        return retval.toString();
    }

    public String formatAccountAttributes(ShadowType shadowType) {
        Validate.notNull(shadowType, "shadowType is null");

        StringBuilder retval = new StringBuilder();
        formatContainerValue(retval, "", shadowType.getAttributes().asPrismContainerValue(), false);
        return retval.toString();
    }

    public String formatObject(PrismObject object) {

        Validate.notNull(object, "object is null");

        StringBuilder retval = new StringBuilder();
        formatContainerValue(retval, "", object.getValue(), false);
        return retval.toString();
    }


    private void formatItemDeltaContent(StringBuilder sb, ItemDelta itemDelta) {
        formatItemDeltaValues(sb, "ADD", itemDelta.getValuesToAdd(), false);
        formatItemDeltaValues(sb, "DELETE", itemDelta.getValuesToDelete(), true);
        formatItemDeltaValues(sb, "REPLACE", itemDelta.getValuesToReplace(), false);
    }

    private void formatItemDeltaValues(StringBuilder sb, String type, Collection<? extends PrismValue> values, boolean mightBeRemoved) {
        if (values != null) {
            for (PrismValue prismValue : values) {
                sb.append("   - " + type + ": ");
                String prefix = "     ";
                formatPrismValue(sb, prefix, prismValue, mightBeRemoved);
                sb.append("\n");
            }
        }
    }

    private void formatPrismValue(StringBuilder sb, String prefix, PrismValue prismValue, boolean mightBeRemoved) {
        if (prismValue instanceof PrismPropertyValue) {
            sb.append(ValueDisplayUtil.toStringValue((PrismPropertyValue) prismValue));
        } else if (prismValue instanceof PrismReferenceValue) {
            sb.append(formatReferenceValue((PrismReferenceValue) prismValue, mightBeRemoved));
        } else if (prismValue instanceof PrismContainerValue) {
            sb.append("\n");
            formatContainerValue(sb, prefix, (PrismContainerValue) prismValue, mightBeRemoved);
        } else {
            sb.append("Unexpected PrismValue type: ");
            sb.append(prismValue);
            LOGGER.error("Unexpected PrismValue type: " + prismValue.getClass() + ": " + prismValue);
        }
    }

    private void formatContainerValue(StringBuilder sb, String prefix, PrismContainerValue containerValue, boolean mightBeRemoved) {
//        sb.append("Container of type " + containerValue.getParent().getDefinition().getTypeName());
//        sb.append("\n");

        List<Item> toBeDisplayed = filterAndOrderItems(containerValue.getItems());

        for (Item item : toBeDisplayed) {
            sb.append(prefix);
            sb.append(" - ");
            sb.append(getItemLabel(item));
            sb.append(": ");
            if (item instanceof PrismProperty) {
                if (item.size() > 1) {
                    for (PrismPropertyValue propertyValue : ((PrismProperty<? extends Object>) item).getValues()) {
                        sb.append("\n");
                        sb.append(prefix + "   - ");
                        sb.append(ValueDisplayUtil.toStringValue(propertyValue));
                    }
                } else if (item.size() == 1) {
                    sb.append(ValueDisplayUtil.toStringValue(((PrismProperty<? extends Object>) item).getValue(0)));
                }
                sb.append("\n");
            } else if (item instanceof PrismReference) {
                if (item.size() > 1) {
                    for (PrismReferenceValue referenceValue : ((PrismReference) item).getValues()) {
                        sb.append("\n");
                        sb.append(prefix + "   - ");
                        sb.append(formatReferenceValue(referenceValue, mightBeRemoved));
                    }
                } else if (item.size() == 1) {
                    sb.append(formatReferenceValue(((PrismReference) item).getValue(0), mightBeRemoved));
                }
                sb.append("\n");
            } else if (item instanceof PrismContainer) {
                for (PrismContainerValue subContainerValue : ((PrismContainer<? extends Containerable>) item).getValues()) {
                    sb.append("\n");
                    String prefixSubContainer = prefix + "   ";
                    formatContainerValue(sb, prefixSubContainer, subContainerValue, mightBeRemoved);
                }
            } else {
                sb.append("Unexpected Item type: ");
                sb.append(item);
                sb.append("\n");
                LOGGER.error("Unexpected Item type: " + item.getClass() + ": " + item);
            }
        }
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

        if (object != null) {
            return PolyString.getOrig(object.asObjectable().getName()) + qualifier;
//                    + " ("
//                    + localPartOfType(object)
//                    + ", oid "
//                    + object.getOid()
//                    + ")";
        } else {
            if (mightBeRemoved) {
                return "(cannot display the name of " + localPart(value.getTargetType()) + ":" + value.getOid() + ", as it might be already removed)";
            } else {
                return localPart(value.getTargetType()) + ":" + value.getOid();
            }
        }
    }

    private PrismObject<? extends ObjectType> getPrismObject(String oid, boolean mightBeRemoved, OperationResult result) {
        try {
            return cacheRepositoryService.getObject(ObjectType.class, oid, result);
        } catch (ObjectNotFoundException e) {
            if (!mightBeRemoved) {
                LoggingUtils.logException(LOGGER, "Couldn't resolve reference when displaying object name within a notification (it might be already removed)", e);
            } else {
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't resolve reference when displaying object name within a notification", e);
        }
        return null;
    }

    private String localPartOfType(Item item) {
        if (item.getDefinition() != null) {
            return localPart(item.getDefinition().getTypeName());
        } else {
            return null;
        }
    }

    private String localPart(QName qname) {
        return qname == null ? null : qname.getLocalPart();
    }

    // we call this on filtered list of item deltas - all of they have definition set
    private String getItemDeltaLabel(ItemDelta itemDelta) {
        if (itemDelta.getDefinition().getDisplayName() != null) {
            return itemDelta.getDefinition().getDisplayName();
        } else {
            String retval = "";
            for (ItemPathSegment segment : itemDelta.getPath().getSegments()) {
                 if (segment instanceof NameItemPathSegment) {
                     if (!retval.isEmpty()) {
                         retval += "/";
                     }
                     retval += ((NameItemPathSegment) segment).getName().getLocalPart();
                 }
            }
            return retval;
        }
    }

    private List<ItemDelta> filterAndOrderItemDeltas(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths) {
        List<ItemDelta> toBeDisplayed = new ArrayList<ItemDelta>(objectDelta.getModifications().size());
        for (ItemDelta itemDelta: objectDelta.getModifications()) {
            if (itemDelta.getDefinition() != null) {
                if (!itemDelta.getDefinition().isOperational() && !NotificationsUtil.isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
                    toBeDisplayed.add(itemDelta);
                }
            } else {
                LOGGER.error("ItemDelta " + itemDelta.getName() + " without definition - WILL NOT BE INCLUDED IN NOTIFICATION. In " + objectDelta);
            }
        }
        Collections.sort(toBeDisplayed, new Comparator<ItemDelta>() {
            @Override
            public int compare(ItemDelta delta1, ItemDelta delta2) {
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
            }
        });
        return toBeDisplayed;
    }

    // we call this on filtered list of items - all of they have definition set
    private String getItemLabel(Item item) {
        return item.getDefinition().getDisplayName() != null ?
                item.getDefinition().getDisplayName() : item.getName().getLocalPart();
    }

    private List<Item> filterAndOrderItems(List<Item> items) {
        List<Item> toBeDisplayed = new ArrayList<Item>(items.size());
        for (Item item : items) {
            if (item.getDefinition() != null) {
                if (!item.getDefinition().isOperational()) {
                    toBeDisplayed.add(item);
                }
            } else {
                LOGGER.error("Item " + item.getName() + " without definition - WILL NOT BE INCLUDED IN NOTIFICATION.");
            }
        }
        Collections.sort(toBeDisplayed, new Comparator<Item>() {
            @Override
            public int compare(Item item1, Item item2) {
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
            }
        });
        return toBeDisplayed;
    }


}
