package org.appxi.lucene.bo;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class TibetanParagraphTokenizerTest {
    public static void main(String[] args) throws Exception {
        String text = "རང་བྱུང་པདྨ་རྒྱལ་པོ་མཐོང་གྱུར་ཅིག །བདག་དང་འདི་ལ་འབྲེལ་བ་ཐོག་པ་རྣམས། །" +
                      "༄༅། །གུ་རུའི་ཚིག་བདུན་གསོལ་འདེབས་ཀྱི་རྣམ་བཤད་པདྨ་དཀར་པོ་ཞེས་བྱ་བ་བཞུགས་སོ། །" +
                      "༄༅། །ན་མོ་གུ་རུ་པདྨ་མཉྫུ་ཤྲཱི་བཛྲ་ཏིཀྵྞ་ཡ། " +
                      "༄༅། །ན་མོ་གུ་རུ་པདྨ་མཉྫུ་ཤྲཱི་བཛྲ་ཏིཀྵྞ་ཡ།\n" +
                      "དུས་གསུམ་སངས་རྒྱས་ཀུན་དངོས་རྡོ་རྗེ་འཆང་། །";

        text = Files.readString(Path.of("S:\\newWords\\yiZuo-src\\test-7ju.txt"));

        Reader reader = new StringReader(text);

        TibetanParagraphTokenizer.streamParagraphs(reader)
                .forEach(token -> System.out.println(token.term()));

//        TibetanSentenceTokenizer.streamSentences(reader)
//                .forEach(token -> System.out.println(token.term()));
    }
}