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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventData {
    private final Object m_data_traversal_lock = new Object();
    private final Map<String, Object> m_container = new HashMap<>();

    private EventData ()
    {
    }

    public <T> EventData put (String key, T value)
    {
        synchronized (m_data_traversal_lock) {
            m_container.put(key, value);
            return this;
        }
    }

    public <T> T take (String key)
    {
        synchronized (m_data_traversal_lock) {
            return (T) m_container.remove(key);
        }
    }

    public boolean is_empty ()
    {
        synchronized (m_data_traversal_lock) {
            return m_container.isEmpty();
        }
    }

    public boolean has (String key)
    {
        synchronized (m_data_traversal_lock) {
            return m_container.containsKey(key);
        }
    }

    public void clear ()
    {
        synchronized (m_data_traversal_lock) {
            m_container.clear();
        }
    }

    public static EventData empty ()
    {
        return new EventData();
    }

    public static <T> EventData initialize (String key, T value)
    {
        return empty().put(key, value);
    }
}
