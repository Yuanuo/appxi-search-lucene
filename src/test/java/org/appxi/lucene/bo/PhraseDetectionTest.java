package org.appxi.lucene.bo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class PhraseDetectionTest {
    public static void main(String[] args) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("S:\\newWords\\yiZuo-src\\test-7ju.txt"))) {

            Analyzer analyzer = new Analyzer() {
                @Override
                protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
                    // 第一步：创建 Tokenizer
                    Tokenizer tokenizer = new TibetanStandardTokenizer();

                    // 第二步：添加 TokenFilter
//        TokenStream filter1 = new LowerCaseFilter(tokenizer); // 转换为小写
                    TokenStream filter2 = new TibetanNgramFilter(tokenizer, 1, 8, 2, "་"); // 合并多字词

                    // 返回最终的 TokenStreamComponents
                    return new Analyzer.TokenStreamComponents(tokenizer, filter2);
                }
            };
            TokenStream stream = analyzer.tokenStream("content", reader);
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

            stream.reset();
            Set<String> tokens = new HashSet<>();
            while (stream.incrementToken()) {
                String token = termAtt.toString();
                tokens.add(token);
                System.out.println(token);
//                if (count > 1000) break;
            }
            stream.end();
            stream.close();

            Files.writeString(Path.of("S:\\newWords\\yiZuo-src\\test-7ju-words.txt"), String.join("\n", tokens));
        }

    }
}
