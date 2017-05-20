package org.tirix.emetamath.properties;

import java.util.Hashtable;
import java.util.Map;

import mmj.lang.Cnst;
import mmj.lang.Sym;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.tirix.emetamath.importWizards.ImportFileWizardPage;
import org.tirix.emetamath.nature.MetamathProjectNature;

public class MetamathProjectPropertyPage extends PropertyPage {

	private static final int TYPE_COLUMN = 0;
	private static final int COLOR_COLUMN = 1;
	private static final int STMT_TYPE_COLUMN = 2;
//	private static final String PATH_TITLE = "Path:";
	private static final String BASE_EXPLORER_URL_TITLE = "Web Explorer Base &URL:";
//	private static final String MAINFILE_TITLE = "Main File:";
	private static final String AUTO_TRANSFORM_TITLE = "Enable Auto Transformations:";

//	private static final int TEXT_FIELD_WIDTH = 50;

	private Text baseExplorerUrlText;
	private Table otherTypesTable;
	private Text provableTypeText;
	private Text mainFileText;
	private Button autoTransformCheckbox;

	private String logicStmtType; // the selected logic statement type
	
	/**
	 * Constructor for SamplePropertyPage.
	 */
	public MetamathProjectPropertyPage() {
		super();
	}

	private MetamathProjectNature getNature() {
		return MetamathProjectNature.getNature((IResource)getElement());
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

//		addFirstSection(composite);
//		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

//	private void addFirstSection(Composite parent) {
//		Composite composite = createDefaultComposite(parent);
//
//		//Label for path field
//		Label pathLabel = new Label(composite, SWT.NONE);
//		pathLabel.setText(PATH_TITLE);
//
//		// Path text field
//		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
//		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
//	}
//
//	private void addSeparator(Composite parent) {
//		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
//		GridData gridData = new GridData();
//		gridData.horizontalAlignment = GridData.FILL;
//		gridData.grabExcessHorizontalSpace = true;
//		separator.setLayoutData(gridData);
//	}

	private void addSecondSection(Composite parent) {
		//Composite composite = createDefaultComposite(parent);
		String mainFileName = "";
		String provableTypeString = "";
		String logicStmtTypeString = "";
		try {
			mainFileName = getNature().getMainFile().getName();
			provableTypeString = getNature().getProvableTypeString();
			logicStmtTypeString = getNature().getLogicStmtTypeString();
			System.out.println("Got provableType="+provableTypeString);
			System.out.println("Got logicStmtType="+logicStmtTypeString);
		} catch (CoreException e1) {
		} catch (NullPointerException e1) {
		}
		logicStmtType = logicStmtTypeString;
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		composite.setLayoutData(fileSelectionData);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		
		//////////////
		// Main File
		Label mainFileLabel = new Label(composite, SWT.NULL);
		mainFileLabel.setText("&Main file name:");
		mainFileText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		mainFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mainFileText.setText(mainFileName);
		mainFileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// TODO - validate ?
			}
		});
		createMainFileChooserButton(composite);
		
		//////////////
		// Provable Assertion
		Label provableTypeLabel = new Label(composite, SWT.NONE);
		provableTypeLabel.setText("Provable Type:");
		provableTypeText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);		//gd.widthHint = convertWidthInCharsToPixels(5);
		provableTypeText.setLayoutData(gd);
		provableTypeText.setText(provableTypeString);
		provableTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// TODO chooser/validation based on LogicalSystem ?
			}
		});
		new Label(composite, SWT.NONE).setEnabled(false);
		
		//////////////
		// Other types and their colors
		Label otherTypesLabel = new Label(composite, SWT.NONE);
		GridData gridData2 = new GridData();
		gridData2.verticalSpan = 2;
		otherTypesLabel.setText("Other Types:");
		otherTypesLabel.setLayoutData(gridData2);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.verticalSpan = 2;
		otherTypesTable = new Table(composite, SWT.SINGLE | SWT.BORDER);
		otherTypesTable.setHeaderVisible(true);
		otherTypesTable.setLinesVisible(true);
		otherTypesTable.setLayoutData(gridData);
	    TableColumn typeColumn = new TableColumn(otherTypesTable, SWT.LEFT);
	    typeColumn.setText("Type");
	    typeColumn.setWidth(100);
	    typeColumn.pack();
	    TableColumn colorColumn = new TableColumn(otherTypesTable, SWT.CENTER);
	    colorColumn.setText("Color");
	    typeColumn.setWidth(50);
	    colorColumn.pack();
	    TableColumn stmtTypeColumn = new TableColumn(otherTypesTable, SWT.LEFT);
	    stmtTypeColumn.setText("Statement");
	    stmtTypeColumn.pack();
	    fillTypesTable(otherTypesTable, getNature().getTypeColors());
		addTableEditor(otherTypesTable);

		createAddRemoveTypeButtons(composite);

		//////////////
		// Base explorer URL field
		Label baseExplorerUrlLabel = new Label(composite, SWT.NONE);
		baseExplorerUrlLabel.setText(BASE_EXPLORER_URL_TITLE);
		baseExplorerUrlText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		baseExplorerUrlText.setLayoutData(gd);
		baseExplorerUrlText.setText(getNature().getWebExplorerURL());
		new Label(composite, SWT.NONE).setEnabled(false);

		//////////////
		// Auto Transformations
		Label autoTransformLabel = new Label(composite, SWT.NONE);
		autoTransformLabel.setText(AUTO_TRANSFORM_TITLE);
		autoTransformCheckbox = new Button(composite, SWT.CHECK);
		autoTransformCheckbox.setText("");
		autoTransformCheckbox.setEnabled(true);
		autoTransformCheckbox.setGrayed(false);
		try {
			autoTransformCheckbox.setSelection(getNature().isAutoTransformationsEnabled());
		} catch (CoreException e) { }
		
	}

	private void fillTypesTable(Table table, Map<Cnst, RGB> typeColors) {
		table.setItemCount(typeColors.size()+1);
		table.removeAll();
		
		// If the logic statement type is not in the list of types with color configured, add it.
		boolean addLogicStmtType = true;
		for(Cnst type:typeColors.keySet()) addLogicStmtType &= !type.getId().equals(logicStmtType);
		if(addLogicStmtType) {
			Cnst logicStmtSym = getNature().getLogicStmtType();
			if(logicStmtSym!=null) typeColors.put(logicStmtSym, table.getDisplay().getSystemColor(SWT.COLOR_BLUE).getRGB());
		}

		// Create the table items
		for(Cnst type:typeColors.keySet()) {
			createTypeTableItem(table, type.getId(), typeColors.get(type));
		}
	}

	private void createTypeTableItem(Table table, String symbol, RGB rgb) {
		Color color = new Color(table.getDisplay(), rgb);
		TableItem item = new TableItem(otherTypesTable, SWT.NONE);
		item.setText(TYPE_COLUMN, symbol);
		//Button button = new Button(table, SWT.PUSH);
		//button.computeSize(SWT.DEFAULT, table.getItemHeight());
		//button.setBackground(color);
		item.setBackground(COLOR_COLUMN, color);
//		TableEditor editor = new TableEditor(otherTypesTable);
//		editor.grabHorizontal = true;
//		editor.minimumHeight = button.getSize().y;
//		editor.minimumWidth = button.getSize().x;
//		editor.setEditor(button, item, 1);
		TableEditor editor = new TableEditor(table);
		Button button = new Button(table, SWT.RADIO);
		button.setSelection(symbol.equals(logicStmtType));
		button.pack();
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button)e.widget).getSelection()) {
					logicStmtType = symbol;
				}
			}
		});
		editor.minimumWidth = button.getSize().x;
		editor.horizontalAlignment = SWT.LEFT;
		editor.setEditor(button, item, STMT_TYPE_COLUMN);
	}
	
	private void addTableEditor(final Table table) {
		// Create an editor object to use for text editing
		final TableEditor editor = new TableEditor(table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		// Use a mouse listener, not a selection listener, since we're interested
		// in the selected column as well as row
		table.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				// Dispose any existing editor
				Control old = editor.getEditor();
				if (old != null) old.dispose();

				// Determine where the mouse was clicked
				Point pt0 = new Point(1, event.y);

				// Determine which row was selected
				final TableItem item = table.getItem(pt0);
				if (item != null) {
					// Determine which column was selected
					int column = -1;
					Point pt = new Point(event.x, event.y);
					for (int i = 0, n = table.getColumnCount(); i < n; i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							// This is the selected column
							column = i;
							break;
						}
					}

					switch(column) {
					case COLOR_COLUMN:
						ColorDialog dialog = new ColorDialog(table.getShell());
						Color color = item.getBackground(COLOR_COLUMN);
						dialog.setRGB(color.getRGB());
						RGB rgb = dialog.open();
						if (rgb != null) {
							if (color != null) color.dispose();
							color = new Color(table.getShell().getDisplay(), rgb);
							item.setBackground(COLOR_COLUMN, color);
						} 
						break;

					case TYPE_COLUMN:
						// Create the Text object for our editor
						final Text text = new Text(table, SWT.NONE);
						text.setForeground(item.getForeground());
	
						// Transfer any text from the cell to the Text control,
						// set the color to match this row, select the text,
						// and set focus to the control
						text.setText(item.getText(column));
						text.setForeground(item.getForeground());
						text.selectAll();
						text.setFocus();
	
						// Recalculate the minimum width for the editor
						editor.minimumWidth = text.getBounds().width;
	
						// Set the control into the editor
						editor.setEditor(text, item, column);
	
						// Add a handler to transfer the text back to the cell
						// any time it's modified
						final int col = column;
						text.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent event) {
								// TODO need to an a valiator here, value shall not be empty, and shall not contain space, at least
								// Set the text of the editor's control back into the cell
								item.setText(col, text.getText());
							}
						});
					break;
					}
				}
			}
		});
	}

	/**
	 * @param composite
	 */
	private void createMainFileChooserButton(Composite composite) {
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(
						getShell(), false, (IProject)getElement(), FilteredResourcesSelectionDialog.CARET_BEGINNING);
				dialog.setMessage("Select new main file");
				dialog.setInitialPattern("*.mm ");
				try {
					dialog.setInitialSelections(new Object[] { getNature().getMainFile() });
				} catch (CoreException ex) {
				}
				if (dialog.open() == ContainerSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result.length == 1) {
						IFile f = (IFile) result[0];
						mainFileText.setText(f.getName());
					}
				}
			}
		});
	}

	/**
	 * @param composite
	 */
	private void createAddRemoveTypeButtons(Composite composite) {
		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add...");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputDialog dialog = new InputDialog(getShell(), "Add type", "Input new type symbol", "", null);
				if (dialog.open() == ContainerSelectionDialog.OK) {
					int itemCount = otherTypesTable.getItemCount();
					String newTypeSymbol = dialog.getValue();
					createTypeTableItem(otherTypesTable, newTypeSymbol, new RGB(0, 0, 0));
					otherTypesTable.setItemCount(itemCount+1);
					otherTypesTable.pack(true);
					otherTypesTable.getParent().layout(true);
				}
			}
		});
		Button removeButton = new Button(composite, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(otherTypesTable.getSelectionCount() > 0) otherTypesTable.remove(otherTypesTable.getSelectionIndex());
				removeButton.setEnabled(false);
			}
		});
		removeButton.setEnabled(false);
		otherTypesTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeButton.setEnabled(otherTypesTable.getSelectionCount() > 0);
			}
		});
	}


