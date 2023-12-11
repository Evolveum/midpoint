/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cleanup;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Utility class that can be used to process objects and remove unwanted items.
 * By default, it removes all operational items and all items marked with optionalCleanup.
 *
 * This behaviour can be configured via {@link CleanupActionProcessor#removeAskActionItemsByDefault}
 * and {@link CleanupActionProcessor#setPaths(List)}.
 */
public class CleanupActionProcessor {

    private CleanupEventListener listener;

    private boolean removeAskActionItemsByDefault = true;

    private boolean ignoreNamespaces;

    private final Map<QName, Map<ItemPath, CleanupPathAction>> paths = new HashMap<>();

    public void setListener(CleanupEventListener listener) {
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
     *
     * @param object
     * @param <O>
     */
    public <O extends ObjectType> void process(PrismObject<O> object) {
        processItemRecursively(object, ItemPath.EMPTY_PATH, new HashMap<>());
    }

    private boolean processItemRecursively(Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions) {
        boolean remove = processItem(item, currentPath, customItemActions);
        if (remove) {
            return true;
        }

        if (item instanceof PrismContainer<?>) {
            boolean emptyBefore = item.isEmpty();

            final List<Item<?, ?>> toBeRemoved = new ArrayList<>();

            for (PrismContainerValue<?> value : (List<PrismContainerValue<?>>) item.getValues()) {
                Collection<Item<?, ?>> items = value.getItems();
                for (Item<?, ?> i : items) {
                    if (processItemRecursively(i, currentPath.append(i.getElementName()), customItemActions)) {
                        toBeRemoved.add(i);
                    }
                }

                items.removeAll(toBeRemoved);
            }

            return !emptyBefore && item.isEmpty();
        }

        return false;

        // probably nothing to do for PrismProperty, PrismReference
        // todo maybe connectorRef, passwords...
    }

    /**
     * @param item
     * @param currentPath
     * @param customItemActions
     * @return true if item should be removed, false otherwise
     */
    private boolean processItem(Item<?, ?> item, ItemPath currentPath, Map<Item<?, ?>, CleanupPathAction> customItemActions) {
        final ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            updateCustomItemActions(item, customItemActions, def.getTypeName());
        }

        CleanupPathAction customAction = customItemActions.get(item);
        if (customAction != null) {
            return switch (customAction) {
                case REMOVE -> true;
                case ASK -> fireOnCleanupItemEvent(item, currentPath);
                default -> false;
            };
        }

        if (def == null) {
            return false;
        }

        if (def.isOperational()) {
            return true;
        }

        if (def.isOptionalCleanup()) {
            return fireOnCleanupItemEvent(item, currentPath);
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

        if (!(parent instanceof PrismContainer<?>)) {
            return foundItems;
        }

        for (PrismContainerValue<?> value : (List<PrismContainerValue<?>>) parent.getValues()) {
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
            if (currentlyFoundItems != null) {
                foundItems.addAll(currentlyFoundItems);
            }

            return;
        }

        for (Item<?, ?> item : currentlyFoundItems) {
            if (!(item instanceof PrismContainer<?>)) {
                return;
            }

            for (PrismContainerValue<?> value : (List<PrismContainerValue<?>>) item.getValues()) {
                findItems(value, rest, foundItems);
            }
        }
    }

    private boolean fireOnCleanupItemEvent(Item<?, ?> item, ItemPath path) {
        if (listener == null) {
            return removeAskActionItemsByDefault;
        }

        return listener.onCleanupItem(new CleanupEvent(item, path));
    }
}
