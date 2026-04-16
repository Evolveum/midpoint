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
 * Tile model for {@link ColumnTileTable}.
 *
 * <p>The tile stores:
 * <ul>
 *   <li><b>O</b> - the primary row object represented by the tile</li>
 *   <li><b>PV</b> - the delegated value type rendered by reusable column definitions</li>
 * </ul>
 *
 * <p>The primary row object must implement {@link ColumnValueProvider}, which guarantees
 * that column rendering can consistently obtain the delegated value used by
 * {@link IColumn} instances.
 *
 * @param <O> primary row object displayed by the tile
 * @param <PV> delegated value type rendered by columns
 */
public class ColumnTile<O extends ColumnValueProvider<PV>, PV extends Serializable> extends Tile<O> {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull
    private final List<IColumn<PV, String>> columns;

    @Nullable
    private String tileCssClass;

    @Nullable
    private String header;

    public ColumnTile(O value, @NotNull List<IColumn<PV, String>> columns) {
        super(null, null);
        this.columns = columns;
        setValue(value);
    }

    public @NotNull List<IColumn<PV, String>> getColumns() {
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

    public ColumnTile<O, PV> header(String header) {
        setHeader(header);
        return this;
    }

    public ColumnTile<O, PV> cssClass(String cssClass) {
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
