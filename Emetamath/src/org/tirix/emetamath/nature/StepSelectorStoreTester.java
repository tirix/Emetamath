package org.tirix.emetamath.nature;

import mmj.pa.StepSelectorStore;

import org.eclipse.core.expressions.PropertyTester;

public class StepSelectorStoreTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		StepSelectorStore store = (StepSelectorStore)receiver;
		if(store == null) return false;
		if(property.equals("hasMoreItems")) return store.isFull();
		return false;
	}
}
