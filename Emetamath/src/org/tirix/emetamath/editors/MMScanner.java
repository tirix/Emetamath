package org.tirix.emetamath.editors;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import mmj.lang.Cnst;
import mmj.lang.Sym;
import mmj.lang.Var;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.nature.MetamathProjectNature.SystemLoadListener;

/**
 * Scanner for Metamath text
 * 
 * This provides the coloring / syntax highlighting
 * 
 * @author Thierry Arnoux
 */
public class MMScanner extends RuleBasedScanner {
	public final static String MM_KEYWORD = "__mm_keyword";
	public final static String MM_CONSTANT = "__mm_constant";
	public final static String MM_VARIABLE = "__mm_variable";
	public final static String MM_DISJOINT = "__mm_disjoint";
	public final static String MM_VARHYP = "__mm_varhyp";
	public final static String MM_LOGHYP = "__mm_loghyp";
	public final static String MM_AXIOM = "__mm_axiom";
	public final static String MM_THEOREM = "__mm_theorem";

	final static String[] mmKeywords = new String[] { "$c", "$v", "$d", "$f", "$e", "$a", "$p", "$=", "${", "$}", "$." };
	
	protected MMScanner() {
		
	}
	
	public MMScanner(final MetamathProjectNature nature, final ColorManager manager) {
		IToken mmLabel = new Token( new TextAttribute( manager.getColor(IMMColorConstants.LABEL)));
		IToken mmKeyword = new Token( new TextAttribute( manager.getColor(IMMColorConstants.KEYWORD)));
		IToken mmSymbol = new Token( new TextAttribute( manager.getColor(IMMColorConstants.SYMBOL)));
		IToken mmConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CONSTANT)));
//		IToken mmWffVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.WFF), null, SWT.ITALIC));
//		IToken mmSetVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.SET), null, SWT.ITALIC));
//		IToken mmClassVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CLASS), null, SWT.ITALIC));
		IToken mmTypeConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.TYPE)));

		setDefaultReturnToken(mmLabel);
		
		IToken mmVariable = new Token(MM_VARIABLE);
		IToken mmDisjoint = new Token(MM_DISJOINT);
		IToken mmVarHyp = new Token(MM_VARHYP);
		IToken mmLogHyp = new Token(MM_LOGHYP);
		IToken mmAxiom = new Token(MM_AXIOM);
		IToken mmTheorem = new Token(MM_THEOREM);

		WordRule keywordRule = new WordRule(new MMTokenDetector());
		for(int i=0;i<mmKeywords.length;i++) keywordRule.addWord(mmKeywords[i], mmKeyword);

		final MMSymbolRule symbolRule = new MMSymbolRule(new MMTokenDetector(), nature, mmConstant, mmTypeConstant, new Hashtable<Cnst, IToken>());

		if(nature != null) {
			nature.addSystemLoadListener(new SystemLoadListener() {
				@Override
				public void systemLoaded() {
					symbolRule.setVariableTokens(createVariableTokens(nature, manager));
				}});
		}
		
		IRule[] rules = new IRule[2];
		//Add rule for processing instructions
		//rules[0] = new MultiLineRule("$v", "$.", new Token( new TextAttribute( manager.getColor(IMMColorConstants.V))));

		// Add generic whitespace rule.
		//rules[1] = new WhitespaceRule(new MMWhitespaceDetector());

		// Add rule for detecting keywords.
		rules[0] = keywordRule;

		// Add rule for detecting Metamath symbols.
		rules[1] = symbolRule;

		setRules(rules);
	}

	public static Map<Cnst, IToken> createVariableTokens(MetamathProjectNature nature, ColorManager manager) {
		Map<Cnst, RGB> typeColors = nature.getTypeColors();
		Hashtable<Cnst, IToken> tokens = new Hashtable<Cnst, IToken>();
		for(Cnst type:typeColors.keySet()) {
			IToken typeToken = new Token( new TextAttribute( manager.getColor(typeColors.get(type)), null, SWT.ITALIC));
			tokens.put(type, typeToken);
		}
		return tokens;
	}
	
//	public IToken nextToken() {
//		IToken token = super.nextToken();
//		
//		return token;
//		}
	public static IRule createSymbolRule(MetamathProjectNature nature, ColorManager manager) {
		IToken mmConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CONSTANT)));
