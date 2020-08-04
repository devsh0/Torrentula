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

package torrentula.bencode;

import java.util.Objects;

final class Serializer {
    private int m_cursor = 0;
    private final byte[] m_buffer;
    private final Element m_root;

    Serializer (final Element root)
    {
        m_root = root;
        m_buffer = new byte[root.size()];
    }

    private void write (byte... byte_string)
    {
        Objects.requireNonNull(byte_string, "Cannot write null!");
        for (byte b : byte_string) m_buffer[m_cursor++] = b;
    }

    private void write (final String s)
    {
        write(s.getBytes());
    }

    private void write_integer (final Element element)
    {
        long number = element.as_integer();
        write("i");
        write(String.valueOf(number));
        write("e");
    }

    private void write_byte_string (final Element element)
    {
        var byte_string = element.as_byte_string();
        String size = String.valueOf(byte_string.length);
        write(size);
        write(":");
        write(byte_string);
    }

    private void write_list (final Element element)
    {
        var list = element.as_list();
        write("l");
        for (Element e : list)
            write_element(e);
        write("e");
    }

    private void write_dictionary (final Element element)
    {
        var map = element.as_dictionary();
        write("d");
        for (String key : map.keySet()) {
            write(String.valueOf(key.length()));
            write(":");
            write(key);
            write_element(map.get(key));
        }
        write("e");
    }

    private void die (final String error)
    {
        throw new RuntimeException(error);
    }

    private void write_element (final Element element)
    {
        var type = element.type();
        switch (type) {
            case DICTIONARY -> write_dictionary(element);
            case LIST -> write_list(element);
            case BYTE_STRING -> write_byte_string(element);
            case INTEGER -> write_integer(element);
            default -> die("Can't encode element `" + type + "`");
        }
    }

    byte[] serialize ()
    {
        write_element(m_root);
        return m_buffer;
    }
}
