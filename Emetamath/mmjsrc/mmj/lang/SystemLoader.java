//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  SystemLoader.java  0.05 08/01/2008
 *
 *      --> 10-Dec-2005: add "prematureEOF" param to finalizeEOF()
 *          so that a user-requested termination of the load at
 *          a label or statement number can exit gracefully (need
 *          to exit nested scopes).
 *
 *  Version 0.04: 04/01/2006 --
 *      --> added addTheorem() variant with compressed proof parms
 *
 *  Version 0.05: 08/01/2008 --
 *      --> Addes addNewChapter(), addNewSection() and
 *          isBookManagerEnabled() for BookManager.java.
 */

package mmj.lang;
import java.util.ArrayList;

import mmj.mmio.SourcePosition;

/**
 *  Interface for loading Metamath statements into a Logic System.
 *  <p>
 *  Interface, initially for mmj.lang.LogicalSystem and passed to
 *  mmj.mmio.Systemizer. Allows a different Logical System
 *  to be substituted. A different use is possible, such as
 *  dumping the parsed .mm file statements somewhere else.
 *  Systemizer has no need to know anything about LogicalSystem
 *  except where to send the data.
 */
public interface SystemLoader {

    /**
     *  Add Cnst to Logical System.
     *
     * @param id  Constant's symbol string to be added to the
     *        Logical System.
     *
     * @return Cnst added to LogicalSystem.
     *
     * @throws   LangException if duplicate symbol, etc.
     */
    Cnst    addCnst(String       id)            throws LangException;

    /**
     *  Add Var to Logical System.
     *
     * @param id  Var's symbol string to be added to the
     *        Logical System.
     *
     * @return Var added to LogicalSystem.
     *
     * @throws  LangException if duplicate symbol, etc.
     */
    Var     addVar(String        id)            throws LangException;

    /**
     *  Add VarHyp to Logical System.
     *
     * @param labelS   String label of variable hypothesis
     * @param typS     String Metamath constant character (type code)
     * @param varS     String Metamath variable character
     *
     * @return VarHyp added to LogicalSystem.
     *
     * @throws       LangException if duplicate symbol, etc.
     *               (see <code>mmj.lang.LangConstants.java</code>)
     */
    VarHyp  addVarHyp(String     labelS,
                      String     typS,
                      String     varS)          throws LangException;

    /**
     *  Add DjVars (Disjoint Variables Restriction) to Logical
     *  System.
     *
     * @param djVar1S    disjoint variable symbol string 1
     * @param djVar2S    disjoint variable symbol string 2
     *
     * @return DjVars (pair) added to LogicalSystem.within
     *         the current scope, *or* the existing DjVars
     *         object in the current scope.
     *
     * @throws   LangException if duplicate vars, etc.
     *           (see <code>mmj.lang.LangConstants.java</code>)
     */
    DjVars  addDjVars(String     djVar1S,
                      String     djVar2S)       throws LangException;

    /**
     *  Add LogHyp (Logical Hypothesis) to Logical System.
     *
     * @param labelS    logical hypothesis label string
     * @param typS      logical hypothesis type code (symbol) string
     * @param symList   list containing expression symbol strings
     *                  (zero or more symbols).
     *
     * @return LogHyp newly constructed LogHyp added to
     *                LogicalSystem.
     *
     * @throws   LangException if duplicate label, undefined vars,
     *           etc.
     */
    LogHyp  addLogHyp(String     labelS,
                      String     typS,
                      ArrayList  symList)       throws LangException;

    /**
     *  Add Axiom to Logical System.
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol
     *                 strings (zero or more symbols).
     *
     * @return Axiom newly constructed Axiom added to LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    Axiom   addAxiom(String      labelS,
                     String      typS,
                     ArrayList   symList)       throws LangException;

    /**
     *  Add Theorem to Logical System.
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol strings
     *                   (zero or more symbols).
     * @param proofList  list containing proof step symbol strings
     *                   (1 or more symbols -- which may be "?" if a
     *                   step is unknown).
     *
     * @return Theorem  newly constructed Theorem added to
     *                  LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    Theorem addTheorem(String    labelS,
                       String    typS,
                       ArrayList symList,
                       ArrayList proofList)
                                                throws LangException;

    /**
     *  Add Theorem to Logical System.
     *  <p>
     *  This variant is invoked when the input contains
     *  a compressed proof.
     *
     * @param labelS   axiom label string
     * @param typS     axiom type code (symbol) string
     * @param symList  list containing axiom expression symbol strings
     *                 (zero or more symbols).
     * @param proofList list containing the contents of the
     *                 parenthesized portion of a compressed proof
     *                 (does not include the parentheses)
     * @param proofBlockList  list containing one or more blocks
     *                 of compressed proof symbols.
     *
     * @return Theorem  newly constructed Theorem added to
     *                  LogicalSystem.
     *
     * @throws       LangException if duplicate label, undefined vars,
     *               etc.
     */
    Theorem addTheorem(String    labelS,
                       String    typS,
                       ArrayList symList,
                       ArrayList proofList,
                       ArrayList proofBlockList)
                                     throws LangException;


    /**
     *  Begin a new (nested) scope level for the Logical System.
     *
     *  @see mmj.lang.ScopeDef
     */
    void    beginScope();

    /**
     *  Ends a (nested) scope level for the Logical System.
     *
     *  @see mmj.lang.ScopeDef
     *
     *  @throws   LangException if scope is already at the global
     *            scope level.
     */
    void    endScope()                          throws LangException;

    /**
     *  EOF processing for Logical System after file loaded.
     *
     * @param  messages Messages object to error reporting.
     *
     * @param  prematureEOF signals LoadLimit requested by user
     *         has been reached, so stop loading even if in the
     *         middle of a scope level.
     *
     * @throws       LangException if scope is NOT at the global
     *               scope level UNLESS premature EOF signalled.
     *               (see <code>mmj.lang.LangConstants.java</code>)
     */
    void    finalizeEOF(MessageHandler  messages,
                        boolean   prematureEOF)  throws LangException;


    /**
     *  Is BookManager enabled?
     *  <p>
     *  If BookManager is enabled then Chapters and Sections
     *  will be stored.
     *  <p>
     * @return true if BookManager is enabled, else false.
     */
    boolean isBookManagerEnabled();

    /**
     *  Add new Chapter.
     *  <p>
     *  See mmj.lang.BookManager.java for more info.
     *  <p>
     * @param  chapterTitle Title of chapter or blank or empty String.
     * @param position the position of the chapter start.
     */
    void addNewChapter(String chapterTitle, SourcePosition position);

    /**
     *  Add new Section.
     *  <p>
     *  See mmj.lang.BookManager.java for more info.
     *  <p>
     * @param  sectionTitle Title of section or blank or empty String.
     * @param position the position of the section start.
     */
    void addNewSection(String sectionTitle, SourcePosition position);

}
