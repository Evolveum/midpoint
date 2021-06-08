/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ext;

import java.util.Objects;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Querydsl "row bean" type related to {@link QExtItem}.
 */
public class MExtItem {

    public Integer id;
    public String itemName;
    public String valueType; // references use ObjectReferenceType#COMPLEX_TYPE
    public MExtItemHolderType holderType;
    public MExtItemCardinality cardinality;

    public static MExtItem of(Integer id, Key key) {
        MExtItem row = new MExtItem();
        row.id = id;
        row.itemName = key.itemName;
        row.valueType = key.valueType;
        row.holderType = key.holderType;
        row.cardinality = key.cardinality;
        return row;
    }

    public Key key() {
        Key key = new Key();
        key.itemName = this.itemName;
        key.valueType = this.valueType;
        key.holderType = this.holderType;
        key.cardinality = this.cardinality;
        return key;
    }

    public static class Key {
        public String itemName;
        public String valueType;
        public MExtItemHolderType holderType;
        public MExtItemCardinality cardinality;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;

            return Objects.equals(itemName, key.itemName)
                    && Objects.equals(valueType, key.valueType)
                    && holderType == key.holderType
                    && cardinality == key.cardinality;
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemName, valueType, holderType, cardinality);
        }
    }

    /** Creates ext item key from item definition and holder type. */
    public static Key keyFrom(ItemDefinition<?> definition, MExtItemHolderType holderType) {
        MExtItem.Key key = new MExtItem.Key();
        key.itemName = QNameUtil.qNameToUri(definition.getItemName());
        key.valueType = QNameUtil.qNameToUri(definition.getTypeName());
        key.cardinality = definition.getMaxOccurs() == 1
                ? MExtItemCardinality.SCALAR : MExtItemCardinality.ARRAY;
        key.holderType = holderType;

        return key;
    }

    @Override
    public String toString() {
        return "MExtItem{" +
                "id=" + id +
                ", itemName=" + itemName +
                ", valueType=" + valueType +
                ", holderType=" + holderType +
                ", cardinality=" + cardinality +
                '}';
    }
}
