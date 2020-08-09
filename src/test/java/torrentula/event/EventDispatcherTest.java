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

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class EventDispatcherTest {
    static class SimpleReactor implements Reactor {
        private volatile boolean message_received = false;

        @Override
        public void react (Event event)
        {
            String message = event.data().take("message");
            message_received = message != null;
            message = message_received ? message : "Failed!";
            System.out.println(message);
        }
    }

    // Naive test. serves the purpose now.
    @Test
    void test_event_hooks () throws InterruptedException
    {
        var event = Event.create("test", Bag.initialize("message", "hello there!"));
        var reactor = new SimpleReactor();
    }
}
