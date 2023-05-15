/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import java.text.ParseException;
import java.util.List;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupResourceStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.parser.AbstractMarkupFilter;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

/**
 * Markup filter that uses Spring static content with content hash in URL
 *
 * @link https://docs.spring.io/spring-boot/docs/2.7.3/reference/html/web.html#web.servlet.spring-mvc.static-content
 *
 * Created by Viliam Repan (lazyman).
 */
public class StaticSpringResourcesMarkupFilter extends AbstractMarkupFilter {

    private static final List<TagFilter> FILTERS = List.of(
            new TagFilter("script", "src"),
            new TagFilter("link", "href")
    );

    private final ResourceUrlProvider resourceUrlProvider;

    public StaticSpringResourcesMarkupFilter(MarkupResourceStream markupResourceStream, ResourceUrlProvider resourceUrlProvider) {
        super(markupResourceStream);

        this.resourceUrlProvider = resourceUrlProvider;
    }

    @Override
    protected MarkupElement onComponentTag(ComponentTag tag) throws ParseException {
        if (tag.isClose()) {
            return tag;
        }

        String wicketIdAttr = getWicketNamespace() + ":" + "id";

        // Don't touch any wicket:id component and any auto-components
        if (tag instanceof WicketTag || tag.isAutolinkEnabled() || tag.getAttributes().get(wicketIdAttr) != null) {
            return tag;
        }

        for (TagFilter filter : FILTERS) {
            filter.onComponentTag(tag, resourceUrlProvider);
        }

        return tag;
    }

    private static class TagFilter {

        private final String tagName;

        private final String attributeName;

        public TagFilter(String tagName, String attributeName) {
            this.tagName = tagName;
            this.attributeName = attributeName;
        }

        public void onComponentTag(ComponentTag tag, ResourceUrlProvider resourceUrlProvider) {
            if (!tagName.equals(tag.getName())) {
                return;
            }

            String attrValue = tag.getAttribute(attributeName);
            if (attrValue == null) {
                return;
            }

            String url = tag.getAttribute(attributeName);
            if (url.endsWith("favicon.ico")) {
                // don't mess up with favicon
                return;
            }

            String newUrl = resourceUrlProvider.getForLookupPath("/" + url);
            if (newUrl != null) {
                newUrl = newUrl.substring(1);
            } else {
                newUrl = url;
            }

            tag.put(attributeName, newUrl);
            tag.setModified(true);
        }
    }
}
