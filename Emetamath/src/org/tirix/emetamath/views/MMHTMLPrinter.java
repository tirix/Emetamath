package org.tirix.emetamath.views;

import java.net.URL;

import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.tirix.emetamath.Activator;
import org.tirix.emetamath.nature.MetamathProjectNature;

import mmj.lang.Assrt;
import mmj.lang.Formula;
import mmj.lang.LogHyp;
import mmj.lang.MObj;
import mmj.lang.Stmt;
import mmj.lang.Sym;
import mmj.lang.Theorem;
import mmj.lang.VarHyp;

public class MMHTMLPrinter {
	public static String printMObj(MetamathProjectNature nature, MObj mobj) {
		StringBuffer buffer = new StringBuffer();
		printMObj(buffer, nature, mobj);
		return buffer.toString();
	}

	public static void printMObj(StringBuffer buffer, MetamathProjectNature nature, MObj mobj) {
		HTMLPrinter.addPageProlog(buffer);
		buffer.append(" <span style=\"");
		FontData fontData = nature.getLabelProvider().getEditorFont().getFontData()[0];
		fontData.setHeight(8);
		addFont(buffer, fontData);
		buffer.append("\">");
		addImage(buffer, nature.getLabelProvider().getImageURL(mobj));
		buffer.append(" <b>"+mobj.toString()+"</b><hr />");
		if(mobj instanceof Assrt) {
			// append hypothesis
			for(LogHyp hyp:((Assrt)mobj).getLogHypArray()) addFormula(buffer, nature, hyp.getFormula());
		}
		if(mobj instanceof Stmt) {
			// append formula
			buffer.append(((Stmt)mobj).getTyp()+" ");
			addFormula(buffer, nature, ((Stmt)mobj).getFormula());
		}
		buffer.append("</span><span style=\"font-size: 8 pt;\">");
		if(mobj.getDescription() != null) buffer.append(mobj.getDescription()+"<br />");
		buffer.append("</span>");
		HTMLPrinter.addPageEpilog(buffer);
	}

	public static void addImage(StringBuffer buffer, URL imageUrl) {
		if (imageUrl != null) {
			buffer.append("<img src=\""+imageUrl.toExternalForm()+"\" style=\"vertical-align:top;\"/>");
		}
	}

	public static void addFont(StringBuffer buffer, FontData fontData) {
		boolean bold= (fontData.getStyle() & SWT.BOLD) != 0;
		boolean italic= (fontData.getStyle() & SWT.ITALIC) != 0;
		
		// See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=155993
		String size= Integer.toString(fontData.getHeight()) + ("carbon".equals(SWT.getPlatform()) ? "px" : "pt"); 
		
		String family= "'" + fontData.getName() + "',sans-serif"; 
		buffer.append("font-size:" + size + ";");
		buffer.append("font-weight:" + (bold ? "bold" : "normal") + ";"); 
		buffer.append("font-style:" + (italic ? "italic" : "normal") + ";"); 
		buffer.append("font-family:" + family + ";"); 
	}
	
	public static void addFormula(StringBuffer buffer, MetamathProjectNature nature, Formula formula) {
		for(Sym sym:formula.getExpr()) buffer.append(nature.getLabelProvider().getHTMLText(sym)+" ");
		buffer.append("<br />");
	}
}
