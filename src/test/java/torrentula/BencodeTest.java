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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BencodeTest {
    @Test
    // Pretty naive test. Serves the purpose for now.
    void test_bencode_parser () {
        var string = "d4:dictd11:dict-item-14:test11:dict-item-25:thinge4:listl11:list-item-111:list-item-2e6:numberi123456e6:string5:valuee";
        var top_level = Bencode.parse(string).as_dictionary();

        var dictionary = top_level.get("dict").as_dictionary();
        assertEquals("test", dictionary.get("dict-item-1").as_string());
        assertEquals("thing", dictionary.get("dict-item-2").as_string());

        var list = top_level.get("list").as_list();
        assertEquals("list-item-1", list.get(0).as_string());
        assertEquals("list-item-2", list.get(1).as_string());

        assertEquals(123456, top_level.get("number").as_integer());
        assertEquals("value", top_level.get("string").as_string());
    }
}