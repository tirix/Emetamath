package org.tirix.emetamath.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class MMWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
