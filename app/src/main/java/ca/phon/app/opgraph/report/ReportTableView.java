package ca.phon.app.opgraph.report;

import ca.phon.app.opgraph.report.tree.TableNode;
import ca.phon.app.opgraph.wizard.ResultTableMouseAdapter;
import ca.phon.app.opgraph.wizard.actions.SaveTableAsAction;
import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.Period;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ReportTableView extends JPanel {

    private JToolBar toolbar;

    private JXTable table;

    private final TableNode tableNode;

    public ReportTableView(TableNode tableNode) {
        super();

        this.tableNode = tableNode;

        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        final TableNodeTableModel tableModel = new TableNodeTableModel(tableNode);
        table = new JXTable(tableModel);
        table.setColumnControlVisible(true);
        table.setSortable(false);
        table.setDefaultRenderer(Period.class, new AgeCellRenderer());
        table.addMouseListener(new ResultTableMouseAdapter(this));
        table.addMouseListener(contextMenuHandler);

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            final String colName = tableModel.getColumnName(i);
            var tblColExt = table.getColumnExt(table.convertColumnIndexToView(i));
            if (tableNode.isIncludeColumns()) {
                tblColExt.setVisible(tableNode.getColumns().size() == 0 || tableNode.getColumns().contains(colName));
            } else {
                tblColExt.setVisible(!tableNode.getColumns().contains(colName));
            }
        }
        SwingUtilities.invokeLater(table::packAll);

        JScrollPane scroller = new JScrollPane(table);
        add(scroller, BorderLayout.CENTER);
    }

    public JXTable getTable() {
        return table;
    }

    public TableNode getTableNode() {
        return tableNode;
    }

    private class AgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel retVal = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof Period) {
                final Formatter<Period> ageFormatter = FormatterFactory.createFormatter(Period.class);
                retVal.setText(ageFormatter.format((Period) value));
            }

            return retVal;
        }
    }

    private void showContextMenu(MouseEvent e) {
        final JPopupMenu popupMenu = new JPopupMenu();
        final List<String> columns = new ArrayList<>();
        final Enumeration<TableColumn> tblColumnEnum = table.getColumnModel().getColumns();
        while(tblColumnEnum.hasMoreElements()) {
            final TableColumn tblColumn = tblColumnEnum.nextElement();
            columns.add(table.getModel().getColumnName(tblColumn.getModelIndex()));
        }
        popupMenu.add(new JMenuItem(new SaveTableAsAction(tableNode, columns, tableNode.getTitle(), TableExporter.TableExportType.CSV)));
        popupMenu.add(new JMenuItem(new SaveTableAsAction(tableNode, columns, tableNode.getTitle(), TableExporter.TableExportType.EXCEL)));
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private final MouseInputAdapter contextMenuHandler = new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if(e.isPopupTrigger())
                showContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(e.isPopupTrigger())
                showContextMenu(e);
        }

    };

}
