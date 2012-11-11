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

/**
 * @author Timo Vesalainen
 */
public class TextUtil 
{
    private static final ResourceBundle r1 = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    private static final ResourceBundle r2 = ResourceBundle.getBundle("fi/hoski/datastore/repository/fields");
    private static final ResourceBundle[] bundles = new ResourceBundle[] { r1, r2 };
    
    public static String getString(String key)
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
