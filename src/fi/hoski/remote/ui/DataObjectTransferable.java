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

import fi.hoski.datastore.repository.DataObject;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * @author Timo Vesalainen
 */
public class DataObjectTransferable<T extends DataObject> implements Transferable
{
    private T dataObject;
    private DataFlavor flavor;

    public DataObjectTransferable(T dataObject)
    {
        if (dataObject == null)
        {
            throw new NullPointerException();
        }
        this.dataObject = dataObject;
        this.flavor = new DataFlavor(dataObject.getClass(), null);
    }

    public DataFlavor getFlavor()
    {
        return flavor;
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return new DataFlavor[]{flavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return this.flavor.match(flavor);
    }

    @Override
    public T getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        if (isDataFlavorSupported(flavor))
        {
            return dataObject;
        }
        else
        {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
