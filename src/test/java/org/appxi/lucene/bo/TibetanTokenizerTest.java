package org.appxi.lucene.bo;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.appxi.lucene.TokenInfo;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class TibetanTokenizerTest {
    public static void main(String[] args) throws Exception {
//        // Simulate a large text input
//        StringBuilder largeText = new StringBuilder();
//        for (int i = 0; i < 1; i++) {
//            largeText.append("༄༅། །།རྒྱ་གར་གྱི་མཁན་པོ་པདྨཱ་ཀ་ར་ཝརྨ་དང་། ཞུ་ཆེན་གྱི་ལོ་ཙཱ་བ་དགེ་སློང་རིན་ཆེན་བཟང་པོས་བསྒྱུར་ཅིང་ཞུས་ཏེ་གཏན་ལ་ཕབ་པའོ།། །།༄༅། །བླ་མ་ལྔ་བཅུ་པའི་རྣམ་བཤད་སློབ་མའི་རེ་བ་ཀུན་སྐོང་ཞེས་བྱ་བ་བཞུགས་སོ།ལ་ཕབ་པའོ།། །།༄༅། །བླ་མ་ལྔ་བཅུ་པའི་རྣམ་བཤད་སློབ་མའི་རེ་བ་ཀུན་སྐོང་ཞེས་བྱ་བ་བཞུགས་སོ།中文测试Hello World ");
//        }

        final Path inputFile = Path.of("S:\\newWords\\yiZuo-src\\test-7ju.txt");
        final String inputText = Files.readString(inputFile);

        Reader reader = new StringReader(inputText);

        // 构建 Trie 树并插入规则
        TibetanWordFilter.Trie trie = new TibetanWordFilter.Trie();
        trie.insert("ཀུན་ཏུ་བཟང་པོ");

        // 输入文本
        Tokenizer tokenizer = new TibetanStandardTokenizer();
        tokenizer.setReader(reader);

        // 应用 TokenFilter
        TokenStream tokenStream = tokenizer;

        tokenStream = new TibetanTrimFilter(tokenStream);

        tokenStream = new TibetanWordFilter(tokenStream, trie);

        // 输出结果

        TokenInfo.forEach(tokenStream, (termAttr, offsetAttr) -> {
            System.out.println(offsetAttr.startOffset() + " - " + offsetAttr.endOffset() + ": " + termAttr.toString());
        });
    }
}
