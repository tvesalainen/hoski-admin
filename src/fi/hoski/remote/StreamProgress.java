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

package fi.hoski.remote;

/**
 * @author Timo Vesalainen
 */
public class StreamProgress implements Progress
{
    private int min;
    private int max;

    @Override
    public void setBounds(int min, int max)
    {
        this.min = min;
        this.max = max;
    }

    @Override
    public void setNote(String note)
    {
        System.err.println(note);
    }

    @Override
    public void setProgress(int ny)
    {
        int p = 100*ny/(max-min);
        System.err.println(p+"% ready...");
    }

    @Override
    public void close()
    {
        System.err.println("ready");
    }

}
