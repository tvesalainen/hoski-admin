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

import fi.hoski.util.Day;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * @author Timo Vesalainen
 */
public class DateTableCellEditor extends AbstractCellEditor implements TableCellEditor, FocusListener
{
    private String title;
    private Day date;
    private JTextField label;

    public DateTableCellEditor(String title)
    {
        this.title = title;
        label = new JTextField();
        label.addFocusListener(this);
    }
        
    @Override
    public Object getCellEditorValue()
    {
        return date;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        date = (Day) value;
        if (date != null)
        {
            label.setText(date.toString());
        }
        return label;
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        date = DateChooser.chooseDate(title, date);
        fireEditingStopped();
    }

    @Override
    public void focusLost(FocusEvent e)
    {
    }
}
