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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpTracker implements Tracker {
    private static final String DEFAULT_TRACKER_URL = "http://tracker.opentrackr.org:1337/announce";
    private final HttpClient m_http;
    private final String m_base_url;
    private final int m_accept_compact = 1;
    private final int m_omit_peer_id = 1;
    private final Client m_torrent_client;
    private String m_event;

    public HttpTracker (Client torrent_client, String base_url)
    {
        // FIXME: Try the default URL first.
        m_base_url = DEFAULT_TRACKER_URL;
        m_torrent_client = torrent_client;
        m_http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private HttpRequest build_request ()
    {
        var state = m_torrent_client.state();
        var uri = new TrackerURIBuilder(m_base_url)
                .append_query("peer_id", m_torrent_client.id())
                .append_query("info_hash", m_torrent_client.info_hash())
                .append_query("port", m_torrent_client.port() + "")
                .append_query("uploaded", state.bytes_uploaded() + "")
                .append_query("downloaded", state.bytes_downloaded() + "")
                .append_query("left", state.bytes_left() + "")
                .append_query("compact", m_accept_compact + "")
                .append_query("no_peer_id", m_omit_peer_id + "")
                // FIXME: the string needs to change for certain events (e.g.: complete)
                .append_query("event", m_event == null ? (m_event = "started") : "")
                .build();
        System.out.println("The URI: " + uri);
        return HttpRequest.newBuilder().GET().uri(uri).build();
    }

    public TrackerResponse announce ()
    {
        try {
            var request = build_request();
            var response = m_http.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            return TrackerResponse.from(response);
        } catch (IOException | InterruptedException exc) {
            if (exc instanceof InterruptedException) {
                System.err.println("Tracker request interrupted!");
                Thread.currentThread().interrupt();
                return null;
            }
            throw new RuntimeException(exc);
        }
    }

    @Override
    public void dispose ()
    {
        // No action needed.
    }
}
