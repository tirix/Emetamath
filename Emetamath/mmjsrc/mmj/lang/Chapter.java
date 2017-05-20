//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  Chapter.java  0.01 08/01/2008
 *
 *  Aug-1-2008:
 *      --> new!
 *
 */

package mmj.lang;

import java.util.ArrayList;

import mmj.mmio.SourceElement;
import mmj.mmio.SourcePosition;

/**
 *  Chapter is a baby class that provides a way
 *  to provide a title for a grouping of Sections.
 *
 *  See BookManager for more details.
 */
public class Chapter implements SourceElement, Comparable<Chapter> {

    private int     chapterNbr;
    private String  chapterTitle;
    private SourcePosition position;

    private Section firstSection;
    private Section lastSection;

    private ArrayList sectionList;

    /**
     *  Sole constructor for Chapter.
     *  <p>
     *  @param chapterNbr is assigned by BookManager.
     *  @param chapterTitle is the extracted descriptive
     *         title from the input Metamath database
     *         or the default title (must be at least
     *         an empty String!)
     * @param position 
     */
   public Chapter(int     chapterNbr,
                   String chapterTitle, SourcePosition position) {
        this.chapterNbr           = chapterNbr;
        this.chapterTitle         = chapterTitle;
        this.position             = position;
        sectionList = new ArrayList();
    }

    /**
     *  Records the presence of a new Section with a Chapter.
     *  <p>
     *  @param section The new Section in the Chapter.
     */
    public void storeNewSection(Section section) {
        if (firstSection == null) {
            firstSection          = section;
        }
        sectionList.add(section);
        lastSection               = section;
    }

    /**
     *  Returns the Chapter Number.
     *  <p>
     *  @return chapterNbr for the Chapter.
     */
    public int getChapterNbr() {
        return chapterNbr;
    }

    /**
     *  Returns the Chapter Title
     *  <p>
     *  @return chapterTitle for the Chapter.
     */
    public String getChapterTitle() {
        return chapterTitle;
    }

    /**
     *  Returns the first Section within the Chapter.
     *  <p>
     *  @return first Section within the Chapter.
     */
    public Section getFirstSection() {
        return firstSection;
    }

    /**
     *  Returns the last Section within the Chapter.
     *  <p>
     *  Note: this may be the same as the first Section.
     *  <p>
     *  @return last Section within the Chapter.
     */
    public Section getLastSection() {
        return lastSection;
    }

    public ArrayList<Section> getSectionList() {
    	return sectionList;
    }
    
    /**
     * Returns the position of this Chapter.
     * @return the position of this Chapter.
     */
    public SourcePosition getPosition() {
		return position;
	}

	@Override
	public int compareTo(Chapter c) {
		return chapterNbr - c.chapterNbr;
	}

	/**
     *  Test code for creating diagnostics.
     *  <p>
     *  @return String of information about the Chapter
     *          formatted into a single line.
     */
    public String toString() {
        return new String(
            LangConstants.CHAPTER_TOSTRING_LITERAL_1
          + getChapterNbr()
          + LangConstants.CHAPTER_TOSTRING_LITERAL_2
          + getChapterTitle()
          + LangConstants.CHAPTER_TOSTRING_LITERAL_3
          + LangConstants.CHAPTER_TOSTRING_LITERAL_4
          + getFirstSection().getSectionNbr()
          + LangConstants.CHAPTER_TOSTRING_LITERAL_5
          + getLastSection().getSectionNbr());
    }
}