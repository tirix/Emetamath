package mmj.pa;

/**
*  CompressedProofStmt
*  Thierry Arnoux, 2011
*  
*  Similar to GeneratedProofStmt, this class represents the compressed proof statement
*  in a proof worksheet.
*  
*/

import java.util.ArrayList;

import org.tirix.mmj.ProofWorksheetText;

import mmj.lang.LangConstants;
import mmj.lang.Stmt;
import mmj.mmio.MMIOConstants;

public class CompressedProofStmt extends GeneratedProofStmt {
	
  /**
   *  Default Constructor.
   */
  public CompressedProofStmt(ProofWorksheet w) {
      super(w);
  }

  /**
   *  Standard Constructor for GeneratedProofStmt.
   *
   *  @param rpnProof Proof Stmt Array in RPN format
   */
  public CompressedProofStmt(ProofWorksheet w,
                            Stmt[] rpnProof) {
      super(w);
      
      ProofWorksheetText text = new ProofWorksheetText(w, 
    		  w.proofAsstPreferences.getRPNProofLeftCol(),
    		  w.proofAsstPreferences.getRPNProofRightCol()
    		  );

      // the codes that will be used for compressing, cf. 
      ArrayList<Stmt> statementsUsed = new ArrayList<Stmt>();
      
      // pre-populate the statements used with the mandatory hypotheses
      for(Stmt s:w.theorem.getMandFrame().hypArray) statementsUsed.add(s);
      
      // the encoded sequence, starting with an empty list
      ArrayList<Integer> encodedSequence = new ArrayList<Integer>();
      
      // add the proof statements in the list
      for(Stmt stmt:rpnProof) {
    	  int index = statementsUsed.indexOf(stmt);
    	  if(index == -1) {
    		  // not found, the statement is appended to the list
    		  index = statementsUsed.size();
    		  statementsUsed.add(stmt);
    	  }
    	  // add the statement index to the encoded sequence
    	  encodedSequence.add(index);
      }
      
      // now write the compressed proof:
      text.append(PaConstants.GENERATED_PROOF_STMT_TOKEN);

      // start by writing the labels of the statements used (except mandatory hyps) 
      text.append(MMIOConstants.MM_BEGIN_COMPRESSED_PROOF_LIST_CHAR);
      for (int i=w.theorem.getMandHypArrayLength();i<statementsUsed.size();i++) {
          text.append(statementsUsed.get(i).getLabel());
      }
      text.append(MMIOConstants.MM_END_COMPRESSED_PROOF_LIST_CHAR);

      // then write the encoded indices
      for (Integer i:encodedSequence) {
          text.append(encodeInteger(i), false);
      }

      text.append(PaConstants.END_PROOF_STMT_TOKEN);
      stmtText = text.toStringBuffer();
      lineCnt = text.getLineCnt();
  }

  /**
   * Returns the encoded integer i as a String
   * Encoding is made according to the Metamath book, Appendix B.
   * 
   * @param i the value to encode, minus 1 (i.e. starting at 0)
   * @return
   */
  public static String encodeInteger(int i) {
	  final char COMPRESS_LOW_START = 'A';
	  final char COMPRESS_HIGH_START = COMPRESS_LOW_START + LangConstants.COMPRESS_LOW_BASE;
	  String str = ""+(char)(COMPRESS_LOW_START + (i % LangConstants.COMPRESS_LOW_BASE));
	  i /= LangConstants.COMPRESS_LOW_BASE;
	  while(i > 0) {
		  str = (char)(COMPRESS_HIGH_START + (i % LangConstants.COMPRESS_HIGH_BASE)) + str;
		  i /= LangConstants.COMPRESS_HIGH_BASE;
	  }
	  return str;
  }
  
  public boolean stmtIsIncomplete() {
      return false;
  }

  /**
   *  Function used for cursor positioning.
   *  <p>
   *
   *  @param fieldId value identify ProofWorkStmt field
   *         for cursor positioning, as defined in
   *         PaConstants.FIELD_ID_*.
   *
   *  @return column of input fieldId or default value
   *         of 1 if there is an error.
   */
  public int computeFieldIdCol(int fieldId) {
      return 1;
  }

  /**
   *  Reformats Derivation Step using TMFF.
   */
  public void tmffReformat() {
  }

}

