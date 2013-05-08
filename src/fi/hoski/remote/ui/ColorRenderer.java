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

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 * @author Timo Vesalainen
 */
public class ColorRenderer extends JLabel
        implements TableCellRenderer
{
    private Border focusBorder = BorderFactory.createLineBorder(Color.BLACK);
    public ColorRenderer()
    {
        setOpaque(true); //MUST do this for background to show up.
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object object,
            boolean isSelected, boolean hasFocus,
            int row, int column)
    {
        setText(object.toString());
        setBackground(Color.PINK);
        if (hasFocus)
        {
            setBorder(focusBorder);
        }
        else
        {
            setBorder(null);
        }

        return this;
    }
}
