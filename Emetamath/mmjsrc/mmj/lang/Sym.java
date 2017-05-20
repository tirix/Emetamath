//********************************************************************/
//* Copyright (C) 2005  MEL O'CAT  mmj2 (via) planetmath (dot) org   */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Sym.java  0.03 11/01/2006
 *
 *  Version 0.03:
 *
 *  Oct-12-2006: - added StmtTbl to constructor and modified to
 *                 conform to Metamath.pdf spec change of 6-24-2006
 *                 prohibiting Stmt label and Sym id namespace
 *                 collisions.
 */

package mmj.lang;

import java.util.Comparator;
import java.util.Map;

/**
 *  Sym holds a declared Metamath symbol and is the base
 *  class for Cnst and Var.
 *
 *  In mmj there is only one occurrence of each symbol,
 *  and this includes constants and variables. For example,
 *  there is only one "(".
 *
 *  @see <a href="../../MetamathERNotes.html">
 *       Nomenclature and Entity-Relationship Notes</a>
 */
public abstract class Sym extends MObj
                                implements Comparable {
    /**
     *  "id" is the character string that uniquely identifies
     *  the Sym.
     *  <p>
     *  <code>Sym</code> is parent of <code>Cnst</code> and
     *  <code>Var</code>, which are the only classes that
     *  can be used in a mmj.lang.Formula.
     *  <p>
     *  id is equivalent to the label of a Stmt -- a unique
     *  identifier, but there is no Metamath specification
     *  against having aSym.id.equal(someStmt.label). Conversely
     *  to Sym, the Stmt.label can *only* be used inside
     *  proofs and expression parse RPN's -- a Stmt.label can
     *  never be used in a Formula, and a Sym.id can never be
     *  used in a proof.
     *  <p>
     *  Because of the design of mmj, the specific value of
     *  id is irrelevant except for sorting/output once LogicalSystem
     *  is loaded. That is because there is only one occurrence
     *  of each Sym in the entire system (and likewise for
     *  Stmt). Throughout mmj comparisons of <code>Sym</code>s
     *  proceeds using the "==" operator, for this very reason:
     *  if the objects are different objects then they have
     *  different <code>id</code>s or <code>label</code>s. We
     *  simply do not *care* what the value of <code>id</code>
     *  is as long as it is not an empty string or a duplicate.
     *
     *  Note: label must NOT be changed after Stmt added to stmtTbl
     *        because stmtTbl is a Map (map behavior undefined)
     */
    private final String  id;

    /**
     *  Construct using sequence number and id string.
     *
     *  @param seq  MObj.seq number
     *  @param id   Sym id string
     *
     *  @throws IllegalArgumentException if id string is empty
     */
    protected Sym(int    seq,
            	  String id) {
        super(seq);
        if (id.length() == 0) {
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_SYM_ID_STRING_EMPTY);
        }
        this.id = id;
    }

    /**
     *  Construct using sequence number and id string.
     *
     *  @param seq     MObj.seq number
     *  @param symTbl  Symbol Table
     *  @param stmtTbl Statement Table
     *  @param id      Sym id string
     *
     *  @throws LangException if Sym.id duplicates the id of
     *          another Sym (Cnst or Var) or a Stmt label.
     */
    public Sym(int     seq,
               Map     symTbl,
               Map     stmtTbl,
               String  id)
                            throws LangException {
        this(seq,
             id);
        if (symTbl.containsKey(id)) {
            throw new LangException(
                LangConstants.ERRMSG_DUP_VAR_OR_CNST_SYM +
                id);
        }
        if (stmtTbl.containsKey(id)) {
            throw new LangException(
                LangConstants.ERRMSG_SYM_ID_DUP_OF_STMT_LABEL_1 +
                id);
        }
    }

    /**
     *  Return Sym.id String
     *
     *  @return Sym.id String
     */
    public String getId() {
        return id;
    }

    /**
     *  Is Sym active?
     *  <p>
     *  <code>Cnst</code>s are always active as they
     *  cannot be defined inside a scope level,
     *  but a Var defined in a scope level
     *  is "inactive" outside of that scope.
     *  <p>
     *  The question "is Sym 'x' active" has relevance
     *  only in relation to a Stmt: only "active"
     *  <code>Sym</code>s can be used in a given
     *  <code>Stmt</code>'s Formula.
     *
     *  @return is Sym "active"
     */
    public abstract boolean isActive();

    /**
     *  Is Sym a Cnst MObj?
     *
     *  @return Returns <code>true</code> if Sym is a Cnst MObj,
     *  otherwise <code>false</code>.
     */
    public abstract boolean isCnst();

    /**
     *  Is Sym a Var MObj?
     *
     *  @return Returns <code>true</code> if Sym is a Var MObj,
     *  otherwise <code>false</code>.
     */
    public abstract boolean isVar();

    /**
     *  converts to String
     *
     *  @return returns Sym.id string;
     *
     */
    public String toString() {
        return id;
    }

    /**
     * Computes hashcode for this Sym
     *
     * @return hashcode for the Sym (Sym.id.hashcode())
     */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Compare for equality with another Sym.
     * <p>
     * Equal if and only if the Sym id strings are equal.
     * and the obj to be compared to this object is not null
     * and is a Sym as well.
     * <p>
     * Note that "equals" is identical to "==" for Sym
     * and Stmt MObj's.
     *
     * @param obj another Sym -- otherwise will return false.
     *
     * @return returns true if equal, otherwise false.
     */
    public boolean equals(Object obj) {
        return (this == obj) ? true
                : !(obj instanceof Sym) ? false
                        : id.equals(((Sym)obj).id);
    }

    /**
     * Compares Sym object based on the primary key, id.
     *
     * @param obj Sym object to compare to this Sym
     *
     * @return returns negative, zero, or a positive int
     * if this Sym object is less than, equal to
     * or greater than the input parameter obj.
     *
     */
    public int compareTo(Object obj) {
        return id.compareTo(((Sym)obj).id);
    }

    /**
     *  ID sequences by Sym.id.
     */
    static public final Comparator ID
            = new Comparator() {
        public int compare(Object o1, Object o2) {
            return (((Sym)o1).id.compareTo(((Sym)o2).id));
        }
    };
}
