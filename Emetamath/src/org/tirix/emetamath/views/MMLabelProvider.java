package org.tirix.emetamath.views;

import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import mmj.lang.Axiom;
import mmj.lang.Chapter;
import mmj.lang.Cnst;
import mmj.lang.LogHyp;
import mmj.lang.MObj;
import mmj.lang.Section;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Theorem;
import mmj.lang.Var;
import mmj.lang.VarHyp;
import mmj.pa.StepSelectorItem;
import mmj.pa.StepSelectorStore;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.editors.IMMColorConstants;
import org.tirix.emetamath.editors.MMSourceViewerConfiguration;
import org.tirix.emetamath.nature.MetamathProjectNature;
import org.tirix.emetamath.views.StepSelectorView.StepSelectorLine;

/**
 * Provides the label for the Metamath elements (chapters, sections, theorems)
 * (eg. for the Outline view)
 * @author Thierry
 */
public class MMLabelProvider extends LabelProvider implements IStyledLabelProvider, IFontProvider {
	public static final String IMG_PATH_CLASS = "icons/mmClass.gif";
	public static final String IMG_PATH_WFF = "icons/mmWff.gif";
	public static final String IMG_PATH_SET = "icons/mmSet.gif";
	public static final String IMG_PATH_CONSTANT = "icons/mmConstant.gif";
	public static final String IMG_PATH_FLOATING = "icons/mmFloating.gif";
	public static final String IMG_PATH_ESSENTIAL = "icons/mmEssential.gif";
	public static final String IMG_PATH_AXIOM = "icons/mmAxiom.gif";
	public static final String IMG_PATH_THEOREM = "icons/mmProof.gif";
	public static final String IMG_PATH_SECTION = "icons/book_blue.gif";
	public final static String IMG_PATH_CHAPTER = "icons/book_red.gif";
	
	private Image chapterImage = Activator.getImage("icons/book_red.gif"); //Activator.getDefault().getImageRegistry().get(Activator.getImageDescriptor("icons/book.gif")); 
    private Image sectionImage = Activator.getImage(IMG_PATH_SECTION); 
	private Image theoremImage = Activator.getImage(IMG_PATH_THEOREM);
    private Image axiomImage = Activator.getImage(IMG_PATH_AXIOM); 
	private Image essentialImage = Activator.getImage(IMG_PATH_ESSENTIAL);
    private Image floatingImage = Activator.getImage(IMG_PATH_FLOATING); 
	private Image constantImage = Activator.getImage(IMG_PATH_CONSTANT);
    private Image setImage = Activator.getImage(IMG_PATH_SET); 
    private Image wffImage = Activator.getImage(IMG_PATH_WFF); 
    private Image classImage = Activator.getImage(IMG_PATH_CLASS); 

    private Image stepHypImage = Activator.getImage("icons/mmStepEssential.gif"); 
    private Image stepItemImage = Activator.getImage("icons/mmStepItem.gif"); 
    private Image stepLastImage = Activator.getImage("icons/mmStepDerivation.gif"); 
    
