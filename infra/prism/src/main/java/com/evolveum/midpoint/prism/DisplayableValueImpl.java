package com.evolveum.midpoint.prism;


import java.io.Serializable;

import com.evolveum.midpoint.util.DisplayableValue;

	public class DisplayableValueImpl implements DisplayableValue, Serializable{
		
		private Object value;
		private String label;
		private String description;
		
		public DisplayableValueImpl(Object value, String label, String description) {
			this.label = label;
			this.value = value;
			this.description = description;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public String getDescription() {
			return description;
		}
		
	}