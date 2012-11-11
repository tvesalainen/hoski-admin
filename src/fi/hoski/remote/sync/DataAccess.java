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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

/**
 *
 * @author Timo Vesalainen
 */
public interface DataAccess
{
    Map<Key,Entity> getAllEntities(SyncTarget target) throws IOException;
    Key insert(SyncTarget target, Entity entity) throws IOException;
    void update(SyncTarget target, Entity entity) throws IOException;
    void delete(SyncTarget target, Collection<Key> keys) throws IOException;
    void move(SyncTarget target, Key from, Key to) throws IOException;
}
