package org.tirix.emetamath.properties;

import java.util.Map;

import mmj.lang.Cnst;

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
	private static final String PATH_TITLE = "Path:";
	private static final String BASE_EXPLORER_URL_TITLE = "Web Explorer Base &URL:";
	private static final String MAINFILE_TITLE = "Main File:";

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text baseExplorerUrlText;
	private Table otherTypesTable;
	private Text provableTypeText;
	private Text mainFileText;

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

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		//Composite composite = createDefaultComposite(parent);

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
		mainFileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// TODO - validate ?
			}
		});
		try {
			mainFileText.setText(getNature().getMainFile().getName());
		} catch (CoreException e1) {
		} catch (NullPointerException e1) {
		}
		createMainFileChooserButton(composite);
		
		//////////////
		// Provable Assertion
		Label provableTypeLabel = new Label(composite, SWT.NONE);
		provableTypeLabel.setText("Provable Type:");
		provableTypeText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);		//gd.widthHint = convertWidthInCharsToPixels(5);
		provableTypeText.setLayoutData(gd);
		try {
			provableTypeText.setText(getNature().getProvableTypeString());
		} catch (CoreException e1) { }
		// TODO chooser/validation based on LogicalSystem ?
		new Label(composite, SWT.NONE).setEnabled(false);
		
		//////////////
		// Other types and their colors
		Label otherTypesLabel = new Label(composite, SWT.NONE);
		otherTypesLabel.setText("Other Types:");
		otherTypesTable = new Table(composite, SWT.SINGLE | SWT.BORDER);
		otherTypesTable.setHeaderVisible(true);
		otherTypesTable.setLinesVisible(true);
		otherTypesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
	    TableColumn typeColumn = new TableColumn(otherTypesTable, SWT.CENTER);
	    typeColumn.setText("Type");
	    typeColumn.pack();
	    TableColumn colorColumn = new TableColumn(otherTypesTable, SWT.CENTER);
	    colorColumn.setText("Color");
	    colorColumn.pack();
	    fillTypesTable(otherTypesTable, getNature().getTypeColors());
		addTableEditor(otherTypesTable);

	    //otherTypesTable.setText(getNature().getProvableType());
		// TODO spawn over 2 columns ?
		new Label(composite, SWT.NONE).setEnabled(false);

		//////////////
		// Base explorer URL field
		Label baseExplorerUrlLabel = new Label(composite, SWT.NONE);
		baseExplorerUrlLabel.setText(BASE_EXPLORER_URL_TITLE);
		baseExplorerUrlText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		baseExplorerUrlText.setLayoutData(gd);
		baseExplorerUrlText.setText(getNature().getWebExplorerURL());

	}

	private void fillTypesTable(Table table, Map<Cnst, RGB> typeColors) {
		table.setItemCount(typeColors.size()+1);
		table.removeAll();
		for(Cnst type:typeColors.keySet()) {
			Color color = new Color(table.getDisplay(), typeColors.get(type));
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(TYPE_COLUMN, type.getId());
//			Button button = new Button(table, SWT.PUSH);
//			button.computeSize(SWT.DEFAULT, table.getItemHeight());
//			button.setBackground(color);
			item.setBackground(COLOR_COLUMN, color);
//			TableEditor editor = new TableEditor(otherTypesTable);
//			editor.grabHorizontal = true;
//			editor.minimumHeight = button.getSize().y;
//			editor.minimumWidth = button.getSize().x;
//			editor.setEditor(button, item, 1);
		}
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

					if(column == 1) {
						ColorDialog dialog = new ColorDialog(table.getShell());
						Color color = item.getBackground(COLOR_COLUMN);
						dialog.setRGB(color.getRGB());
						RGB rgb = dialog.open();
						if (rgb != null) {
							if (color != null) color.dispose();
							color = new Color(table.getShell().getDisplay(), rgb);
							item.setBackground(COLOR_COLUMN, color);
						} 
						return;
					}
					
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
							// Set the text of the editor's control back into the cell
							item.setText(col, text.getText());
						}
					});
				}
			}
		});
	}

	private Map<Cnst, RGB> extractTypeColors() {
		return null;
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

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		// Populate the explorer URL text field with the default value
		baseExplorerUrlText.setText(MetamathProjectNature.EXPLORER_BASE_URL_DEFAULT_VALUE);
		provableTypeText.setText(MetamathProjectNature.PROVABLE_TYPE_DEFAULT_VALUE);
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
			getNature().setMainFile(newMainFile);
		} catch (CoreException e) {
			// TODO display error message
			return false;
		}
		return true;
	}

}