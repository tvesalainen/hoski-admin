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

package fi.hoski.remote.ui;

import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

/**
 * @author Timo Vesalainen
 */
public class TableSelectionHandler 
{
    private JTable table;
    private int row;
    private int col;

    public TableSelectionHandler(JTable table)
    {
        this.table = table;
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    Object source = e.getSource();
                    if (source instanceof ListSelectionModel)
                    {
                        ListSelectionModel lsm = (ListSelectionModel) source;
                        row = lsm.getMinSelectionIndex();
                        changed(row, col);
                    }
                }
            }
        });
        table.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    Object source = e.getSource();
                    if (source instanceof ListSelectionModel)
                    {
                        ListSelectionModel lsm = (ListSelectionModel) source;
                        col = lsm.getMinSelectionIndex();
                        changed(row, col);
                    }
                }
            }

        });
    }
    private void changed(int row, int col)
    {
        table.editCellAt(row, col);
        Component editorComponent = table.getEditorComponent();
        if (editorComponent instanceof JTextComponent)
        {
            JTextComponent text = (JTextComponent) editorComponent;
            text.selectAll();
        }
        if (editorComponent instanceof JCheckBox)
        {
            JCheckBox checkBox = (JCheckBox) editorComponent;
        }
        //System.err.println(row+", "+col);
    }
    
}
