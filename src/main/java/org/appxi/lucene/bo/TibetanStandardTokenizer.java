package org.appxi.lucene.bo;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;

import java.io.IOException;

public final class TibetanStandardTokenizer extends Tokenizer {
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final RollingCharBuffer inputBuffer = new RollingCharBuffer();
    private int inputBufferPos = 0;

    private char[] tokenBuffer;
    private int tokenLength;

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
        tokenLength = 0;
        tokenBuffer = new char[8];

        int startOffset = inputBufferPos;
        Character.UnicodeBlock prevCharUnicodeBlock = null, currCharUnicodeBlock;
        while (true) {
            final int currChar = inputBuffer.get(inputBufferPos++);
            if (currChar == -1) { // EOF
                inputBufferPos -= 1;
                break;
            }

            if (currChar == '\n') {
                if (tokenLength > 0) {
                    break;
                }
                continue;
            }

            final int nextChar = inputBuffer.get(inputBufferPos);
            if (currChar == '\r' && nextChar == '\n') {
                if (tokenLength > 0) {
                    inputBufferPos += 1; // skip nextChar
                    break;
                }
                continue;
            }

            currCharUnicodeBlock = Character.UnicodeBlock.of(currChar);
            // 处理混合语种的文本，遇到藏文之外的不同语种文本，除了用空格分词外不进行分词而保持原样
            if (prevCharUnicodeBlock != null && currCharUnicodeBlock != prevCharUnicodeBlock) {
                inputBufferPos -= 1;
                break;
            }

            // 遇到分词符：英文间隔符' '
            if (currChar == ' ') {
                // 连续的空格，不立即分词
                if (nextChar == currChar) {
                    continue;
                }
                // 无token，不立即分词
                if (tokenLength == 0) {
                    continue;
                }
                // 有token，立即分词
                break;
            }
            // 下标/着重字符，忽略
            if (currChar == '༵') {
                continue;
            }

            // 遇到分词符：藏文间隔符'་'
            if (currChar == '་') {
                // 无token，不立即分词
                if (tokenLength == 0) {
                    continue;
                }
                break;
            }

            // 遇到分词符：藏文结束符'།'，此字符后面可能再出现多个空格或藏文结束符'།'
            if (currChar == '།') {
                addTokenChar(currChar);
                while (true) {
                    final int testChar = inputBuffer.get(inputBufferPos);
                    if (testChar == ' ' || testChar == currChar) {
                        addTokenChar(testChar);
                        inputBufferPos++;
                        continue;
                    }
                    break;
                }
                break;
            }

            addTokenChar(currChar);
            prevCharUnicodeBlock = currCharUnicodeBlock;
            //
            // 遇到藏文字符'༄'，则认为后续开始的可能是标题，需要结束当前部分
            if (nextChar == '༄') {
                break;
            }
            if (nextChar == '༔' || nextChar == 'ཿ') {
                break;
            }
            if (nextChar == '།' && Character.getType(currChar) != Character.getType(nextChar)) {
                break;
            }
        }

        if (tokenLength <= 0) {
            return false;
        }

        termAtt.setEmpty().append(new String(tokenBuffer, 0, tokenLength));
        offsetAtt.setOffset(startOffset, inputBufferPos);
        return true;
    }

    private void addTokenChar(int tokenChar) {
        if (tokenLength == tokenBuffer.length) {
            int newSize = tokenBuffer.length * 2;
            char[] newBuffer = new char[newSize];
            System.arraycopy(tokenBuffer, 0, newBuffer, 0, tokenLength);
            tokenBuffer = newBuffer;
        }
        tokenBuffer[tokenLength++] = (char) tokenChar;
    }

    public static boolean isTibetanCharacter(int c) {
        return c >= 'ༀ' && c <= '\u0FFF';
    }

    public static boolean isTibetanLetter(final int c) {
        return c >= 'ཀ' && c <= 'ྼ';
    }
}
