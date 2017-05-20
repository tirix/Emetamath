package org.tirix.emetamath.editors.proofassistant;

import java.util.Hashtable;

import mmj.lang.Cnst;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.IMMColorConstants;
import org.tirix.emetamath.editors.MMScanner;
import org.tirix.emetamath.editors.MMWhitespaceDetector;
import org.tirix.emetamath.editors.MMScanner.MMSymbolRule;
import org.tirix.emetamath.editors.MMScanner.MMTokenDetector;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

public class MMPScanner extends MMScanner {
	private static final IToken TOKEN_ERROR = new Token( new TextAttribute( new Color(Display.getCurrent(), 255, 255, 0), new Color(Display.getCurrent(), 255, 0, 0), SWT.BOLD));
	
	public MMPScanner(final MetamathProjectNature nature, final ColorManager manager) {
		IToken mmLabel = new Token( new TextAttribute( manager.getColor(IMMColorConstants.LABEL)));
		IToken mmKeyword = new Token( new TextAttribute( manager.getColor(IMMColorConstants.KEYWORD)));
		IToken mmConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CONSTANT)));
		IToken mmTypeConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.TYPE)));
//		IToken mmWffVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.WFF), null, SWT.ITALIC));
//		IToken mmSetVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.SET), null, SWT.ITALIC));
//		IToken mmClassVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CLASS), null, SWT.ITALIC));
		
		setDefaultReturnToken(mmLabel);

		final IRule[] rules = new IRule[3];
		// Add generic whitespace rule.
		rules[0] = new WhitespaceRule(new MMWhitespaceDetector());

		// Add rule for parsing metamath proof lines.
		rules[1] = new MMPRule(new MMLabelDetector(), mmKeyword, mmLabel);

		// Add rule for detecting Metamath symbols.
		rules[2] = new MMSymbolRule(new MMTokenDetector(), nature, mmConstant, mmTypeConstant, new Hashtable<Cnst, IToken>());

		setRules(rules);

		nature.addSystemLoadListener(new SystemLoadListener() {
			@Override
			public void systemLoaded() {
				((MMSymbolRule)rules[2]).setVariableTokens(createVariableTokens(nature, manager));
			}});
	}
		
	public static class MMPRule implements IRule {
		final static int LABEL = 0, LABEL_SEPARATOR = 1, HYPOTHESIS = 2, HYPOTHESIS_SEPARATOR = 3, APPLIED_STATEMENT = 4, RESULTING_FORMULA = 5;
		IWordDetector labelDetector;
		IToken keywordToken;
		IToken labelToken;
		int currentStatus; 
		
		public MMPRule(IWordDetector labelDetector, IToken keywordToken, IToken labelToken) {
			this.labelDetector = labelDetector;
			this.keywordToken = keywordToken;
			this.labelToken = labelToken;
			currentStatus = 0;
		}

		public IToken evaluate(ICharacterScanner scanner) {
			if(scanner.getColumn() == 0) currentStatus = LABEL;
			int c = scanner.read();
			if(c == EOF) return Token.EOF;
			switch(currentStatus) {
			case LABEL:
				currentStatus = LABEL_SEPARATOR;
				if(!labelDetector.isWordPart((char) c)) return TOKEN_ERROR;
				while(labelDetector.isWordPart((char)c)) c = scanner.read();
				scanner.unread();
				return labelToken;

			case LABEL_SEPARATOR:
				currentStatus = HYPOTHESIS;
				if(c == ':') return keywordToken;
				return TOKEN_ERROR;

			case HYPOTHESIS:
				currentStatus = HYPOTHESIS_SEPARATOR;
				if(c == '?') return labelToken;
				while(labelDetector.isWordPart((char)c)) c = scanner.read();
				scanner.unread();
				return labelToken;
			
			case HYPOTHESIS_SEPARATOR:
				if(c == ',') currentStatus = HYPOTHESIS;
				else currentStatus = APPLIED_STATEMENT;
				if(c == ':' || c == ',') return keywordToken;
				return TOKEN_ERROR;

			case APPLIED_STATEMENT:
				currentStatus = RESULTING_FORMULA;
				while(labelDetector.isWordPart((char)c)) c = scanner.read();
				scanner.unread();
				return labelToken;

			case RESULTING_FORMULA:
				scanner.unread();
				return Token.UNDEFINED;

			default:
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	/**
	 * Recognizes labels as a combination of letters, digits, and the characters hyphen, underscore, and period. 
	 * @author Thierry Arnoux
	 */
	public static class MMLabelDetector implements IWordDetector {

		public boolean isWordPart(char c) {
			return (c < 128 && Character.isLetterOrDigit(c)) || c == '-' || c == '_' || c == '.';
		}

		public boolean isWordStart(char c) {
			return isWordPart(c);
		}
	}
}
