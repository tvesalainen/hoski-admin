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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * @author Timo Vesalainen
 */
public class FitTable extends JTable
{
    private static final Pattern NUMERIC = Pattern.compile("[0-9\\,\\.\\- ]+");
    public FitTable(TableModel dm)
    {
        super(dm);
    }

    @Override
    public void print(Graphics g)
    {
        int totalColumnWidth = columnModel.getTotalColumnWidth();
        Graphics2D gg = (Graphics2D) g;
        FontRenderContext fontRenderContext = gg.getFontRenderContext();
        for (int col=0;col<columnModel.getColumnCount();col++)
        {
            TableColumn column = columnModel.getColumn(col);
            int max = 0;
            boolean numeric = true;
            for (int row=0;row<getRowCount();row++)
            {
                Object value = dataModel.getValueAt(row, col);
                String str = value.toString();
                Matcher matcher = NUMERIC.matcher(str);
                if (!matcher.matches())
                {
                    numeric = false;
                }
                TableCellRenderer cellRenderer = getCellRenderer(row, col);
                Component component = cellRenderer.getTableCellRendererComponent(this, value, false, false, row, col);
                Font font = component.getFont();
                Rectangle2D stringBounds = font.getStringBounds(str, fontRenderContext);
                max = Math.max(max, (int)(1.5*stringBounds.getWidth()));
            }
            if (numeric)
            {
                column.setMaxWidth(max);
            }
            else
            {
                column.setMinWidth(0);
                column.setMaxWidth(max);
            }
        }
        int left = totalColumnWidth - columnModel.getTotalColumnWidth();
        int hiddenTotal = 0;
        for (int col=0;col<columnModel.getColumnCount();col++)
        {
            TableColumn column = columnModel.getColumn(col);
            hiddenTotal += column.getMaxWidth()-column.getWidth();
        }
        float ratio = (float)left/(float)hiddenTotal;
        for (int col=0;col<columnModel.getColumnCount();col++)
        {
            TableColumn column = columnModel.getColumn(col);
            int hidden = column.getMaxWidth()-column.getWidth();
            if (hidden > 0)
            {
                column.setMinWidth(column.getWidth()+(int)(ratio*(float)hidden));
            }
        }
        totalColumnWidth = columnModel.getTotalColumnWidth();
        revalidate();
        super.paint(g);
    }

}
