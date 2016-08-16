package ca.phon.app.opgraph.nodes.query;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import ca.gedge.opgraph.OpContext;
import ca.gedge.opgraph.OpNodeInfo;
import ca.gedge.opgraph.app.extensions.NodeSettings;
import ca.gedge.opgraph.exceptions.ProcessingException;
import ca.phon.query.report.datasource.DefaultTableDataSource;
import ca.phon.query.report.datasource.TableDataSource;
import ca.phon.script.PhonScript;
import ca.phon.script.PhonScriptContext;
import ca.phon.script.PhonScriptException;
import ca.phon.ui.text.PromptedTextField;

/**
 * Add a new column to the given table using
 * a user-entered script.
 * 
 */
@OpNodeInfo(
		category="Report",
		description="Add a new column to the given table using a script.",
		name="Add Column",
		showInLibrary=true
)
public class AddColumnNode extends TableScriptNode {

	private final static String DEFAULT_SCRIPT = "/*\n" + 
			"params = {label, \"Add a new column to the input table using javascript.\", \"<html><b>Add column to table</b></html>\"}\n" + 
			";\n" + 
			"*/\n" + 
			"\n" + 
			"function getRowValue(table, row) {\n" + 
			"	return new String();\n" + 
			"}\n" + 
			"";
	
	// settings
	private String columnName = "NewColumn";
	
	private int columnIndex = -1;
	
	private PromptedTextField columnIndexField;
	
	// UI
	private PromptedTextField columnNameField;
	
	public AddColumnNode() {
		super(DEFAULT_SCRIPT);
	}

	@Override
	public void operate(OpContext context) throws ProcessingException {
		final PhonScript phonScript = getScript();
		final PhonScriptContext scriptContext = phonScript.getContext();
		
		final TableDataSource table = super.getInputTable(context);
		
		final DefaultTableDataSource outputTable = new DefaultTableDataSource();
		
		int columnCount = table.getColumnCount() + 1;
		int colIndex = (getColumnIndex() >= 0 ? getColumnIndex() : columnCount-1);
		
		try {
			final Scriptable scope = scriptContext.getEvaluatedScope();
			scriptContext.installParams(scope);
			
			for(int row = 0; row < table.getRowCount(); row++) {
				checkCanceled();
				
				final Object[] rowData = new Object[columnCount];
				
				
				for(int col = 0; col < colIndex; col++) {
					rowData[col] = table.getValueAt(row, col);
				}
				for(int col = colIndex+1; (col-1) < table.getColumnCount(); col++) {
					rowData[col] = table.getValueAt(row, col-1);
				}
				
				Object newVal = scriptContext.callFunction(scope, "getRowValue", 
						table, row);
				if(newVal instanceof NativeJavaObject) {
					newVal = ((NativeJavaObject)newVal).unwrap();
				}
				
				rowData[colIndex] = (newVal != null ? newVal : new String());
				
				// add row to outputtable
				outputTable.addRow(rowData);
			}
		} catch (PhonScriptException e) {
			throw new ProcessingException(null, e);
		}
		
		for(int col = 0; col < colIndex; col++) 
			outputTable.setColumnTitle(col, table.getColumnTitle(col));
		outputTable.setColumnTitle(colIndex, getColumnName());
		for(int col = colIndex+1; (col-1) < table.getColumnCount(); col++) 
			outputTable.setColumnTitle(col, table.getColumnTitle(col-1));
		
		context.put(tableOutput, outputTable);
	}
	
	@Override
	protected JPanel createSettingsPanel() {
		final JPanel retVal = super.createSettingsPanel();
		
		final GridBagLayout layout = new GridBagLayout();
		final JPanel settingsPanel = new JPanel(layout);
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 2, 2, 2);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		settingsPanel.add(new JLabel("Column Name:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		columnNameField = new PromptedTextField("Enter new column name");
		columnNameField.setText(this.columnName);
		settingsPanel.add(columnNameField, gbc);
		
		++gbc.gridy;
		gbc.gridx = 0;
		gbc.weightx = 0.0;
		settingsPanel.add(new JLabel("Column Index:"), gbc);
		++gbc.gridx;
		gbc.weightx = 1.0;
		columnIndexField = new PromptedTextField("Enter column index (optional)");
		if(columnIndex >= 0)
			columnIndexField.setText(Integer.toString(columnIndex));
		settingsPanel.add(columnIndexField, gbc);
		
		retVal.add(settingsPanel, BorderLayout.NORTH);
		
		return retVal;
	}
	
	public String getColumnName() {
		return (columnNameField != null ? columnNameField.getText() : columnName);
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public int getColumnIndex() {
		return (columnIndexField != null ? Integer.parseInt(columnIndexField.getText()) : columnIndex);
	}
	
	public void setColumnIndex(int colIndex) {
		this.columnIndex = colIndex;
		if(columnIndexField != null) {
			columnIndexField.setText(Integer.toString(colIndex));
		}
	}

	@Override
	public Properties getSettings() {
		final Properties retVal = super.getSettings();
		
		retVal.setProperty("columnIndex", Integer.toString(getColumnIndex()));
		retVal.setProperty("column", getColumnName());
		
		return retVal;
	}

	@Override
	public void loadSettings(Properties properties) {
		super.loadSettings(properties);
		
		setColumnName(properties.getProperty("column", "MyColumn"));
		setColumnIndex(Integer.parseInt(properties.getProperty("columnIndex", "-1")));
	}

}
