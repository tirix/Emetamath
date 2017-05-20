package org.tirix.emetamath.editors.proofassistant;

import mmj.pa.PaConstants;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.tirix.emetamath.editors.ColorManager;
import org.tirix.emetamath.editors.IMMColorConstants;
import org.tirix.emetamath.editors.MMScanner;
import org.tirix.emetamath.editors.MMWhitespaceDetector;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class MMPHeaderScanner extends MMScanner {
	private static final IToken TOKEN_ERROR = new Token( new TextAttribute( new Color(Display.getCurrent(), 255, 255, 0), new Color(Display.getCurrent(), 255, 0, 0), SWT.BOLD));
	
	public MMPHeaderScanner(MetamathProjectNature nature, ColorManager manager) {
		IToken mmLabel = new Token( new TextAttribute( manager.getColor(IMMColorConstants.LABEL)));
		IToken mmKeyword = new Token( new TextAttribute( manager.getColor(IMMColorConstants.KEYWORD)));
		
		setDefaultReturnToken(mmKeyword);

		IRule[] rules = new IRule[2];
		// Add generic whitespace rule.
		rules[0] = new WhitespaceRule(new MMWhitespaceDetector());

		// Add rule for parsing metamath proof worksheet header lines.
		rules[1] = new MMPHeaderRule(new MMLabelDetector(), mmKeyword, mmLabel);

		setRules(rules);
	}
		
	public static class MMPHeaderRule extends PatternRule {
		final static int HDR = 0, MM = 1, PROOF_ASST = 2, THEOREM = 3, THEOREM_LABEL = 4, LOC_AFTER = 5, LOC_AFTER_LABEL = 6;
		IWordDetector labelDetector;
		IToken keywordToken;
		IToken labelToken;
		int currentStatus; 
		
		public MMPHeaderRule(IWordDetector labelDetector, IToken keywordToken, IToken labelToken) {
			super("$(", "LOC_AFTER=", keywordToken, (char)0, true); // some dummy, as we only want to reuse some utility functions 
			this.labelDetector = labelDetector;
			this.keywordToken = keywordToken;
			this.labelToken = labelToken;
			currentStatus = 0;
		}

		public IToken evaluate(ICharacterScanner scanner) {
			if(scanner.getColumn() == 0) currentStatus = HDR;
			int c = scanner.read();
			if(c == EOF) return Token.EOF;
			switch(currentStatus) {
			case HDR:
				currentStatus = MM;
				if(c != PaConstants.HEADER_STMT_TOKEN.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.HEADER_STMT_TOKEN.toCharArray(), true)) 
					if(!sequenceDetected(scanner, "$)".toCharArray(), true)) 
						return TOKEN_ERROR;
				return keywordToken;

			case MM:
				currentStatus = PROOF_ASST;
				if(c != PaConstants.HEADER_MM_TOKEN.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.HEADER_MM_TOKEN.toCharArray(), true)) return TOKEN_ERROR;
				return keywordToken;

			case PROOF_ASST:
				currentStatus = THEOREM;
				if(c != PaConstants.HEADER_PROOF_ASST_TOKEN.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.HEADER_PROOF_ASST_TOKEN.toCharArray(), true)) return TOKEN_ERROR;
				return keywordToken;

			case THEOREM:
				currentStatus = THEOREM_LABEL;
				if(c != PaConstants.HEADER_THEOREM_EQUAL_PREFIX.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.HEADER_THEOREM_EQUAL_PREFIX.toCharArray(), true)) return TOKEN_ERROR;
				return keywordToken;

			case THEOREM_LABEL:
				currentStatus = LOC_AFTER;
				while(labelDetector.isWordPart((char)c)) c = scanner.read();
				scanner.unread();
				return labelToken;

			case LOC_AFTER:
				currentStatus = LOC_AFTER_LABEL;
				if(c != PaConstants.HEADER_LOC_AFTER_EQUAL_PREFIX.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.HEADER_LOC_AFTER_EQUAL_PREFIX.toCharArray(), true)) return TOKEN_ERROR;
				return keywordToken;

			case LOC_AFTER_LABEL:
				currentStatus = HDR;
				if(c == '?') return labelToken;
				while(labelDetector.isWordPart((char)c)) c = scanner.read();
				scanner.unread();
				return labelToken;

			default:
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}
}
