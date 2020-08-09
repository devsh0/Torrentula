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
    private static final Object s_reactor_traversal_lock = new Object();
    private static final ExecutorService s_dispatcher_service = Executors.newSingleThreadExecutor();
    private static Map<Event, List<Reactor>> s_reactors = new HashMap<>();

    static void dispatch (Event event)
    {
        // Exceptions will be caught by the executor. Worker thread won't die.
        s_dispatcher_service.submit(() -> {
            var reactors = get_reactors(event);
            if (reactors == null) return;
            for (var reactor : reactors)
                reactor.react(event);
        });
    }

    private static List<Reactor> get_reactors (Event event)
    {
        synchronized (s_reactor_traversal_lock) {
            return s_reactors.get(event);
        }
    }

    private static void clear_reactors ()
    {
        synchronized (s_reactor_traversal_lock) {
            s_reactors.clear();
            s_reactors = null;
        }
    }

    public static void shutdown_immediately ()
    {
        clear_reactors();
        s_dispatcher_service.shutdownNow();
    }

    public static void shutdown ()
    {
        clear_reactors();
        s_dispatcher_service.shutdown();
    }

    static void add_reactor (Event event, Reactor reactor)
    {
        synchronized (s_reactor_traversal_lock) {
            var existing_reactors = get_reactors(event);
            if (existing_reactors == null) {
                var list = new ArrayList<Reactor>();
                list.add(reactor);
                s_reactors.put(event, list);
                return;
            }
            existing_reactors.add(reactor);
        }
    }

    static void remove_reactor (Event event, Reactor reactor)
    {
        synchronized (s_reactor_traversal_lock) {
            var reactors = get_reactors(event);
            reactors.removeIf(r -> r == reactor);
        }
    }

    static void remove_all_reactors (Event event)
    {
        synchronized (s_reactor_traversal_lock) {
            s_reactors.remove(event);
        }
    }

    static void remove_reactors_of_type (Event event, Class<? extends Reactor> klass)
    {
        synchronized (s_reactor_traversal_lock) {
            var reactors = get_reactors(event);
            reactors.removeIf(r -> r.getClass().equals(klass));
        }
    }
}
