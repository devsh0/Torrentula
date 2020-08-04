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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Decoder {
    private long m_offset = 0;
    private final BufferedInputStream m_stream;

    Decoder (final InputStream stream)
    {
        m_stream = new BufferedInputStream(stream);
    }

    private void die (final String message)
    {
        try {
            var builder = new StringBuilder();
            var limit = Math.max(m_stream.available(), 20);
            for (int i = 0; i < limit; i++)
                builder.append(read());
            final var vicinity = builder.toString();
            System.err.println("Failed before reaching: " + vicinity);
            throw new RuntimeException(message);
        } catch (IOException ioe) {
            // ignore
        }
    }

    private byte peek () throws IOException
    {
        m_stream.mark(1);
        int next = read();
        m_stream.reset();
        return (byte)next;
    }

    private byte read () throws IOException
    {
        int next = m_stream.read();
        if (next == -1)
            die("Attempted reading beyond EOF!");
        m_offset++;
        return (byte)next;
    }

    private boolean is_digit (final byte c)
    {
        return (c >= '0' && c <= '9');
    }

    private long parse_positive_integer () throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        while (is_digit(peek()))
            builder.append((char)read());
        return Long.parseLong(builder.toString());
    }

    private BencodeElement parse_byte_string () throws IOException
    {
        long start = offset();
        int length = (int)parse_positive_integer();
        byte[] bytes = new byte[length];
        if (read() != ':') die("Element not byte string!");
        for (int i = 0; i < length; i++)
            bytes[i] = read();
        long size = offset() - start;
        return BencodeElement.wrap(bytes, size);
    }

    private BencodeElement parse_integer () throws IOException
    {
        long start = offset();
        if (read() != 'i') die("Element not integer!");
        boolean is_negative = peek() == '-';
        // Consume the sign.
        if (is_negative) read();
        final long absolute = parse_positive_integer();
        if (read() != 'e') die("Element not integer!");
        long size = offset() - start;
        return BencodeElement.wrap(is_negative ? -absolute : absolute, size);
    }

    private BencodeElement.Type get_next_element_type () throws IOException
    {
        final byte next = peek();
        if (is_digit(next)) return BencodeElement.Type.BYTE_STRING;
        return switch (next) {
            case 'l' -> BencodeElement.Type.LIST;
            case 'd' -> BencodeElement.Type.DICTIONARY;
            case 'i' -> BencodeElement.Type.INTEGER;
            default -> BencodeElement.Type.UNKNOWN;
        };
    }

    private BencodeElement parse_next_element () throws IOException
    {
        final BencodeElement.Type type= get_next_element_type();
        switch (type) {
            case LIST:  return parse_list();
            case BYTE_STRING: return parse_byte_string();
            case DICTIONARY: return parse_dictionary();
            case INTEGER: return parse_integer();
            default: die("Couldn't parse `" + type + "`!");
        }
        // Won't ever reach here.
        return null;
    }

    private BencodeElement parse_list () throws IOException
    {
        long start = offset();
        final List<BencodeElement> list = new ArrayList<>();
        if (read() != 'l') die("Element not list!");
        while (peek() != 'e') {
            final var next_element = parse_next_element();
            list.add(next_element);
        }
        // Consume the 'e'.
        read();
        long size = offset() - start;
        return BencodeElement.wrap(list, size);
    }

    private BencodeElement parse_dictionary () throws IOException
    {
        long start = offset();
        final Map<String, BencodeElement> map = new HashMap<>();
        if (read() != 'd') die("Element not map!");
        while (peek() != 'e') {
            final String key = parse_byte_string().as_string();
            if (key.isEmpty()) die("Empty keys not allowed in dictionary!");
            map.put(key, parse_next_element());
        }
        // Consume the 'e'.
        read();
        long size = offset() - start;
        return BencodeElement.wrap(map, size);
    }

    private long offset ()
    {
        return m_offset;
    }

    private void dispose () {
        try {
            m_stream.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't close reader!");
            throw new RuntimeException(ioe);
        }
    }

    BencodeElement decode ()
    {
        try {
            return parse_next_element();
        } catch (IOException ioe) {
            System.err.println("Couldn't finish parsing!");
            throw new RuntimeException(ioe);
        } finally {
            dispose();
        }
    }
}
