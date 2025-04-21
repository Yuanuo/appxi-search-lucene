package org.appxi.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public record TokenInfo(String term, int startOffset, int endOffset) {
    public static Stream<TokenInfo> streamTokens(TokenStream tokenStream) {
        try {
            final OffsetAttribute offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);
            final CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);

            tokenStream.reset();

            return Stream.generate(() -> {
                try {
                    if (tokenStream.incrementToken()) {
                        return new TokenInfo(termAtt.toString(), offsetAtt.startOffset(), offsetAtt.endOffset());
                    }
                    tokenStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).takeWhile(Objects::nonNull);
        } catch (Exception ignored) {
        }
        return Stream.empty();
    }

    public static void forEach(TokenStream tokenStream, BiConsumer<CharTermAttribute, OffsetAttribute> consumer) {
        try (TokenStream tokenizer = tokenStream) {
            final OffsetAttribute offsetAtt = tokenizer.addAttribute(OffsetAttribute.class);
            final CharTermAttribute termAtt = tokenizer.addAttribute(CharTermAttribute.class);

            tokenizer.reset();

            while (tokenizer.incrementToken()) {
                consumer.accept(termAtt, offsetAtt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
