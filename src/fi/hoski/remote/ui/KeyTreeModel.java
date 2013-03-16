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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import fi.hoski.datastore.DSUtils;
import fi.hoski.datastore.Events;
import fi.hoski.datastore.Races;
import fi.hoski.datastore.repository.KeyInfo;
import fi.hoski.remote.DataStoreService;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * @author Timo Vesalainen
 */
public class KeyTreeModel implements TreeModel
{
    private DataStoreService dss;
    private KeyWrapper root;
    private Map<KeyWrapper,List<KeyWrapper>> childs = new HashMap<>();
    private List<TreeModelListener> listeners = new ArrayList<>();
    private Map<KeyWrapper,Entity> entityMap = new HashMap<>();
    private Map<String,String> childMap = new HashMap<>();
    private Component component;

    public KeyTreeModel(DataStoreService dss, Key root, String... kinds)
    {
        this.dss = dss;
        this.root = new KeyWrapper(root, dss, dss, dss);
        for (int ii=1;ii<kinds.length;ii++)
        {
            childMap.put(kinds[ii-1], kinds[ii]);
        }
    }

    @Override
    public Object getRoot()
    {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        List<KeyWrapper> list = get((KeyWrapper)parent);
        return list.get(index);
    }

    @Override
    public int getChildCount(Object parent)
    {
        List<KeyWrapper> list = get((KeyWrapper)parent);
        return list.size();
    }

    @Override
    public boolean isLeaf(Object node)
    {
        if (node instanceof KeyWrapper)
        {
            KeyWrapper kw = (KeyWrapper) node;
            String kind = kw.getKey().getKind();
            if (childMap.containsKey(kind))
            {
                List<KeyWrapper> list = get(kw);
                return list.size() == 0;
            }
        }
        return true;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        List<KeyWrapper> list = get((KeyWrapper)parent);
        return list.indexOf(child);
    }

    private List<KeyWrapper> get(KeyWrapper parent)
    {
        List<KeyWrapper> list = childs.get(parent);
        if (list == null)
        {
            if (component != null)
            {
                component.setCursor(Admin.busyCursor);
                component.repaint();
            }
            Key parentKey = parent.getKey();
            String parentKind = parentKey.getKind();
            String kind = childMap.get(parentKind);
            assert kind != null;
            List<Entity> entities = dss.getChilds(parentKey, kind);
            list = new ArrayList<KeyWrapper>();
            for (Entity entity : entities)
            {
                KeyWrapper kw = new KeyWrapper(entity.getKey(), dss, dss, dss);
                entityMap.put(kw, entity);
                list.add(kw);
            }
            childs.put(parent, list);
            if (component != null)
            {
                component.setCursor(Admin.defaultCursor);
                component.repaint();
            }
        }
        return list;
    }

    public void setComponent(Component component)
    {
        this.component = component;
    }
    
    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        listeners.remove(l);
    }

    public class KeyWrapper
    {
        private Key key;
        private KeyInfo info;
        private DSUtils entities;
        private Events events;
        private Races races;

        public KeyWrapper(Key key, DSUtils entities, Events events, Races races)
        {
            this.key = key;
            this.entities = entities;
            this.events = events;
            this.races = races;
        }

        public Key getKey()
        {
            return key;
        }

        @Override
        public String toString()
        {
            if (info == null)
            {
                try
                {
                    info = new KeyInfo(entities, events, races, "", key, true);
                }
                catch (EntityNotFoundException ex)
                {
                    return ex.getMessage();
                }
            }
            return "<html>"+info.getLabel()+"</html>";
        }
        
    }
}