//		IToken mmWffVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.WFF), null, SWT.ITALIC));
//		IToken mmSetVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.SET), null, SWT.ITALIC));
//		IToken mmClassVariable = new Token( new TextAttribute( manager.getColor(IMMColorConstants.CLASS), null, SWT.ITALIC));
		IToken mmTypeConstant = new Token( new TextAttribute( manager.getColor(IMMColorConstants.TYPE)));
		return new MMSymbolRule(new MMTokenDetector(), nature, mmConstant, mmTypeConstant, createVariableTokens(nature, manager));
	}
	
	public static class MMProofScanner extends MMScanner {
		public MMProofScanner(ColorManager manager) {
			IToken mmKeyword = new Token( new TextAttribute( manager.getColor(IMMColorConstants.KEYWORD)));
			IToken mmProof = new Token( new TextAttribute( manager.getColor(IMMColorConstants.PROOF)));

			setDefaultReturnToken(mmProof);
			
			WordRule keywordRule = new WordRule(new MMTokenDetector());
			for(int i=0;i<mmKeywords.length;i++) keywordRule.addWord(mmKeywords[i], mmKeyword);

			setRules(new IRule[] { keywordRule });
		}
	}
	
	/**
	 * @author Thierry Arnoux
	 */
	public static class MMSymbolRule implements IRule {
		/** Our Word Detector */
		MMTokenDetector fDetector;

		/** Buffer used for pattern detection. */
		private StringBuffer fBuffer= new StringBuffer();

		/** The Project Nature which provides us the logical system */
		MetamathProjectNature nature;
		
		IToken constantToken, typeToken;
		protected Map<Cnst, IToken> variableTokens;
		
		public MMSymbolRule(MMTokenDetector tokenDetector, MetamathProjectNature nature, IToken mmConstant, IToken mmTypeConstant, Map<Cnst, IToken> mmVariableTokens) {
				// IToken mmWffVariable, IToken mmSetVariable, IToken mmClassVariable, 
			this.fDetector = tokenDetector;
			this.constantToken = mmConstant;
			this.typeToken = mmTypeConstant;
			this.variableTokens = mmVariableTokens;
//			this.wffToken = mmWffVariable;
//			this.setToken = mmSetVariable;
//			this.classToken = mmClassVariable;
			this.nature = nature;
			}

		public void setVariableTokens(Map<Cnst, IToken> mmVariableTokens) {
			this.variableTokens = mmVariableTokens;
			}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c= scanner.read();
			if (c != ICharacterScanner.EOF && fDetector.isWordStart((char) c)) {
				fBuffer.setLength(0);
				do {
					fBuffer.append((char) c);
					c= scanner.read();
				} while (c != ICharacterScanner.EOF && fDetector.isWordPart((char) c));
				if(c != ICharacterScanner.EOF) scanner.unread();

				return getTokenFor(fBuffer.toString());
			}
			if(c != ICharacterScanner.EOF) scanner.unread();
			return Token.UNDEFINED;
		}

		private IToken getTokenFor(String tokenStr) {
			if(nature == null || !nature.isLogicalSystemLoaded()) return Token.UNDEFINED;
			Sym sym = (Sym)nature.getLogicalSystem().getSymTbl().get(tokenStr);
			if(sym == null) return Token.UNDEFINED;
			if(sym instanceof Cnst) {
				if(nature.isType((Cnst)sym)) return typeToken;
				return constantToken;
			}
			Cnst type = nature.getType(sym);
			if(type == null) return Token.UNDEFINED;
			IToken token = variableTokens.get(type);
			if(token == null) return Token.UNDEFINED;
			return token;
//			if(nature.isSet((Var)sym)) return setToken;
//			if(nature.isClass((Var)sym)) return classToken;
//			if(nature.isWff((Var)sym)) return wffToken;
//			return Token.UNDEFINED;
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

	/**
	 * Recognizes all 94 characters mentioned in Metamath language specification as token characters 
	 * These are the upper and lower ASCII letters, digits, and 32 special characters.
	 * This is equivalent to all ASCII characters between code 33(!) and 126(~), inclusive.
	 * @author Thierry Arnoux
	 */
	public static class MMTokenDetector implements IWordDetector {

		public boolean isWordPart(char c) {
			return c >= '!' && c <= '~';
		}

		public boolean isWordStart(char c) {
			return isWordPart(c);
		}
	}
}
