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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JDialog;
import javax.swing.JTable;

/**
 * @author Timo Vesalainen
 */
public abstract class TableSelector implements KeyListener, MouseListener
{
    private JTable table;
    private JDialog dialog;
    private int row;
    private int col;
    private int minClickCount = 2;

    public TableSelector(JTable table, JDialog dialog)
    {
        this.table = table;
        this.dialog = dialog;
    }

    public void setMinClickCount(int minClickCount)
    {
        this.minClickCount = minClickCount;
    }

    public int getCol()
    {
        return col;
    }

    public int getRow()
    {
        return row;
    }
    
    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == 10)
        {
            col = table.getSelectedColumn();
            row = table.getSelectedRow();
            if (selected(row, col))
            {
                dialog.setVisible(false);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() >= minClickCount)
        {
            col = table.getSelectedColumn();
            row = table.getSelectedRow();
            if (selected(row, col))
            {
                dialog.setVisible(false);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    protected abstract boolean selected(int row, int col);
}
