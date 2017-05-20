//********************************************************************/
//* Copyright (C) 2005  MEL O'CAT  mmj2 (via) planetmath (dot) org   */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  EarleyParser.java  0.03 08/01/2007
 *
 *  Version 0.02 - 09/26/2005
 *
 *  Version 0.03 - 08/01/2007
 *      - tidy up
 */

package mmj.verify;

import mmj.lang.*;
import java.util.*;

/**
 *  EarleyParser is my implementation of the Earley Parse
 *  algorithm, enhanced with Lookahead and various attempts
 *  at efficiency in Java.
 *  <p>
 *  The primary difference between this Earley Parser and
 *  the standard version is that input expressions are
 *  "pre-parsed" with variable hypotheses replacing the
 *  variables in the expression to be parsed. One reason
 *  for this change is that Var's and VarHyp's in Metamath
 *  are not necessarily in global scope; variable "x" might
 *  be a set now but later be a foobar. A drawback of this
 *  change is increased difficulty in building ParseTrees
 *  after EarleyParse finishes doing its "parse recognition"
 *  algorithm. Another drawback is that a restriction is
 *  placed on coding a grammatical Type Code, such as "wff"
 *  as a constant in a formula (ex. "xyz $a |- ( wff -> ph ) $."
 *  is prohibited because the "wff" is picked up as a
 *  something where a VarHyp should be...assuming the file
 *  that contains that statement uses "wff" as a Type Code.)
 *  <p>
 *  Another major change to the algorithm is that, while
 *  nulls and type conversions are handled in the mmj Grammar,
 *  a process of "combinatorial explosion" is used during
 *  the grammar-generation process, and so EarleyParser
 *  input "grammar" is solely the Notation Rules and
 *  unaltered VarHyp's. A grammar rule match to a subsequence
 *  of an expression may point to a Notation Rule that is
 *  a composite function of a Notation Syntax Axiom, plus
 *  any number of Nulls Permitted and Type Conversion Axioms;
 *  so when Earley is done, a fair amount of work still needs
 *  to happen to generate the output Parse tree/RPN for that
 *  match! It would definitely be feasible to revert the
 *  EarleyParse code to handle nulls and type conversions
 *  but extra invocations of Predictor and Completor are
 *  needed, as described in "Practical Techniques..." (see below)
 *  AND that still leaves the problem of how to generate
 *  an unambiguous grammar from a database like set.mm
 *  that contains "removable" or "non-essential ambiguities"
 *  such as overloaded functions.
 *  <p>
 *  The output of EarleyParser is an array of ParseTrees,
 *  and there is no arbitrary limit on the number of these
 *  trees in the event of ambiguity -- except that imposed
 *  by the user in establishing the parseTreeArray. The sequence
 *  of the output trees *is* arbitrary however (I have no
 *  clue), but there are solid reasons to believe that
 *  duplicate trees will never be generated, under any
 *  circumstances.
 *  <p>
 *  The Earley Parse algorithm write-up talks incessantly about
 *  Item Sets. These are supposed to be actual *sets*, with
 *  no duplications in any one set. To accomplish that efficiently
 *  in Java was a coding issue. Instead of deleting duplicates
 *  after they are generated, I attempt to prevent generation
 *  of duplicate Earley Items in the first place using the
 *  pPredictorTyp and pBringForwardTyp arrays. And instead
 *  of coding the Item Sets as Java Sets I used arrays,
 *  pre-allocated for speed. The concession to quality is
 *  that I do do a duplicate check when adding an item to
 *  the pCompletedItem array -- no duplicates in that array
 *  means no duplicate Parse Trees, end of story (assuming
 *  the "build Parse Tree" logic isn't buggy.)
 *  <p>
 *  EarleyParser is my 3rd attempt at coding a parser capable
 *  of handling Metamath's set.mm database. BottomUpParser
 *  was my second attempt. Set.mm's "supeu" was too hairy for
 *  Bottom Up, but the algorithm does work  -- when it actually
 *  gets around to computing an answer. My first attempt handled
 *  propositional logic just fine but died deep into set.mm
 *  and has since been deleted (not even a backup lives on...)
 *  <p>
 *  The nice thing about the Earley Parse algorithm is that
 *  it does handle all context free grammars and it does
 *  handle ambiguous grammars. If it works for set.mm then
 *  it is a helluva parser, because set.mm gives professional
 *  logicians gray hair with its intricate syntax!
 *  <p>
 *  See Dick Grune and Ceriel J.H. Jacob's "Parsing Techniques
 *  -- A Practical Guide", page 149 in Edition 1 -- which is the
 *  139th page in the .pdf book file, available at:
 *  @see <a href="http://www.cs.vu.nl/%7Edick/PTAPG.html">
 *       Parsing Techniques -- A Practical Guide</a>
 *  <p>
 *  (Note: I went completely bald after working on this project.
 *  Fortunately, Dick Grune was incredibly friendly and helped me
 *  to understand some gnarly concepts. My hair may return,
 *  we shall see! While we're on the subject of heroes, Norm Megill
 *  has the patience of a saint. As far as I am concerned he is
 *  a prime candidate for the Nobel Prize. One day they will
 *  build statues of Norman Megill, and small boys will
 *  go to bed with Metamath bubble gum trading cards under
 *  their pillows...
 *  <p>
 *  @see <a href="../../EarleyParseFunctionAlgorithm.html">
 *       More on the Earley Parse Function Algorithm</a>
 */
public class EarleyParser implements GrammaticalParser {

    private   int                   retryCnt    = -1;

    private   int                   pMax;

    private   int                   pItemSetMax;
    private   int                   pItemSetHighwater;
    private   int[]                 pItemSetCnt;
    private   int                   pItemSetIndex;

    private   int                   pCompletedItemSetMax;
    private   int                   pCompletedItemSetHighwater;
    private   int[]                 pCompletedItemSetCnt;
    private   int                   pCompletedItemSetIndex;

    private   int                   pBringForwardTypMax;
    private   int[]                 pBringForwardTypCnt;
    private   int                   pBringForwardTypIndex;

    private   int                   pPredictorTypMax;
    private   int                   pPredictorTypCnt;

    private   EarleyItem[][]        pItem;
    private   EarleyItem[][]        pCompletedItem;

    private   Cnst[][]              pBringForwardTyp;
    private   Cnst[]                pPredictorTyp;


    private   ParseNodeHolder[] emptyParamArray =
                                    new ParseNodeHolder[0];

    /**
     *  these are stored globally only to avoid parameter passing.
     */

    private   Grammar               grammar;

    private   ParseTree[]           parseTreeArray;
    private   Cnst                  formulaTyp;
    private   ParseNodeHolder[]     parseNodeHolderExpr;
    private   int                   highestSeq;

    private   int                   parseCnt;

    private   Cnst                  startRuleTyp;
    private   ParseNodeHolder[]     expr;

    private   int                   twinTreesNeeded;
    private   int                   twinTreesCnt;

    //rules only loaded once
    private   int                   rulesTypMax;
    private   int                   rulesTypCnt;
    private   Cnst[][]              ruleTypAndFIRSTTyp;

    static boolean debug = false;
    
    /**
     *  Construct using reference to Grammar and a parameter
     *  signifying the maximum length of a formula in the database.
     *
     *  @param grammarIn Grammar object
     *  @param maxFormulaLengthIn gives us a hint about what to expect.
     */
    public EarleyParser(Grammar grammarIn,
                        int     maxFormulaLengthIn) {

        grammar                   = grammarIn;

        pMax                      = maxFormulaLengthIn + 1;
        if (pMax < 10) {
            pMax = 10;
        }

        pItemSetMax               =
            grammar.getNotationGRSet().size();

        if (pItemSetMax <
                GrammarConstants.EARLEY_PARSE_MIN_ITEMSET_MAXIMUM) {
            pItemSetMax           =
                GrammarConstants.EARLEY_PARSE_MIN_ITEMSET_MAXIMUM;
        }

        pCompletedItemSetMax      = (pItemSetMax /
                GrammarConstants.EARLEY_PARSE_CITEMSET_ITEMSET_RATIO);

        pBringForwardTypMax       = grammar.getVarHypTypSet().size()
                                    + 2;

        pPredictorTypMax          = pBringForwardTypMax;

        //defer array building until reInitArrays(0) in parseExpr
    }

