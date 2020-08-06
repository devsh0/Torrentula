package torrentula.tracker;

import torrentula.bencode.Bencode;
import torrentula.bencode.Element;

class TrackerResponse {
    private final Element m_root;

    private TrackerResponse (byte[] bencoded)
    {
        System.out.println("Response: " + new String(bencoded));
        m_root = Bencode.deserialize(bencoded);
    }

    static TrackerResponse from (byte[] bencoded)
    {
        return new TrackerResponse(bencoded);
    }
}
