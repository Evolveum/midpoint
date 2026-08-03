/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.nio.file.Path;
import java.util.List;

/**
 * One generated AsciiDoc page with an output path relative to the documentation output directory.
 */
public final class RenderedAsciiDocPage {

    private final Path relativePath;
    private final List<String> lines;

    public RenderedAsciiDocPage(Path relativePath, List<String> lines) {
        this.relativePath = relativePath;
        this.lines = List.copyOf(lines);
    }

    public Path relativePath() {
        return relativePath;
    }

    public List<String> lines() {
        return lines;
    }
}
