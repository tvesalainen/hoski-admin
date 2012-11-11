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
package fi.hoski.sailwave;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Timo Vesalainen
 */
public class Fleet
{

    public static final String SCRNAME = "scrname";
    public static final String SCRFIELD = "scrfield";
    public static final String SCRVALUE = "scrvalue";
    public static final String SCRPOINTSYSTEM = "scrpointsystem";
    public static final String SCRRATINGSYSTEM = "scrratingsystem";
    public static final String SCRPARENT = "scrparent";
    private static final String[] FIELDS = new String[]
    {
        SCRNAME,
        SCRFIELD,
        SCRVALUE,
        SCRPOINTSYSTEM,
        SCRRATINGSYSTEM,
        SCRPARENT
    };
    
    private List<String[]> list = new ArrayList<String[]>();
    private String scrratingsystem;
    private int parent;
    private String name;
    private String pointssystem;
    private String field;
    private String value;

    public static boolean accept(String field)
    {
        for (String f : FIELDS)
        {
            if (f.equals(field))
            {
                return true;
            }
        }
        return false;
    }
    public String getField()
    {
        return field;
    }

    public List<String[]> getList()
    {
        return list;
    }

    public String getName()
    {
        return name;
    }

    public int getParent()
    {
        return parent;
    }

    public String getPointssystem()
    {
        return pointssystem;
    }

    public String getScrratingsystem()
    {
        return scrratingsystem;
    }

    public String getValue()
    {
        if (value != null)
        {
            return value;
        }
        else
        {
            return scrratingsystem;
        }
    }
    
    public String getClassname()
    {
        if ("Class".equals(field))
        {
            return value;
        }
        else
        {
            return "";
        }
    }
    public void add(String[] ar)
    {
        list.add(ar);
        if (SCRNAME.equals(ar[0]))
        {
            name = ar[1];
        }
        if (SCRFIELD.equals(ar[0]))
        {
            field = ar[1];
        }
        if (SCRVALUE.equals(ar[0]))
        {
            value = ar[1];
        }
        if (SCRPOINTSYSTEM.equals(ar[0]))
        {
            pointssystem = ar[1];
        }
        if (SCRRATINGSYSTEM.equals(ar[0]))
        {
            scrratingsystem = ar[1];
        }
        if (SCRPARENT.equals(ar[0]))
        {
            parent = Integer.parseInt(ar[1]);
        }
    }

}
