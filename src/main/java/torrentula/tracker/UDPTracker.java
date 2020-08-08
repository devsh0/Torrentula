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
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class UDPTracker implements Tracker {
    private static final String DEFAULT_TRACKER_URL = "udp://tracker.opentrackr.org:1337/announce";

    private final Object m_lock = new Object();

    private final DatagramSocket m_socket;
    private final SocketAddress m_tracker_address;
    private final ExecutorService m_worker = Executors.newSingleThreadExecutor();

    private TrackerState m_state = TrackerState.DISCONNECTED;
    private long m_connection_id;

    private final Client m_torrent_client;

    private interface Callback {
        void on_success (final DatagramPacket result);

        void on_failure (final Throwable throwable);
    }

    UDPTracker (Client client, String tracker)
    {
        try {
            var remote = URI.create(DEFAULT_TRACKER_URL);
            m_tracker_address = new InetSocketAddress(remote.getHost(), remote.getPort());
            m_socket = new DatagramSocket(new InetSocketAddress(0));
            m_torrent_client = client;
            connect();
        } catch (SocketException exception) {
            dispose();
            throw new RuntimeException(exception);
        }
    }

    private void kill (Throwable throwable)
    {
        dispose();
        System.err.println(throwable.getClass().getName() + ": " + throwable.getMessage());
        throw new RuntimeException(throwable);
    }

    private void kill (String message)
    {
        kill(new RuntimeException(message));
    }

    private void send_message (final DatagramPacket request_packet, final Callback callback)
    {
        m_worker.submit(() -> {
            try {
                m_socket.send(request_packet);
                final int length = 4096;
                byte[] response_buffer = new byte[length];
                var response_packet = new DatagramPacket(response_buffer, length);
                m_socket.receive(response_packet);
                callback.on_success(response_packet);
            } catch (IOException exception) {
                callback.on_failure(exception);
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

            send_message(packet, new Callback() {
                @Override
                public void on_success (DatagramPacket result)
                {
                    try {
                        final var buffer = ByteBuffer.wrap(result.getData());
                        if (packet.getLength() < 16)
                            kill("Response packet < 16 bytes!");
                        if (buffer.getInt() != action)
                            kill("Request-Response action mismatch!");
                        if (buffer.getInt() != tran_id)
                            kill("Request-Response transaction id mismatch!");
                        synchronized (m_lock) {
                            m_connection_id = buffer.getLong();
                            System.out.println("Connection ID: " + m_connection_id);
                            m_state = TrackerState.CONNECTED;
                        }
                    } catch (Exception e) {
                        kill(e);
                    }
                }

                @Override
                public void on_failure (Throwable throwable)
                {
                    kill(throwable);
                }
            });
    }

    @Override
    public void dispose ()
    {
        synchronized (m_lock) {
            m_state = TrackerState.DISPOSED;
        }
        m_socket.close();
        // One of its own thread may be shutting down worker.
        // This is slightly abrupt in nature.
        m_worker.shutdownNow();
    }

    public TrackerState state ()
    {
        synchronized (m_lock) {
            return m_state;
        }
    }

    public boolean disposed ()
    {
        synchronized (m_lock) {
            return state() == TrackerState.DISPOSED;
        }
    }

    public boolean connected ()
    {
        synchronized (m_lock) {
            return state() == TrackerState.CONNECTED;
        }
    }

    @Override
    public TrackerResponse announce ()
    {
        return null;
    }
}
