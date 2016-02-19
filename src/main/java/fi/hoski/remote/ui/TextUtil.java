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

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * @author Timo Vesalainen
 */
public class TextUtil 
{
    private static final ResourceBundle r1 = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    private static final ResourceBundle r2 = ResourceBundle.getBundle("fi/hoski/datastore/repository/fields");
    private static final ResourceBundle[] bundles = new ResourceBundle[] { r1, r2 };
    
    public static void populate(JMenu menu, String key)
    {
        menu.setText(getText(key));
        menu.setToolTipText(getTooltipText(key));
    }
    public static void populate(JMenuItem menuItem, String key)
    {
        menuItem.setText(getText(key));
        menuItem.setToolTipText(getTooltipText(key));
    }
    public static void populate(JButton button, String key)
    {
        button.setText(getText(key));
        button.setToolTipText(getTooltipText(key));
    }
    public static String getTooltipText(String key)
    {
        String ttkey = key+" TT";
        for (ResourceBundle rb : bundles)
        {
            try
            {
                return rb.getString(ttkey);
            }
            catch (MissingResourceException ex)
            {
                
            }
        }
        return null;
    }
    public static String getText(String key)
    {
        for (ResourceBundle rb : bundles)
        {
            try
            {
                return rb.getString(key);
            }
            catch (MissingResourceException ex)
            {
                
            }
        }
        return key;
    }
}
