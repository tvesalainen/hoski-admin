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
 * LastInput.java
 *
 * Created on 16. helmikuuta 2007, 7:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fi.hoski.remote.ui;

import java.io.File;
import java.util.prefs.Preferences;

/**
 *
 * @author tkv
 */
public class LastInput
{
    private static final Preferences preferences = Preferences.userNodeForPackage(LastInput.class);
    
    public static File getFile(String key)
    {
        String filename = get(key);
        if (filename != null)
        {
            File file = new File(filename);
            if (file.exists())
            {
                return file;
            }
        }
        return null;
    }
    
    public static File getDirectory(String key)
    {
        String filename = get(key);
        if (filename != null)
        {
            File file = new File(filename);
            if (file.exists())
            {
                return file.getParentFile();
            }
        }
        return null;
    }
    
    public static boolean is(String key)
    {
        return is(key, false);
    }
    
    public static boolean is(String key, boolean def)
    {
        String str = get(key);
        if (str != null)
        {
            return Boolean.parseBoolean(str);
        }
        return def;
    }
    
    public static void set(String key, File file)
    {
        set(key, file.getPath());
    }
    
    public static void set(String key, boolean bb)
    {
        set(key, Boolean.toString(bb));
    }
    
    public static String get(String key)
    {
        return preferences.get(key, "");
    }
    
    public static void set(String key, String value)
    {
        preferences.put(key, value);
    }

    public static void remove(String key)
    {
        preferences.remove(key);
    }
}
