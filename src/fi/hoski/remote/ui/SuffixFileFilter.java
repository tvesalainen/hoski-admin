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
 * SuffixFileFilter.java
 *
 * Created on 14. tammikuuta 2007, 11:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fi.hoski.remote.ui;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author tkv
 */
public class SuffixFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter
{
    private String suffix;
    private String description;
    /** Creates a new instance of SuffixFileFilter */
    public SuffixFileFilter(String suffix)
    {
        this.suffix = suffix.toLowerCase();
        this.description = suffix.toLowerCase();
    }

    /** Creates a new instance of SuffixFileFilter */
    public SuffixFileFilter(String suffix, String description)
    {
        this.suffix = suffix;
        this.description = description;
    }

    public boolean accept(File f)
    {
        if (f.isFile())
        {
            return f.getName().endsWith(suffix);
        }
        return true;
    }

    public String getDescription()
    {
        return description;
    }
    
}
