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

    public static final String ScrName = "scrname";
    public static final String ScrField = "scrfield";
    public static final String ScrValue = "scrvalue";
    public static final String ScrPointSystem = "scrpointsystem";
    public static final String ScrRatingSystem = "scrratingsystem";
    public static final String ScrParent = "scrparent";
    
    private List<String[]> list = new ArrayList<>();
    private int number = -1;

    private String getField()
    {
        return get(ScrField);
    }

    public List<String[]> getList()
    {
        return list;
    }

    public int getParent()
    {
        String p = get(ScrParent);
        if (p != null)
        {
            return Integer.parseInt(p);
        }
        return -1;
    }

    public String getPointsSystem()
    {
        return get(ScrPointSystem);
    }

    public void setPointSystem(String value)
    {
        set(ScrPointSystem, value);
    }
    public String getRatingSystem()
    {
        return get(ScrRatingSystem);
    }
    public void setRatingSystem(String value)
    {
        set(ScrRatingSystem, value);
    }

    public String getFleet()
    {
        String value = get(ScrValue);
        if (value != null)
        {
            return value;
        }
        else
        {
            return getRatingSystem();
        }
    }
    public void setFleet(String value)
    {
        set(ScrValue, value);
    }
    public String getClassname()
    {
        String field = getField();
        if ("Class".equals(field))
        {
            return get(ScrValue);
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
    }

    private String get(String field)
    {
        for (String[] ar : list)
        {
            if (field.equals(ar[0]))
            {
                return ar[1];
            }
        }
        return null;
    }
    private void set(String field, String value)
    {
        for (String[] ar : list)
        {
            if (field.equals(ar[0]))
            {
                ar[1] = value;
                return;
            }
        }
        list.add(new String[]{field, value, String.valueOf(number)});
    }
    public void write(CSVWriter writer)
    {
        for (String[]  ar : list)
        {
            writer.writeNext(ar);
        }
    }

}
