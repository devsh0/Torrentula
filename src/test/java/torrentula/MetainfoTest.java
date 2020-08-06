package torrentula;

import org.junit.jupiter.api.Test;
import torrentula.bencode.Bencode;

import java.nio.file.Paths;

public class MetainfoTest {
    @Test
    void test_metainfo ()
    {
        var torrent_info = Bencode.deserialize(Paths.get("kamikaze.torrent")).as_dictionary();
        var metainfo = Metainfo.from(torrent_info);
        System.out.println(metainfo.info_hash().length);
    }
}
