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
        if (m_prefix == null)
            m_prefix = "?";
        else m_prefix = "&";
        m_uri += m_prefix + key + "=" + value;
        return this;
    }

    private boolean escape_required (byte c)
    {
        return !((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || (c == '.' || c == '-' || c == '_' || c == '~'));
    }

    TrackerURIBuilder append_query (String key, ByteBuffer buffer)
    {
        Objects.requireNonNull(buffer, "Byte string is null!");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < buffer.remaining(); i++)
            builder.append(String.format("%%%02x", buffer.get(i)));
        append_query(key, builder.toString());
        return this;
    }

    URI build ()
    {
        return URI.create(m_uri);
    }
}
