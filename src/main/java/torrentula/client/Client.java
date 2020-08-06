package torrentula.client;

import torrentula.Metainfo;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {
    public static final byte[] ID;

    static {
        var prefix = "torrentula:" + System.currentTimeMillis() + ":";
        byte[] data;
        try {
            var id =  prefix + System.getProperty("user.name");
            var sha1 = MessageDigest.getInstance("SHA-1");
            data = sha1.digest(id.getBytes());
        } catch (NoSuchAlgorithmException exception) {
            System.err.println(exception.getMessage());
            data = "x".repeat(20).getBytes();
        }
        ID = data;
    }

    private final Metainfo m_metainfo;
    private final int m_port;
    private final ClientState m_state;

    public Client (Metainfo info, int port)
    {
        m_metainfo = info;
        m_port = port;
        m_state = new ClientState(this, m_metainfo.torrent_size());
    }

    public ByteBuffer info_hash ()
    {
        return ByteBuffer.wrap(m_metainfo.info_hash()).asReadOnlyBuffer();
    }

    public ByteBuffer id ()
    {
        return ByteBuffer.wrap(ID).asReadOnlyBuffer();
    }

    public int port ()
    {
        return m_port;
    }

    public ClientState state ()
    {
        return m_state;
    }
}