    /**
     *  parseExpr - returns 'n' = the number of ParseTree
     *  objects generated for the input formula and stored in
     *  parseTreeArray.
     *  <p>
     *  The user can control whether or not the first successful
     *  parse is returned by passing in an array of length = 1.
     *
     *  @param parseTreeArrayIn holds generated ParseTrees, therefore,
     *         length must be greater than 0. The user can control
     *         whether or not the first successful parse is returned
     *         by passing in an array of length = 1. If the array
     *         length is greater than 1 the function attempts to fill
     *         the array with alternate parses (which if found, would
     *         indicate grammatical ambiguity.) Returned ParseTree
     *         objects are stored in array order -- 0, 1, 2, ... --
     *         and the contents of unused array entries is unspecified.
     *         If more than one ParseTree is returned, the returned
     *         ParseTrees are guaranteed to be different (no duplicate
     *         ParseTrees are returned!)
     *
     *  @param formulaTypIn    Cnst Type Code of the Formula to
     *         be parsed.
     *
     *  @param parseNodeHolderExprIn    Formula's Expression,
     *         preloaded into a ParseNodeHolder[] (see
     *         Formula(dot)getParseNodeHolderExpr(mandVarHypArray).
     *
     *  @param highestSeqIn  restricts the parse to statements with a
     *         sequence number less than or equal to highestSeq. Set
     *         this to Integer(dot)MAX_VALUE to enable grammatical parsing
     *         using all available rules.
     *
     *  @return int    specifies the number of ParseTree objects stored
     *         into parseTreeArray; a number greater than 1 indicates
     *         grammatical ambiguity, by definition, since there is
     *         more than one way to parse the formula.
     */
    public int parseExpr(ParseTree[]       parseTreeArrayIn,
                         Cnst              formulaTypIn,
                         ParseNodeHolder[] parseNodeHolderExprIn,
                         int               highestSeqIn,
                         boolean debug)
                                            throws VerifyException {
        parseTreeArray            = parseTreeArrayIn;
        formulaTyp                = formulaTypIn;
        parseNodeHolderExpr       = parseNodeHolderExprIn;
        highestSeq                = highestSeqIn;
        this.debug = debug;
        
        parseCnt = 0;

        if (parseNodeHolderExpr.length == 0) {
            return EarleyParserSpecialCase1();
        }

        if (parseNodeHolderExpr.length == 1 &&
             !(parseNodeHolderExpr[0].mObj.isCnst())) {
            return EarleyParserSpecialCase2();
        }

        prepareLen1CnstGimmesEtc();  // loads expr, etc.

        if (expr.length > pMax) {
            pMax = expr.length + 10;
            initArrays(pMax,
                       pItemSetMax,
                       pCompletedItemSetMax,
                       pBringForwardTypMax,
                       pPredictorTypMax);
        }

        boolean needToRetry = true;
        reInitArrays(0);
        while (needToRetry) {
            try {
                parseCnt    = 0;
                parse();           // parse expr now...
                needToRetry = false;
            }
            catch(ArrayIndexOutOfBoundsException e) {
                if (++retryCnt >
                    GrammarConstants.MAX_PARSE_RETRIES) {
                    throw new IllegalStateException(
                        GrammarConstants.ERRMSG_MAX_RETRIES_EXCEEDED_1
                      + GrammarConstants.MAX_PARSE_RETRIES
                      + GrammarConstants.ERRMSG_MAX_RETRIES_EXCEEDED_2
                        );
                }
                System.err.println(
                    GrammarConstants.ERRMSG_RETRY_TO_BE_INITIATED
                    + e.getMessage());
                reInitArrays(retryCnt);
            }
        }

        return parseCnt;
    }

    /**
     *  EarleyParserSpecialCase1 --
     *  Expression length = 0, occurs on Nulls Permitted Syntax
     *  Axioms or, in theory, on a Logical Hypothesis, Logical
     *  Axiom or Theorem (statements beginning with a valid
     *  Theorem TypeCode such as "|-").
     *  <p>
     *  In both cases, the parse
     *  should return a Nulls Permitted result, but the question
     *  is, for which Type Code? Answer: if the parsed Formula
     *  Type Code is a Provable Logic Statement Type code, then
     *  return a Nulls Permitted parse for one of the Logical
     *  Statement Type Codes (and if 2 results are possible then
     *  that is an ambiguity); otherwise, if the parsed Formula
     *  Type Code is not a Provable Logic Statement Type Code,
     *  then return a Nulls Permitted parse result for any of
     *  the non-Provable Logic Statement Type Codes, which
     *  includes the Logical Statement Type Codes (eg. if Formula
     *  Type Code is "wff" then if there is a Nulls Permitted Rule
     *  for "wff", return that, else no parse is possible.)
     *
     *  @return parse count -- number of parse trees generated.
     */
    private   int EarleyParserSpecialCase1() {

        NullsPermittedRule  gR;

        if (formulaTyp.getIsProvableLogicStmtTyp()) {
            Cnst[] logicStmtTypArray = grammar.getLogicStmtTypArray();
            for (int i = 0;
                 i < logicStmtTypArray.length;
                 i++) {
                gR                =
                    logicStmtTypArray[i].getNullsPermittedGR();
                if (gR != null &&
                    gR.getMaxSeqNbr() <= highestSeq) {
                    parseTreeArray[parseCnt++] =
                        new ParseTree(
                            gR.buildGrammaticalParseNode(
                                emptyParamArray));
                    if (parseCnt >= parseTreeArray.length) {
                        break;
                    }
                }
            }
        }
        else {
            List nullsPermittedGRList
                                  = grammar.getNullsPermittedGRList();
            int i = nullsPermittedGRList.indexOf(formulaTyp);
            if (i != -1) {
                gR =
                    (NullsPermittedRule)
                        nullsPermittedGRList.get(i);
                if (gR.getMaxSeqNbr() <= highestSeq) {
                    parseTreeArray[parseCnt++] =
                        new ParseTree(
                            gR.buildGrammaticalParseNode(
                                emptyParamArray));
                }
            }
        }
        return parseCnt;
    }


    /**
     *  EarleyParserSpecialCase2 --
     *  Expression with Length = 1 containing
     *  just a Variable occurs on Type Conversion Syntax Axioms,
     *  on Variable Hypothesis Statements and on Logical
     *  Statements (see ax-mp in set(dot)mm).
     *  <p>
     *  If the parsed Formula's
     *  Type Code is a Provable Logic Statement Type Code then
     *  the parse result should be just a Variable Hypothesis
     *  parse tree -- otherwise, the parse result should be a
     *  Type Conversion parse tree (NOTE: when parsing a
     *  Metamath database in its entirety, we do not send
     *  variable hypotheses through the parser, therefore we
     *  ignore that scenario in this special case.)
     *
     *  @return parse count -- number of parse trees generated.
     */
    private   int EarleyParserSpecialCase2() {

        TypeConversionRule  gR;

        if (formulaTyp.getIsProvableLogicStmtTyp()) {
            parseTreeArray[parseCnt++] =
                new ParseTree(parseNodeHolderExpr[0].parseNode);
        }
        else {
            gR = formulaTyp.findFromTypConversionRule(
                   ((VarHyp)parseNodeHolderExpr[0].mObj).getTyp());
            if (gR != null &&
                gR.getMaxSeqNbr() <= highestSeq) {
                parseTreeArray[parseCnt++] =
                    new ParseTree(
                        gR.buildGrammaticalParseNode(
                            parseNodeHolderExpr));
            }
        }
        return parseCnt;
    }

    private  void prepareLen1CnstGimmesEtc()
                                throws VerifyException {

        Cnst         cnst;
        NotationRule notationRule;

        expr                 = new ParseNodeHolder[
                                     parseNodeHolderExpr.length + 1];
        int          src     = 0;
        for (int dest = 1; dest < expr.length; src++, dest++) {

            if (parseNodeHolderExpr[src].mObj.isCnst()) {
                cnst         = (Cnst)parseNodeHolderExpr[src].mObj;
                if (cnst.getIsGrammaticalTyp()) {
                    throw new VerifyException(
                      GrammarConstants.ERRMSG_EXPR_USES_TYP_AS_CNST_1
                      + cnst.toString()
                      +
                      GrammarConstants.ERRMSG_EXPR_USES_TYP_AS_CNST_2
                      );
                }

                notationRule = cnst.getLen1CnstNotationRule();
                if (notationRule != null &&
                    notationRule.getIsGimmeMatchNbr() == 1) {

                    expr[dest] = new
                        ParseNodeHolder(
                            notationRule.buildGrammaticalParseNode(
                                emptyParamArray));

                    continue;
                }
            }
            expr[dest] = parseNodeHolderExpr[src];
        }
    }

