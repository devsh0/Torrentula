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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Tracker {
    final Object m_lock = new Object();
    final ExecutorService m_executor = Executors.newSingleThreadExecutor();
    TrackerState m_state = TrackerState.DISCONNECTED;

    private final TrackerEventEmitter m_emitter = new TrackerEventEmitter(this);

    enum TrackerState {
        DISCONNECTED,
        CONNECTED,
        DISPOSED,
    }

    interface RequestCallback {
        void on_success (final TrackerResponse result);

        void on_failure (final Throwable throwable);
    }

    abstract TrackerResponse announce ();

    TrackerEventEmitter event_emitter ()
    {
        return m_emitter;
    }

    void dispose ()
    {
        m_executor.shutdownNow();
        synchronized (state_lock()) {
            m_state = TrackerState.DISPOSED;
        }
    }

    TrackerState state ()
    {
        synchronized (state_lock()) {
            return m_state;
        }
    }

    boolean connected ()
    {
        synchronized (state_lock()) {
            return state() == TrackerState.CONNECTED;
        }
    }

    Object state_lock ()
    {
        return m_lock;
    }

    boolean disposed ()
    {
        synchronized (m_lock) {
            return state() == TrackerState.DISPOSED;
        }
    }
}
