package org.appxi.lucene.bo;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;

public class TibetanTrimFilter extends TokenFilter {
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public TibetanTrimFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        while (input.incrementToken()) {
            char c = termAttr.charAt(termAttr.length() - 1);
            if (c == '།' || c == '༔' || c == 'ཿ' || c == ' ') {
                continue;
            }

            String term = termAttr.toString();

            if (c == '་') {
                term = term.substring(0, term.length() - 1);
            }

            termAttr.setEmpty().append(term);
            return true;
        }

        return false;
    }
}