    private void parse() throws VerifyException {

        int  p               = 1;
        int  pLast           = expr.length - 1; // [0] unused

        Cnst eP              = expr[1].getCnstOrTyp();
        Cnst ePPlus1         = null;
        if (expr.length > 2) {
            ePPlus1          = expr[2].getCnstOrTyp();
        }

        initDataStructureContents(0);

        /**
         *  Derive Start Rule Type code(s).
         */
        startRuleTyp         = null;
        if (formulaTyp.getIsProvableLogicStmtTyp()) {
            if (grammar.getLogicStmtTypArray().length > 0) {
                startRuleTyp = (grammar.getLogicStmtTypArray())[0];
                addToPredictorTypSet(startRuleTyp);
            }
            else {
                throw new IllegalStateException(
                    GrammarConstants.ERRMSG_START_RULE_TYPE_UNDEF_1
                    + formulaTyp
                    +
                    GrammarConstants.ERRMSG_START_RULE_TYPE_UNDEF_2);
            }
        }
        else {
            startRuleTyp     = formulaTyp;
            addToPredictorTypSet(startRuleTyp);

            /**
             *  add Types that convert to the Formula's Type
             *  because a Start Type may have only a Type Conversion
             *  Rule and not be picked up otherwise in The Predictor.
             */
            TypeConversionRule[] fromRules
                             =  startRuleTyp.getConvFromTypGRArray();
            if (fromRules != null) {
                for (int i = 0; i < fromRules.length; i++) {
                    addToPredictorTypSet(fromRules[i].getConvTyp());
                }
            }
        }

        earleyPredictor(0,  // to load itemset[0], initial itemset
                        eP,
                        highestSeq);

        int earleyCnt        = 0;
        do {
            initDataStructureContents(p);
            earleyCnt        = earleyScanner(p,
                                         eP);
            earleyCnt       += earleyCompletor(p);
            earleyCnt       += earleyPredictor(p,
                                           ePPlus1,
                                           highestSeq);
            ++p;
            if (p > pLast ||
                earleyCnt == 0 ) {
                break;
            }
            eP = ePPlus1;
            if (p >= pLast) {
                ePPlus1      = null;
            }
            else {
                ePPlus1      = expr[p + 1].getCnstOrTyp();
            }
        }
        while (true);
// /* --
//      dumpItemSets();
// */

        if (earleyCnt == 0 &&
            p <= pLast) {
            parseCnt = (p - 1) * -1;
        }
        else {
            buildTrees();
        }
    }

    private   int earleyPredictor(int  currPos,
                                  Cnst nextSym,
                                  int  maxSeq) {
    	if(debug) {
	      StringBuffer s = new StringBuffer();
	      s.append("--- earleyPredictor:");
	      s.append(" currPos "); s.append(currPos);
	      s.append(" nextSym "); s.append(nextSym);
	      s.append(" maxSeq ");  s.append(maxSeq);
	      s.append(" predTyp[");
	      for (int i = 0; i < pPredictorTypCnt; i++) {
	          s.append(pPredictorTyp[i]);
	          s.append(" ");
	      }
	      s.append("]");
	      System.out.println(s);
    	}
 
        int          predictedCnt = 0;
        LinkedList   typRules;
        NotationRule notationRule;
        Cnst         ruleExprFirstSym;
        Iterator     ruleIterator;

        /**
         *  Double loop to thwart loop optimizer in javac.
         *  Problem is that pPredictorTypCnt is
         *  updated indirectly inside the loop...Uh-oh.
         */
        int          qStart    = 0;
        int          qMax      = pPredictorTypCnt;
        while (true) {

            typLoop: for (int i = qStart;
                              i < qMax;
                              i++) {

                if ((typRules = pPredictorTyp[i].getEarleyRules())
                    == null) {
                    continue typLoop;
                }
                if (!pPredictorTyp[i].earleyFIRSTContainsSymbol(
                                                       nextSym)) {
                    continue typLoop;
                }

                ruleIterator = typRules.iterator();

                ruleLoop: while (ruleIterator.hasNext()) {
                    notationRule =
                        (NotationRule)ruleIterator.next();
                    if (notationRule.getMaxSeqNbr() > maxSeq) {
                        continue typLoop; //done with typ
                    }

                    ruleExprFirstSym =
                        notationRule.getRuleFormatExprFirst();
                   /**
                    *  oops? confusing here...remove following check?
                    *  just add the blasted thing!?#$? I think not...
                    *  we already know that the rule Type FIRST set
                    *  contains nextSym -- ok,fine -- but now we want
                    *  to weed out the list of mismatched Terminals
                    *  where "(" not = "-.", and where
                    *  ruleExprFirstSym is a Type Code and that
                    *  Type's EarleyFIRST set does not contain
                    *  nextSym. (Initially the code checked
                    *  for == (equal) and that excluded far
                    *  too many -- the valid ones! haha. OK,
                    *  here goes...
                    */
                    if (ruleExprFirstSym == nextSym ||
                        ruleExprFirstSym.earleyFIRSTContainsSymbol(
                                                        nextSym)) {
                        addPredictionToItemSet(
                                    currPos,           //itemset nbr
                                    notationRule,      //rule
                                    ruleExprFirstSym); //dotAfter
                        ++predictedCnt;
                    }
                }
            }
            if (qMax >= pPredictorTypCnt) {
                break;
            }
            qStart             = qMax;
            qMax               = pPredictorTypCnt;
        }
        return predictedCnt;
    }

    private   int earleyScanner(int  currPos,
                                Cnst currSym) {

    	if(debug)
    	  System.out.println(
	          "--- earleyScanner:"
	          + " currPos "  + currPos
	          + " currSym "  + currSym);

    	return getMatchesCloneAndUpdateDot(
                    currPos - 1,  // scan set number
                    currPos,      // comp/active Set Nbr
                    currSym);     // scan symbol

    }

    private int earleyCompletor(int currPos) {
// /*--
//      System.out.println(
//          "--- earleyCompletor:"
//          + " currPos "  + currPos);
// */
        int          outputCnt = 0;

        EarleyItem[] completedItemSet
                               = pCompletedItem[currPos];
        EarleyItem   completedItem;
        int          mMinus1;
        Cnst         typR;

        /**
         *  Double loop to thwart loop optimizer in javac.
         *  Problem is that pCompletedItemSetCnt[currPos] is
         *  updated indirectly inside the loop...Uh-oh.
         */
        int          qStart    = 0;
        int          qMax      = pCompletedItemSetCnt[currPos];
        while (true) {

            for (pCompletedItemSetIndex = qStart;
                 pCompletedItemSetIndex < qMax;
                 pCompletedItemSetIndex++) {
                completedItem  =
                    completedItemSet[pCompletedItemSetIndex];
                mMinus1        =
                    completedItem.atIndex - 1;
                typR           =
                    completedItem.rule.getGrammarRuleTyp();
                if (addToBringForwardTypSet(mMinus1,
                                            typR)) {
                    outputCnt +=
                        getMatchesCloneAndUpdateDot(
                                mMinus1,  // scan set number
                                currPos,  // comp/active Set Nbr
                                typR);    // scan symbol
                }
            }
            if (qMax >= pCompletedItemSetCnt[currPos]) {
                break;
            }
            qStart             = qMax;
            qMax               = pCompletedItemSetCnt[currPos];
        }
        return outputCnt;
    }

    private int getMatchesCloneAndUpdateDot(int  scanSetNbr,
                                            int  outputSetNbr,
                                            Cnst scanSymbol) {

    if(debug)
      System.out.println(
          "--- getMatchesCloneAndUpdateDot:"
          + " scanSetNbr "   + scanSetNbr
          + " outputSetNbr " + outputSetNbr
          + " scanSymbol "   + scanSymbol);

        int             outputCnt = 0;
        EarleyItem[]    scanItemSet = pItem[scanSetNbr];
        EarleyItem      earleyItem;
        NotationRule    notationRule;
        int             newDotIndex;
        Cnst            newAfterDot;

        scanLoop: for (int i = 0; i < pItemSetCnt[scanSetNbr]; i++) {
            earleyItem = scanItemSet[i];
            if (earleyItem.afterDot != scanSymbol) {
                continue scanLoop;
            }
            newDotIndex = earleyItem.dotIndex + 1;
            newAfterDot =
                earleyItem.rule.getRuleFormatExprIthSymbol(
                                                        newDotIndex);
            if (newAfterDot == null) {
                addItemToCompletedItemSet(outputSetNbr,
                                          earleyItem);
            }
            else {
                addActiveItemToItemSet(outputSetNbr,
                                       earleyItem,
                                       newDotIndex,
                                       newAfterDot);
            }
            ++outputCnt;
        }

        return outputCnt;
    }

