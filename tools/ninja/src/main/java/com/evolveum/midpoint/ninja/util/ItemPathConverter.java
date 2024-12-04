/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converter and validator for ItemPath objects used in command-line parsing.
 * <p>
 * This class converts string values to ItemPath objects. It supports the usage of namespace prefixes
 * by declaring them, for example:
 * <pre>{@code
 * -ei "declare namespace key = 'namespaceLink'; extension/key:itemName"
 * }</pre>
 */
public class ItemPathConverter implements IStringConverter<ItemPath>, IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null) {
            throw new ParameterException("Unknown value 'null' for option " + name);
        }

        List<ItemPath> itemPaths = getItemPaths(value);

        if (itemPaths.isEmpty()) {
            throw new ParameterException("Unknown value " + value + " for option " + name);
        }

    }

    @Override
    public ItemPath convert(String value) {
        return parseItemPath(value);
    }

    private @NotNull ItemPath parseItemPath(@NotNull String pathString) {
        return ItemPathHolder.parseFromString(pathString, new HashMap<>());
    }

    @NotNull
    private List<ItemPath> getItemPaths(@NotNull String value) {
        List<ItemPath> itemPaths = new ArrayList<>();
        String[] split = value.split(",");
        for (String separateItems : split) {
            ItemPath itemPath = parseItemPath(separateItems);
            itemPaths.add(itemPath);
        }
        return itemPaths;
    }

}
