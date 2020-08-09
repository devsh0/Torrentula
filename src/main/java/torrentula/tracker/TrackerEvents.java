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

import torrentula.event.Bag;
import torrentula.event.Event;

class TrackerEvents extends Event {
    private TrackerEvents (String event_id, Tracker emitter, Bag data)
    {
        super(event_id, data);
        data.put(Fields.Emitter, emitter);
    }

    static void fire_connected (Tracker emitter, Bag bag)
    {
        new TrackerEvents(EventId.Connected, emitter, bag).fire();
    }

    static void fire_connection_failed (Tracker emitter, Bag bag)
    {
        new TrackerEvents(EventId.ConnectionFailed, emitter, bag);
    }

    static void fire_announce_failed (Tracker emitter, Bag bag)
    {
        new TrackerEvents(EventId.AnnounceFailed, emitter, bag);
    }

    static void fire_disconnected (Tracker emitter, String... msg)
    {
        String message = (msg == null || msg.length == 0) ? "" : msg[0];
        new TrackerEvents(EventId.Disconnected, emitter, Bag.initialize(Fields.Message, message));
    }

    interface Fields {
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
