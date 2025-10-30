/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * A tile model that represents a row of data rendered using a list of {@link IColumn}s.
 * It extends the basic {@link Tile} functionality with column definitions and layout metadata.
 *
 * @author tchrapovic
 */
public class ColumnTile<T extends Serializable> extends Tile<T> {

    @Serial private static final long serialVersionUID = 1L;

    /** List of columns to render in this tile. */
    @NotNull
    private final List<IColumn<T, String>> columns;

    /** Optional CSS classes for tile layout (Bootstrap-based, etc.). */
    @Nullable
    private String tileCssClass;

    /** Optional header text, if you want to override the default title. */
    @Nullable
    private String header;

    public ColumnTile(T value, @NotNull List<IColumn<T, String>> columns) {
        super(null, null);
        this.columns = columns;
        setValue(value);
    }

    public @NotNull List<IColumn<T, String>> getColumns() {
        return columns;
    }

    public @Nullable String getTileCssClass() {
        return tileCssClass;
    }

    public void setTileCssClass(@Nullable String tileCssClass) {
        this.tileCssClass = tileCssClass;
    }

    public @Nullable String getHeader() {
        return header;
    }

    public void setHeader(@Nullable String header) {
        this.header = header;
    }

    public ColumnTile<T> header(String header) {
        setHeader(header);
        return this;
    }

    public ColumnTile<T> cssClass(String cssClass) {
        setTileCssClass(cssClass);
        return this;
    }

    @Override
    public int compareTo(@NotNull Tile o) {
        if (getTitle() == null) {
            return -1;
        }
        if (o.getTitle() == null) {
            return 1;
        }
        return getTitle().compareToIgnoreCase(o.getTitle());
    }
}
