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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JViewport;

/**
 * @author Timo Vesalainen
 */
public class InfoViewport extends JViewport 
{
    private String[] words;

    public InfoViewport(String text)
    {
        this.words = text.split(" ");
    }
    
    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        Rectangle bounds = getBounds();
        int width = 6*bounds.width/10;
        List<String> rows = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int wIndex = 0;
        FontMetrics fontMetrics = g.getFontMetrics();
        Rectangle2D rr = fontMetrics.getStringBounds(sb.toString(), g);
        while (true)
        {
            if (rr.getWidth() >= width)
            {
                rows.add(sb.toString());
                sb.delete(0, sb.length());
            }
            if (wIndex >= words.length)
            {
                rows.add(sb.toString());
                break;
            }
            if (sb.length() > 0)
            {
                sb.append(' ');
            }
            sb.append(words[wIndex++]);
            rr = fontMetrics.getStringBounds(sb.toString(), g);
        }
        int charHeight = (int) rr.getHeight();
        int yy = (bounds.height - rows.size()*charHeight)/2;
        int xx = bounds.width/10;
        int baseLine = yy + charHeight;
        for (String r : rows)
        {
            g.drawString(r, xx, baseLine);
            baseLine += charHeight;
        }
    }

}
