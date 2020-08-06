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

package torrentula;

import torrentula.bencode.Element;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Metainfo {
    public static class Fileinfo {
        private final Path m_full_path;
        private final long m_length;

        Fileinfo (final long length, String... path_pieces)
        {
            if (path_pieces.length < 1)
                throw new RuntimeException("Path has zero elements!");
            m_length = length;
            m_full_path = Paths.get(path_pieces[0], Arrays.copyOfRange(path_pieces, 1, path_pieces.length));
        }

        public Path path ()
        {
            return m_full_path;
        }

        public long size ()
        {
            return m_length;
        }

        String to_string (int padding)
        {
            String indent = " ".repeat(padding);
            var builder = new StringBuilder();
            builder.append(indent).append("{\n")
                    .append(indent).append("\tpath: ").append(m_full_path.toString()).append("\n")
                    .append(indent).append("\tsize: ").append(m_length).append(" bytes").append("\n")
                    .append(indent).append("}\n");
            return builder.toString();
        }
    }

    enum Mode {
        SINGLE_FILE,
        MULTIPLE_FILE
    }

    private final Map<String, Element> m_source;
    private final String m_tracker_url;
    private final String m_parent_directory_or_file_name;
    private final long m_piece_length;
    private final List<byte[]> m_piece_checksums;
    private final Mode m_mode;
    private final List<Fileinfo> m_fileinfo_list;

    private final long m_torrent_size;
    private final MessageDigest m_sha1;

    Metainfo (
            final Map<String, Element> source,
            final String tracker_url,
            final String name,
            final long piece_length,
            final List<byte[]> piece_checksums,
            final Mode mode,
            final List<Fileinfo> files)
    {
        m_source = source;
        m_tracker_url = tracker_url;
        m_parent_directory_or_file_name = name;
        m_piece_length = piece_length;
        m_piece_checksums = piece_checksums;
        m_mode = mode;
        m_fileinfo_list = files;

        long torrent_size = 0;
        for (var fileinfo : files)
            torrent_size += fileinfo.size();
        m_torrent_size = torrent_size;

        try {
            m_sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException exc) {
            throw new  RuntimeException(exc);
        }
    }

    public String tracker_url ()
    {
        return m_tracker_url;
    }

    public Path parent_directory ()
    {
        return m_mode == Mode.SINGLE_FILE ? Paths.get(System.getProperty("user.dir")) : Paths.get(m_parent_directory_or_file_name);
    }

    public long piece_length ()
    {
        return m_piece_length;
    }

    public byte[] checksum_at (final int index)
    {
        return m_piece_checksums.get(index);
    }

    public Mode mode ()
    {
        return m_mode;
    }

    public int piece_count ()
    {
        return m_piece_checksums.size();
    }

    public Fileinfo file_info_at (final int index)
    {
        return m_fileinfo_list.get(index);
    }

    public int file_count ()
    {
        return m_fileinfo_list.size();
    }

    public byte[] info_hash ()
    {
        return m_sha1.digest(m_source.get("info").serialize());
    }

    public long torrent_size ()
    {
        return m_torrent_size;
    }

    public String to_string (int padding)
    {
        var indent = " ".repeat(padding);
        var double_indent = padding == 0 ? " ".repeat(4) : indent.repeat(2);
        var builder = new StringBuilder();
        builder.append(indent).append("{\n")
                .append(double_indent).append("tracker-url: ").append(m_tracker_url).append("\n")
                .append(double_indent).append("parent-directory: ").append(parent_directory()).append("\n")
                .append(double_indent).append("piece-length: ").append(m_piece_length).append(" bytes\n")
                .append(double_indent).append("piece-count: ").append(m_piece_checksums.size()).append("\n")
                .append(double_indent).append("mode: ").append(m_mode).append("\n")
                .append(double_indent).append("files: ").append("\n");
        for (var fileinfo : m_fileinfo_list)
            builder.append(fileinfo.to_string(4 + double_indent.length()));
        builder.append(indent).append("}");
        return builder.toString();
    }

    public String to_string ()
    {
        return to_string(0);
    }

    static class Builder {
        private Map<String, Element> m_source;
        private String m_tracker_url;
        private String m_name;
        private long m_piece_length;
        private List<byte[]> m_piece_checksums;
        private Mode m_mode;
        private List<Fileinfo> m_files;


        public Metainfo build ()
        {
            if (m_source == null
                    || m_tracker_url == null || m_tracker_url.isEmpty()
                    || m_name == null || m_name.isEmpty()
                    || m_piece_length <= 0
                    || m_piece_checksums == null || m_piece_checksums.size() == 0
                    || m_mode == null
                    || m_files == null || m_files.size() == 0)
                throw new RuntimeException("Invalid metafile!");

            return new Metainfo(
                    m_source,
                    m_tracker_url,
                    m_name,
                    m_piece_length,
                    m_piece_checksums,
                    m_mode,
                    m_files);
        }

        public Builder set_piece_checksums (final Map<String, Element> info)
        {
            byte[] bytes = info.get("pieces").as_byte_string();
            int length = bytes.length;
            if (length % 20 != 0)
                throw new RuntimeException("Invalid piece checksum length!");
            m_piece_checksums = new ArrayList<>(length / 20);
            int index = 0;
            while (length > 0) {
                byte[] temp = new byte[20];
                System.arraycopy(bytes, index, temp, 0, 20);
                m_piece_checksums.add(temp);
                index += 20;
                length -= 20;
            }
            return this;
        }

        public Builder set_mode (final Map<String, Element> info)
        {
            if (info.containsKey("length"))
                m_mode = Mode.SINGLE_FILE;
            else if (info.containsKey("files"))
                m_mode = Mode.MULTIPLE_FILE;
            else throw new RuntimeException("Couldn't guess mode!");
            return this;
        }

        public Builder set_files (final Map<String, Element> info)
        {
            boolean single_file = m_mode == Mode.SINGLE_FILE;
            if (single_file) {
                m_files = new ArrayList<>();
                m_files.add(new Fileinfo(info.get("length").as_integer(), m_name));
                return this;
            }

            var file_list = info.get("files").as_list();
            m_files = new ArrayList<>(file_list.size());
            for (var file_element : file_list) {
                var file_dictionary = file_element.as_dictionary();
                var paths = file_dictionary.get("path").as_list();
                var path_pieces = new String[paths.size()];
                for (int i = 0; i < path_pieces.length; i++)
                    path_pieces[i] = paths.get(i).as_string();
                var file = new Fileinfo(file_dictionary.get("length").as_integer(), path_pieces);
                m_files.add(file);
            }
            return this;
        }
    }

    public static Metainfo from (final Map<String, Element> metainfo)
    {
        Builder builder = new Builder();
        var info = metainfo.get("info").as_dictionary();

        builder.m_source = metainfo;
        builder.m_tracker_url = metainfo.get("announce").as_string();
        builder.m_name = info.get("name").as_string();
        builder.m_piece_length = info.get("piece length").as_integer();
        return builder.set_piece_checksums(info)
                .set_mode(info)
                .set_files(info)
                .build();
    }
}
