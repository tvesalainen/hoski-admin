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

package fi.hoski.remote;

import java.awt.Component;
import javax.swing.ProgressMonitor;

/**
 * @author Timo Vesalainen
 */
public class UIProgress implements Progress
{
    private ProgressMonitor monitor;

    public UIProgress(Component parent, String title)
    {
        monitor = new ProgressMonitor(parent, title, "", 0, 100);
    }
    
    @Override
    public void setBounds(int min, int max)
    {
        monitor.setMinimum(min);
        monitor.setMaximum(max);
    }

    @Override
    public void setNote(String note)
    {
        monitor.setNote(note);
    }

    @Override
    public void setProgress(int ny)
    {
        monitor.setProgress(ny);
    }

    @Override
    public void close()
    {
        System.err.println("ready");
        monitor.close();
    }

}
