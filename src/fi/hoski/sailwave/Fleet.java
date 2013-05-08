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

import au.com.bytecode.opencsv.CSVWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    private List<String[]> list = new ArrayList<>();
    private String scrratingsystem;
    private int parent;
    private String name;
    private String pointssystem;
    private String field;
    private String value;
    private int number = -1;

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
    public static int getNumber(String[] ar)
    {
        if ("scrcode".equals(ar[0]))
        {
            String[] ss = SailWaveFile.split(ar[1]);
            return Integer.parseInt(ss[14]);
        }
        else
        {
            return Integer.parseInt(ar[2]);
        }
    }

    public int getNumber()
    {
        return number;
    }
    
    public Fleet copy(int newNumber)
    {
        String n = String.valueOf(newNumber);
        Fleet nf = new Fleet();
        for (String[] ar : list)
        {
            String[] car = Arrays.copyOf(ar, ar.length);
            if ("scrcode".equals(ar[0]))
            {
                String[] ss = SailWaveFile.split(ar[1]);
                ss[14] = n;
                car[1] = SailWaveFile.join(ss);
            }
            else
            {
                car[2] = n;
            }
            nf.add(car);
        }
        return nf;
    }
    public void add(String[] ar)
    {
        int n = getNumber(ar);
        assert number == -1 || number == n;
        number = n;
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

    public void write(CSVWriter writer)
    {
        for (String[]  ar : list)
        {
            writer.writeNext(ar);
        }
    }

    void setName(String clazz)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