    private MetamathProjectNature nature;
	static Font fEditorFont;

//    private final static MMStyler fWffStyler = new MMStyler(IMMColorConstants.WFF, false);
//    private final static MMStyler fSetStyler = new MMStyler(IMMColorConstants.SET, false);
//    private final static MMStyler fClassStyler = new MMStyler(IMMColorConstants.CLASS, false);
    private static Map<Cnst, MMStyler> fVarStylers;
	private final static MMStyler fTypeConstantStyler = new MMStyler(IMMColorConstants.TYPE, false);
    private final static MMStyler fConstantStyler = new MMStyler(IMMColorConstants.CONSTANT, false);
    private final static MMStyler fStatementStyler = new MMStyler(IMMColorConstants.LABEL, false);
    private final static Styler fStepSelectorLineStyler = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.font = fEditorFont;
		}};

	public MMLabelProvider() {
		FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
		fEditorFont = fontRegistry.get(MMSourceViewerConfiguration.EDITOR_FONT_REGISTRY_NAME);
	}
		
    public void setNature(MetamathProjectNature nature) {
    	this.nature = nature;
    	fVarStylers = new Hashtable<Cnst, MMStyler>();
    	Map<Cnst, RGB> typeColors = nature.getTypeColors();
    	for(Cnst type:typeColors.keySet()) {
    		fVarStylers.put(type, new MMStyler(typeColors.get(type), false));
    	}
    }
    
    @Override
	public Image getImage(Object element) {
		if(element instanceof StepSelectorItem) element = ((StepSelectorItem)element).assrt;
		if(element instanceof Chapter) return   chapterImage;
		if(element instanceof Section) return   sectionImage;
		if(element instanceof Theorem) return   theoremImage;
		if(element instanceof Axiom)   return     axiomImage;
		if(element instanceof LogHyp)  return essentialImage;
		if(element instanceof VarHyp)  return  floatingImage;
		if(element instanceof Cnst)    return  constantImage;
		if(element instanceof Var) {
			return nature.getTypeIcons().get(nature.getType((Var)element));
//			if(nature != null && nature.isSet((Var)element)) return setImage;
//			if(nature != null && nature.isClass((Var)element)) return classImage;
//			if(nature != null && nature.isWff((Var)element)) return wffImage;
//			return setImage;
		}

		if(element instanceof StepSelectorLine) return ((StepSelectorLine)element).lastLine ? stepLastImage : stepItemImage;
		return super.getImage(element);
	};

	public URL getImageURL(Object element) {
		String path = null;
		if(element instanceof Chapter) path = IMG_PATH_CHAPTER;
		if(element instanceof Section) path =  IMG_PATH_SECTION;
		if(element instanceof Theorem) path =  IMG_PATH_THEOREM;
		if(element instanceof Axiom)   path =  IMG_PATH_AXIOM;
		if(element instanceof LogHyp)  path =  IMG_PATH_ESSENTIAL;
		if(element instanceof VarHyp)  path =   IMG_PATH_FLOATING;
		if(element instanceof Cnst)    path =   IMG_PATH_CONSTANT;
		if(element instanceof Var) {
			path = nature.getTypeIconURLs().get(nature.getType((Var)element));
//			if(nature != null && nature.isSet((Var)element)) path =  IMG_PATH_SET;
//			if(nature != null && nature.isClass((Var)element)) path =  IMG_PATH_CLASS;
//			if(nature != null && nature.isWff((Var)element)) path =  IMG_PATH_WFF;
//			if(path == null) path = IMG_PATH_SET;
		}
		return Activator.getImageURL(path);
	}

	public Image getStepHypImage() {
		return stepHypImage;
	}
	
	public Image getStepItemImage() {
		return stepItemImage;
	}
	
	public Image getStepLastImage() {
		return stepLastImage;
	}
	
	@Override
	public String getText(Object element) {
		if(element instanceof StepSelectorStore) return "Step "+((StepSelectorStore)element).getStep()+" Unifiable Assertions";
		if(element instanceof StepSelectorItem) element = ((StepSelectorItem)element).assrt;
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

	@Override
	public Font getFont(Object element) {
		if(element instanceof StepSelectorLine) return fEditorFont;
		return null; // use the default font
	}

	public Font getEditorFont() {
		return fEditorFont;
	}
	
	public MetamathProjectNature getNature() {
		return nature;
	};

	public void dispose() { 
        chapterImage.dispose(); 
        chapterImage = null; 
        sectionImage.dispose(); 
        sectionImage = null; 
        axiomImage.dispose(); 
        axiomImage = null; 
        theoremImage.dispose(); 
        theoremImage = null; 
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
        stepHypImage.dispose(); 
        stepHypImage = null; 
        stepItemImage.dispose(); 
        stepItemImage = null; 
        stepLastImage.dispose(); 
        stepLastImage = null; 
	}

	public String getHTMLText(Sym element) {
		MMStyler styler = null;
		if(element instanceof Cnst) {
			if(nature.isType(element)) styler = fTypeConstantStyler;
			else styler = fConstantStyler; 
		}
		if(element instanceof Var) styler = fVarStylers.get((Var)element);
//		if(element instanceof Var && nature.isSet((Var)element)) styler = fSetStyler;
//		if(element instanceof Var && nature.isClass((Var)element)) styler = fClassStyler;
//		if(element instanceof Var && nature.isWff((Var)element)) styler = fWffStyler;
		StringBuffer str = new StringBuffer();
		if(styler == null) str.append(((MObj)element).toString());
		else str.append(styler.getHTMLText(((MObj)element).toString()));
		return str.toString();
	}

	public StyledString getStyledText(Object element) {
		if(element instanceof StepSelectorLine) return new StyledString(((StepSelectorLine)element).toString(), fStepSelectorLineStyler);
		if(element instanceof StepSelectorItem) element = ((StepSelectorItem)element).assrt;
		if(element instanceof Chapter) return new StyledString(((Chapter)element).getChapterTitle());
		if(element instanceof Section) return new StyledString(((Section)element).getSectionTitle());
		StyledString str = new StyledString(); 
		if(element instanceof StepSelectorStore) { str.append("Step "); str.append(((StepSelectorStore)element).getStep(), fStatementStyler); str.append(" Unifiable Assertions"); return str; }
		if(element instanceof Stmt) str.append(((MObj)element).toString(), fStatementStyler);
		if(element instanceof Cnst) str.append(((MObj)element).toString(), fConstantStyler);
		if(element instanceof Var) str.append(((MObj)element).toString(), fVarStylers.get((Var)element));
//		if(element instanceof Var && nature.isSet((Var)element)) str.append(((MObj)element).toString(), fSetStyler);
//		if(element instanceof Var && nature.isClass((Var)element)) str.append(((MObj)element).toString(), fClassStyler);
//		if(element instanceof Var && nature.isWff((Var)element)) str.append(((MObj)element).toString(), fWffStyler);
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
		
		public String getHTMLText(String text) {
			StringBuffer str = new StringBuffer(); 
			if(italic) str.append("<i>");
			str.append("<span style=\"color: ");
			appendColor(str, foreground.getRGB());
			str.append("\">"+text+"</span>");
			if(italic) str.append("</i>");
			return str.toString();
		}

		private static void appendColor(StringBuffer buffer, RGB rgb) {
			buffer.append('#');
			appendAsHexString(buffer, rgb.red);
			appendAsHexString(buffer, rgb.green);
			appendAsHexString(buffer, rgb.blue);
		}

		private static void appendAsHexString(StringBuffer buffer, int intValue) {
			String hexValue= Integer.toHexString(intValue);
			if (hexValue.length() == 1)
				buffer.append('0');
			buffer.append(hexValue);
		}
	}
}
