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

import com.google.appengine.api.datastore.Text;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import fi.hoski.remote.DataStoreService;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * TableModel for list of entities. Each entity property must have same type
 * of value.
 * @author Timo Vesalainen
 */
public class DataObjectListTableModel<T extends DataObject> extends AbstractTableModel
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    private List<T> list;
    private DataObjectModel model;
    private String[] editable;

    public DataObjectListTableModel(DataObjectModel model, List<T> list)
    {
        this.model = model;
        this.list = list;
    }
    
    public void setEditable(String... properties)
    {
        editable = properties;
    }
    public T getObject(int rowIndex)
    {
        return list.get(rowIndex);
    }
    
    public int remove(T dataObject)
    {
        int index = list.indexOf(dataObject);
        if (index != -1)
        {
            list.remove(index);
            fireTableRowsDeleted(index, index);
        }
        return index;
    }
    public void add(T dataObject)
    {
        boolean add = list.add(dataObject);
        assert add;
        int row = list.size()-1;
        fireTableRowsInserted(row, row);
    }
    public void add(int index, T dataObject)
    {
        list.add(index, dataObject);
        fireTableRowsInserted(index, index);
    }
    @Override
    public int getRowCount()
    {
        return list.size();
    }

    @Override
    public int getColumnCount()
    {
        return model.getProperties().length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return model.getLabel(model.getProperty(columnIndex));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return model.getType(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        String property = model.getProperty(columnIndex);
        if (editable != null)
        {
            for (String p : editable)
            {
                if (property.equals(p))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        T c = list.get(rowIndex);
        String property = model.getProperty(columnIndex);
        Object value = c.get(property);
        if (value == null)
        {
            value = c.getTypeDefault(property);
        }
        if (value instanceof Text)
        {
            Text text = (Text) value;
            return text.getValue();
        }
        return value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        T c = list.get(rowIndex);
        c.set(model.getProperty(columnIndex), aValue);
    }
    public KeyListener getRemover(DataStoreService dss)
    {
        return new Remover(dss);
    }
    public class Remover extends KeyAdapter
    {
        private DataStoreService dss;

        public Remover(DataStoreService dss)
        {
            this.dss = dss;
        }
        
        @Override
        public void keyPressed(KeyEvent e)
        {
            if (KeyEvent.VK_DELETE == e.getKeyCode())
            {
                JTable table = (JTable) e.getComponent();
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1)
                {
                    RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
                    if (rowSorter != null)
                    {
                        selectedRow = rowSorter.convertRowIndexToModel(selectedRow);
                    }
                    T dataObject = getObject(selectedRow);
                    String msg = uiBundle.getString("CONFIRM DELETE")+": "+dataObject.toString();
                    if (JOptionPane.showConfirmDialog(table, msg, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        remove(dataObject);
                        dss.delete(dataObject);
                    }
                }
            }
        }
        
    }
}
