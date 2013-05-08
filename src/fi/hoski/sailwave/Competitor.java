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

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Timo Vesalainen
 */
public class Competitor extends Base
{

    public Competitor()
    {
        set("comphigh", "0", null, "");
        set("compexclude","0",null,"");
        set("rdisc","0",null,"4");
        set("rrestyp","0",null,"4");
        set("srat","0",null,"4");
        set("rrset","0",null,"4");
    }
    
    public void setAll(Map<String,Object> map)
    {
        for (Entry<String,Object> entry : map.entrySet())
        {
            String property = entry.getKey();
            try
            {
                CompetitorField cf = CompetitorField.valueOf(property.toUpperCase());
                set("comp"+cf.name().toLowerCase(), entry.getValue().toString(), null, "");
            }
            catch (IllegalArgumentException ex)
            {
            }
        }
    }
    
    public String get(CompetitorField field)
    {
        String[] ar = get("comp"+field.name().toLowerCase());
        if (ar != null)
        {
            return ar[1];
        }
        else
        {
            return null;
        }
    }
    void setNumber(int number)
    {
        String n = Integer.toString(number);
        for (String[] ar : map.values())
        {
            ar[2] = n;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Competitor)
        {
            Competitor oth = (Competitor) obj;
            String nat1 = get(CompetitorField.NAT);
            String sailNo1 = get(CompetitorField.SAILNO);
            String cls1 = get(CompetitorField.CLASS);
            String nat2 = oth.get(CompetitorField.NAT);
            String sailNo2 = oth.get(CompetitorField.SAILNO);
            String cls2 = oth.get(CompetitorField.CLASS);
            return equals(nat1, nat2) && equals(sailNo1, sailNo2);// && equals(cls1, cls2);
        }
        return false;
    }
    private boolean equals(String s1, String s2)
    {
        if (s1 == null && s2 == null)
        {
            return true;
        }
        if (s1 != null)
        {
            return s1.equalsIgnoreCase(s2);
        }
        return false;
    }
}
