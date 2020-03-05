package com.evolveum.prism.data;

public interface PrismItem<V extends PrismValue<V>> {

    Iterable<V> allValues();

}
