/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * TODO merge with {@link com.evolveum.midpoint.schema.validator.ObjectValidator}
 *  also see {@link com.evolveum.midpoint.schema.validator.ObjectUpgradeValidator}
 *
 * Utility class that can be used to process objects and remove unwanted items.
 * By default, it removes all operational items and all items marked with optionalCleanup.
 *
 * This behaviour can be configured via {@link CleanupActionProcessor#removeAskActionItemsByDefault}
 * and {@link CleanupActionProcessor#setPaths(List)}.
 */
public class CleanupActionProcessor {

    private CleanupHandler handler;

    private boolean removeAskActionItemsByDefault = true;

    private boolean ignoreNamespaces;

    private boolean removeContainerIds;

    private final Map<QName, Map<ItemPath, CleanupPathAction>> paths = new HashMap<>();

    public boolean isRemoveContainerIds() {
        return removeContainerIds;
    }

    public void setRemoveContainerIds(boolean removeContainerIds) {
        this.removeContainerIds = removeContainerIds;
    }

    public void setHandler(CleanupHandler handler) {
        this.handler = handler;
    }

    public void setPaths(List<CleanupPath> paths) {
        if (paths == null) {
            paths = new ArrayList<>();
        }

        this.paths.clear();
        for (CleanupPath path : paths) {
            Map<ItemPath, CleanupPathAction> actions = this.paths.computeIfAbsent(path.getType(), k -> new HashMap<>());
            actions.put(path.getPath(), path.getAction());
        }
    }

    /**
     * If set to true, items marked with annotation "optionalCleanup"
     * (e.g. {@link ItemDefinition#isOptionalCleanup()} is true) will be removed.
     *
     * @param removeAskActionItemsByDefault
     */
    public void setRemoveAskActionItemsByDefault(boolean removeAskActionItemsByDefault) {
        this.removeAskActionItemsByDefault = removeAskActionItemsByDefault;
    }

    /**
     * If set to true, namespaces will be ignored when searching for prism items using customized {@link CleanupPath}s.
     *
     * @param ignoreNamespaces
     */
    public void setIgnoreNamespaces(boolean ignoreNamespaces) {
        this.ignoreNamespaces = ignoreNamespaces;
    }

    /**
     * Processes object (modifies it) and removes unwanted items.
     */
    public CleanupResult process(@NotNull PrismContainer<?> container) {
        CleanupResult result = new CleanupResult();

        processItemRecursively(container, ItemPath.EMPTY_PATH, new HashMap<>(), container, result);

        return result;
    }

    private boolean processItemRecursively(
            Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions,
            PrismContainer<?> source, CleanupResult result) {

        boolean remove = processItem(item, currentPath, customItemActions, source, result);
        if (remove) {
            return true;
        }

        if (!item.isEmpty() && item.getDefinition() != null) {
            ItemDefinition<?> def = item.getDefinition();

            if (item instanceof PrismProperty<?> property) {
                if (ProtectedStringType.COMPLEX_TYPE.equals(def.getTypeName())) {
                    fireProtectedStringCleanup(
                            createEvent(source, currentPath, (PrismProperty<ProtectedStringType>) property, result));
                }
            } else if (item instanceof PrismReference) {
                fireReferenceCleanup(
                        createEvent(source, currentPath, (PrismReference) item, result));
            }
        }

        if (item instanceof PrismContainer<?> pc) {
            boolean emptyBefore = pc.hasNoValues();

            final List<Item<?, ?>> toBeRemoved = new ArrayList<>();

            for (PrismContainerValue<?> value : pc.getValues()) {
                if (removeContainerIds) {
                    value.setId(null);
                }

                Collection<Item<?, ?>> items = value.getItems();
                for (Item<?, ?> i : items) {
                    if (processItemRecursively(i, currentPath.append(i.getElementName()), customItemActions, source, result)) {
                        toBeRemoved.add(i);
                    }
                }

                items.removeAll(toBeRemoved);
            }

            ItemDefinition<?> def = item.getDefinition();
            if (MappingType.COMPLEX_TYPE.equals(def.getTypeName())) {
                fireMissingMappingNameCleanup(
                        createEvent(source, currentPath, (PrismContainer<MappingType>) item, result));
            }

            return !emptyBefore && item.hasNoValues();
        }

        return false;
    }

    /**
     * @return true if item should be removed, false otherwise
     */
    private boolean processItem(
            Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions,
            PrismContainer<?> source, CleanupResult result) {

        final ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            updateCustomItemActions(item, customItemActions, def.getTypeName());
        }

        CleanupPathAction customAction = customItemActions.get(item);
        if (customAction != null) {
            return switch (customAction) {
                case REMOVE -> true;
                case ASK -> fireConfirmOptionalCleanup(createEvent(source, currentPath, item, result));
                default -> false;
            };
        }

        if (def == null) {
            return false;
        }

        if (def.isOptionalCleanup()) {
            return fireConfirmOptionalCleanup(createEvent(source, currentPath, item, result));
        }

        if (def.isOperational()) {
            return true;
        }

        return false;
    }

    private void updateCustomItemActions(Item<?, ?> item, Map<Item<?, ?>, CleanupPathAction> customItemActions, QName type) {
        Map<ItemPath, CleanupPathAction> actions = paths.getOrDefault(type, Map.of());
        if (actions.isEmpty()) {
            return;
        }

        actions.forEach((path, action) -> {
            if (path.isEmpty()) {
                customItemActions.put(item, action);
                return;
            }

            List<Item<?, ?>> foundItems = findItems(item, path);
            foundItems.forEach(i -> customItemActions.put(i, action));
        });
    }

    private List<Item<?, ?>> findItems(Item<?, ?> parent, ItemPath named) {
        List<Item<?, ?>> foundItems = new ArrayList<>();

        if (!(parent instanceof PrismContainer<?> pc)) {
            return foundItems;
        }

        for (PrismContainerValue<?> value : pc.getValues()) {
            findItems(value, named, foundItems);
        }

        return foundItems;
    }

    private void findItems(PrismContainerValue<?> parent, ItemPath named, List<Item<?, ?>> foundItems) {
        if (named.isEmpty()) {
            return;
        }

        ItemName first = named.firstToName();
        ItemPath rest = named.rest();

        List<Item<?, ?>> currentlyFoundItems = new ArrayList<>();
        if (ignoreNamespaces) {
            for (Item<?, ?> item : parent.getItems()) {
                if (Objects.equals(first.getLocalPart(), item.getElementName().getLocalPart())) {
                    currentlyFoundItems.add(item);
                }
            }
        } else {
            Item<?, ?> item = parent.findItem(first);
            currentlyFoundItems.add(item);
        }

        if (rest.isEmpty()) {
            foundItems.addAll(currentlyFoundItems);

            return;
        }

        for (Item<?, ?> item : currentlyFoundItems) {
            if (!(item instanceof PrismContainer<?> pc)) {
                return;
            }

            for (PrismContainerValue<?> value : pc.getValues()) {
                findItems(value, rest, foundItems);
            }
        }
    }

    private <T> CleanupEvent<T> createEvent(PrismContainer<?> object, ItemPath path, T item, CleanupResult result) {
        return new CleanupEvent<>(object, path, item, result);
    }

    private void fireProtectedStringCleanup(CleanupEvent<PrismProperty<ProtectedStringType>> event) {
        if (handler == null) {
            return;
        }

        handler.onProtectedStringCleanup(event);
    }

    private void fireReferenceCleanup(CleanupEvent<PrismReference> event) {
        if (handler == null) {
            return;
        }

        handler.onReferenceCleanup(event);
    }

    private boolean fireConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        if (handler == null) {
            return removeAskActionItemsByDefault;
        }

        return handler.onConfirmOptionalCleanup(event);
    }

    private void fireMissingMappingNameCleanup(CleanupEvent<PrismContainer<MappingType>> event) {
        if (handler == null) {
            return;
        }

        handler.onMissingMappingNameCleanup(event);
    }
}