    private void addItemToCompletedItemSet(
                                   int          scanSetNbr,
                                   EarleyItem   oldItem) {
// /* --
//      System.out.println(
//          "--- addItemToCompletedItemSet:"
//          + " scanSetNbr "   + scanSetNbr
//          + " oldItem "      + oldItem);
// */


        pCompletedItemSetIndex    =
                                   pCompletedItemSetCnt[scanSetNbr];
        EarleyItem[] pCompletedSet
                                  = pCompletedItem[scanSetNbr];

        /**
         *  Eliminate dups. This may be a redundant check. I
         *  think it is because of the Completor and BringForward
         *  Type Sets -- belt *and* suspenders?
         */
        for (int i = 0; i < pCompletedItemSetIndex; i++) {
            if (oldItem.equals(pCompletedSet[i])) {
                return;
            }
        }

        /**
         *  OK dummy, *now* increment the counter :)
         */
        ++pCompletedItemSetCnt[scanSetNbr];

        EarleyItem completedItem  =
            pCompletedSet[pCompletedItemSetIndex];

        if (completedItem == null) {
            completedItem         = new EarleyItem();
            pCompletedSet[pCompletedItemSetIndex]
                                  = completedItem;
        }

        completedItem.rule        = oldItem.rule;
        completedItem.atIndex     = oldItem.atIndex;
        completedItem.dotIndex    = oldItem.dotIndex + 1;
        completedItem.afterDot    = null;
    }

    private   void addActiveItemToItemSet(
                                   int          outputSetNbr,
                                   EarleyItem   oldItem,
                                   int          newDotIndex,
                                   Cnst         newAfterDot) {
// /*
//      System.out.println(
//          "--- addActiveItemToItemSet:"
//          + " outputSetNbr "   + outputSetNbr
//          + " oldItem "        + oldItem
//          + " newDotIndex "    + newDotIndex
//          + " newAfterDot "    + newAfterDot);
// */

        pItemSetIndex             = pItemSetCnt[outputSetNbr]++;
        EarleyItem activeItem     = pItem[outputSetNbr][pItemSetIndex];
        if (activeItem == null) {
            activeItem            = new EarleyItem();
            pItem[outputSetNbr][pItemSetIndex]
                                  = activeItem;
        }
        activeItem.rule           = oldItem.rule;
        activeItem.atIndex        = oldItem.atIndex;
        activeItem.dotIndex       = newDotIndex;
        activeItem.afterDot       = newAfterDot;
        if (newAfterDot.getIsVarTyp()) {
            addToPredictorTypSet(newAfterDot);
        }
    }


    private   void addPredictionToItemSet(
                        int          currPos,            //itemset nbr
                        NotationRule notationRule,       //rule
                        Cnst         ruleExprFirstSym) { //dotAfter
// /*
//      System.out.println(
//          "--- addPredictionToItemSet:"
//          + " currPos "        + currPos
//          + " notationRule "
//              + notationRule.getRuleNbr()
//              + ":"
//              + notationRule.getBaseSyntaxAxiom().getLabel()
//          + " ruleExprFirstSym "  + ruleExprFirstSym);
// */
        pItemSetIndex         = pItemSetCnt[currPos]++;
        EarleyItem earleyItem = pItem[currPos][pItemSetIndex];
        if (earleyItem == null) {
            earleyItem        = new EarleyItem();
            pItem[currPos][pItemSetIndex]
                              = earleyItem;
        }
        earleyItem.rule       = notationRule;
        earleyItem.atIndex    = currPos + 1;
        earleyItem.dotIndex   = 1;
        earleyItem.afterDot   = ruleExprFirstSym;
        if (ruleExprFirstSym != null &&
            ruleExprFirstSym.getIsVarTyp()) {
            addToPredictorTypSet(ruleExprFirstSym);
        }
    }

    private   void addToPredictorTypSet(Cnst typ) {
        for (int i = 0; i < pPredictorTypCnt; i++) {
            if (typ == pPredictorTyp[i]) {
                return;
            }
        }
        pPredictorTyp[pPredictorTypCnt++] = typ;
    }

    private   boolean addToBringForwardTypSet(int  prevSetNbr,
                                              Cnst typ) {
        Cnst[] bringForwardTyp = pBringForwardTyp[prevSetNbr];
        for (int i = 0; i < pBringForwardTypCnt[prevSetNbr]; i++) {
            if (bringForwardTyp[i] == typ) {
                return false;
            }
        }
        pBringForwardTypIndex = pBringForwardTypCnt[prevSetNbr]++;
        bringForwardTyp[pBringForwardTypIndex] = typ;
        return true;
    }

    private   void initDataStructureContents(int p) {
        pItemSetCnt[p]              = 0;
        pItemSetIndex               = 0;
        pCompletedItemSetCnt[p]     = 0;
        pCompletedItemSetIndex      = 0;
        // we use all bringForward up to p for each p!
        for (int i = 0; i <= p; i++) {
            pBringForwardTypCnt[i]  = 0;
        }
        pPredictorTypCnt            = 0;

    }

