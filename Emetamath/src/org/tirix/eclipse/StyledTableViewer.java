package org.tirix.eclipse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

public class StyledTableViewer extends TableViewer {
	protected int styledColumn;
	protected Map<TableItem, TableEditor> editorMap;
	protected IRule fRule;

	public StyledTableViewer(Table table, int styledColumn) {
		super(table);
		this.styledColumn = styledColumn;
		this.editorMap = new HashMap<TableItem, TableEditor>();
	}

	public void setRule(IRule rule) {
		this.fRule = rule;
	}

	public Control getControl(TableItem item) {
		TableEditor tableEditor = editorMap.get(item);
		if(tableEditor == null) return null;
		return tableEditor.getEditor();
	}

	public Font getDefaultFont(Object element) {
		IBaseLabelProvider labelProvider = getLabelProvider();
		if(labelProvider instanceof ITableFontProvider) {
			return ((ITableFontProvider)labelProvider).getFont(element, styledColumn);
		}
		return null;
	}

	@Override
	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
		boolean oldBusy = isBusy();
		setBusy(true);
		try {
			super.doUpdateItem(widget, element, fullMap);
			if (widget instanceof TableItem) {
				final TableItem item = (TableItem) widget;

				String text = item.getText(styledColumn);
				StyledText textWidget = createStyledText(getTable(), text, element);
				TableEditor tableEditor = editorMap.get(item);
				if(tableEditor == null) {
					tableEditor = new TableEditor(getTable());
					tableEditor.horizontalAlignment = SWT.LEFT;
					tableEditor.verticalAlignment = SWT.CENTER;
					tableEditor.grabHorizontal = true;
					//tableEditor.grabVertical = true;
					tableEditor.minimumWidth = 50;
					editorMap.put(item, tableEditor);
				}
				Control oldWidget = tableEditor.getEditor();
				if(oldWidget != null) oldWidget.dispose();
				tableEditor.setEditor(textWidget, item, styledColumn);
				if(widget.isDisposed()) {
					editorMap.remove(item);
					tableEditor.dispose();
				}
			}
		} finally {
			setBusy(oldBusy);
		}
	}

	@Override
	protected void doRemove(int start, int end) {
		for(int i = start; i <= end; i++) {
			TableItem item = getTable().getItem(i);
			TableEditor tableEditor = editorMap.get(item);
			if(tableEditor != null) {
				Control widget = tableEditor.getEditor();
				if(widget != null) widget.dispose();
				tableEditor.dispose();
			}
			editorMap.remove(item);
		}
		super.doRemove(start, end);
	}
	
	private StyledText createStyledText(Composite parent, String text, Object element) {
		StyledText widget = new StyledText(parent, SWT.NONE);
		widget.setText(text);
		widget.setForeground(getTable().getForeground());
		widget.setBackground(getTable().getBackground());
		widget.setFont(getDefaultFont(element));
		widget.setEditable(false);
		TextPresentation presentation = new TextPresentation(text.length() / 4); 
		//presentation.setDefaultStyleRange(createRange(0, text.length(), defaultTextAttribute));
		if(fRule == null) {
			//addRange(presentation, 0, text.length(), defaultTextAttribute);
		}
		else {
			StringScanner scanner = new StringScanner(text, fRule, null);
			scanner.createPresentation(presentation);
		}
		Iterator<StyleRange> iterator = presentation.getAllStyleRangeIterator();
		while(iterator.hasNext()) widget.setStyleRange(iterator.next());
		return widget;
	}

	/**
	 * Adds style information to the given text presentation.
	 *
	 * @param presentation the text presentation to be extended
	 * @param offset the offset of the range to be styled
	 * @param length the length of the range to be styled
	 * @param attr the attribute describing the style of the range to be styled
	 */
	protected static void addRange(TextPresentation presentation, int offset, int length, TextAttribute attr) {
		if (attr == null) return;
		int style= attr.getStyle();
		int fontStyle= style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
		StyleRange styleRange= new StyleRange(offset, length, attr.getForeground(), attr.getBackground(), fontStyle);
		styleRange.strikeout= (style & TextAttribute.STRIKETHROUGH) != 0;
		styleRange.underline= (style & TextAttribute.UNDERLINE) != 0;
		styleRange.font= attr.getFont();
		presentation.addStyleRange(styleRange);
	}

	protected static class StringScanner implements ICharacterScanner {
		TextAttribute defaultTextAttribute;
		IToken defaultToken;
		int tokenOffset;
		String text;
		IRule rule;
		int offset;
		
		public StringScanner(String text, IRule rule, TextAttribute defaultTextAttribute) {
			this.defaultTextAttribute = defaultTextAttribute;
			this.defaultToken = new Token(defaultTextAttribute);
			this.text = text;
			this.rule = rule;
			offset = 0;
		}

		public void createPresentation(TextPresentation presentation) {
			TextAttribute lastAttribute= defaultTextAttribute;
			boolean firstToken= true;
			int lastStart= 0;
			int length= 0;

			while (true) {
				IToken token= nextToken();
				if (token.isEOF())
					break;

				TextAttribute attribute= (TextAttribute)token.getData();
				if (lastAttribute != null && lastAttribute.equals(attribute)) {
					length += offset - tokenOffset;
					firstToken= false;
				} else {
					if (!firstToken) addRange(presentation, lastStart, length, lastAttribute);
					firstToken= false;
					lastAttribute= attribute;
					lastStart= tokenOffset;
					length= offset - tokenOffset;
				}
			}
			addRange(presentation, lastStart, length, lastAttribute);
		}

		public IToken nextToken() {
			tokenOffset= offset;

			IToken token= (rule.evaluate(this));
			if (!token.isUndefined()) return token;
			if (read() == EOF) return Token.EOF;
			return defaultToken;
		}
		
		@Override
		public int getColumn() {
			throw new RuntimeException("Not implemented : getColumn");
		}

		@Override
		public char[][] getLegalLineDelimiters() {
			throw new RuntimeException("Not implemented : getLegalLineDelimiters");
		}

		@Override
		public int read() {
			if(offset >= text.length()) return EOF;
			return text.charAt(offset++);
		}

		@Override
		public void unread() {
			if(offset <= 0) return;
			offset--;
		}
	}
}
