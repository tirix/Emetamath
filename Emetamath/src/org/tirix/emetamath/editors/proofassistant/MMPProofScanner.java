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

public class MMPProofScanner extends MMScanner {
	private static final IToken TOKEN_ERROR = new Token( new TextAttribute( new Color(Display.getCurrent(), 255, 255, 0), new Color(Display.getCurrent(), 255, 0, 0), SWT.BOLD));
	
	public MMPProofScanner(MetamathProjectNature nature, ColorManager manager) {
		IToken mmLabel = new Token( new TextAttribute( manager.getColor(IMMColorConstants.PROOF)));
		IToken mmKeyword = new Token( new TextAttribute( manager.getColor(IMMColorConstants.KEYWORD)));
		
		setDefaultReturnToken(mmKeyword);

		IRule[] rules = new IRule[2];
		// Add generic whitespace rule.
		rules[0] = new WhitespaceRule(new MMWhitespaceDetector());

		// Add rule for parsing metamath proof worksheet header lines.
		rules[1] = new MMPProofRule(new MMLabelDetector(), mmKeyword, mmLabel);

		setRules(rules);
	}
		
	public static class MMPProofRule extends PatternRule {
		IWordDetector labelDetector;
		IToken keywordToken;
		IToken labelToken;
		boolean inProof; 
		
		public MMPProofRule(IWordDetector labelDetector, IToken keywordToken, IToken labelToken) {
			super("$(", "LOC_AFTER=", keywordToken, (char)0, true); // some dummy, as we only want to reuse some utility functions 
			this.labelDetector = labelDetector;
			this.keywordToken = keywordToken;
			this.labelToken = labelToken;
		}

		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			if(c == EOF) return Token.EOF;

			if(!inProof) 
			{
				if(c != PaConstants.GENERATED_PROOF_STMT_TOKEN.charAt(0)) return TOKEN_ERROR;
				if(!sequenceDetected(scanner, PaConstants.GENERATED_PROOF_STMT_TOKEN.toCharArray(), true)) return TOKEN_ERROR;
				inProof = true;
				return keywordToken;
			}

			if(c == PaConstants.END_PROOF_STMT_TOKEN.charAt(0)) {
				if(!sequenceDetected(scanner, PaConstants.END_PROOF_STMT_TOKEN.toCharArray(), true)) return TOKEN_ERROR;
				inProof = false;
				return keywordToken;
			}

			if(!labelDetector.isWordPart((char)c)) return TOKEN_ERROR;
			
			while(labelDetector.isWordPart((char)c)) c = scanner.read();
			scanner.unread();
			return labelToken;
		}
	}
}
