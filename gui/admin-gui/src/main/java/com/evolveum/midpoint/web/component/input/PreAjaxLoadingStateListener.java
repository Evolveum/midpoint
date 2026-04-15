/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;

public class PreAjaxLoadingStateListener extends AjaxCallListener {

    private final String markupId;
    private final String afterIconClass;
    private final String beforeIconCss;
    private final String loadingText;

    public PreAjaxLoadingStateListener(String markupId,
            String beforeIconCss,
            String afterIconClass,
            String loadingText) {
        this.markupId = markupId;
        this.afterIconClass = afterIconClass;
        this.beforeIconCss = beforeIconCss;
        this.loadingText = loadingText;
    }

    //TODO: move to a common place, for js scripts
    @Override
    public CharSequence getBeforeHandler(Component component) {
        return "var $b = $('#" + markupId + "');" +
                "var $i = $b.find('i');" +
                "$i.removeClass('" + beforeIconCss + "').addClass('" + afterIconClass + "');" +
                "var $text = $b.contents().filter(function(){ return this.nodeType === 3; });" +
                "if ($text.length) { $text[0].nodeValue = ' " + loadingText + "'; }" +
                "$b.prop('disabled', true);";
    }

}
