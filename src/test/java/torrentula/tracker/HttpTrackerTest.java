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

import org.junit.jupiter.api.Test;
import torrentula.Metainfo;
import torrentula.bencode.Bencode;
import torrentula.bencode.Element;
import torrentula.client.Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTrackerTest
{
    private List<PeerAddress> get_peers (String address)
    {
        try
        {
            Element torrent = Bencode.deserialize(Paths.get("kamikaze.torrent"));
            Metainfo metainfo = Metainfo.from(torrent.as_dictionary());
            Client client = new Client(metainfo, 6881);
            Tracker tracker = new HttpTracker(client, address);
            return tracker.request_peers();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test_tracker_response ()
    {
        String[] trackers = {"http://tracker.opentrackr.org:1337/announce","https://tracker.imgoingto.icu:443/announce"};
        for (String tracker : trackers)
        {
            var peers = get_peers(tracker);
            assertTrue(peers.size() > 0);
            for (PeerAddress peer : peers)
            {
                var peer_addr = new InetSocketAddress(peer.host(), peer.port());
                assertEquals("/" + peer.toString(), peer_addr.toString());
            }
        }
    }
}
