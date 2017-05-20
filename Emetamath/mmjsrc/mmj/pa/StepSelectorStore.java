//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * StepSelectorStore.java  0.01 03/01/2008
 *
 * Version 0.01:
 * ==> New.
 */

package mmj.pa;

import java.util.LinkedList;
import java.util.List;

import mmj.lang.Assrt;

/**
 * StepSelectorStore accumulates StepSelector results.
 * <p>
 * The design is rudimentary because it does not sort or select which results to
 * keep -- at this time.
 * <p>
 * Its main purpose is to provide a simple data store for StepSelectorSearch.
 */
public class StepSelectorStore {

    private final int maxResults;
    private int cntResults = 0;

    private final List<StepSelectorItem> storeList = new LinkedList<>();

    /** 
     * Step String corresponding to the ProofWorksheet step (number)
     * for which the StepSelector was run.
     */
    public String step;
    
    /**
     * Simple factory to hide constructor details.
     *
     * @param proofAsstPreferences contains parameters needed by StepSelector
     *            and friends.
     * @return the new StepSelectorStore
     */
    public static StepSelectorStore createStepSelectorStore(
        final ProofAsstPreferences proofAsstPreferences, final String step)
    {
        return new StepSelectorStore(proofAsstPreferences, step);
    }

    /**
     * Simple constructor for the most basic StepSelectorStore.
     *
     * @param proofAsstPreferences contains parameters needed by StepSelector
     *            and friends.
     */
    public StepSelectorStore(final ProofAsstPreferences proofAsstPreferences, final String step) {
        maxResults = proofAsstPreferences.stepSelectorMaxResults.get();
        this.step = step;
    }

    /**
     * Creates a StepSelectorResults object using the contents of the data
     * store.
     * <p>
     * An extra item is added to the StepSelectorResults. If the store contains
     * one extra result (beyond the specified StepSelectorMaxResults parameter),
     * then "***MORE***" is output at the end of the StepSelectorResults
     * selection array along with a null Assrt. Otherwise it outputs
     * "***END***". The purpose it to inform the user that rerunning the
     * StepSelector may or may not be useful.
     * @return StepSelectorResults object for display on the StepSelectorDialog.
     */
    public StepSelectorResults getResults()
    {
        final String endLiteral;
        if (isFull())
            endLiteral = PaConstants.STEP_SELECTOR_LIST_MORE_LITERAL;
        else
            endLiteral = PaConstants.STEP_SELECTOR_LIST_END_LITERAL;

        final Assrt[] refArray = new Assrt[storeList.size()+1];
        final String[] selectionArray = new String[storeList.size()+1];

        int n = 0;
        for (final StepSelectorItem item : storeList) {
            refArray[n] = item.assrt;
            final StringBuilder sb = new StringBuilder("<html>");
            String delim = "";
            for (final String element : item.selection) {
                sb.append(delim).append(element.replace("&", "&amp;")
                    .replace("<", "&lt;").replace(">", "&gt;"));
                delim = "<br/>";
            }
            selectionArray[n++] = sb.append("</html>").toString();
        }
        refArray[n] = null;
        selectionArray[n] = endLiteral;
 
        return new StepSelectorResults(step, refArray, selectionArray);
    }

    /**
     * Adds a StepSelectorSearch result to the data store if the store is not
     * already full.
     *
     * @param assrt Assertion that is unifiable with the proof step.
     * @param selection Strings showing formula as it will appear in the
     *            StepSelectorDialog.
     * @return true if store is now full, otherwise false.
     */
    public boolean add(final Assrt assrt, final String[] selection) {
        storeList.add(new StepSelectorItem(assrt, selection));
        cntResults++;
        return isFull();
    }

    /**
     * Checks to see if the store has fewer result items than permitted
     * according the Proof Assistant Preferences StepSelectorMaxResults value.
     *
     * @return true if store is full otherwise false.
     */
    public boolean isFull() {
        return cntResults >= maxResults;
    }
    
    /** 
     * Checks to see is more items can be fetched
     * @return true if this store proposes more items.
     */
    public boolean hasMore() {
    	return isFull();
    }
    
    /**
	 * @return the storeList
	 */
	public StepSelectorItem[] getStoreItems() {
		return (StepSelectorItem[]) storeList.subList(0, storeList.size()).toArray(new StepSelectorItem[storeList.size()]);
	}

    /**
	 * @return the storeList, except the last "null" item
	 */
	public boolean isEmpty() {
		return storeList.size() == 0;
	}

	/**
	 * @return the step
	 */
	public String getStep() {
		return step;
	}
}
