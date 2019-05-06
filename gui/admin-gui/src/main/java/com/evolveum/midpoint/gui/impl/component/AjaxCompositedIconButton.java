/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.component;

import java.util.Map.Entry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

/**
 * @author Viliam Repan (lazyman)
 * @author skublik
 */
public abstract class AjaxCompositedIconButton extends AjaxLink<String> {

    private static final long serialVersionUID = 1L;

    private IModel<String> title;
    private CompositedIcon icon;

    public AjaxCompositedIconButton(String id, CompositedIcon icon, IModel<String> title) {
        super(id);

        this.title = title;
        this.icon =icon;
        
        add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return " position-relative ";
            }
        }));
        
        add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return !AjaxCompositedIconButton.this.isEnabled() ? "disabled" : "";
            }
        }));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (title != null) {
            add(AttributeModifier.replace("title", title));
        }
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        StringBuilder sb = new StringBuilder();

        CompositedIcon icon = this.icon;
        if(icon.hasBasicIcon()) {
        	sb.append("<i class=\"").append(icon.getBasicIcon()).append("\"");
        	if (icon.hasBasicIconHtmlColor()){
        	    sb.append(" style=\"color: " + icon.getBasicIconHtmlColor() + ";\"");
            }
        	sb.append("></i> ");
        }

        if(icon.hasLayerIcons()) {
        	for(IconType entry : icon.getLayerIcons()) {
        	    if (entry == null){
        	        continue;
                }
        		if (StringUtils.isNotEmpty(entry.getCssClass())) {
                    sb.append("<i class=\"").append(entry.getCssClass()).append("\"");
                    if (StringUtils.isNotEmpty(entry.getColor())) {
                        sb.append(" style=\"color: ").append(entry.getColor()).append(";\"");
                    }
                    sb.append("></i> ");
                }
        	}
        }

        replaceComponentTagBody(markupStream, openTag, sb.toString());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }
}
