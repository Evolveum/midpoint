package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;

import com.evolveum.midpoint.schema.query.TypedQuery;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AggregateQuery<T extends Containerable> {


    private final Class<T> root;
    private final PrismContainerDefinition<T> rootDefinition;

    private ObjectFilter filter;
    private final Set<ItemPath> groupBy = new HashSet<>();

    private final List<ObjectOrdering> orderBy = new ArrayList<>();

    private final List<ResultItem> items = new ArrayList<>();

    public AggregateQuery(Class<T> containerableClass, PrismContainerDefinition<T> def) {
        root = containerableClass;
        rootDefinition = def;
    }

    public static  <T extends Containerable> AggregateQuery<T> forType(Class<T> containerableClass) {
        PrismContainerDefinition<T> def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(containerableClass);
        return new AggregateQuery<>(containerableClass, def);
    }

    public AggregateQuery<T> retrieve(ItemName itemPath) {
        var definition = rootDefinition.findItemDefinition(itemPath);
        return select(new Retrieve(itemPath, itemPath, definition));
    }

    public AggregateQuery<T> retrieve(ItemName name, ItemPath itemPath) {
        return select(new Retrieve(name, itemPath, rootDefinition.findItemDefinition(itemPath)));
    }

    public AggregateQuery<T> count(ItemName name, ItemPath path) {
        return select(new Count(name, path));
    }

    private AggregateQuery<T> select(ResultItem retrieve) {
        // TODO: Add checks for name uniqueness and other parts of API contracts
        items.add(retrieve);
        return this;
    }

    /**
     * Sets item path by which aggragate results are grouped by, currently only one item path is supported.
     *
     * @param path
     */
    public void groupBy(ItemPath path) {
        // ItemPath to group by
        groupBy.add(path);
    }

    public Class<T> getRoot() {
        return root;
    }

    public Collection<ResultItem> getItems() {
        return items;
    }

    public Collection<ItemPath> getGroupBy() {
        return groupBy;
    }

    public AggregateQuery<T> retrieveFullObject(ItemName path) {
        return retrieveFullObject(path, path);
    }

    public AggregateQuery<T> retrieveFullObject(ItemName name, ItemPath itemPath) {
        return select(new Dereference(name, itemPath, rootDefinition.findItemDefinition(itemPath)));
    }

    public ResultItem getResultItem(ItemName name) {
        return items.stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);
    }

    public void orderBy(ResultItem resultItem, OrderDirection orderDirection) {
        orderBy.add(new AggregateOrdering(resultItem, orderDirection));

    }

    public AggregateQuery<T> filter(ObjectFilter filter) {
        this.filter = filter;
        return this;
    }

    public AggregateQuery<T> filter(String query) throws SchemaException {
        return filter(TypedQuery.parse(root, query).toObjectQuery().getFilter());
    }

    public List<ObjectOrdering> getOrdering() {
        return orderBy;
    }

    public ObjectFilter getFilter() {
        return filter;
    }

    public abstract static class ResultItem {
        private final ItemName name;

        private final ItemPath path;
        private final ItemDefinition<?> definition;

        public ResultItem(ItemName name, ItemPath path, ItemDefinition<?> definition) {
            this.name = name;
            this.path = path;
            this.definition = definition;
        }

        public ItemPath getPath() {
            return path;
        }

        public ItemName getName() {
            return name;
        }

        public ItemDefinition<?> getDefinition() {
            return definition;
        }
    }

    public static class Retrieve extends ResultItem {

        public Retrieve(ItemName name, ItemPath path, ItemDefinition<?> definition) {
            super(name, path, definition);
        }
    }

    public static class Dereference extends ResultItem {

        public Dereference(ItemName name, ItemPath path, ItemDefinition<?> definition) {
            super(name, path, definition);
        }
    }

    public static class Count extends ResultItem {

        public Count(ItemName name, ItemPath path) {
            super(name, path, null);
        }
    }

    public static class AggregateOrdering implements ObjectOrdering {

        private OrderDirection direction;
        private ResultItem item;

        public AggregateOrdering(ResultItem resultItem, OrderDirection orderDirection) {
            direction = orderDirection;
            item = resultItem;
        }

        @Override
        public ItemPath getOrderBy() {
            return null;
        }

        public ResultItem getItem() {
            return item;
        }

        @Override
        public @Nullable OrderDirection getDirection() {
            return direction;
        }

        @Override
        public boolean equals(Object o, boolean exact) {
            return false;
        }
    }
}
