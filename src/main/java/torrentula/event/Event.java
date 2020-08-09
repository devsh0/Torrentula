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

import java.util.Objects;

public class Event {
    private final String m_id;
    private final Bag m_bag;

    private Event (String id, Bag data)
    {
        m_id = id;
        m_bag = data;
    }

    public String id ()
    {
        return m_id;
    }

    public Bag data ()
    {
        return m_bag;
    }

    @Override
    public boolean equals (Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Event event = (Event) o;
        return m_id.equals(event.m_id);
    }

    @Override
    public int hashCode ()
    {
        return m_id.hashCode();
    }

    public static Event create (String id, Bag bag) {
        Objects.requireNonNull(id, "Key may not be empty!");
        bag = bag == null ? Bag.empty() : bag;
        return new Event(id, bag);
    }

    public void add_reactor (Reactor reactor)
    {
        EventDispatcher.add_reactor(this, reactor);
    }

    public void remove_reactor (Reactor reactor)
    {
        EventDispatcher.remove_reactor(this, reactor);
    }

    public void remove_all_reactors ()
    {
        EventDispatcher.remove_all_reactors(this);
    }

    public void remove_reactors_of_type (Class<? extends Reactor> klass)
    {
        EventDispatcher.remove_reactors_of_type(this, klass);
    }

    public void fire ()
    {
        EventDispatcher.dispatch(this);
    }
}
