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

import fi.hoski.remote.DataStore;
import fi.hoski.remote.DataStoreService;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

/**
 * @author Timo Vesalainen
 */
public abstract class BackupBase
{
    protected DataStoreService dss = DataStore.getDss();
    protected File lastDirectory;
    protected JFrame frame;
    protected FileFilter fileFilter;

    public BackupBase(JFrame frame, FileFilter fileFilter)
    {
        this.frame = frame;
        this.fileFilter = fileFilter;
    }
    
    public void load() throws IOException
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(fileFilter);
        if (lastDirectory != null)
        {
            fc.setCurrentDirectory(lastDirectory);
        }
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
            File file = fc.getSelectedFile();
            lastDirectory = fc.getCurrentDirectory();
            load(file);
        }
    }
    protected abstract void load(File file) throws IOException;

    public void store() throws IOException
    {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(fileFilter);
        if (lastDirectory != null)
        {
            fc.setCurrentDirectory(lastDirectory);
        }
        fc.showSaveDialog(frame);
        File selectedFile = fc.getSelectedFile();
        store(selectedFile);
    }

    protected abstract void store(File selectedFile) throws IOException;

}
