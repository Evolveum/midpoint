package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;

@FunctionalInterface
public interface ItemVisibilityHandler extends Serializable{

	
	boolean isVisible(ItemWrapper wrapper);
}
