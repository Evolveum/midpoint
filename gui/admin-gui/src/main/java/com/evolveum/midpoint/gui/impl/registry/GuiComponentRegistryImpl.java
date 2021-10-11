/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class GuiComponentRegistryImpl implements GuiComponentRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(GuiComponentRegistryImpl.class);

    List<GuiComponentFactory> guiComponentFactories = new ArrayList<>();

    Map<QName, Class<?>> wrapperPanels = new HashMap<>();

    List<ItemWrapperFactory<?,?,?>> wrapperFactories = new ArrayList<>();

    @Override
    public void addToRegistry(GuiComponentFactory factory) {
        guiComponentFactories.add(factory);

        Comparator<? super GuiComponentFactory> comparator =
                (f1,f2) -> {

                    Integer f1Order = f1.getOrder();
                    Integer f2Order = f2.getOrder();

                    if (f1Order == null) {
                        if (f2Order != null) {
                            return 1;
                        }
                        return 0;
                    }

                    if (f2Order == null) {
                        if (f1Order != null) {
                            return -1;
                        }
                    }

                    return Integer.compare(f1Order, f2Order);

                };

        guiComponentFactories.sort(comparator);

    }

    public void registerWrapperPanel(QName typeName, Class<?> panelClass) {
        if (wrapperPanels.containsKey(typeName)) {
            if (!panelClass.equals(wrapperPanels.get(typeName))) {
                wrapperPanels.replace(typeName, wrapperPanels.get(typeName), panelClass);
                return;
            }
            return;
        }
        wrapperPanels.put(typeName, panelClass);
    }

    public Class<?> getPanelClass(QName typeName) {
        return wrapperPanels.get(typeName);
    }



    @Override
    public <T> GuiComponentFactory findValuePanelFactory(ItemWrapper itemWrapper) {


        Optional<GuiComponentFactory> opt = guiComponentFactories.stream().filter(f -> f.match(itemWrapper)).findFirst();
        if (!opt.isPresent()) {
            LOGGER.trace("No factory found for {}", itemWrapper.debugDump());
            return null;
        }
        GuiComponentFactory factory = opt.get();
        LOGGER.trace("Found component factory {} for {}", factory, itemWrapper.debugDump());
        return factory;
    }

    public <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> ItemWrapperFactory<IW, VW, PV> findWrapperFactory(ItemDefinition<?> def) {
        Optional<ItemWrapperFactory<IW, VW, PV>> opt = (Optional) wrapperFactories.stream().filter(f -> f.match(def)).findFirst();
        if (!opt.isPresent()) {
            LOGGER.trace("Could not find factory for {}.", def);
            return null;
        }

        ItemWrapperFactory<IW, VW, PV> factory = opt.get();
        LOGGER.trace("Found factory: {}", factory);
        return factory;

    }

    public <O extends ObjectType> PrismObjectWrapperFactory<O> getObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
        return (PrismObjectWrapperFactory) findWrapperFactory(objectDef);
    }

    @Override
    public void addToRegistry(ItemWrapperFactory factory) {
        wrapperFactories.add(factory);

        Comparator<? super ItemWrapperFactory> comparator = (f1, f2) -> {

            Integer f1Order = f1.getOrder();
            Integer f2Order = f2.getOrder();

            if (f1Order == null) {
                if (f2Order != null) {
                    return 1;
                }
                return 0;
            }

            if (f2Order == null) {
                if (f1Order != null) {
                    return -1;
                }
            }

            return Integer.compare(f1Order, f2Order);

        };

        wrapperFactories.sort(comparator);

    }



}
