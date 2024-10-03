/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.RUtil;

@Ignore
@Entity
@Table(name = "m_ext_item", indexes = {
        @Index(name = "iExtItemDefinition", unique = true, columnList = "itemName, itemType, kind")
})
public class RExtItem {

    private Integer id;
    private String name;
    private String type;
    private RItemKind kind;

    public Key toKey() {
        return new Key(name, type, kind);
    }

    public static class Key {
        public final String name;
        public final String type;
        public final RItemKind kind;

        public Key(String name, String type, RItemKind kind) {
            this.name = name;
            this.type = type;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(name, key.name) &&
                    Objects.equals(type, key.type) &&
                    kind == key.kind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, kind);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", kind=" + kind +
                    '}';
        }
    }

    // required by hibernate
    @SuppressWarnings("unused")
    public RExtItem() {
    }

    private RExtItem(Key key) {
        this.name = key.name;
        this.type = key.type;
        this.kind = key.kind;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    @Column(name = "itemName", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getName() {
        return name;
    }

    @Column(name = "itemType", length = RUtil.COLUMN_LENGTH_QNAME)        // to avoid collisions with reserved words
    public String getType() {
        return type;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RItemKind getKind() {
        return kind;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setKind(RItemKind kind) {
        this.kind = kind;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static RExtItem.Key createKeyFromDefinition(ItemDefinition<?> definition) {
        String name = RUtil.qnameToString(definition.getItemName());
        String type = RUtil.qnameToString(definition.getTypeName());
        RItemKind kind = RItemKind.getTypeFromItemDefinitionClass(definition.getClass());
        return new Key(name, type, kind);
    }

    @NotNull
    public static RExtItem createFromDefinition(ItemDefinition<?> definition) {
        return new RExtItem(createKeyFromDefinition(definition));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RExtItem)) {
            return false;
        }

        RExtItem rExtItem = (RExtItem) o;
        return id.equals(rExtItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RExtItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", kind=" + kind +
                '}';
    }
}
