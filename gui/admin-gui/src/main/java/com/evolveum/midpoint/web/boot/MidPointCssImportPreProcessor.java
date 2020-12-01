/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import java.util.ArrayList;
import java.util.List;

import ro.isdc.wro.model.resource.processor.impl.css.CssImportPreProcessor;

/**
 * <p>Css import preprocessor used by wro to preprocess imports in less
 * (Less)CssImportPreProcessor doesn't work properly when @import (reference) is used
 * which cases a problems with AdminLTE.less compilation.
 * </p>
 * <p>Another reason for this preprocessor is, that if the import statement doesn't
 * mention the type of the file to be imported (e.g mixin instead of mixin.less),
 * the less file is not found and its content is not added to the compiled css.
 * </p>
 */
public class MidPointCssImportPreProcessor extends CssImportPreProcessor {

    @Override
    protected List<String> findImports(String css) {
        List<String> imports =  new MidPointCssImportInspector(css).findImports();
        List<String> finalizedImports = new ArrayList<>(imports.size());
        for (String i : imports) {
            if (i.endsWith(".less")) {
                finalizedImports.add(i);
            } else {
                finalizedImports.add(i + ".less");
            }

        }
        return finalizedImports;
    }

    @Override
    protected String removeImportStatements(final String cssContent) {
        return new MidPointCssImportInspector(cssContent).removeImportStatements();
    }

}
