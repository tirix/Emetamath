package mmj.pa;

/**
 * This class handles all the text formatting
 * for Proof Worksheets
 * @author Thierry
 *
 */
public class ProofWorksheetText {
	private StringBuilder text;
    private int lineCnt;
    private int col, left, right;
	private String indentLeft;
    
    public ProofWorksheetText(ProofWorksheet w, int left, int right) {
//        left  = w.proofAsstPreferences.getRPNProofLeftCol();
//        right = w.proofAsstPreferences.getRPNProofRightCol();
    	this.left = left;
    	this.right = right;
        StringBuffer indentLeftSb = new StringBuffer(left - 1);
		for (int i = 1; i < left; i++) {
			indentLeftSb.append(' ');
		}
		indentLeft = indentLeftSb.toString();
		
		text = new StringBuilder();
//		Start indenting nicely after the proof token 
//		text.append(PaConstants.GENERATED_PROOF_STMT_TOKEN);
//		text.append(' ');
//		col = 4;
//		for ( ; col < left; col++) {
//			text.append(' ');
//		}
    }

    public void append(String token) {
    	append(token, true);
    }
    
	public void append(String token, boolean addSpace) {
		col += token.length();
		if (col == right) {
			text.append(token);
			text.append("\n");
			++lineCnt;
			text.append(indentLeft);
			col = left;
		}
		else {
			if (col > right) {
				text.append("\n");
				++lineCnt;
				text.append(indentLeft);
				col = left;
			}
			text.append(token);
			if(addSpace) {
				text.append(' ');
				++col;
			}
		}
	}

	public void append(char token) {
		append(""+token, true);
	}

	public String toString() {
		return text.toString();
	}

	public StringBuilder toStringBuilder() {
		StringBuilder sb = new StringBuilder();
		sb.append(text.toString());
		return sb;
	}

	public int getLineCnt() {
		return lineCnt;
	}
}
