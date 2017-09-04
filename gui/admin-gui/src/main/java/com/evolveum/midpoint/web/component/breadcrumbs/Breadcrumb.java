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

package com.evolveum.midpoint.web.component.breadcrumbs;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Breadcrumb object that is stored in the session. It represents the way "back" to the main menu.
 *
 * We need to be extra careful about the memory references here. This object goes in the session.
 * Therefore we cannot allow models to be stored in the session. The models may have references
 * to (possibly big) pages and other rich objects. The references are there mostly to load the
 * models. But we do not want that. We want to store only the values. Therefore the model values
 * are copied to simple strings on model detach().
 *
 * @author Viliam Repan (lazyman)
 * @author semancik
 */
public class Breadcrumb implements Serializable, DebugDumpable {
	private static final long serialVersionUID = 1L;

	private static final Trace LOG = TraceManager.getTrace(Breadcrumb.class);

    transient private IModel<String> labelModel;
    private String label;
    transient private IModel<String> iconModel;
    private String icon;
    private boolean useLink = false;
    private boolean visible = true;

    public Breadcrumb() {
    }

    public Breadcrumb(IModel<String> labelModel) {
        this(labelModel, null);
    }

    public Breadcrumb(IModel<String> labelModel, IModel<String> icon) {
        setLabel(labelModel);
        setIcon(icon);
    }

    public PageParameters getParameters() {
        return null;
    }

    public IModel<String> getLabel() {
    	if (labelModel == null && label != null) {
    		labelModel = new AbstractReadOnlyModel<String>() {
    			private static final long serialVersionUID = 1L;
    			@Override
                public String getObject() {
    				return label;
    			}
    		};
    	}
    	return labelModel;
    }

    public void setLabel(final IModel<String> label) {
    	if (label == null) {
    		this.labelModel = null;
            return;
        }

    	this.labelModel = new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                try {
                    return label.getObject();
                } catch (Exception ex) {
                    LOG.warn("Couldn't load breadcrumb model value", ex);
                    return null;
                }
            }

			@Override
			public void detach() {
				super.detach();
				Breadcrumb.this.label = label.getObject();
				Breadcrumb.this.labelModel = null;
			}

        };
    }

    public IModel<String> getIcon() {
    	if (iconModel == null && icon != null) {
    		iconModel = new AbstractReadOnlyModel<String>() {
    			private static final long serialVersionUID = 1L;
    			@Override
                public String getObject() {
    				return icon;
    			}
    		};
    	}
    	return iconModel;
    }

    public void setIcon(final IModel<String> icon) {
    	if (icon == null) {
    		this.iconModel = null;
            return;
        }

    	this.iconModel = new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                try {
                    return icon.getObject();
                } catch (Exception ex) {
                    LOG.warn("Couldn't load breadcrumb model value", ex);
                    return null;
                }
            }

			@Override
			public void detach() {
				super.detach();
				Breadcrumb.this.icon = icon.getObject();
				Breadcrumb.this.iconModel = null;
			}

        };
    }

    public boolean isUseLink() {
        return useLink;
    }

    public void setUseLink(boolean useLink) {
        this.useLink = useLink;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public WebPage redirect() {
        throw new UnsupportedOperationException("Should be implemented in a subclass");
    }

	public RestartResponseException getRestartResponseException() {
		throw new UnsupportedOperationException("Should be implemented in a subclass");
	}

	private <T extends Serializable> IModel<T> wrapModel(final IModel<T> model) {
        if (model == null) {
            return null;
        }

        return new AbstractReadOnlyModel<T>() {

            @Override
            public T getObject() {
                try {
                    return model.getObject();
                } catch (Exception ex) {
                    LOG.warn("Couldn't load breadcrumb model value", ex);
                    return null;
                }
            }

			@Override
			public void detach() {
				super.detach();
				model.getObject();
			}

        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        //we don't compare label/icon models, we would need to compare models values
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{labelModel, iconModel});
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(this.getClass().getSimpleName());
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "labelModel", labelModel==null?"":labelModel.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "label", label, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "iconModel", iconModel==null?"":iconModel.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "icon", icon, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "useLink", useLink, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "visible", visible, indent+1);
		extendsDebugDump(sb, indent);
		return sb.toString();
	}

	protected void extendsDebugDump(StringBuilder sb, int indent) {

	}
}
