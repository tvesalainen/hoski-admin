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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Timo Vesalainen
 */
public class Base
{
    protected Map<String,String[]> map = new HashMap<String,String[]>();

    protected void add(String[] ar)
    {
        map.put(ar[0], ar);
    }
    protected String[] get(String key)
    {
        return map.get(key);
    }
    protected void set(String key, String arg1, String arg2, String arg3)
    {
        String[] ar = new String[]{key, arg1, arg2, arg3}; 
        map.put(key, ar);
    }
    public void write(CSVWriter writer)
    {
        for (String[]  ar : map.values())
        {
            writer.writeNext(ar);
        }
    }
    
    public static String[] split(String str)
    {
        int begin = 0;
        int end = str.indexOf('|');
        if (end == -1)
        {
            return new String[] {str};
        }
        List<String> list = new ArrayList<String>();
        while (end != -1)
        {
            list.add(str.substring(begin, end));
            begin = end+1;
            end = str.indexOf('|', begin);
        }
        list.add(str.substring(begin));
        return list.toArray(new String[list.size()]);
    }
    public static String join(String... cols)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : cols)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append('|');
            }
            sb.append(s);
        }
        return sb.toString();
    }
    public static void main(String[] args) 
    {
        try
        {
            //String s1 = "|11:00:00|Place|Start 1|||0||0|0||||1";
            String s1 = "1|CarriedFwd|386|No|No|20||";
            String[] ss = Base.split(s1);
            String s2 = Base.join(ss);
            if (!s1.equals(s2))
            {
                System.err.println(s1+" != "+s2);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
