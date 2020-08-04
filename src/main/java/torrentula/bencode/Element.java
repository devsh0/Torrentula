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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Element {
    enum Type {
        UNKNOWN,
        INTEGER,
        LIST,
        DICTIONARY,
        BYTE_STRING
    }

    private final Object m_value;
    private final Type m_type;
    private final int m_size;

    private Element (final Object value, final int size)
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

    public void validate_type_or_throw (final Type type)
    {
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

    public byte[] as_byte_string ()
    {
        validate_type_or_throw(Type.BYTE_STRING);
        return (byte[]) m_value;
    }

    public byte[] serialize ()
    {
        return Bencode.serialize(this);
    }

    private boolean integer_equal (final Element element)
    {
        long this_int = as_integer();
        long that_int = element.as_integer();
        return this_int == that_int;
    }

    private boolean byte_string_equal (final Element element)
    {
        var this_string = as_byte_string();
        var that_string = element.as_byte_string();
        return Arrays.equals(this_string, that_string);
    }

    private boolean list_equal (final Element element)
    {
        var this_list = as_list();
        var that_list = element.as_list();
        if (this_list.size() != that_list.size())
            return false;
        for (int i = 0; i < that_list.size(); i++) {
            var e1 = this_list.get(i);
            var e2 = that_list.get(i);
            if (!e1.equals(e2))
                return false;
        }
        return true;
    }

    private boolean dictionary_equal (final Element element)
    {
        var this_dict = as_dictionary();
        var that_dict = element.as_dictionary();
        if (this_dict.entrySet().size() != that_dict.entrySet().size())
            return false;
        for (var key : this_dict.keySet()) {
            if (!that_dict.containsKey(key))
                return false;
            var v1 = this_dict.get(key);
            var v2 = that_dict.get(key);
            if (!v1.equals(v2))
                return false;
        }
        return true;
    }

    private boolean element_equal (final Element element)
    {
        return switch (m_type) {
            case INTEGER -> integer_equal(element);
            case BYTE_STRING -> byte_string_equal(element);
            case LIST -> list_equal(element);
            case DICTIONARY -> dictionary_equal(element);
            default -> false;
        };
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Element))
            return false;
        final Element element = (Element) other;
        if (element.type() != type())
            return false;
        return element_equal(element);
    }

    @Override
    public int hashCode ()
    {
        return size();
    }

    public int size ()
    {
        return m_size;
    }

    public static Element wrap (final Object value, final int size)
    {
        return new Element(value, size);
    }
}
