/*
 * Copyright (C) 2020 Devashish Jaiswal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package torrentula.tracker;

import torrentula.event.EventData;
import torrentula.event.Event;

class TrackerEventEmitter {
    private final Tracker m_emitter;

    TrackerEventEmitter (Tracker emitter)
    {
        m_emitter = emitter;
    }

    private EventData prepare_bag (EventData... data)
    {
        var tmp = data == null || data.length == 0? EventData.empty() : data[0];
        tmp.put(DataFields.Emitter, m_emitter);
        return tmp;
    }

    void fire_connected (EventData... data)
    {
        var tmp = prepare_bag(data);
        Event.create(EventId.Connected, tmp).fire();
    }

    void fire_connection_failed (EventData... data)
    {
        var tmp = prepare_bag(data);
        Event.create(EventId.ConnectionFailed, tmp).fire();
    }

    void fire_announce_failed (EventData... data)
    {
        var tmp = prepare_bag(data);
        Event.create(EventId.AnnounceFailed, tmp).fire();
    }

    void fire_disconnected (EventData... data)
    {
        var tmp = prepare_bag(data);
        Event.create(EventId.Disconnected, tmp).fire();
    }

    interface DataFields {
        static final String Emitter = "$_emitter";
        static final String Message = "$_message";
        static final String ConnectionId = "$_connection_id";
    }

    private interface EventId {
        static final String Connected = "$_connected";
        static final String Disconnected = "$_disconnected";
        static final String ConnectionFailed = "$_connection_failed";
        static final String AnnounceFailed = "$_announce_failed";
    }
}
