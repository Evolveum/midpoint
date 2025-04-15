/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

/**
 * This behavior is used for bootstrap tooltips. Just add this behaviour to {@link org.apache.wicket.markup.html.basic.Label}.
 * Label must have title set (e.g. wicket:message="title:YOUR_LOCALIZATION_PROPERTY_KEY").
 *
 * @author lazyman
 */
public class TooltipBehavior extends Behavior {

    @Override
    public void onConfigure(final Component component) {
        component.setOutputMarkupId(true);

        component.add(AttributeModifier.append("data-html", "true"));
        component.add(AttributeModifier.replace("data-toggle", "tooltip"));
        component.add(new AttributeModifier("data-placement", getDataPlacement()) {

            @Override
            protected String newValue(String currentValue, String replacementValue) {
                if (StringUtils.isEmpty(currentValue)) {
                    return replacementValue;
                }
                return currentValue;
            }
        });
    }

    /* added for WCAG tooltip issues 5.2.5, 5.2.6 */
    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        String markupId = component.getMarkupId();
        String js = String.format("""
            (function() {
                var $el = $('#%s');
                $el.attr('tabindex', '0');
                $el.tooltip({ trigger: 'manual' });

                let isHovered = false;
                let isTooltipHovered = false;

                function showTooltip() {
                    $el.tooltip('show');

                    setTimeout(function() {
                        var $tooltip = $('.tooltip');

                        $tooltip.on('mouseenter', function () {
                            isTooltipHovered = true;
                        }).on('mouseleave', function () {
                            isTooltipHovered = false;
                            checkHide();
                        });
                    }, 100);
                }

                function checkHide() {
                    setTimeout(function () {
                        if (!isHovered && !isTooltipHovered && !$el.is(':focus')) {
                            $el.tooltip('hide');
                        }
                    }, 150);
                }

                $el.on('mouseenter focus', function () {
                    isHovered = true;
                    showTooltip();
                });

                $el.on('mouseleave blur', function () {
                    isHovered = false;
                    checkHide();
                });

                $(document).on('keydown', function(e) {
                    if (e.key === "Escape") {
                        $el.tooltip('hide');
                    }
                });

                $(document).on('focusin', function(e) {
                    if (!$(e.target).closest('.tooltip, #%s').length) {
                        $el.tooltip('hide');
                    }
                });
            })();
            """, markupId, markupId);

        response.render(OnDomReadyHeaderItem.forScript(js));
    }

    public String getDataPlacement() {
        return "right";
    }
}
