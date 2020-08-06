package torrentula.client;

public class ClientState {
    private final Client m_client;
    private final long m_torrent_size;
    private long m_uploaded;
    private long m_downloaded;
    private long m_left;

    ClientState (Client client, long torrent_size)
    {
        m_client = client;
        m_torrent_size = torrent_size;
    }

    public synchronized long bytes_uploaded ()
    {
        return m_uploaded;
    }

    public synchronized long bytes_downloaded ()
    {
        return m_downloaded;
    }

    public synchronized long bytes_left ()
    {
        return m_left;
    }

    synchronized void update_uploaded_byte_count (long update)
    {
        m_uploaded = update;
    }

    synchronized void update_downloaded_byte_count (long update)
    {
        m_downloaded = update;
        m_left = m_torrent_size - m_downloaded;
    }
}
