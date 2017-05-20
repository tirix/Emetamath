//********************************************************************/
//* Copyright (C) 2005, 2006, 2007, 2008                             */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  TlPreferences.java  0.01 08/01/2008
 *
 *  Version 0.01:
 *      - new.
 */

package mmj.tl;
import java.io.File;

import mmj.lang.LogicalSystem;
import mmj.lang.TheoremLoaderException;

/**
 *  Holds user settings/preferences used by the
 *  Theorem Loader.
 */
public class TlPreferences {

    private   MMTFolder      mmtFolder;

    private   String         djVarsOption;
    private   boolean        djVarsMerge;
    private   boolean        djVarsReplace;
    private   boolean        djVarsNoUpdate;

    private   boolean        auditMessages;
    private   boolean        storeFormulasAsIs;

    private   int            storeMMIndentAmt;
    private   int            storeMMRightCol;

    private   String         provableLogicStmtTypeParm;

    /**
     *  Constructor for TlPreferences.
     *  <p>
     *  @param logicalSystem LogicalSystem object.
     */
    public TlPreferences(LogicalSystem logicalSystem) {

        mmtFolder                 = new MMTFolder();

        setDjVarsOption(
            TlConstants.THEOREM_LOADER_DJ_VARS_OPTION_DEFAULT);

        setAuditMessages(
            TlConstants.THEOREM_LOADER_AUDIT_MESSAGES_DEFAULT);

        setStoreFormulasAsIs(
            TlConstants.THEOREM_LOADER_STORE_FORMULAS_ASIS_DEFAULT);

        setStoreMMIndentAmt(
            TlConstants.THEOREM_LOADER_STORE_MM_INDENT_AMT_DEFAULT);

        setStoreMMRightCol(
            TlConstants.THEOREM_LOADER_STORE_MM_RIGHT_COL_DEFAULT);

        setProvableLogicStmtTypeParm(
            logicalSystem.
                getProvableLogicStmtTypeParm());
    }

    /**
     *  Get the MMTFolder in use now. Constructor for TlPreferences.
     *  <p>
     *  @return MMTFolder in use now, which may be pointing to a
     *          null File object if not yet specified.
     */
    public MMTFolder getMMTFolder() {
        return mmtFolder;
    }

    /**
     *  Get the Dj Vars Option string.
     *  <p>
     *  @return Dj Vars Option string.
     */
    public String getDjVarsOption() {
        return djVarsOption;
    }

    /**
     *  Get the Dj Vars Merge Option flag.
     *  <p>
     *  @return the Dj Vars Merge Option flag.
     */
    public boolean getDjVarsMerge() {
        return djVarsMerge;
    }

    /**
     *  Get the Dj Vars Replace Option flag.
     *  <p>
     *  @return the Dj Vars Replace Option flag.
     */
    public boolean getDjVarsReplace() {
        return djVarsReplace;
    }

    /**
     *  Get the Dj Vars NoUpdate Option flag.
     *  <p>
     *  @return the Dj Vars NoUpdate Option flag.
     */
    public boolean getDjVarsNoUpdate() {
        return djVarsNoUpdate;
    }

    /**
     *  Get the auditMessages flag.
     *  <p>
     *  @return auditMessages flag.
     */
    public boolean getAuditMessages() {
        return auditMessages;
    }

    /**
     *  Get the storeFormulasAsIs flag.
     *  <p>
     *  @return storeFormulasAsIs flag.
     */
    public boolean getStoreFormulasAsIs() {
        return storeFormulasAsIs;
    }

    /**
     *  Get the storeMMIndentAmt.
     *  <p>
     *  @return storeMMIndentAmt.
     */
    public int getStoreMMIndentAmt() {
        return storeMMIndentAmt;
    }

    /**
     *  Get the storeMMRightCol.
     *  <p>
     *  @return storeMMRightCol.
     */
    public int getStoreMMRightCol() {
        return storeMMRightCol;
    }

    /**
     *  Get the cached value of the Provable Logic Statement
     *  Type string value.
     *  <p>
     *  @return provableLogicStmtTypeParm.
     */
    public String getProvableLogicStmtTypeParm() {
        return provableLogicStmtTypeParm;
    }