//	private Composite createDefaultComposite(Composite parent) {
//		Composite composite = new Composite(parent, SWT.NULL);
//		GridLayout layout = new GridLayout();
//		layout.numColumns = 2;
//		composite.setLayout(layout);
//
//		GridData data = new GridData();
//		data.verticalAlignment = GridData.FILL;
//		data.horizontalAlignment = GridData.FILL;
//		composite.setLayoutData(data);
//
//		return composite;
//	}
//
	
	private Map<Cnst,RGB> getTypeColors() {
		Hashtable<Cnst, RGB> typeColors = new Hashtable<Cnst, RGB>();
		for(int i=0;i<otherTypesTable.getItemCount();i++) {
			TableItem item = otherTypesTable.getItem(i);
			String typeSymbol = item.getText(TYPE_COLUMN);
			Cnst type = (Cnst)getNature().getLogicalSystem().getSymTbl().get(typeSymbol);
			RGB color = item.getBackground(COLOR_COLUMN).getRGB();
			typeColors.put(type, color);
		}
		return typeColors;
	}
	
	protected void performDefaults() {
		// Populate the explorer URL text field with the default value
		logicStmtType = MetamathProjectNature.LOGIC_STMT_TYPE_DEFAULT_VALUE;
		baseExplorerUrlText.setText(MetamathProjectNature.EXPLORER_BASE_URL_DEFAULT_VALUE);
		provableTypeText.setText(MetamathProjectNature.PROVABLE_TYPE_DEFAULT_VALUE);
		autoTransformCheckbox.setSelection(MetamathProjectNature.AUTO_TRANSFORMATIONS_ENABLED_DEFAULT_VALUE);
		fillTypesTable(otherTypesTable, getNature().getDefaultTypeColors());
	}

	public boolean performOk() {
		// store the value in the explorer URL text field
		IFile newMainFile = ((IProject)getElement()).getFile(mainFileText.getText());
		if(!newMainFile.getFileExtension().equals("mm")) { 
   			MessageBox messageBox = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR);
   			messageBox.setText("Main File Selection");
   			messageBox.setMessage("Main file has to be a Metamath Language file (.mm file extension)");
   			messageBox.open();
			return false;
		}
		try {
			getNature().setWebExplorerURL(baseExplorerUrlText.getText());
			getNature().setProvableType((Cnst)(getNature().getMObj(provableTypeText.getText())));
			getNature().setLogicStmtType((Cnst)(getNature().getMObj(logicStmtType)));
			getNature().setTypeColors(getTypeColors());
			getNature().setMainFile(newMainFile);
			getNature().setAutoTransformationsEnabled(autoTransformCheckbox.getSelection());
		} catch (CoreException e) {
			// TODO display error message
			return false;
		}
		return true;
	}

}