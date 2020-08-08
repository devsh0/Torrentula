package torrentula.tracker;

import org.junit.jupiter.api.Test;
import torrentula.Metainfo;
import torrentula.bencode.Bencode;
import torrentula.client.Client;

import java.nio.file.Paths;

public class HttpTrackerTest {
    @Test
    public void test_tracker ()
    {
        var torrent_info = Bencode.deserialize(Paths.get("friends.torrent")).as_dictionary();
        var metainfo = Metainfo.from(torrent_info);
        var client = new Client(metainfo, 6881);
        var tracker = new HttpTracker(client, metainfo.tracker_url());
        tracker.announce();
    }
}
