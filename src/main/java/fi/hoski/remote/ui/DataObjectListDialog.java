/*
 * Copyright (C) 2012 Helsingfors Segelklubb ry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * To change this template, select Tools | Templates
 * and open the template in the editor.
 */

package fi.hoski.remote.ui;

import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/**
 * @author Timo Vesalainen
 */
public class DataObjectListDialog<T extends DataObject> extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    private JTable table;
    private List<T> list;
    private List<T> selected;
    private DataObjectModel model;
    private String title;

    public DataObjectListDialog(
            Frame owner, 
            String title, 
            String action, 
            DataObjectModel model,
            final List<T> list
            )
    {
        super(owner, title);
        this.title = title;
        this.list = list;
        this.model = model;
        
        final DataObjectListDialog<T> t = this;
        
        //setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        DataObjectListTableModel<T> etm = new DataObjectListTableModel<>(model, list);
        table = new FitTable(etm);
        TableSelectionHandler tsh = new TableSelectionHandler(table);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(String.class, new StringTableCellRenderer());
        add(scrollPane, BorderLayout.CENTER);
        TableSelector ts = new TableSelector(table, this) {

            @Override
            protected boolean selected(int row, int col)
            {
                t.selected();
                return true;
            }
        };
        table.addKeyListener(ts);
        table.addMouseListener(ts);
        
        
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString(action));
        ActionListener okAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                selected();
            }

        };
        ok.addActionListener(okAction);
        panel.add(ok);
        
        JButton cancel = new JButton(uiBundle.getString("CANCEL"));
        ActionListener cancelAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        };
        cancel.addActionListener(cancelAction);
        panel.add(cancel);

        setLocation(10, 10);
        //setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        pack();
    }

    private void selected()
    {
        TableCellEditor cellEditor = table.getCellEditor();
        if (cellEditor != null)
        {
            cellEditor.stopCellEditing();
        }
        int[] selectedRows = table.getSelectedRows();
        selected = new ArrayList<>();
        for (int ii : selectedRows)
        {
            selected.add(list.get(ii));
        }
        setVisible(false);
    }
    
    public void setSelectionModel(ListSelectionModel newModel)
    {
        table.setSelectionModel(newModel);
    }

    public void setSelectionMode(int selectionMode)
    {
        table.setSelectionMode(selectionMode);
    }

    public void addColumn(TableColumn aColumn)
    {
        table.addColumn(aColumn);
    }

    public void setAutoCreateRowSorter(boolean on)
    {
        table.setAutoCreateRowSorter(on);
    }
    public List<T> select()
    {
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        setVisible(true);
        return selected;
    }
    
    public void print() throws PrinterException
    {
        MessageFormat header = new MessageFormat(title+uiBundle.getString("PAGE HEADER"));
        table.print(JTable.PrintMode.FIT_WIDTH, header, null);
    }
}
