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

import com.google.appengine.api.datastore.Text;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * @author Timo Vesalainen
 */
public class ComponentAccessor 
{
    public static void set(JComponent c, Object value)
    {
        if (value instanceof Date)
        {
            DateFormat format = DateFormat.getDateInstance();
            Date d = (Date) value;
            value = format.format(d);
        }
        if (value instanceof Text)
        {
            Text text = (Text) value;
            value = text.getValue();
        }
        if (c instanceof JTextComponent)
        {
            JTextComponent tc = (JTextComponent) c;
            if (value != null)
            {
                tc.setText(value.toString());
            }
            else
            {
                tc.setText("");
            }
        }
        else
        {
            if (c instanceof AbstractButton)
            {
                AbstractButton ab = (AbstractButton) c;
                Boolean b = (Boolean) value;
                ab.setSelected(b != null && b);
            }
            else
            {
                if (c instanceof JComboBox)
                {
                    JComboBox cb = (JComboBox) c;
                    cb.setSelectedItem(value);
                }
                else
                {
                    throw new UnsupportedOperationException(c.toString());
                }
            }
        }
    }
    public static Object get(JComponent c)
    {
        if (c instanceof JTextComponent)
        {
            JTextComponent tc = (JTextComponent) c;
            return tc.getText();
        }
        else
        {
            if (c instanceof AbstractButton)
            {
                AbstractButton ab = (AbstractButton) c;
                return ab.isSelected();
            }
            else
            {
                if (c instanceof JComboBox)
                {
                    JComboBox cb = (JComboBox) c;
                    return cb.getSelectedItem();
                }
                else
                {
                    throw new UnsupportedOperationException(c.toString());
                }
            }
        }
    }
}
