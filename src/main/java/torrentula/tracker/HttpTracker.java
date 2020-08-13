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

import torrentula.client.Client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class HttpTracker extends Tracker {
    private static final String TrackerUrl = "http://tracker.opentrackr.org:1337/announce";
    private final HttpClient m_http;
    private final String m_tracker_address;

    public HttpTracker (String tracker)
    {
        // FIXME: Currently we are ignoring the tracker URL found in torrents.
        m_tracker_address = TrackerUrl;
        m_http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        event_emitter().fire_connected();
    }

    private HttpRequest build_request (Client torrent_client)
    {
        // FIXME: This is temporary.
        var event = "started";
        var state = torrent_client.state();
        int m_accept_compact = 1;
        int m_omit_peer_id = 1;

        var uri = new TrackerURIBuilder(m_tracker_address)
                .append_query("peer_id", torrent_client.id())
                .append_query("info_hash", torrent_client.info_hash())
                .append_query("port", torrent_client.port())
                .append_query("uploaded", state.bytes_uploaded())
                .append_query("downloaded", state.bytes_downloaded())
                .append_query("left", state.bytes_left())
                .append_query("compact", m_accept_compact)
                .append_query("no_peer_id", m_omit_peer_id)
                .append_query("event", event)
                .build();

        return HttpRequest.newBuilder().GET().uri(uri).build();
    }
}
