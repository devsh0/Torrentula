package torrentula.bencode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BencodeElement {
    enum Type {
        UNKNOWN,
        INTEGER,
        LIST,
        DICTIONARY,
        BYTE_STRING
    }
    
    private final Object m_value;
    private final Type m_type;
    private final long m_size;

    private BencodeElement (final Object value, final long size)
    {
        if (value instanceof List)
            m_type = Type.LIST;
        else if (value instanceof Map)
            m_type = Type.DICTIONARY;
        else if (value instanceof Long)
            m_type = Type.INTEGER;
        else if (value instanceof byte[])
            m_type = Type.BYTE_STRING;
        else throw new RuntimeException("Invalid value!");

        m_value = value;
        m_size = size;
    }

    public Type type ()
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
        validate_type_or_throw(Type.BYTE_STRING);
        return new String((byte[]) m_value, StandardCharsets.UTF_8);
    }

    public Map<String, BencodeElement> as_dictionary ()
    {
        validate_type_or_throw(Type.DICTIONARY);
        return (Map<String, BencodeElement>) m_value;
    }

    public List<BencodeElement> as_list ()
    {
        validate_type_or_throw(Type.LIST);
        return (List<BencodeElement>) m_value;
    }

    public byte[] as_byte_string () {
        validate_type_or_throw(Type.BYTE_STRING);
        return (byte[]) m_value;
    }

    public long size ()
    {
        return m_size;
    }

    public static BencodeElement wrap (final Object value, final long size)
    {
        return new BencodeElement(value, size);
    }
}
