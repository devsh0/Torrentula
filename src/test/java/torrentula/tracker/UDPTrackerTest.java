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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import torrentula.Metainfo;
import torrentula.bencode.Bencode;
import torrentula.client.Client;

import java.nio.file.Paths;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UDPTrackerTest {
    @Test
    public void test_tracker ()
    {
        var torrent_info = Bencode.deserialize(Paths.get("tos.torrent")).as_dictionary();
        var metainfo = Metainfo.from(torrent_info);
        var client = new Client(metainfo, 1000);
        var tracker = new UDPTracker(client, metainfo.tracker_url());
        final var worker = Executors.newScheduledThreadPool(1);
        try {
            var result = worker.scheduleWithFixedDelay(() -> {
                if (tracker.connected())
                    worker.shutdown();
            }, 0, 1, TimeUnit.SECONDS);
            final int timeout = 5;
            result.get(timeout, TimeUnit.SECONDS);
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException exc) {
            assertTrue(tracker.connected());
        } finally {
            worker.shutdownNow();
            tracker.dispose();
        }
    }
}
