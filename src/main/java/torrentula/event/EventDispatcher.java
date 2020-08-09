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

package torrentula.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class EventDispatcher {
    private static final Object s_listener_traversal_lock = new Object();
    private static final ExecutorService s_dispatcher_service = Executors.newSingleThreadExecutor();
    private static Map<Event, List<EventListener>> s_listeners = new HashMap<>();

    static void dispatch (Event event)
    {
        // Exceptions will be caught by the executor. Worker thread won't die.
        s_dispatcher_service.submit(() -> {
            var listeners = get_listeners(event);
            if (listeners == null) return;
            for (var listener : listeners)
                listener.react(event);
        });
    }

    private static List<EventListener> get_listeners (Event event)
    {
        synchronized (s_listener_traversal_lock) {
            return s_listeners.get(event);
        }
    }

    private static void clear_listeners ()
    {
        synchronized (s_listener_traversal_lock) {
            s_listeners.clear();
            s_listeners = null;
        }
    }

    public static void shutdown_immediately ()
    {
        clear_listeners();
        s_dispatcher_service.shutdownNow();
    }

    public static void shutdown ()
    {
        clear_listeners();
        s_dispatcher_service.shutdown();
    }

    static void add_listener (Event event, EventListener listener)
    {
        synchronized (s_listener_traversal_lock) {
            var existing_listeners = get_listeners(event);
            if (existing_listeners == null) {
                var list = new ArrayList<EventListener>();
                list.add(listener);
                s_listeners.put(event, list);
                return;
            }
            existing_listeners.add(listener);
        }
    }

    static void remove_listener (Event event, EventListener listener)
    {
        synchronized (s_listener_traversal_lock) {
            var listeners = get_listeners(event);
            listeners.removeIf(r -> r == listener);
        }
    }

    static void remove_listeners_listening_to (Event event)
    {
        synchronized (s_listener_traversal_lock) {
            s_listeners.remove(event);
        }
    }

    static void remove_listeners_of_type (Event event, Class<? extends EventListener> klass)
    {
        synchronized (s_listener_traversal_lock) {
            var listeners = get_listeners(event);
            listeners.removeIf(r -> r.getClass().equals(klass));
        }
    }
}