    /**
     *  loadEarleyFIRSTandRules --
     *  "clever" optimizations makes this really long :) sigh.
     */
    private   void loadEarleyFIRSTandRules() {

        int firstCapacity  = (grammar.getSymTblSize() * 4) / 3;

        //rules only loaded once
        rulesTypMax    = grammar.getVarHypTypSet().size() + 2;
        rulesTypCnt    = 0;
        ruleTypAndFIRSTTyp = new Cnst[rulesTypMax][];
        for (int i = 0; i < rulesTypMax; i++) {
            ruleTypAndFIRSTTyp[i]  = new Cnst[rulesTypMax];
        }

        NotationRule notationRule;
        Cnst         typ;
        Cnst         first;
        LinkedList   ruleSet;
        HashSet      firstSet;
        HashSet      nonTerminalHashSet;

        Iterator iterator  = grammar.getNotationGRSet().iterator();
        while (iterator.hasNext()) {
            notationRule   = (NotationRule)iterator.next();
            typ            = notationRule.getGrammarRuleTyp();
            if ((ruleSet   = typ.getEarleyRules()) == null) {
                ruleSet    = new LinkedList();
                typ.setEarleyRules(ruleSet);
            }
            ruleSet.add(notationRule);

            first = notationRule.getRuleFormatExprFirst();
            if (first.getIsVarTyp()) {
                addTypToRuleTypAndFIRSTTyp(typ, first);
            }
            else {
                addTypToRuleTypAndFIRSTTyp(typ, null);
            }

            firstSet       = typ.getEarleyFIRST();
            if (firstSet == null) {
                firstSet   = new HashSet(firstCapacity);
                typ.setEarleyFIRST(firstSet);
            }
            firstSet.add(first);
        }

        boolean changed  = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < rulesTypCnt; i++) {
                for (int j = 1;
                     ruleTypAndFIRSTTyp[i][j] != null;
                     j++) {
                    for (int ji = 0; ji < rulesTypCnt; ji++) {
                        if (ruleTypAndFIRSTTyp[i][j] ==
                            ruleTypAndFIRSTTyp[ji][0]) {
                            for (int m = 1;
                                 ruleTypAndFIRSTTyp[ji][m] != null;
                                 m++) {
                                if (addToFIRSTTyp(
                                        i,
                                        ruleTypAndFIRSTTyp[ji][m])) {
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        HashSet hashSetI;
        HashSet hashSetJ;
        for (int i = 0; i < rulesTypCnt; i++) {
            hashSetI = ruleTypAndFIRSTTyp[i][0].getEarleyFIRST();
            for (int j = 1;
                 ruleTypAndFIRSTTyp[i][j] != null;
                 j++) {
                /**
                 *  a grammatical Type Code may not have any Syntax
                 *  Axioms -- and no earleyRules or earleyFIRST.
                 */
                hashSetJ = ruleTypAndFIRSTTyp[i][j].getEarleyFIRST();
                if (hashSetJ        != null &&
                    hashSetJ.size() != 0) {
                    hashSetI.addAll(hashSetJ);
                }
            }
        }
    }

    private   void addTypToRuleTypAndFIRSTTyp(Cnst typ,
                                              Cnst firstTyp) {
        int i = 0;
        typLoop: while (true) {
            if (i < rulesTypCnt) {
                if (typ == ruleTypAndFIRSTTyp[i][0]) {
                    break;
                }
                else {
                    ++i;
                    continue typLoop;
                }
            }
            else {
                ruleTypAndFIRSTTyp[i][0] = typ;
                ++rulesTypCnt;
                break;
            }
        }

        // don't add null or Typ to itself
        if (firstTyp == null ||
            firstTyp == typ) {
            return;
        }

        int j = 1; //skip Typ which is in [i][0]
        firstLoop: while (true) {
            if (ruleTypAndFIRSTTyp[i][j] == firstTyp) {
                return;
            }
            if (ruleTypAndFIRSTTyp[i][j] == null) {
                ruleTypAndFIRSTTyp[i][j] = firstTyp;
                return;
            }
            ++j;
        }
    }

    private   boolean addToFIRSTTyp(int  typIndex,
                                    Cnst firstTyp) {
        if (firstTyp == null ||
            ruleTypAndFIRSTTyp[typIndex][0] == firstTyp) {
            return false;
        }

        int j = 1; //skip Typ which is in [i][0]
        while (true) {
            if (ruleTypAndFIRSTTyp[typIndex][j] == firstTyp) {
                return false;
            }
            if (ruleTypAndFIRSTTyp[typIndex][j] == null) {
                ruleTypAndFIRSTTyp[typIndex][j] = firstTyp;
                return true;
            }
            ++j;
        }
    }

//  private void dumpItemSets() {
//
//      EarleyItem cSet[];
//      EarleyItem aSet[];
//      EarleyItem c;
//      EarleyItem a;
//      Cnst[]     e;
//      StringBuffer eS;
//      StringBuffer exprString = new StringBuffer();
//      for (int i = 1; i <= (expr.length - 1); i++) {
//          exprString.append(expr[i].getCnstOrTyp());
//          exprString.append(" ");
//      }
//
//      System.out.println(
//          "***ItemSets for expr="
//          + exprString);
//
//      for (int i = (expr.length - 1); i >= 0; i--) {
//          System.out.println(
//              "    ---CompletedItemSet " + i + "---");
//          cSet = pCompletedItem[i];
//          for (int j = 0; j < pCompletedItemSetCnt[i]; j++) {
//              System.out.println(
//                  "        "
//                  + cSet[j].toString());
//          }
//          System.out.println(" ");
//          System.out.println(
//              "    ---ItemSet " + i + "---");
//          aSet = pItem[i];
//          for (int j = 0; j < pItemSetCnt[i]; j++) {
//              System.out.println(
//                  "        "
//                  + aSet[j].toString());
//          }
//          System.out.println(" ");
//      }
//  }

    private   void reInitArrays(int retry) throws VerifyException {
        if (retryCnt == -1) {
            retryCnt = 0;
            loadEarleyFIRSTandRules();
            initArrays(pMax,
                       pItemSetMax,
                       pCompletedItemSetMax,
                       pBringForwardTypMax,
                       pPredictorTypMax);
            return;
        }
        retryCnt = retry;

        if (pItemSetIndex > pItemSetHighwater) {
            pItemSetHighwater = pItemSetIndex;
        }

        if (pCompletedItemSetIndex > pCompletedItemSetHighwater) {
            pCompletedItemSetHighwater = pCompletedItemSetIndex;
        }

        if (retryCnt == 0) {
            return;
        }

        /**
         *  "active" item set size cannot? be larger than the number
         *  of rules, and because of the Predictor is likely to
         *  be much smaller. Hmmmmm...better safe than sorry.
         */
        int hardFailureMaximum;
        if (grammar.getNotationGRSet().size() <
                GrammarConstants.EARLEY_PARSE_MIN_ITEMSET_MAXIMUM) {

            hardFailureMaximum    = 2 *
                GrammarConstants.EARLEY_PARSE_MIN_ITEMSET_MAXIMUM;
        }
        else {
            hardFailureMaximum    = 2 *
                grammar.getNotationGRSet().size();
        }

        if (pItemSetIndex >= pItemSetMax) {
            if (pItemSetMax < hardFailureMaximum) {
                pItemSetMax *= 2;
                if (pItemSetMax > hardFailureMaximum) {
                    pItemSetMax = hardFailureMaximum;
                }
            }
            else {
                throw new VerifyException(
                    GrammarConstants.ERRMSG_EARLEY_ITEMSET_OVERFLOW_1
                    + pItemSetMax
                    +
                    GrammarConstants.ERRMSG_EARLEY_ITEMSET_OVERFLOW_2
                    );
            }
        }

        if (pCompletedItemSetIndex >= pCompletedItemSetMax) {
            if (pCompletedItemSetMax < hardFailureMaximum) {
                pCompletedItemSetMax *= 2;
                if (pCompletedItemSetMax > hardFailureMaximum) {
                    pCompletedItemSetMax = hardFailureMaximum;
                }
            }
            else {
                throw new VerifyException(
                  GrammarConstants.ERRMSG_EARLEY_C_ITEMSET_OVERFLOW_1
                  + pCompletedItemSetMax
                  +
                  GrammarConstants.ERRMSG_EARLEY_C_ITEMSET_OVERFLOW_2
                  );
            }
        }


        initArrays(pMax,
                   pItemSetMax,
                   pCompletedItemSetMax,
                   pBringForwardTypMax,
                   pPredictorTypMax);

    }

    private   void initArrays(int max,
                              int itemSetMax,
                              int completedItemSetMax,
                              int bringForwardTypMax,
                              int predictorTypMax) {

        pMax                    = max;
        pItemSetMax             = itemSetMax;
        pCompletedItemSetMax    = completedItemSetMax;
        pBringForwardTypMax     = bringForwardTypMax;
        pPredictorTypMax        = predictorTypMax;

        pItem                   = new EarleyItem[pMax][];
        pCompletedItem          = new EarleyItem[pMax][];
        pBringForwardTyp        = new Cnst[pMax][];
        for (int i = 0; i < pMax; i++) {
            pItem[i]            = new EarleyItem[pItemSetMax];
            pCompletedItem[i]   = new EarleyItem[
                                                pCompletedItemSetMax];
            pBringForwardTyp[i] = new Cnst[pBringForwardTypMax];
        }

        pItemSetCnt             = new int[pMax];
        pCompletedItemSetCnt    = new int[pMax];
        pBringForwardTypCnt     = new int[pMax];

        pPredictorTyp           = new Cnst[pPredictorTypMax];

    }


    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // ------------------                     --------------------
    // ------------------ t r e e   b u i l d --------------------
    // -------------------                    --------------------
    // -------------------      s t u f f     --------------------
    // -------------------                    --------------------
    // -------------------      b e l o w     --------------------
    // -------------------                    --------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------


    /**
     *  build tree(s) from Completed Itemsets, Start Rule Type
     *  and expr. Fun stuff :)
     */
    private   void buildTrees() {

        try {
            buildTreeForTyp(startRuleTyp,
                            null);
            if (parseCnt < parseTreeArray.length) {
                TypeConversionRule[] fromRule =
                    startRuleTyp.getConvFromTypGRArray();
                if (fromRule != null) {
                    for (int i = 0;
                         parseCnt < parseTreeArray.length &&
                         i < fromRule.length;
                         i++) {
                        buildTreeForTyp(fromRule[i].getConvTyp(),
                                        fromRule[i]);
                    }
                }
            }
        }
        catch(ArrayIndexOutOfBoundsException e) {
            /*
             *  Catch this and rename to prevent retry
             *  in other EarleyParser code.
             */
            throw new IllegalStateException(
                GrammarConstants.ERRMSG_FATAL_ARRAY_INDEX_ERROR,
                e);  //chained exception
        }
    }

    private   void buildTreeForTyp(
                    Cnst               searchTyp,
                    TypeConversionRule typeConversionRule) {

// /*
//      if (typeConversionRule == null) {
//      System.out.println(
//          "--- buildTreeForTyp:"
//          + " searchTyp "      + searchTyp
//          );
//      }
//      else {
//      System.out.println(
//          "--- buildTreeForTyp:"
//          + " searchTyp "      + searchTyp
//          + " typeConversionRule "
//              + typeConversionRule.getRuleNbr()
//              + ":"
//              + typeConversionRule.getBaseSyntaxAxiom().getLabel()
//          );
//      }
// */

        int               exprFrom
                                  = 1;
        int               exprThru
                                  = expr.length - 1;
        EarleyItem[]      completedSet
                                  = pCompletedItem[exprThru];
        EarleyItem        earleyItem;
        EarleyRuleMap     earleyRuleMap;
        ParseNodeHolder   firstRootHolder;
        ParseNodeHolder   nextRootHolder;
        ParseNodeHolder[] convParam
                                  = new ParseNodeHolder[1];
        itemLoop: for (int i = (pCompletedItemSetCnt[exprThru] - 1);
                       i >= 0;
                       i--) {
            earleyItem = completedSet[i];
            if (earleyItem.atIndex != exprFrom ||
                earleyItem.rule.getGrammarRuleTyp() != searchTyp) {
                continue itemLoop;
            }
            twinTreesCnt          = 0;
            twinTreesNeeded       = parseTreeArray.length
                                          - parseCnt
                                          - 1;
            earleyRuleMap         = new EarleyRuleMap(i,
                                                      exprFrom,
                                                      exprThru);
            earleyRuleMap.loadRuleMap();
            if (earleyRuleMap.ruleMapParseNodeHolder == null) {
                throw new IllegalStateException(
                  GrammarConstants.ERRMSG_EARLEY_HYP_PARAMS_NOTFND_1
                   + exprThru
                   +
                   GrammarConstants.ERRMSG_EARLEY_HYP_PARAMS_NOTFND_2
                   + exprFrom
                   +
                   GrammarConstants.ERRMSG_EARLEY_HYP_PARAMS_NOTFND_3
                   + earleyItem.toString());
            }

// /*
//      System.out.println(
//      "--- buildTreeForTyp():itemLoop"
//      + " ruleMapParseNodeHolder twinChain: "
//      + earleyRuleMap.ruleMapParseNodeHolder.dumpTwinChainToString(
//          ));
// /*
            firstRootHolder       =
                earleyRuleMap.ruleMapParseNodeHolder;
            nextRootHolder        = firstRootHolder;
            do {
                if (typeConversionRule == null) {
                    parseTreeArray[parseCnt++]
                                  = new ParseTree(
                        nextRootHolder.parseNode);
                }
                else {
                    convParam[0]  = nextRootHolder;
                    parseTreeArray[parseCnt++]
                                  = new ParseTree(
                        typeConversionRule.buildGrammaticalParseNode(
                            convParam));
                }
                if (parseCnt >= parseTreeArray.length) {
                    break itemLoop;
                }
                nextRootHolder    = nextRootHolder.fwd;
            } while (nextRootHolder != firstRootHolder &&
                     parseCnt < parseTreeArray.length);
        }
    }

    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // ------------------                       ------------------
    // ------------------ I N N E R   C L A S S ------------------
    // -------------------                      ------------------
    // -------------------    EarleyRuleMap     ------------------
    // -------------------                      ------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------
    // -----------------------------------------------------------

    /**
     *  EarleyRuleMap is a complicated, recursive structure that
     *  is used in building a ParseTree using Earley Parse
     *  CompletedItemSets for an expression that has been
     *  successfully parsed.
     *
     *  (This is the second implementation of the tree building
     *  problem. The first go-round used recursive calls
     *  and was completely indecipherable, even with a diagram,
     *  as mysterious to the author just 5 minutes after completion
     *  of testing as quantum physics or politics.)
     */
    private   class EarleyRuleMap {

        /**
         *  Index into completedItemSet[exprThru] of EarleyItem
         *  that maps from a GrammarRule to a portion of an
         *  expression being parsed.
         *  <p>
         *  <ul>
         *    <li> rule = completedItemSet[exprThru][itemIndex].rule;
         *
         *    <li> A value of '-1' indicates search of Completed Item
         *         Set should proceed from the beginning at the thru
         *         set,
         *
         *    <li> A value of '-2' indicates that the search has
         *         already been (totally) performed and nothing more
         *         should or can be done.
         *  </ul>
         */
        int             itemIndex;

        /**
         *  Type Code from EarleyItem.rule, here for convenience
         */
        Cnst            typ;

        /**
         *  Index position of this map as a
         *  GrammarRule hypothesis within a higher
         *  level map's GrammarRule.
         */
        int             hypPos;


        /**
         *  Index location of start of parameter in the
         *  expression being parsed.
         */
        int             exprFrom;

        /**
         *  Index location of end of parameter in the
         *  expression being parsed.
         */
        int             exprThru;

        /**
         *  Array of (sub)EarleyRuleMaps, one element for each
         *  hypothesis of the GrammarRule of the (super)EarleyRuleMap.
         */
        EarleyRuleMap[] hypMap;

        /**
         *  ruleMapParseNodeHolder:
         *  after full loading of EarleyRuleMap
         *  contains reference to ParseNodeHolder of root
         *  of parse tree for the rule map.
         *  <p>
         *  The ParseNodeHolder
         *  contains "fwd" and "bwd" references forming a
         *  twin chain that enables multiple parse trees
         *  to be generated (for ambiguous grammar parses).
         *  <p>
         *  Note that a hypMap can contain a parseNodeHolder
         *  to just a VarHyp with no corresponding rule
         *  from EarleyParse in the Completed ItemSet; this
         *  is because only NotationRules are input directly
         *  to the parser, with NullsPermitted and
         *  TypeConversion rules being "pre-parsed" into the
         *  input expr.
         */
        ParseNodeHolder ruleMapParseNodeHolder;

        /**
         *  firstParseNodeHolder -- first generated in chain.
         *  Has to be allocated to build the trees eventually
         *  so might as well do it here and keep it together
         *  with ruleMapParseNodeHolder (this is just record-keeping).
         */
        ParseNodeHolder firstParseNodeHolder;


        /**
         *  default constructor
         */
        private   EarleyRuleMap() {
            itemIndex             = -1;
            hypPos                = -1;
        }

        /**
         *  creates rule map
         */
        private   EarleyRuleMap(int itemIndex,
                                int exprFrom,
                                int exprThru) {
            this.itemIndex        = itemIndex;
            this.exprFrom         = exprFrom;
            this.exprThru         = exprThru;
            hypPos                = -1;
        }

        /**
         *  initializes hypMap[i] within a rule map.
         *
         *  @param hypPos index in ruleFormatExpression
         *                where a sub-map must be constructed.
         *  @param ruleFormatExpr expression from grammar rule.
         */
        private   EarleyRuleMap(int    hypPos,
                                Cnst[] ruleFormatExpr) {
            itemIndex             = -1;
            typ                   = ruleFormatExpr[hypPos];
            this.hypPos           = hypPos;
        }

        private   void loadRuleMap() {
// /*
//          System.out.println(
//              "--- loadRuleMap:"
//              + " exprThru "      + exprThru
//              + " itemIndex "     + itemIndex
//              + " ruleFormatExpr="
//          + pCompletedItem[
//              exprThru][itemIndex].rule.getRuleFormatExprAsString()
//          );
// */

            EarleyItem earleyItem =
                pCompletedItem[exprThru][itemIndex];

            GrammarRule rule      = earleyItem.rule;
            Cnst[] ruleFormatExpr = rule.getRuleFormatExpr();

            /**
             *  if ruleFormatExpr longer than parse expr
             *  subsequence then this is the wrong rule;
             *  it can only be <= parse expr length.
             */
            if (ruleFormatExpr.length > (1
                                         + exprThru
                                         - exprFrom)) {
                return;
            }

            typ                   = rule.getGrammarRuleTyp();

            int nbrHyps           = rule.getNbrHypParamsUsed();
            hypMap                = new EarleyRuleMap[nbrHyps];
            if (nbrHyps == 0) {
                if (ruleFormatExpr.length == (1
                                              + exprThru
                                              - exprFrom)) {
                    /**
                     *  A rare "all constant" grammar
                     *  rule. Presumably an identical match
                     *  to the parse expr subsequence -- if not
                     *  EarleyParse has a big bug! So, generate
                     *  the parse node and vamoose, we're done.
                     */
                    ruleMapParseNodeHolder
                                  = new
                        ParseNodeHolder(
                            rule.buildGrammaticalParseNode(
                                emptyParamArray));
                    ruleMapParseNodeHolder.initTwinChain();
                }
                return;
            }

            if (!findFirstSetOfHypMapRules(hypMap,
                                           rule,
                                           exprFrom,
                                           exprThru,
                                           ruleFormatExpr)) {
                return;
            }

            do {
                finishLoadingHypMapEntries();
                updateParseNodeHolderWithParams(rule);
                if (twinTreesCnt >= twinTreesNeeded) {
                    break;
                }
                /**
                 *  Try to find another set of matching
                 *  hyp's for the rule, beginning where
                 *  we left off. This is perverse, but
                 *  because the Earley CompletedSets complete
                 *  at the end, we finds sets of hyp's in
                 *  reverse order, from last to first -- but
                 *  now, we pick up the search at the first
                 *  hyp to see if there is another match
                 *  at the same from/thru sub-sequence of
                 *  the expression.
                 */
                if (!loadMatchingHypsForRule(
                        hypMap,
                        0,                 // startMapIndex!
                        exprFrom,
                        exprThru,
                        ruleFormatExpr)) {
                    break;
                }
                ++twinTreesCnt;
            } while (true);
        }

        private   void finishLoadingHypMapEntries() {
// */
//          System.out.println(
//              "--- finishLoadingHypMapEntries():"
//              + " hypMap.length " + hypMap.length
//              );
// */

            for (int i = 0; i < hypMap.length; i++) {
                if (hypMap[i].ruleMapParseNodeHolder == null) {
                    hypMap[i].loadRuleMap();
                    if (hypMap[i].ruleMapParseNodeHolder == null) {
                        throw new IllegalStateException(
                GrammarConstants.ERRMSG_EARLEY_HYPMAP_PARAMS_NOTFND_1
                            + i
                            +
                GrammarConstants.ERRMSG_EARLEY_HYPMAP_PARAMS_NOTFND_2
                            + hypMap[i].exprFrom
                            +
                GrammarConstants.ERRMSG_EARLEY_HYPMAP_PARAMS_NOTFND_3
                            + hypMap[i].exprThru
                            +
                GrammarConstants.ERRMSG_EARLEY_HYPMAP_PARAMS_NOTFND_4
                            + pCompletedItem[
                                hypMap[i].exprThru][
                                hypMap[i].itemIndex].toString());
                    }
                }
            }
        }

        /**
         *  generate -- or add twins to - the ruleMapParseNodeHolder
         *  for this EarleyRuleMap. This requires going through
         *  all combinations of the hypMap array of parseNodeHolders
         *  and *their* twins, and generating a parse sub-tree
         *  for each combination (i.e. if there are 3 hypMaps
         *  for this EarleyRuleMap and each has a primary/first
         *  ruleMapParseNodeHolder + 1 twin at ruleMapParseNodeHolder.fwd,
         *  then there would be 2**3 output parse sub-trees.)
         *
         *  Note: as written, this routine doesn't track the
         *  twinTreesCnt or twinTreesNeeded variables -- it
         *  *assumes* that the routine that generated these
         *  twins has already done so. Uh oh :)
         */
        private   void updateParseNodeHolderWithParams(
                                                GrammarRule rule) {
// */
//          System.out.println(
//              "--- updateParseNodeHolderWithParams():"
//              + " rule " + rule.getRuleNbr()
//              + ":"
//              + rule.getBaseSyntaxAxiom().getLabel()
//          + " ruleFormatExpr="  + rule.getRuleFormatExprAsString()
//              );
// */

            ParseNodeHolder[] paramArray
                                  = new ParseNodeHolder[
                                        hypMap.length];
            for (int i = 0; i < hypMap.length; i++) {
                hypMap[i].firstParseNodeHolder
                                  = hypMap[i].ruleMapParseNodeHolder;
                paramArray[i]     = hypMap[i].ruleMapParseNodeHolder;
            }

            int             spin;
            boolean         carry;
            twinLoop: do {
                ParseNodeHolder next
                                  = new
                    ParseNodeHolder(
                        rule.buildGrammaticalParseNode(
                            paramArray));

                if (ruleMapParseNodeHolder == null) {
                    ruleMapParseNodeHolder
                                  = next;
                    ruleMapParseNodeHolder.initTwinChain();
                }
                else {
                    ruleMapParseNodeHolder.addToTwinChain(next);
                }

// */
//              System.out.println(
//                  "--- updateParseNodeHolderWithParams():twinLoop:"
//                  + " twinTreesCnt = "    + twinTreesCnt
//                  + " twinTreesNeeded = " + twinTreesNeeded
//                  + " ruleMapParseNodeHolder twinChain: "
//                  + ruleMapParseNodeHolder.dumpTwinChainToString()
//                  );
// */

                /**
                 *  "spin" refers to something similar to
                 *  turning tumblers on a combination lock
                 *  that has multiple wheels, but is also
                 *  similar to adding 1 to a number and
                 *  having to "carry 1" when the last digit
                 *  value is reached at a given position;
                 *  if the final position overflows with a
                 *  carry then that means we are done.
                 */
                spin              = hypMap.length - 1;
                spinLoop: do {
                    if (paramArray[spin].fwd !=
                            hypMap[spin].firstParseNodeHolder) {
                        paramArray[spin]
                                  = paramArray[spin].fwd;
                        carry     = false;
                        break spinLoop;
                    }
                    paramArray[spin]
                                  = hypMap[spin].firstParseNodeHolder;
                    carry         = true;
                } while (--spin >= 0);
            } while (!carry);
        }


        /**
         *  We are given a closed subsequence of expr, exprFrom and
         *  exprThru, as well as a GrammarRule which supposedly
         *  "matches" that subset. The rule is the top level rule
         *  for a parse of the expr subset, in other words.
         *
         *  Our job is to "map" each rule hypothesis parameter to a
         *  subsequence of the expr subsequence, and to
         *  double-check that the remaining sections, which are
         *  constants like ")" are identical; this enables us to
         *  guarantee that we have produced correct mappings even
         *  though the Earley Completed ItemSets may contain
         *  multiple parsings (if the grammar is ambiguous.)
         *
         *  In this routine we return the first mapping we find
         *  even though there may be multiple valid mappings.
         *
         *  This is a "breadth-first" mapping operation. The
         *  returned <code>EarleyRuleMap</code> elements
         *  may themselves require deeper mappings. The reason
         *  for breadth-first is that want to quickly determine
         *  the invalid hypothesis mappings.
         *
         *   @param hypMap -- (i/o parameter) array to be loaded
         *
         *   @param rule -- The GrammarRule matched by the
         *    EarleyParser to a subsequence of <code>expr</code>
         *
         *   @param exprFromR -- the leftmost position within
         *          <code>expr</code> for this rule.
         *
         *   @param exprThruR -- the rightmost position within
         *          <code>expr</code> for this rule.
         *
         *   @return true for success, false for "no cigar".
         *
         */
        private   boolean findFirstSetOfHypMapRules(
                              EarleyRuleMap[] hypMap,
                              GrammarRule         rule,
                              int                 exprFromR,
                              int                 exprThruR,
                              Cnst[]              ruleFormatExpr) {

// */
//          System.out.println(
//              "--- findFirstSetOfHypMapRules():"
//              + " exprFromR " + exprFromR
//              + " exprThruR " + exprThruR
//              + " rule " + rule.getRuleNbr()
//              + ":"
//              + rule.getBaseSyntaxAxiom().getLabel()
//          + " ruleFormatExpr="  + rule.getRuleFormatExprAsString()
//              );
// */

            int[]  hypPos         = rule.getRuleHypPos();
            for (int i = 0; i < hypMap.length; i++) {
                hypMap[i]         = new EarleyRuleMap(
                                            hypPos[i],
                                            ruleFormatExpr);
            }

            /**
             *  The known quantities are the leftmost position
             *  of the first rule hypothesis, and the rightmost
             *  position of the last rule hypothesis. Everything
             *  else must be deduced, starting from these facts.
             */
            hypMap[0].exprFrom         = exprFromR + hypPos[0];
            int lastMapIndex           = hypMap.length - 1;
            hypMap[lastMapIndex].exprThru
                                    = exprThruR -
                                      (ruleFormatExpr.length -
                                       1 -
                                       hypPos[lastMapIndex]);

            /**
             *  Verify that the terminal symbols to the right
             *  of the rule's last hypothesis match the
             *  corresponding symbols in the subsequence of
             *  expr that we are mapping. A mismatch indicates
             *  a programming error because we have been told
             *  that the rule DOES match this subsequence of
             *  expr!
             */
            if (!verifyRightTweeners(hypMap[lastMapIndex].exprThru,
                                     hypMap[lastMapIndex].hypPos,
                                     ruleFormatExpr.length,
                                     ruleFormatExpr)) {
                throw new IllegalStateException(
                    GrammarConstants.ERRMSG_RIGHT_TWEENER_ERROR_1
                    + hypMap[lastMapIndex].exprThru
                    + GrammarConstants.ERRMSG_RIGHT_TWEENER_ERROR_2
                    + hypMap[lastMapIndex].hypPos
                    + GrammarConstants.ERRMSG_RIGHT_TWEENER_ERROR_3
                    + GrammarRule.showRuleFormatExprAsString(
                            ruleFormatExpr)
                    );
            }

            /**
             *  Verify that the terminal symbols to the left
             *  of the rule's first hypothesis match the
             *  corresponding symbols in the subsequence of
             *  expr that we are mapping. A mismatch indicates
             *  a programming error because we have been told
             *  that the rule DOES match this subsequence of
             *  expr!
             */
            if (!verifyRightTweeners(exprFromR - 1,
                                     -1,
                                     hypMap[0].hypPos,
                                     ruleFormatExpr)) {
                throw new IllegalStateException(
                    GrammarConstants.ERRMSG_LEFT_TWEENER_ERROR_1
                    + (exprFromR - 1)
                    + GrammarConstants.ERRMSG_LEFT_TWEENER_ERROR_2
                    + hypMap[0].hypPos
                    + GrammarConstants.ERRMSG_LEFT_TWEENER_ERROR_3
                    + GrammarRule.showRuleFormatExprAsString(
                            ruleFormatExpr)
                    );
            }

            /**
             *  Now, load the hypMap in reverse order while
             *  doing the detective work to find a set of
             *  mappings that "fit" the facts. This means
             *  that a partial fit may fail and that we have
             *  to reverse course and re-do part of the
             *  search.
             */
            return loadMatchingHypsForRule(hypMap,
                                           lastMapIndex,
                                           exprFromR,
                                           exprThruR,
                                           ruleFormatExpr);
        }

        private   boolean loadMatchingHypsForRule(
                                EarleyRuleMap[] hypMap,
                                int             startMapIndex,
                                int             exprFromR,
                                int             exprThruR,
                                Cnst[]          ruleFormatExpr) {

// */
//          System.out.println(
//              "--- loadMatchingHypsForRule():"
//              + " startMapIndex " + startMapIndex
//              + " exprFromR "     + exprFromR
//              + " exprThruR "     + exprThruR
//              );
// */


            int mapIndex          = startMapIndex;
            int lastMapIndex      = hypMap.length - 1;
            mapLoop: while (true) {

// */
//          System.out.println(
//              "--- loadMatchingHypsForRule():mapLoop:"
//              + " mapIndex = " + mapIndex
//              );
// */
                if (loadOneHypMapEntry(hypMap,
                                      mapIndex,
                                      exprFromR,
                                      exprThruR,
                                      ruleFormatExpr)) {
                    if (mapIndex == 0) {
                        return true;
                    }
                    --mapIndex;
                    continue mapLoop;
                }
                if (mapIndex >= lastMapIndex) {
                    return false;
                }
                for (int z = mapIndex; z >= 0; z--) {
                    hypMap[z].itemIndex
                                  = -1;
                    hypMap[z].ruleMapParseNodeHolder
                                  = null;
                }
                ++mapIndex;
                continue mapLoop;
            }
        }

        private   boolean loadOneHypMapEntry(
                              EarleyRuleMap[] hypMap,
                              int             mapIndex,
                              int             exprFromR,
                              int             exprThruR,
                              Cnst[]          ruleFormatExpr) {

// */
//          System.out.println(
//              "--- loadOneHypMapEntry():"
//              + " mapIndex "      + mapIndex
//              + " exprFromR "     + exprFromR
//              + " exprThruR "     + exprThruR
//              + " hypMap[mapIndex].itemIndex "
//                          + hypMap[mapIndex].itemIndex
//              + " hypMap[mapIndex].exprThru "
//                          + hypMap[mapIndex].exprThru
//              );
// */

            EarleyRuleMap hypMapEntry
                                  = hypMap[mapIndex];

            /**
             *  -2 = already loaded without a rule, nothing more
             *       to do on subsequent call.
             */
            if (hypMapEntry.itemIndex == -2) {
                return false;
            }

            hypMapEntry.ruleMapParseNodeHolder
                                  = null; //init to null just in case

            EarleyRuleMap prevMapEntry
                                  = null;
            if (mapIndex > 0) {
                prevMapEntry      = hypMap[mapIndex - 1];
            }


            EarleyItem[]      completedSet
                                  = pCompletedItem[
                                        hypMapEntry.exprThru];
            EarleyItem        earleyItem;

            if (hypMapEntry.itemIndex < 0) {
                hypMapEntry.itemIndex
                                  =
                    pCompletedItemSetCnt[hypMapEntry.exprThru];
            }
            itemLoop: while (true) {

// */
//              System.out.println(
//                  "--- loadOneHypMapEntry():itemLoop:"
//                  + " hypMapEntry.itemIndex "
//                  +   hypMapEntry.itemIndex
//                  );
// */


                if (--hypMapEntry.itemIndex < 0) {
                    break itemLoop;
                }

                earleyItem        = completedSet[
                                        hypMapEntry.itemIndex];

                if (earleyItem.rule.getGrammarRuleTyp() !=
                    hypMapEntry.typ)      {
                    continue itemLoop;
                }

                if (mapIndex == 0) {
                    if (hypMapEntry.exprFrom == earleyItem.atIndex) {
                        return true; //successful mapping
                    }
                    else {
                        continue itemLoop; //does not fit the facts!
                    }
                }

                if (earleyItem.atIndex < exprFromR) {
                    continue itemLoop;     //does not fit the facts;
                }

                /**
                 *  map shorter subsequence of expr to earleyItem rule
                 *  taking us deeper into the parse!
                 */
                hypMapEntry.exprFrom = earleyItem.atIndex;

                prevMapEntry.exprThru = hypMapEntry.exprFrom
                                        - (hypMapEntry.hypPos
                                           - prevMapEntry.hypPos);
                if (prevMapEntry.exprThru < exprFromR) {
                    continue itemLoop; //does not fit the facts
                }

                if (!verifyRightTweeners(
                        prevMapEntry.exprThru,   //exprPos
                        prevMapEntry.hypPos,     //ruleFormatExprPos
                        hypMapEntry.hypPos,      //ruleCompareStop
                        ruleFormatExpr)) {       //ruleFormatExpr
                    continue itemLoop; //does not fit the facts
                }

                /**
                 *  Reset the itemIndex of the previous hypMapEntry
                 *  to -1 which will force him to search the
                 *  completedItem[prevMapEntry.exprThru] all over
                 *  again -- the previous value is irrelevant
                 *  because we have changed this hypMapEntry's
                 *  completedItem, thus generating a unique tree.
                 *  (Failure to do this leads to a null pointer
                 *  exception, at best!)
                 */
                prevMapEntry.itemIndex = -1;



                return true; // bingo
            }

// */
//          System.out.println(
//              "--- loadOneHypMapEntry():beyond itemLoop:"
//              + " hypMapEntry.itemIndex "
//              +   hypMapEntry.itemIndex
//              );
// */


            /**
             *  ok, no success here with the Completed ItemSets,
             *  but we must also try for a one symbol match directly
             *  to expr -- which would be the case for a VarHyp
             *  or other pre-parsed Stmt already loaded into
             *  expr.
             */
            if (expr[hypMapEntry.exprThru].mObj.isCnst()  ||
                expr[hypMapEntry.exprThru].parseNode.getStmt(
                                                        ).getTyp()
                !=
                hypMapEntry.typ) {
                return false;
            }

            if (mapIndex == 0) {
                if (hypMapEntry.exprFrom == hypMapEntry.exprThru) {
                    hypMapEntry.itemIndex
                                  = -2; // code = no rule
                    hypMapEntry.ruleMapParseNodeHolder
                                  = expr[hypMapEntry.exprThru];

                    hypMapEntry.ruleMapParseNodeHolder.initTwinChain();
                    return true;
                }
                else {
                    return false;
                }
            }

            hypMapEntry.exprFrom     = hypMapEntry.exprThru;

            prevMapEntry.exprThru = hypMapEntry.exprFrom
                                    - (hypMapEntry.hypPos
                                       - prevMapEntry.hypPos);

            if (prevMapEntry.exprThru < exprFromR) {
                return false;               //does not fit the facts
            }

            if (!verifyRightTweeners(
                    prevMapEntry.exprThru,   //exprPos
                    prevMapEntry.hypPos,     //ruleFormatExprPos
                    hypMapEntry.hypPos,      //ruleCompareStop
                    ruleFormatExpr)) {       //ruleFormatExpr
                return false;
            }

            hypMapEntry.itemIndex = -2; // code = no rule
            hypMapEntry.ruleMapParseNodeHolder
                                  = expr[hypMapEntry.exprThru];
            hypMapEntry.ruleMapParseNodeHolder.initTwinChain();

            /**
             *  Reset the itemIndex of the previous hypMapEntry
             *  to -1 which will force him to search the
             *  completedItem[prevMapEntry.exprThru] all over
             *  again -- the previous value is irrelevant
             *  because we have changed this hypMapEntry's
             *  completedItem, thus generating a unique tree.
             *  (Failure to do this leads to a null pointer
             *  exception!)
             */
            prevMapEntry.itemIndex = -1;

            return true;
        }

        private   boolean verifyRightTweeners(int    exprPos,
                                              int    rulePos,
                                              int    ruleCompareStop,
                                              Cnst[] ruleFormatExpr) {
            while (++rulePos < ruleCompareStop) {
                if (expr[++exprPos].getCnstOrTyp() !=
                    ruleFormatExpr[rulePos]) {
                    return false;
                }
            }
            return true;
        }
    }

}

