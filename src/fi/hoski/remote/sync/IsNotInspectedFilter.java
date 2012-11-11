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

package fi.hoski.remote.sync;

import fi.hoski.datastore.repository.ResultSetData;
import fi.hoski.datastore.repository.ResultSetFilter;
import fi.hoski.util.Day;
import java.sql.SQLException;

/**
 * @author Timo Vesalainen
 */
public class IsNotInspectedFilter implements ResultSetFilter 
{
    private Day now = new Day();
    private InspectionHandler ih;

    public IsNotInspectedFilter(InspectionHandler ih)
    {
        this.ih = ih;
    }
    
    @Override
    public boolean accept(ResultSetData data) throws SQLException
    {
        Integer veneID = (Integer) data.get("VeneID");
        return !ih.isInspected(veneID, 2, now.getYear());
    }

}
