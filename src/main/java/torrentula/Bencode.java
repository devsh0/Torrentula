package torrentula;

import java.io.*;
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
        STRING,
        LIST,
        DICTIONARY
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
            else if (value instanceof String)
                m_type = Type.STRING;
            else if (value instanceof Map)
                m_type = Type.DICTIONARY;
            else if (value instanceof Long)
                m_type = Type.INTEGER;
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
            validate_type_or_throw(Type.STRING);
            return (String) m_value;
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

        public static Element wrap (final Object value)
        {
            return new Element(value);
        }
    }

    private final InputStream m_stream;
    private final BufferedReader m_reader;

    private Bencode (final InputStream stream)
    {
        m_stream = stream;
        m_reader = new BufferedReader(new InputStreamReader(m_stream, StandardCharsets.UTF_8));
    }

    private char peek_char () throws IOException
    {
        m_reader.mark(1);
        char next = read_char();
        m_reader.reset();
        return next;
    }

    private char read_char () throws IOException
    {
        int next = m_reader.read();
        if (next == -1)
            throw new IOException("Attempted reading beyond EOF!");
        return (char) next;
    }

    private boolean is_digit (final char c)
    {
        return (c >= '0' && c <= '9');
    }

    private Element parse_positive_integer () throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        while (is_digit(peek_char()))
            builder.append(read_char());
        return Element.wrap(Long.parseLong(builder.toString()));
    }

    private Element parse_string () throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        int length = (int)parse_positive_integer().as_integer();

        if (read_char() != ':')
            throw new RuntimeException("Element not string!");
        while (length-- > 0)
            builder.append(read_char());
        return Element.wrap(builder.toString());
    }

    private Element parse_integer () throws IOException
    {
        var exception = new RuntimeException("Element not integer!");
        if (read_char() != 'i') throw exception;
        boolean is_negative = peek_char() == '-';
        // Consume the sign.
        if (is_negative) read_char();
        final long absolute = parse_positive_integer().as_integer();
        final Element int_element = Element.wrap(is_negative ? -absolute : absolute);
        if (read_char() != 'e') throw exception;
        return int_element;
    }

    private Type get_next_element_type () throws IOException
    {
        final char next = peek_char();
        if (is_digit(next)) return Type.STRING;
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
            case STRING -> parse_string();
            case DICTIONARY -> parse_dictionary();
            case INTEGER -> parse_integer();
            default -> throw new RuntimeException("Couldn't parse `" + type + "`!");
        };
    }

    private Element parse_list () throws IOException
    {
        final List<Element> list = new ArrayList<>();
        if (read_char() != 'l')
            throw new RuntimeException("Element not list!");
        while (peek_char() != 'e') {
            final var next_element = parse_next_element();
            list.add(next_element);
        }
        // Consume the 'e'.
        read_char();
        return Element.wrap(list);
    }

    private Element parse_dictionary () throws IOException
    {
        var exception = new RuntimeException("Element not map!");
        final Map<String, Element> map = new HashMap<>();
        if (read_char() != 'd')
            throw exception;
        while (peek_char() != 'e') {
            final String key = parse_string().as_string();
            if (key.isEmpty())
                throw new RuntimeException("Empty keys not allowed in dictionary!");
            map.put(key, parse_next_element());
        }
        // Consume the 'e'.
        read_char();
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
