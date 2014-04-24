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
/*
 * To change this template, select Tools | Templates
 * and open the template in the editor.
 */

package fi.hoski.remote.ui;

import fi.hoski.remote.DataStoreProxy;
import fi.hoski.remote.DataStoreService;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import fi.hoski.remote.DataStore;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

/**
 * @author Timo Vesalainen
 */
public class DataObjectChooser<T extends DataObject>
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    private static DataStoreService dss = DataStore.getDss();
    private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    
    private DataObjectModel model;
    private List<T> list;
    private String title;
    private String action;
    private boolean selectAlways;

    public DataObjectChooser(DataObjectModel model, List<T> list, String title, String action)
    {
        this.model = model;
        this.list = list;
        this.title = title;
        this.action = action;
    }

    public List<T> choose()
    {
        if (list.size() > 1 || selectAlways)
        {
            DataObjectListDialog<T> ed = new DataObjectListDialog<T>(
                    null, 
                    title, 
                    action, 
                    model,
                    list
                    );
            ed.setSelectionMode(selectionMode);
            return ed.select();
        }
        else
        {
            if (list.isEmpty())
            {
                JOptionPane.showMessageDialog(null, uiBundle.getString("NOTHING TO CHOOSE"), title, JOptionPane.INFORMATION_MESSAGE);
            }
            return list;
        }
    }

    public boolean isSelectAlways()
    {
        return selectAlways;
    }

    public void setSelectAlways(boolean selectAlways)
    {
        this.selectAlways = selectAlways;
    }

    public void setSelectionMode(int selectionMode)
    {
        this.selectionMode = selectionMode;
    }
    
}
