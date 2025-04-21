package org.appxi.lucene.bo;

import org.apache.lucene.analysis.Tokenizer;
import org.appxi.lucene.TokenInfo;

import java.io.Reader;
import java.util.stream.Stream;

public final class TibetanSentenceTokenizer extends TibetanParagraphTokenizer {
    @Override
    protected boolean isEndChar(int c) {
        return c == '༔' || c == 'ཿ' || super.isEndChar(c);
    }

    @Override
    protected boolean isEndMark(String str) {
        return str.matches("(.*)[།༔ཿ]$");
    }

    public static Stream<TokenInfo> streamSentences(Reader reader) {
        final Tokenizer tokenizer = new TibetanSentenceTokenizer();
        tokenizer.setReader(reader);
        return TokenInfo.streamTokens(tokenizer);
    }
}
