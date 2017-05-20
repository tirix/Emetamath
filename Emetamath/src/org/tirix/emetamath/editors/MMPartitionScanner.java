package org.tirix.emetamath.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * High-level partitioner scanner for Metamath format
 * Partitions a metamath document into either comment or text sections
 * 
 * Note: This partitioner does not provide coloring.
 * For coloring inside of Metamath formulas, see MMScanner.
 * @see MMScanner
 * @author Thierry Arnoux
 */
public class MMPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MM_TYPESETTING = "__mm_typesetting";
	public final static String MM_FILEINCLUSION = "__mm_fileinclusion";
	public final static String MM_COMMENT = "__mm_comment";
	public final static String MM_PROOF = "__mm_proof";

	public MMPartitionScanner() {
		IToken mmTypeSetting = new Token(MM_TYPESETTING);
		IToken mmComment = new Token(MM_COMMENT);
		IToken mmProof = new Token(MM_PROOF);
		IToken mmFileInclusion = new Token(MM_FILEINCLUSION);
		IToken mmText = new Token(IDocument.DEFAULT_CONTENT_TYPE);

		// Don't enable this, otherwise one partition per character is created!
		//setDefaultReturnToken(mmText);
		
		IPredicateRule[] rules = new IPredicateRule[4];

		rules[0] = new MultiLineRule("$t", "$)", mmTypeSetting);
		rules[1] = new MultiLineRule("$(", "$)", mmComment);
		rules[2] = new MultiLineRule("$=", "$.", mmProof);
		rules[3] = new MultiLineRule("$[", "$]", mmFileInclusion);
		
		setPredicateRules(rules);
	}
}
