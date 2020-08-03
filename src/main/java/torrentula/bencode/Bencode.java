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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bencode {
    private final BufferedInputStream m_stream;

    private Bencode (final InputStream stream)
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
        return (byte)next;
    }

    private boolean is_digit (final byte c)
    {
        return (c >= '0' && c <= '9');
    }

    private BencodeElement parse_positive_integer () throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        while (is_digit(peek()))
            builder.append((char)read());
        return BencodeElement.wrap(Long.parseLong(builder.toString()));
    }

    private BencodeElement parse_byte_array () throws IOException
    {
        int length = (int)parse_positive_integer().as_integer();
        byte[] bytes = new byte[length];
        if (read() != ':') die("Element not byte array!");
        for (int i = 0; i < length; i++)
            bytes[i] = read();
        return BencodeElement.wrap(bytes);
    }

    private BencodeElement parse_integer () throws IOException
    {
        if (read() != 'i') die("Element not integer!");
        boolean is_negative = peek() == '-';
        // Consume the sign.
        if (is_negative) read();
        final long absolute = parse_positive_integer().as_integer();
        final BencodeElement int_element = BencodeElement.wrap(is_negative ? -absolute : absolute);
        if (read() != 'e') die("Element not integer!");
        return int_element;
    }

    private BencodeElement.Type get_next_element_type () throws IOException
    {
        final byte next = peek();
        if (is_digit(next)) return BencodeElement.Type.BYTE_ARRAY;
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
            case BYTE_ARRAY: return parse_byte_array();
            case DICTIONARY: return parse_dictionary();
            case INTEGER: return parse_integer();
            default: die("Couldn't parse `" + type + "`!");
        }
        // Won't ever reach here.
        return null;
    }

    private BencodeElement parse_list () throws IOException
    {
        final List<BencodeElement> list = new ArrayList<>();
        if (read() != 'l') die("Element not list!");
        while (peek() != 'e') {
            final var next_element = parse_next_element();
            list.add(next_element);
        }
        // Consume the 'e'.
        read();
        return BencodeElement.wrap(list);
    }

    private BencodeElement parse_dictionary () throws IOException
    {
        final Map<String, BencodeElement> map = new HashMap<>();
        if (read() != 'd') die("Element not map!");
        while (peek() != 'e') {
            final String key = parse_byte_array().as_string();
            if (key.isEmpty()) die("Empty keys not allowed in dictionary!");
            map.put(key, parse_next_element());
        }
        // Consume the 'e'.
        read();
        return BencodeElement.wrap(map);
    }

    private void dispose () {
        try {
            m_stream.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't close reader!");
            throw new RuntimeException(ioe);
        }
    }

    private BencodeElement parse ()
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

    public static BencodeElement parse (final String data)
    {
        var stream = new ByteArrayInputStream(data.getBytes());
        return new Bencode(stream).parse();
    }

    public static BencodeElement parse (final Path path)
    {
        try {
            var stream = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
            return new Bencode(stream).parse();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
