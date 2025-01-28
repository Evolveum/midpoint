/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Utility class that can be used to process objects and remove unwanted items.
 * By default, it removes all operational items and all items marked with optionalCleanup.
 *
 * This behaviour can be configured via {@link ObjectCleaner#removeAskActionItemsByDefault}
 * and {@link ObjectCleaner#setPaths(List)}.
 */
public class ObjectCleaner {

    private CleanerListener listener;

    private boolean removeAskActionItemsByDefault = true;

    private boolean ignoreNamespaces;

    private boolean removeContainerIds;

    private boolean removeObjectVersion;

    private boolean removeMetadata;

    private final Map<QName, Map<ItemPath, CleanupPathAction>> paths = new HashMap<>();

    public boolean isRemoveContainerIds() {
        return removeContainerIds;
    }

    public void setRemoveContainerIds(boolean removeContainerIds) {
        this.removeContainerIds = removeContainerIds;
    }

    public boolean isRemoveMetadata() {
        return removeMetadata;
    }

    public void setRemoveMetadata(boolean removeMetadata) {
        this.removeMetadata = removeMetadata;
    }

    public boolean isRemoveObjectVersion() {
        return removeObjectVersion;
    }

    public void setRemoveObjectVersion(boolean removeObjectVersion) {
        this.removeObjectVersion = removeObjectVersion;
    }

    public void setListener(CleanerListener listener) {
        this.listener = listener;
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

    public CleanupResult process(@NotNull PrismContainerValue<?> containerValue) {
        CleanupResult result = new CleanupResult();

        processContainerValue(containerValue, ItemPath.EMPTY_PATH, new HashMap<>(), containerValue, result);

        return result;
    }

    private boolean processItemRecursively(
            Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions,
            Object source, CleanupResult result) {

        boolean remove = processItem(item, currentPath, customItemActions, source, result);
        if (remove) {
            return true;
        }

        if (!item.isEmpty() && item.getDefinition() != null) {
            if (item instanceof PrismReference) {
                fireReferenceCleanup(
                        createEvent(source, currentPath, (PrismReference) item, result));
            }
        }

        if (item instanceof PrismContainer<?> pc) {
            boolean emptyBefore = pc.hasNoValues();

            for (PrismContainerValue<?> value : pc.getValues()) {
                processContainerValue(value, currentPath, customItemActions, source, result);
            }

            return !emptyBefore && item.hasNoValues();
        }

        return false;
    }

    private void processContainerValue(
            PrismContainerValue<?> value, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions,
            Object source, CleanupResult result) {

        List<Item<?, ?>> toBeRemoved = new ArrayList<>();

        if (removeContainerIds) {
            value.setId(null);
        }

        if (removeObjectVersion && (value instanceof PrismObjectValue<?> pov)) {
            pov.setVersion(null);
        }

        Collection<Item<?, ?>> items = value.getItems();
        for (Item<?, ?> i : items) {
            if (processItemRecursively(i, currentPath.append(i.getElementName()), customItemActions, source, result)) {
                toBeRemoved.add(i);
            }
        }

        items.removeAll(toBeRemoved);
    }

    /**
     * @return true if item should be removed, false otherwise
     */
    private boolean processItem(
            Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions,
            Object source, CleanupResult result) {

        Boolean remove = fireItemCleanup(createEvent(source, currentPath, item, result));
        if (BooleanUtils.isTrue(remove)) {
            return true;
        } else if (BooleanUtils.isFalse(remove)) {
            return false;
        }

        if (removeMetadata) {
            List<? extends PrismValue> values = item.getValues();
            if (values != null) {
                values.forEach(v -> v.deleteValueMetadata());
            }
        }

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

    private <T> CleanupEvent<T> createEvent(Object source, ItemPath path, T item, CleanupResult result) {
        return new CleanupEvent<>(source, path, item, result);
    }

    private Boolean fireItemCleanup(CleanupEvent<Item<?, ?>> event) {
        if (listener == null) {
            return null;
        }

        return listener.onItemCleanup(event);
    }

    private void fireReferenceCleanup(CleanupEvent<PrismReference> event) {
        if (listener == null) {
            return;
        }

        listener.onReferenceCleanup(event);
    }

    private boolean fireConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        if (listener == null) {
            return removeAskActionItemsByDefault;
        }

        return listener.onConfirmOptionalCleanup(event);
    }
}
