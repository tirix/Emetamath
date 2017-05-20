package org.tirix.emetamath.nature;

import org.eclipse.core.expressions.PropertyTester;

public class BuildStatusTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		MetamathProjectNature nature = (MetamathProjectNature)receiver;
		if(nature == null) return false;
		if(property.equals("isLogicalSystemLoaded")) return nature.logicalSystemLoaded;
		return false;
	}
}
