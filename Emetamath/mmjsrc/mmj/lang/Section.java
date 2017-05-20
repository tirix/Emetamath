//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Section.java  0.01 08/01/2008
 *
 *  Aug-1-2008:
 *      --> new!
 *
 */

package mmj.lang;

import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;

/**
 *  Section is a rudimentary class containing information
 *  for BookManager about the grouping of statements in
 *  a Chapter within a Metamath database.
 *
 *  See BookManager for more information.
 */
public class Section implements SourceElement, Comparable<Section> {

    private Chapter sectionChapter;
    private int     sectionNbr;
    private String  sectionTitle;
    private SourcePosition position;

	private int     lastMObjNbr;
	private int     mObjCount;

    /**
     *  Sole constructor for Section..
     *  <p>
     *  @param sectionChapter the Chapter to which the Section
     *         belongs.
     *  @param sectionNbr is assigned by BookManager.
     *  @param sectionTitle is the extracted descriptive
     *         title from the input Metamath database
     *         or the default title (must be at least
     *         an empty String!)
     */
    public Section(Chapter sectionChapter,
                   int     sectionNbr,
                   String  sectionTitle,
                   SourcePosition position) {

        this.sectionChapter       = sectionChapter;
        this.sectionNbr           = sectionNbr;
        this.sectionTitle         = sectionTitle;
        this.position             = position;
        this.mObjCount			  = 0;

        sectionChapter.storeNewSection(this);
    }

    /**
     *  Assigns an MObj to a Chapter and Section and
     *  computes the MObj SectionMObjNbr.
     *  <p>
     *  This function is intended for use by LogicalSystem
     *  and it is this function which actually updates
     *  the MOBj with the computed SectionMOBjNbr.
     *  <p>
     *  Note: the MObj is assigned a new sectionMObjNbr
     *        only if MObj has not already been assigned one.
     *        The reason this is necessary even with
     *        updates performed during the initial load of
     *        the input .mm file is that a Metamath Var can
     *        be declared in multiple locations within the
     *        file. These multiple declarations occur within
     *        separate Metamath Scopes and outside of the
     *        scope the Var is considered to be "inactive",
     *        so subsequent re-declarations are considered
     *        to be re-activations. The bottom line is that only
     *        the first declaration is assigned a sectionMObjNbr.
     *  <p>
     *  @param mObj the MObj to be assigned to a Chapter and
     *         Section and updated with SectionMObjNbr.
     *  @return true only if the operation is completed successfully,
     *          meaning that the MObj has a zero sectionMObjNbr
     *          prior to the update.
     */
    public boolean assignChapterSectionNbrs(MObj mObj) {
        int n                     = mObj.getSectionMObjNbr();
        if (n != 0) {
            return false;
        }
        mObj.setSectionMObjNbr(++lastMObjNbr);
        mObj.setChapterNbr(sectionChapter.getChapterNbr());
        mObj.setSectionNbr(sectionNbr);
        this.mObjCount++;
        return true;
    }

    /**
     *  Returns the Chapter to which the Section is assigned.
     *  <p>
     *  @return the Chapter to which the Section is assigned.
     */
    public Chapter getSectionChapter() {
        return sectionChapter;
    }

    /**
     *  Returns the sectionNbr for the Section.
     *  <p>
     *  @return the sectionNbr for the Section.
     */
    public int getSectionNbr() {
        return sectionNbr;
    }

    /**
     *  Returns the sectionTitle for the Section.
     *  <p>
     *  @return the sectionTitle for the Section.
     */
    public String getSectionTitle() {
        return sectionTitle;
    }

    /**
     *  Sets the value of the sectionTitle for the Section.
     *  <p>
     *  The title must be, at least, an empty String.
     *  <p>
     *  @param sectionTitle Description or Title of the
     *         Section.
     */
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle         = sectionTitle;
    }

    /**
     *  Returns the last MObj number within the section.
     *  <p>
     *  The highest MObj number is the same as the last
     *  because additions are made only at the end of
     *  a Section and new MObj numbers are generated
     *  from 1 by 1 within each Section.
     *  <p>
     *  @return final MObj number within the Section.
     */
    public int getLastMObjNbr() {
        return lastMObjNbr;
    }

    /**
     *  Test code for creating diagnostics.
     *  <p>
     *  @return String of information about the Section
     *          formatted into a single line.
     */
    public String toString() {
        return new String(
            LangConstants.SECTION_TOSTRING_LITERAL_1
          + sectionChapter.getChapterNbr()
          + LangConstants.SECTION_TOSTRING_LITERAL_2
          + getSectionNbr()
          + getSectionCategoryDisplayCaption()
          + LangConstants.SECTION_TOSTRING_LITERAL_3
          + getSectionTitle()
          + LangConstants.SECTION_TOSTRING_LITERAL_4
          + LangConstants.SECTION_TOSTRING_LITERAL_5
          + getLastMObjNbr());
    }

    /**
     *  Returns a string caption for the Section category
     *  code.
     *  <p>
     *  See LangConstants.SECTION_DISPLAY_CAPTION.
     *  <p>
     *  @return caption for the Section category code.
     */
    public String getSectionCategoryDisplayCaption() {
        return LangConstants.
                SECTION_DISPLAY_CAPTION[
                    getSectionCategoryCd()];
    }

    /**
     *  Returns the Section Category Code.
     *  <p>
     *  See LangConstants.SECTION_NBR_CATEGORIES.
     *  <p>
     *  @return the Section Category Code.
     */
    public int getSectionCategoryCd() {
        return Section.getSectionCategoryCd(sectionNbr);
    }

    /**
     *  Returns the Section Category Code for a Section number.
     *  <p>
     *  See LangConstants.SECTION_NBR_CATEGORIES.
     *  <p>
     *  @param s section number
     *  @return the Section Category Code.
     */
    public static int getSectionCategoryCd(int s) {

        int n                     =
            s % LangConstants.SECTION_NBR_CATEGORIES;

        if (n == 0) {
            return LangConstants.SECTION_NBR_CATEGORIES;
        }
        else {
            return n;
        }
    }

	@Override
	public int compareTo(Section s) {
		if(sectionChapter != s.sectionChapter) return sectionChapter.compareTo(s.sectionChapter);
		return sectionNbr - s.sectionNbr;
	}

    /**
     * Returns the position of this Section.
     * @return the position of this Section.
     */
    public SourcePosition getPosition() {
		return position;
	}
}