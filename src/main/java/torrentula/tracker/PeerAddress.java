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

import torrentula.bencode.Element;

import javax.net.ssl.HostnameVerifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PeerAddress
{
    private String m_host;
    private int m_port;

    private PeerAddress (String host, int port)
    {
        m_host = host;
        m_port = port;
    }

    public static List<PeerAddress> from (byte[] peers)
    {
        if (peers.length % 6.0 != 0)
            throw new RuntimeException("Invalid format for peer address (assumed BYTE_STRING)!");
        int peer_count = peers.length / 6;
        List<PeerAddress> peer_list = new ArrayList<>(peer_count);
        var buffer = ByteBuffer.wrap(peers);
        for (int i = 0; i < peer_count; i++)
        {
            byte[] ip = {0, 0, 0, 0};
            try
            {
                buffer.get(ip);
                String host = Inet4Address.getByAddress(ip).getHostAddress();
                int port = buffer.getShort();
                peer_list.add(new PeerAddress(host, port));
            } catch (UnknownHostException exception)
            {
                exception.printStackTrace();
            }
        }
        return peer_list;
    }

    public static List<PeerAddress> from (List<Element> peers)
    {
        List<PeerAddress> peer_list = new ArrayList<>(peers.size());
        peers.forEach(element -> {
            var dict = element.as_dictionary();
            var host = dict.get("ip").as_string();
            var port = (int)dict.get("port").as_integer();
            peer_list.add(new PeerAddress(host, port));
        });
        return peer_list;
    }

    public String host ()
    {
        return m_host;
    }

    public int port ()
    {
        return m_port;
    }

    @Override
    public String toString()
    {
        return host() + ":" + port();
    }
}
