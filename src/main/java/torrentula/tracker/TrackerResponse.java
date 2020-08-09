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

import java.net.DatagramPacket;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;

class TrackerResponse {
    private final boolean m_success;
    private final ByteBuffer m_buffer;
    private final long m_length;

    private TrackerResponse (HttpResponse<byte[]> response)
    {
        var body = response.body();
        m_success = response.statusCode() == 200;
        m_length = body.length;
        m_buffer = ByteBuffer.wrap(body);
    }

    private TrackerResponse (DatagramPacket packet)
    {
        var body = packet.getData();
        m_success = body != null && body.length > 0;
        m_length = packet.getLength();
        m_buffer = ByteBuffer.wrap(body == null ? "".getBytes() : body);
    }

    boolean success ()
    {
        return m_success;
    }

    long length ()
    {
        return m_length;
    }

    ByteBuffer data ()
    {
        return m_buffer.asReadOnlyBuffer();
    }

    static TrackerResponse from (DatagramPacket packet)
    {
        return new TrackerResponse(packet);
    }

    static TrackerResponse from (HttpResponse<byte[]> response)
    {
        return new TrackerResponse(response);
    }
}
