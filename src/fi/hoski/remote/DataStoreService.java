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

import com.google.appengine.api.datastore.EntityNotFoundException;
import fi.hoski.datastore.*;
import fi.hoski.mail.MailService;
import fi.hoski.sms.SMSService;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * @author Timo Vesalainen
 */
public interface DataStoreService extends DSUtils, Events, Users, MailService, SMSService, PatrolShifts, Races
{
    void synchronize(Progress progress) throws IOException;

    void inspectAllLightBoats(int maxMemberNo) throws SQLException, ClassNotFoundException;
    void inspectionFix1() throws SQLException, ClassNotFoundException;
    void swapLog(Appendable a) throws IOException, EntityNotFoundException;
}