    /**
     *  Validate and set the Dj Vars Option.
     *  <p>
     *  If valid the dvVarsMerge, djVarsReplace and
     *  djVarsNoUpdate flags are set, as well as the
     *  Dj Vars option itself. If invalid no updates
     *  are made.
     *  <p>
     *  @param s Dj Vars Option string.
     *  @return true if Dj Vars Option valid, else false.
     */
    public boolean setDjVarsOption(String s) {
        if (s == null) {
            return false; //error
        }

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s.compareToIgnoreCase(
              TlConstants.
                THEOREM_LOADER_DJ_VARS_OPTION_NO_UPDATE)
            == 0) {

            djVarsNoUpdate        = true;
            djVarsMerge           = false;
            djVarsReplace         = false;
            djVarsOption          = s;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
              TlConstants.
                THEOREM_LOADER_DJ_VARS_OPTION_MERGE)
            == 0) {

            djVarsNoUpdate        = false;
            djVarsMerge           = true;
            djVarsReplace         = false;
            djVarsOption          = s;
            return true; //no error
        }

        if (s.compareToIgnoreCase(
              TlConstants.
                THEOREM_LOADER_DJ_VARS_OPTION_REPLACE)
            == 0) {

            djVarsNoUpdate        = false;
            djVarsMerge           = false;
            djVarsReplace         = true;
            djVarsOption          = s;
            return true; //no error
        }

        return false;
    }

    /**
     *  Validate and set the Audit Messages Option.
     *  <p>
     *  If valid the auditMessages flag is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s Audit Messages option.
     *  @return true if Audit Messages Option valid, else false.
     */
    public boolean setAuditMessages(String s) {
        if (s == null) {
            return false; //error
        }

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_1) == 0   ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_2) == 0   ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_3) == 0)  {
            setAuditMessages(true);
            return true;
        }

        if (s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_1) == 0  ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_2) == 0  ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_3) == 0) {
            setAuditMessages(false);
            return true;
        }

        return false;
    }

    /**
     *  Set the auditMessages flag.
     *  <p>
     *  @param b auditMessages flag.
     */
    public void setAuditMessages(boolean b) {
        auditMessages             = b;
    }

    /**
     *  Validate and set the Store Formulas AsIs Option.
     *  <p>
     *  If valid the storeFormulasAsIs flag is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s Store Formulas AsIs option.
     *  @return true if Store Formulas AsIs Option valid, else false.
     */
    public boolean setStoreFormulasAsIs(String s) {
        if (s == null) {
            return false; //error
        }

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_1) == 0   ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_2) == 0   ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_TRUE_3) == 0)  {
            setStoreFormulasAsIs(true);
            return true;
        }

        if (s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_1) == 0  ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_2) == 0  ||
            s.compareToIgnoreCase(
                TlConstants.SYNONYM_FALSE_3) == 0) {
            setStoreFormulasAsIs(false);
            return true;
        }

        return false;
    }

    /**
     *  Set the storeFormulasAsIs flag.
     *  <p>
     *  @param b storeFormulasAsIs flag.
     */
    public void setStoreFormulasAsIs(boolean b) {
        storeFormulasAsIs         = b;
    }

    /**
     *  Validate and set the Store MM Indent Amt Option.
     *  <p>
     *  If valid the storeMMIndentAmt is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s Store MM Indent Amt string.
     *  @return true if Store MM Indent Amt Option valid, else false.
     */
    public boolean setStoreMMIndentAmt(String s) {

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s != null) {

            try {
                int n             = Integer.parseInt(s);

                if (n >=
                        TlConstants.
                            THEOREM_LOADER_STORE_MM_INDENT_AMT_MIN
                    &&
                    n <=
                        TlConstants.
                            THEOREM_LOADER_STORE_MM_INDENT_AMT_MAX) {

                    setStoreMMIndentAmt(n);
                    return true;
                }
            }
            catch (NumberFormatException e) {
            }
        }
        return false;
    }

    /**
     *  Set the storeMMIndentAmt.
     *  <p>
     *  @param n storeMMIndentAmt.
     */
    public void setStoreMMIndentAmt(int n) {
        storeMMIndentAmt          = n;
    }


    /**
     *  Validate and set the Store MM Right Col Option.
     *  <p>
     *  If valid the storeMMRightCol is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s Store MM Right Col string.
     *  @return true if Store MM Right Col Option valid, else false.
     */
    public boolean setStoreMMRightCol(String s) {

        // Note: do not modify any settings unless
        //       the input is valid -- therefore,
        //       no default settings are made here
        //       ...
        //       [  ]
        //

        if (s != null) {

            try {
                int n             = Integer.parseInt(s);

                if (n >=
                        TlConstants.
                            THEOREM_LOADER_STORE_MM_RIGHT_COL_MIN
                    &&
                    n <=
                        TlConstants.
                            THEOREM_LOADER_STORE_MM_RIGHT_COL_MAX) {

                    setStoreMMRightCol(n);
                    return true;
                }
            }
            catch (NumberFormatException e) {
            }
        }
        return false;
    }

    /**
     *  Set the storeMMRightCol.
     *  <p>
     *  @param n storeMMRightCol.
     */
    public void setStoreMMRightCol(int n) {
        storeMMRightCol           = n;
    }

    /**
     *  Validate and set the Provable Logic Stmt Type Parm.
     *  <p>
     *  If valid the provableLogicStmtTypeParm is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s Provable Logic Stmt Type Parm string.
     *  @return true if Provable Logic Stmt Type Parm. valid,
     *          else false.
     */
    public boolean setProvableLogicStmtTypeParm(String s) {
        if (s == null ||
            s.length() == 0) {
            return false; //error
        }

        provableLogicStmtTypeParm = s;

        return true;
    }

    /**
     *  Set the MMT Folder using a pathname String.
     *  <p>
     *  If valid the mmtFolder is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param s MMT Folder pathname, absolute or relative.
     *  @return null if no errors, otherwise error message string.
     */
    public String setMMTFolder(String s) {
        try {
            mmtFolder             = new MMTFolder(s);
            return null;
        }
        catch (TheoremLoaderException e) {
            return e.getMessage();
        }
    }

    /**
     *  Set the MMT Folder using a File object.
     *  <p>
     *  If valid the mmtFolder is set.
     *  If invalid, no updates are made.
     *  <p>
     *  @param file MMT Folder File object.
     *  @return null if no errors, otherwise error message string.
     */
    public String setMMTFolder(File file) {
        try {
            mmtFolder             = new MMTFolder(file);
            return null;
        }
        catch (TheoremLoaderException e) {
            return e.getMessage();
        }
    }
}
