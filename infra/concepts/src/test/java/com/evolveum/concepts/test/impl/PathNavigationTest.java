package com.evolveum.concepts.test.impl;

import static org.testng.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.Test;

import com.evolveum.concepts.PathNavigable;
import com.evolveum.concepts.PathNavigation;
import com.evolveum.concepts.PathNavigator;
import com.google.common.collect.ImmutableList;

public class PathNavigationTest {

    public static class Node {

        private final String id;
        private final Map<String, Node> children = new HashMap<>();

        public Node(String string) {
            id = string;
        }

        public Optional<Node> childOptional(String id) {
           return Optional.ofNullable(children.get(id));
        }

        public Node childNullable(String id) {
            return children.get(id);
        }

        public Node child(String id) throws Exception {
            var ret = children.get(id);
            if (ret == null) {
                throw new Exception("Not Found");
            }
            return ret;
        }


        public Node add(Node child) {
            children.put(child.id, child);
            return this;
        }

        public String getId() {
            return id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Node))
                return false;
            Node other = (Node) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }
    }

    private static final String ROOT = "root";
    private static final String CHILDLESS = "childless";
    private static final String CHILDS = "childs";
    private static final String LEAF = "leaf";
    private static final String NESTED = "nested";
    private static final String NESTED_LEAF = "nestedLeaf";
    private static final Integer ROOT_ZERO = 0;

    private static Node TREE = node(ROOT)
            .add(node(CHILDLESS))
            .add(node(CHILDS)
                    .add(node(LEAF))
                    .add(node(NESTED)
                            .add(node(NESTED_LEAF)))
                    );


    private static final Node node(String id) {
        return new Node(id);
    }

    @Test
    public void testSuccessfulNavigation() {
        var nodesZeroToFour = onlyInRange(0, 4);
        assertFound(nodesZeroToFour, PathNavigation.of(ROOT_ZERO), 0);
        assertFound(nodesZeroToFour, PathNavigation.of(ROOT_ZERO, 1, 2, 3, 4), 4);
    }

    @Test
    public void testFailedNavigation() {
        var nodesZeroToTwo = onlyInRange(0, 2);
        var nodesTwoToFour = onlyInRange(2, 4);
        PathNavigation<Integer, Integer> path = PathNavigation.of(ROOT_ZERO, 1, 2, 3, 4);
        path = assertNotFound(nodesZeroToTwo, path, 2);
        assertEquals(path.unresolvedSegments(), ImmutableList.of(3,4));
        assertFound(nodesTwoToFour, path, 4);
    }

    @Test
    public void testFailedAlternatives() {
        var optionalBased = PathNavigator.fromOptionable(Node::childOptional);
        var exceptionBased = PathNavigator.fromFailable(Exception.class, Node::child);

        ImmutableList<String> path = ImmutableList.of(CHILDS, LEAF, "nonExistant");
        var boundPath = PathNavigation.from(TREE, path);
        assertNotFound(boundPath, optionalBased, node(LEAF));
        assertNotFound(boundPath, exceptionBased, node(LEAF));
    }


    private <N,S> void assertNotFound(PathNavigation<N,S> start, PathNavigator<N, S> navigator, N lastFound) {
        var result = start.navigate(navigator);
        assertFalse(result.isFound());
        assertEquals(result.lastFound(), lastFound);
    }

    @Test
    public void usage() {
        var treeNavigator = PathNavigator.from(Node::childNullable);
        PathNavigable<Node, String> root = treeNavigator.toNavigable(TREE);
        ImmutableList<String> path = ImmutableList.of(CHILDS, LEAF);
        PathNavigation<Node, String> nav = root.resolve(path);
        assertEquals(nav.get().id, LEAF);
        treeNavigator.startIn(TREE).resolve(path).get();
    }

    @Test
    public void withGetParent() {
        var treeNavigator = PathNavigator.from(Node::childNullable).withReturnSegment("..");
        PathNavigable<Node, String> root = treeNavigator.toNavigable(TREE);
        List<String> path = ImmutableList.of(CHILDS, LEAF, "..");
        PathNavigation<Node, String> nav = root.resolve(path);
        assertEquals(nav.get().id, CHILDS);
        treeNavigator.startIn(TREE).resolve(path).get();
    }

    @Test
    public void withGetParentOutside() {
        var treeNavigator = PathNavigator.from(Node::childNullable).withReturnSegment("..");
        PathNavigable.Hierarchical<Node, String> root = treeNavigator.toNavigable(TREE, TREE, TREE.childNullable(CHILDLESS));
        List<String> path = ImmutableList.of("..", CHILDS, LEAF, "..", LEAF);
        PathNavigation.Stacked<Node, String> nav = root.resolve(path);
        assertEquals(nav.get().id, LEAF);
        assertEquals(nav.allResolved(), ImmutableList.of(TREE, TREE, node(CHILDS), node(LEAF)));
    }

    private void assertFound(PathNavigator<Integer, Integer> nodes, PathNavigation<Integer, Integer> path, Integer expected) {
        path = path.navigate(nodes);
        assertTrue(path.isFound(), "Path should be found");
        assertEquals(path.get(), expected);
        assertNotNull(path.resolvedSegments());
        assertTrue(path.unresolvedSegments().isEmpty());
    }

    private PathNavigation<Integer, Integer> assertNotFound(PathNavigator<Integer, Integer> nodes, PathNavigation<Integer, Integer> path, Integer expected) {
        path = path.navigate(nodes);
        assertFalse(path.isFound(), "Path should be not found");
        assertEquals(path.lastFound(), expected);
        return path;
    }


    private PathNavigator<Integer, Integer> onlyInRange(int min, int max) {
        return (node,value) -> (min <= node && node < max)? value : null;
    }
}
