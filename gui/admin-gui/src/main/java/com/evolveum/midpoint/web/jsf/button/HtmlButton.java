/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.jsf.button;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import java.util.ArrayList;
import java.util.List;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;

/**
 *
 * @author Vilo Repan
 */
@FacesComponent("com.evolveum.midpoint.web.jsf.button.HtmlButton")
public class HtmlButton extends HtmlCommandLink {

    @Override
    public String getType() {
        return HtmlButton.class.getName();
    }

    @Override
    public String getRendererType() {
        return "HtmlButtonRenderer";
    }

    public String getImg() {
        return (java.lang.String) getStateHelper().eval("img");
    }

    public void setImg(String image) {
        getStateHelper().put("img", image);
        handleAttribute("img", image);
    }

    public String getButtonType() {
        String type = (java.lang.String) getStateHelper().eval("buttonType");
        if (type == null) {
            type = "regular";
        }

        return type;
    }

    public void setButtonType(String type) {
        getStateHelper().put("buttonType", type);
        handleAttribute("buttonType", type);
    }
    private static final String OPTIMIZED_PACKAGE = "javax.faces.component.";

    @SuppressWarnings("unchecked")
	private void handleAttribute(String name, Object value) {
        List<String> setAttributes = (List<String>) this.getAttributes().get("javax.faces.component.UIComponentBase.attributesThatAreSet");
        if (setAttributes == null) {
            String cname = this.getClass().getName();
            if (cname != null && cname.startsWith(OPTIMIZED_PACKAGE)) {
                setAttributes = new ArrayList<String>(6);
                this.getAttributes().put("javax.faces.component.UIComponentBase.attributesThatAreSet", setAttributes);
            }
        }
        if (setAttributes != null) {
            if (value == null) {
                ValueExpression ve = getValueExpression(name);
                if (ve == null) {
                    setAttributes.remove(name);
                }
            } else if (!setAttributes.contains(name)) {
                setAttributes.add(name);
            }
        }
    }
}
