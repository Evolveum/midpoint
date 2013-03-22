package com.evolveum.midpoint.prism;

public interface Matchable<T> {

	
	public boolean match(T other);
}
