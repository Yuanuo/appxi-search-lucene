package org.appxi.lucene.bo;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.appxi.lucene.TokenInfo;

import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;

public class TibetanParagraphTokenizer extends Tokenizer {
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    protected final RollingCharBuffer inputBuffer = new RollingCharBuffer();
    protected int inputBufferPos = 0;

    protected final StringBuilder tokenBuffer = new StringBuilder();

    @Override
    public void reset() throws IOException {
        super.reset();
        inputBufferPos = 0;
        inputBuffer.reset(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        inputBuffer.freeBefore(inputBufferPos);
        tokenBuffer.setLength(0);

        final int startOffset = inputBufferPos;
        while (true) {
            final int currChar = inputBuffer.get(inputBufferPos++);
            if (currChar == -1) { // EOF
                break;
            }

            final int nextChar = inputBuffer.get(inputBufferPos);
            if (currChar == '\r' && nextChar == '\n') {
                if (!tokenBuffer.isEmpty()) {
                    inputBufferPos += 1; // skip nextChar
                    break;
                }
                continue;
            }
            if (currChar == '\n') {
                if (!tokenBuffer.isEmpty()) {
                    break;
                }
                continue;
            }

            // 遇到分词符：藏文结束符'།'，此字符后面可能再出现多个空格或藏文结束符'།'
            if (isEndChar(currChar)) {
                final int markPos = tokenBuffer.length();
                tokenBuffer.append((char) currChar);
                // 连续读取空格或藏文结束符'།'
                while (true) {
                    final int testChar = inputBuffer.get(inputBufferPos);
                    if (testChar == -1) { // EOF
                        break;
                    }
                    if (testChar == ' ' || testChar == currChar) {
                        tokenBuffer.append((char) testChar);
                        inputBufferPos++; // skip testChar
                        continue;
                    }
                    break;
                }
                final String markStr = tokenBuffer.substring(markPos - 1).stripTrailing();
                if (tokenBuffer.charAt(0) == '༄') {
                    // 类似：༄༅། །。。。། ། ，需要保留此符号至后续第2个“”། 的文字内容
                    if (tokenBuffer.toString().matches("༄[༅། ]+།")) {
                        continue;
                    }
                    if (markStr.matches("(.*)(།+)$")) {
                        break;
                    }
                }
                if (isEndMark(markStr)) {
                    break;
                }
                continue;
            }
            tokenBuffer.append((char) currChar);

            // 遇到藏文字符'༄'，则认为后续开始的另一部分，需要结束当前部分
            if (nextChar == '༄') {
                break;
            }
        }

        if (tokenBuffer.isEmpty()) {
            return false;
        }

        termAtt.setEmpty().append(tokenBuffer);
        offsetAtt.setOffset(startOffset, inputBufferPos);
        return true;
    }

    protected boolean isEndChar(int c) {
        return c == '།';
    }

    protected boolean isEndMark(String str) {
        return str.matches("(.*)([ །]+)།$");
    }

    public static Stream<TokenInfo> streamParagraphs(Reader reader) {
        final Tokenizer tokenizer = new TibetanParagraphTokenizer();
        tokenizer.setReader(reader);
        return TokenInfo.streamTokens(tokenizer);
    }
}
