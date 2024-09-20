/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Stores all links of given link class.
 */
class LinkStore implements DebugDumpable {

    @NotNull private final LinkClassDefinition linkClassDefinition;

    /** Preliminary implementation. */
    @NotNull private final Set<Link> links = ConcurrentHashMap.newKeySet();

    LinkStore(@NotNull LinkClassDefinition linkClassDefinition) {
        this.linkClassDefinition = linkClassDefinition;
    }

    public void clear() {
        links.clear();
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder("Link store for " + linkClassDefinition.getName(), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Links", links, indent + 1);
        return sb.toString();
    }

    void addLink(DummyObject first, DummyObject second) {
        links.add(new Link(first, second));
    }

    void deleteLink(DummyObject first, DummyObject second) {
        links.removeIf(link -> link.first.equals(first) && link.second.equals(second));
    }

    public @NotNull LinkClassDefinition getLinkClassDefinition() {
        return linkClassDefinition;
    }

    /** Returns objects that should be deleted by cascade. */
    @NotNull Set<DummyObject> removeLinksFor(@NotNull DummyObject deletedObject) {
        boolean cascadeIfFirst = linkClassDefinition.getSecondParticipant().isCascadeDelete();
        boolean cascadeIfSecond = linkClassDefinition.getFirstParticipant().isCascadeDelete();
        Set<DummyObject> toBeDeleted = new HashSet<>();
        for (Iterator<Link> iterator = links.iterator(); iterator.hasNext(); ) {
            Link link = iterator.next();
            if (link.first.equals(deletedObject)) {
                iterator.remove();
                if (cascadeIfFirst) {
                    toBeDeleted.add(link.second);
                }
            } else if (link.second.equals(deletedObject)) {
                iterator.remove();
                if (cascadeIfSecond) {
                    toBeDeleted.add(link.first);
                }
            }
        }
        return toBeDeleted;
    }

    Collection<DummyObject> getLinkedObjects(DummyObject dummyObject, LinkDefinition linkDef) {
        return links.stream()
                .map(link -> link.getLinkedObject(dummyObject, linkDef))
                .filter(Objects::nonNull)
                .toList();
    }

    static class Link {

        @NotNull private final DummyObject first;
        @NotNull private final DummyObject second;

        Link(@NotNull DummyObject first, @NotNull DummyObject second) {
            argCheck(!first.equals(second), "Both objects are the same: %s (currently not supported)", first);
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Link link = (Link) o;
            return Objects.equals(first, link.first) && Objects.equals(second, link.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public String toString() {
            return first + " <-> " + second;
        }

        public boolean matches(@NotNull String id) {
            return id.equals(first.getId()) || id.equals(second.getId());
        }

        public boolean matches(DummyObject dummyObject, LinkDefinition linkDef) {
            if (linkDef.isFirst()) {
                return first.equals(dummyObject);
            } else {
                return second.equals(dummyObject);
            }
        }

        public @Nullable DummyObject getLinkedObject(DummyObject dummyObject, LinkDefinition linkDef) {
            if (linkDef.isFirst()) {
                if (first.equals(dummyObject)) {
                    return second;
                } else {
                    return null;
                }
            } else {
                if (second.equals(dummyObject)) {
                    return first;
                } else {
                    return null;
                }
            }
        }
    }
}
