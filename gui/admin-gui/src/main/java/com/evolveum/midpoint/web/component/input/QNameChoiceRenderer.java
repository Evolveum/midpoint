package com.evolveum.midpoint.web.component.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

public class QNameChoiceRenderer implements IChoiceRenderer<QName> {
	
	private static final long serialVersionUID = 1L;
	private static Map<String, String> prefixMap;
	
	static {
		prefixMap = new HashMap<String, String>();
		prefixMap.put(SchemaConstantsGenerated.NS_ICF_SCHEMA, "icfs:");
		prefixMap.put(SchemaConstantsGenerated.NS_CAPABILITIES, "cap:");
		prefixMap.put(SchemaConstantsGenerated.NS_COMMON, "c:");
		prefixMap.put(SchemaConstantsGenerated.NS_QUERY, "q:");
		prefixMap.put(MidPointConstants.NS_RI, "ri:");
	}

	private boolean usePrefix = false;
	
	public QNameChoiceRenderer(){
		this(false);
	}

	public QNameChoiceRenderer(boolean usePrefix) {
		super();
		this.usePrefix = usePrefix;
	}

	@Override
	public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
		return choices.getObject().get(Integer.parseInt(id));
	}

	@Override
	public Object getDisplayValue(QName object) {
		StringBuilder sb = new StringBuilder();
		if (usePrefix){
			String prefix = prefixMap.get(object.getNamespaceURI());
			if (StringUtils.isNotBlank(prefix)){
				sb.append(prefix);
			}
		}
	
		sb.append(object.getLocalPart());
		return sb.toString();
	}

	@Override
	public String getIdValue(QName object, int index) {
		return Integer.toString(index);
	}

}
