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

package torrentula;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bencode {

    enum Type {
        UNKNOWN,
        INTEGER,
        LIST,
        DICTIONARY,
        BYTE_ARRAY
    }

    @SuppressWarnings("unchecked")
    static class Element {
        private final Object m_value;
        private final Type m_type;

        private Element (final Object value)
        {
            m_value = value;
            if (value instanceof List)
                m_type = Type.LIST;
            else if (value instanceof Map)
                m_type = Type.DICTIONARY;
            else if (value instanceof Long)
                m_type = Type.INTEGER;
            else if (value instanceof byte[])
                m_type = Type.BYTE_ARRAY;
            else throw new RuntimeException("Invalid value!");
        }

        public Type get_type ()
        {
            return m_type;
        }

        public void validate_type_or_throw (final Type type) {
            if (m_type != type)
                throw new RuntimeException(String.format("Expected `%s`, found `%s`!", type, m_type));
        }

        public long as_integer ()
        {
            validate_type_or_throw(Type.INTEGER);
            return (Long) m_value;
        }

        public String as_string ()
        {
            validate_type_or_throw(Type.BYTE_ARRAY);
            return new String((byte[]) m_value, StandardCharsets.UTF_8);
        }

        public Map<String, Element> as_dictionary ()
        {
            validate_type_or_throw(Type.DICTIONARY);
            return (Map<String, Element>) m_value;
        }

        public List<Element> as_list ()
        {
            validate_type_or_throw(Type.LIST);
            return (List<Element>) m_value;
        }

        public byte[] as_byte_array () {
            validate_type_or_throw(Type.BYTE_ARRAY);
            return (byte[]) m_value;
        }

        public static Element wrap (final Object value)
        {
            return new Element(value);
        }
    }

    private final BufferedInputStream m_reader;

    private Bencode (final InputStream stream)
    {
        m_reader = new BufferedInputStream(stream);
    }

    private byte peek () throws IOException
    {
        m_reader.mark(1);
        int next = read();
        m_reader.reset();
        return (byte)next;
    }

    private byte read () throws IOException
    {
        int next = m_reader.read();
        if (next == -1)
            throw new IOException("Attempted reading beyond EOF!");
        return (byte)next;
    }

    private boolean is_digit (final byte c)
    {
        return (c >= '0' && c <= '9');
    }

    private Element parse_positive_integer () throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        while (is_digit(peek()))
            builder.append((char)read());
        return Element.wrap(Long.parseLong(builder.toString()));
    }

    private Element parse_byte_array () throws IOException
    {
        int length = (int)parse_positive_integer().as_integer();
        byte[] bytes = new byte[length];

        if (read() != ':')
            throw new RuntimeException("Element not byte array!");
        for (int i = 0; i < length; i++)
            bytes[i] = read();
        return Element.wrap(bytes);
    }

    private Element parse_integer () throws IOException
    {
        var exception = new RuntimeException("Element not integer!");
        if (read() != 'i') throw exception;
        boolean is_negative = peek() == '-';
        // Consume the sign.
        if (is_negative) read();
        final long absolute = parse_positive_integer().as_integer();
        final Element int_element = Element.wrap(is_negative ? -absolute : absolute);
        if (read() != 'e') throw exception;
        return int_element;
    }

    private Type get_next_element_type () throws IOException
    {
        final byte next = peek();
        if (is_digit(next)) return Type.BYTE_ARRAY;
        return switch (next) {
            case 'l' -> Type.LIST;
            case 'd' -> Type.DICTIONARY;
            case 'i' -> Type.INTEGER;
            default -> Type.UNKNOWN;
        };
    }

    private Element parse_next_element () throws IOException
    {
        final Type type = get_next_element_type();
        return switch (type) {
            case LIST -> parse_list();
            case BYTE_ARRAY -> parse_byte_array();
            case DICTIONARY -> parse_dictionary();
            case INTEGER -> parse_integer();
            default -> throw new RuntimeException("Couldn't parse `" + type + "`!");
        };
    }

    private Element parse_list () throws IOException
    {
        final List<Element> list = new ArrayList<>();
        if (read() != 'l')
            throw new RuntimeException("Element not list!");
        while (peek() != 'e') {
            final var next_element = parse_next_element();
            list.add(next_element);
        }
        // Consume the 'e'.
        read();
        return Element.wrap(list);
    }

    private Element parse_dictionary () throws IOException
    {
        var exception = new RuntimeException("Element not map!");
        final Map<String, Element> map = new HashMap<>();
        if (read() != 'd')
            throw exception;
        while (peek() != 'e') {
            final String key = parse_byte_array().as_string();
            if (key.isEmpty())
                throw new RuntimeException("Empty keys not allowed in dictionary!");
            map.put(key, parse_next_element());
        }
        // Consume the 'e'.
        read();
        return Element.wrap(map);
    }

    private void dispose () {
        try {
            m_reader.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't close reader!");
            throw new RuntimeException(ioe);
        }
    }

    private Element parse ()
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

    public static Element parse (final String data)
    {
        var stream = new ByteArrayInputStream(data.getBytes());
        return new Bencode(stream).parse();
    }

    public static Element parse (final Path path)
    {
        try {
            var stream = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
            return new Bencode(stream).parse();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
