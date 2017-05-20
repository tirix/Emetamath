package org.tirix.emetamath.editors.proofassistant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class MMPPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MM_COMMENT = "__mm_comment";

	public MMPPartitionScanner() {
		IToken mmComment = new Token(MM_COMMENT);
		IToken mmText = new Token(IDocument.DEFAULT_CONTENT_TYPE);

		// Don't enable this, otherwise one partition per character is created!
		//setDefaultReturnToken(mmText);
		
		IPredicateRule[] rules = new IPredicateRule[1];

		rules[0] = new MultiLineRule("$(", "$)", mmComment);

		setPredicateRules(rules);
	}
}
