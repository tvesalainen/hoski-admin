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
package fi.hoski.orc.pls;

import fi.hoski.google.docs.RandomAccessWorksheet;


/**
 *
 * @author tkv
 */
public class RankingPoints
{
    private RandomAccessWorksheet sheet;

    public RankingPoints(RandomAccessWorksheet sheet)
    {
        this.sheet = sheet;
    }

    public double points(int position, int count)
    {
        return sheet.getDouble(position-1, count-1);
    }
}
