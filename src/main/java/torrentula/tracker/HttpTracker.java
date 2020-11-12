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

import torrentula.bencode.Bencode;
import torrentula.bencode.Element;
import torrentula.client.Client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class HttpTracker extends Tracker
{
    private static final String TRACKER_URL = "https://tracker.imgoingto.icu:443/announce";
    private final HttpClient m_http;
    private final String m_tracker_address;
    private final Client m_client;

    public HttpTracker (Client client, String tracker)
    {
        // FIXME: Currently we are ignoring the tracker URL found in torrents.
        m_tracker_address = tracker;
        m_http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        m_client = client;
    }

    private HttpRequest build_request ()
    {
        // FIXME: This is temporary.
        var event = "started";
        var state = m_client.state();
        int m_accept_compact = 1;
        int m_omit_peer_id = 1;

        var uri = new TrackerURIBuilder(m_tracker_address)
                .append_query("peer_id", m_client.id())
                .append_query("info_hash", m_client.info_hash())
                .append_query("port", m_client.port())
                .append_query("uploaded", state.bytes_uploaded())
                .append_query("downloaded", state.bytes_downloaded())
                .append_query("left", state.bytes_left())
                .append_query("compact", m_accept_compact)
                .append_query("no_peer_id", m_omit_peer_id)
                .append_query("event", event)
                .build();
        return HttpRequest.newBuilder().GET().uri(uri).build();
    }


    @Override
    public List<PeerAddress> request_peers () throws  InterruptedException, IOException
    {
        HttpRequest request = build_request();
        byte[] response = m_http.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        Map<String, Element> peer_info = Bencode.deserialize(response).as_dictionary();
        Element peers = peer_info.get("peers");
        if (peers.type() == Element.Type.BYTE_STRING)
            return PeerAddress.from(peers.as_byte_string());
        if (peers.type() == Element.Type.LIST)
            return PeerAddress.from(peers.as_list());
        throw new RuntimeException("The 'peers' key contains data in invalid form!");
    }
}
