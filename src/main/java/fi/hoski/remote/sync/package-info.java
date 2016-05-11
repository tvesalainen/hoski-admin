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

/**
 * This package contains classes for database synchronization. Synchronized
 * objects are back-office database. Currently ms-access and google datastore.
 * 
 * Access to ms-access is through odbc-jdbc bridge which still exist in java 7
 * but is removed from java 8. Such bridges can be lisenced from several vendors
 * if needed.
 * 
 * Access to google datastore is through remote-api.
 * 
 * Synchronized database tables are listed in properties file. Properties file
 * contains all needed details like keys, foreign keys, excluded columns ,...
 * 
 * Properties file contains also which part is master in synchronizing. In most 
 * cases master is back-office. Vartiovuoro table master is remote datastore.
 * 
 * Classes:
 * <p>
 * Synchronizer2 is the main engine for process.
 * 
 * SyncTarget handless properties file parsing.
 * 
 * DataAccess is interface for synchronizing parties. It has two implementations:
 * DatabaseAccess and DatastoreAccess.
 */