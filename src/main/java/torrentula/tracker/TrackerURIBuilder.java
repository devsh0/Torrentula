package torrentula.tracker;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;

class TrackerURIBuilder {
    private String m_uri;
    private String m_prefix;

    TrackerURIBuilder (String base_url)
    {
        m_uri = base_url;
    }

    TrackerURIBuilder append_query (String key, String value)
    {
        m_prefix = m_prefix == null ? "?" : "&";
        m_uri += m_prefix + key + "=" + value;
        return this;
    }

    private boolean escape_required (char c)
    {
        return !((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || (c == '.' || c == '-' || c == '_' || c == '~')
                || (c == '(' || c == ')' || c == '!' || c == '*'));
    }

    TrackerURIBuilder append_query (String key, ByteBuffer buffer)
    {
        Objects.requireNonNull(buffer, "Empty buffer supplied for query value!");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < buffer.remaining(); i++) {
            char ch = (char) buffer.get(i);
            if (escape_required(ch)) {
                builder.append("%");
                String hex = Integer.toHexString(ch & 0xFF).toUpperCase();
                hex = hex.length() == 1 ? "0" + hex : hex;
                builder.append(hex);
            } else builder.append(ch);
        }
        append_query(key, builder.toString());
        return this;
    }

    TrackerURIBuilder append_query (String key, long value)
    {
        return append_query(key, value + "");
    }

    TrackerURIBuilder append_query (String key, int value)
    {
        return append_query(key, value + "");
    }

    URI build ()
    {
        return URI.create(m_uri);
    }
}
