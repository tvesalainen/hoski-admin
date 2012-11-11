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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * @author Timo Vesalainen
 */
public class TextTable extends JTable
{

    public TextTable(TableModel dm)
    {
        super(dm);
    }

    @Override
    public void paint(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics;
        drawCentered("Test", getHeight()/2, getWidth()/2, g);
        Rectangle bounds = getBounds();
        Rectangle bounds1 = getParent().getBounds();
        for (int x=0;x<bounds1.width;x+=100)
        {
            int h = bounds1.height;
            for (int y=0;y<h;y+=100)
            {
                String s = "("+x+","+y+")";
                g.drawString(s, x+10, y+10);
            }
        }
        super.paint(graphics);
    }
/*
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics;
        drawCentered("Test", getHeight()/2, getWidth()/2, g);
        Rectangle bounds = getBounds();
        Rectangle bounds1 = getParent().getBounds();
        for (int x=0;x<getWidth();x+=100)
        {
            int h = getHeight();
            for (int y=0;y<h;y+=100)
            {
                String s = "("+x+","+y+")";
                g.drawString(s, x+10, y+10);
            }
        }
    }

*/
    public void drawCentered(String str, int x, int y, Graphics2D gg)
    {
        FontMetrics fontMetrics = gg.getFontMetrics();
        Rectangle2D rr = fontMetrics.getStringBounds(str, gg);
        int descent = fontMetrics.getMaxDescent();
        int xx = (int)(x-rr.getCenterX());
        int yy = (int)(y-rr.getCenterY());
        int ext = 0;
        gg.clearRect(xx-ext, yy-(int)rr.getHeight()-ext+descent, (int)rr.getWidth()+2*ext, (int)rr.getHeight()+2*ext);
        gg.drawString(str, xx, yy);
    }
    
}
