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
import java.util.List;

/**
 * @author Timo Vesalainen
 */
public class Race extends Base
{
    public static final String RACEDATE = "racedate";
    public static final String RACESTART = "racestart";
    public static final String RACENAME = "racename";
    public static final String RACERANK = "racerank";
    
    private List<String[]> list = new ArrayList<String[]>();
    private String[] date;
    private String[] time;
    private String[] name;
    private String[] rank;
    
    public void add(String[] ar)
    {
        list.add(ar);
        if (RACEDATE.equals(ar[0]))
        {
            date = ar;
        }
        if (RACESTART.equals(ar[0]) && time == null)    // we are interested in first start
        {
            time = ar;
        }
        if (RACENAME.equals(ar[0]))
        {
            name = ar;
        }
        if (RACERANK.equals(ar[0]))
        {
            rank = ar;
        }
    }

    public String getDate()
    {
        if (date != null)
        {
            return date[1];
        }
        else
        {
            return null;
        }
    }

    public String getName()
    {
        if (name != null)
        {
            return name[1];
        }
        else
        {
            return null;
        }
    }

    public String getTime()
    {
        if (time != null)
        {
            return Base.split(time[1])[1];
        }
        else
        {
            return null;
        }
    }
    
    public int getStartNumber()
    {
        if (rank != null)
        {
            return Integer.parseInt(rank[1]);
        }
        else
        {
            return -1;
        }
    }
    
    public void setDate(String date)
    {
        if (this.date != null)
        {
            this.date[1] = date;
        }
        else
        {
            set(RACEDATE, date, null, "");
        }
    }
    public void setName(String name)
    {
        this.name[1] = name;
    }
    public void setTime(String time)
    {
        String[] ss = Base.split(this.time[1]);
        ss[1] = time;
        this.time[1] = Base.join(ss);
    }
}
