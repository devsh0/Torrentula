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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Bencode {
    private Bencode ()
    {
    }

    public static Element deserialize (final byte[] data)
    {
        return new Deserializer(new ByteArrayInputStream(data)).deserialize();
    }

    public static Element deserialize (final String data)
    {
        return deserialize(data.getBytes());
    }

    public static Element deserialize (final Path path)
    {
        try {
            return deserialize(Files.readAllBytes(path));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static byte[] serialize (final Element element)
    {
        return new Serializer(element).serialize();
    }
}
