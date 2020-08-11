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

import torrentula.event.EventData;

import static torrentula.tracker.TrackerEventEmitter.DataFields;

import java.net.*;
import java.nio.ByteBuffer;

class UDPTracker extends Tracker {
    private static final String TrackerUrl = "udp://tracker.opentrackr.org:1337/announce";

    private DatagramSocket m_socket;
    private final SocketAddress m_tracker_address;
    private final int m_socket_timeout = 15000; // Milliseconds.

    UDPTracker (String tracker)
    {
        // FIXME: Currently we are ignoring the tracker URL found in torrents.
        var remote = URI.create(TrackerUrl);
        m_tracker_address = new InetSocketAddress(remote.getHost(), remote.getPort());
        try {
            m_socket = new DatagramSocket(new InetSocketAddress(0));
            m_socket.setSoTimeout(m_socket_timeout);
            connect();
        } catch (SocketException exc) {
            String msg = String.format("%s\n%s", exc.getClass().getName(), exc.getMessage());
            EventData data = EventData.initialize(DataFields.Message, msg);
            event_emitter().fire_connection_failed(data);
        }
    }

    private void send_message (final DatagramPacket packet, final RequestCallback callback)
    {
        m_executor.submit(() -> {
            try {
                m_socket.send(packet);
                final int length = 4096;
                byte[] response_buffer = new byte[length];
                var response_packet = new DatagramPacket(response_buffer, length);
                m_socket.receive(response_packet);
                callback.on_success(TrackerResponse.from(response_packet));
            } catch (Throwable th) {
                callback.on_failure(th);
            }
        });
    }

    private void connect ()
    {
        final long conn_id = 0x41727101980L;
        final int action = 0;
        final int tran_id = 1;
        final var buffer = ByteBuffer.allocate(16).putLong(conn_id).putInt(action).putInt(tran_id);
        final var packet = new DatagramPacket(buffer.array(), 0, 16, m_tracker_address);

        send_message(packet, new RequestCallback() {
            @Override
            public void on_success (TrackerResponse result)
            {
                try {
                    final var data = result.data();
                    if (result.length() < 16) {
                        String message = "Response packet < 16 bytes!";
                        EventData event_data = EventData.initialize(DataFields.Message, message);
                        event_emitter().fire_connection_failed(event_data);
                        return;
                    }

                    if (data.getInt() != action) {
                        String message = "Request-Response action mismatch!";
                        EventData event_data = EventData.initialize(DataFields.Message, message);
                        event_emitter().fire_connection_failed(event_data);
                        return;
                    }

                    if (data.getInt() != tran_id) {
                        String message = "Request-Response transaction id mismatch!";
                        EventData event_data = EventData.initialize(DataFields.Message, message);
                        event_emitter().fire_connection_failed(event_data);
                        return;
                    }

                    synchronized (state_lock()) {
                        var connection_id = data.getLong();
                        m_state = TrackerState.CONNECTED;
                        var event_data = EventData.initialize(DataFields.ConnectionId, connection_id);
                        event_emitter().fire_connected(event_data);
                    }
                } catch (Throwable exc) {
                    String msg = String.format("%s\n%s", exc.getClass().getName(), exc.getMessage());
                    EventData data = EventData.initialize(DataFields.Message, msg);
                    event_emitter().fire_connection_failed(data);
                }
            }

            @Override
            public void on_failure (Throwable exc)
            {
                String msg = String.format("%s\n%s", exc.getClass().getName(), exc.getMessage());
                EventData data = EventData.initialize(DataFields.Message, msg);
                event_emitter().fire_connection_failed(data);
            }
        });
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        m_socket.close();
    }

    @Override
    public TrackerResponse announce ()
    {
        return null;
    }
}
