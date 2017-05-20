package org.tirix.emetamath.editors;

import mmj.lang.Axiom;
import mmj.lang.Chapter;
import mmj.lang.Cnst;
import mmj.lang.LogHyp;
import mmj.lang.MObj;
import mmj.lang.Section;
import mmj.lang.Stmt;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.lang.VarHyp;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.MetamathUI;
import org.tirix.emetamath.nature.MetamathProjectNature;

/**
 * Provides the label for the Metamath elements (chapters, sections, theorems)
 * (eg. for the Outline view)
 * @author Thierry
 */
public class MMLabelProvider extends LabelProvider implements IStyledLabelProvider {
	private Image chapterImage = Activator.getImage("icons/book_red.gif"); //Activator.getDefault().getImageRegistry().get(Activator.getImageDescriptor("icons/book.gif")); 
    private Image sectionImage = Activator.getImage("icons/book_blue.gif"); 
	private Image theoremImage = Activator.getImage("icons/mmProof.gif");
    private Image axiomImage = Activator.getImage("icons/mmAxiom.gif"); 
	private Image essentialImage = Activator.getImage("icons/mmEssential.gif");
    private Image floatingImage = Activator.getImage("icons/mmFloating.gif"); 
	private Image constantImage = Activator.getImage("icons/mmConstant.gif");
    private Image setImage = Activator.getImage("icons/mmSet.gif"); 
    private Image wffImage = Activator.getImage("icons/mmWff.gif"); 
    private Image classImage = Activator.getImage("icons/mmClass.gif"); 
    private MetamathProjectNature nature;

    private final static Styler fWffStyler = new MMStyler(IMMColorConstants.WFF, false);
    private final static Styler fSetStyler = new MMStyler(IMMColorConstants.SET, false);
    private final static Styler fClassStyler = new MMStyler(IMMColorConstants.CLASS, false);
    private final static Styler fConstantStyler = new MMStyler(IMMColorConstants.CONSTANT, false);
    private final static Styler fStatementStyler = new MMStyler(IMMColorConstants.LABEL, false);

    public void setNature(MetamathProjectNature nature) {
    	this.nature = nature;
    }
    
    @Override
	public Image getImage(Object element) {
		if(element instanceof Chapter) return   chapterImage;
		if(element instanceof Section) return   sectionImage;
		if(element instanceof Theorem) return   theoremImage;
		if(element instanceof Axiom)   return     axiomImage;
		if(element instanceof LogHyp)  return essentialImage;
		if(element instanceof VarHyp)  return  floatingImage;
		if(element instanceof Cnst)    return  constantImage;
		if(element instanceof Var) {
			if(nature != null && nature.isSet((Var)element)) return setImage;
			if(nature != null && nature.isClass((Var)element)) return classImage;
			if(nature != null && nature.isWff((Var)element)) return wffImage;
			return setImage;
		}
		return super.getImage(element);
	};

	@Override
	public String getText(Object element) {
		if(element instanceof Chapter) return ((Chapter)element).getChapterTitle();
		if(element instanceof Section) return ((Section)element).getSectionTitle();
		if(element instanceof MObj) {
			String desc = ((MObj)element).getDescription(); 
			if(desc == null) return ((MObj)element).toString();
			int endIndex = desc.indexOf('.');
			return ((MObj)element).toString()+" - "+(endIndex == -1 ? desc : desc.substring(0, endIndex));
		}
		return super.getText(element);
	};

	public void dispose() { 
        chapterImage.dispose(); 
        chapterImage = null; 
        sectionImage.dispose(); 
        sectionImage = null; 
        axiomImage.dispose(); 
        axiomImage = null; 
        essentialImage.dispose(); 
        essentialImage = null; 
        floatingImage.dispose(); 
        floatingImage = null; 
        constantImage.dispose(); 
        constantImage = null; 
        setImage.dispose(); 
        setImage = null; 
        wffImage.dispose(); 
        wffImage = null; 
        classImage.dispose(); 
        classImage = null; 
	}

	@Override
	public StyledString getStyledText(Object element) {
		if(element instanceof Chapter) return new StyledString(((Chapter)element).getChapterTitle());
		if(element instanceof Section) return new StyledString(((Section)element).getSectionTitle());
		StyledString str = new StyledString(); 
		if(element instanceof Stmt) str.append(((MObj)element).toString(), fStatementStyler);
		if(element instanceof Cnst) str.append(((MObj)element).toString(), fConstantStyler);
		if(element instanceof Var && nature.isSet((Var)element)) str.append(((MObj)element).toString(), fSetStyler);
		if(element instanceof Var && nature.isClass((Var)element)) str.append(((MObj)element).toString(), fClassStyler);
		if(element instanceof Var && nature.isWff((Var)element)) str.append(((MObj)element).toString(), fWffStyler);
		if(!(element instanceof MObj)) return new StyledString(super.getText(element));
		if(str.length() == 0) str.append(((MObj)element).toString());

		String desc = ((MObj)element).getDescription(); 
		if(desc == null) return str;
		int endIndex = desc.indexOf('.');
		str.append(" - "+(endIndex == -1 ? desc : desc.substring(0, endIndex)), StyledString.QUALIFIER_STYLER); //fDescriptionStyler);
		return str;
	}
    
    protected static class MMStyler extends Styler {
    	Color foreground;
    	boolean italic;
    	
		public MMStyler(RGB rgb, boolean italic) {
			this.foreground = new Color(Display.getCurrent(), rgb);
			this.italic = italic;
		}

		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = foreground;
			//if(italic) textStyle.font = new Font(Display.getCurrent(), getModifiedFontData(textStyle.font.getFontData(), SWT.ITALIC));;
		}
	};
}
