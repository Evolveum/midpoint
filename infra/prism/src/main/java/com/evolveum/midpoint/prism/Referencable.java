package com.evolveum.midpoint.prism;

/**
 * 
 * @author Katka Valalikova
 *
 */
public interface Referencable {

	
	public PrismReferenceValue asReferenceValue();
	
	public void setupReferenceValue(PrismReferenceValue value);
}
