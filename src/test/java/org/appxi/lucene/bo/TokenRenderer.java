package org.appxi.lucene.bo;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public interface TokenRenderer {

    /**
     * 渲染 TokenStream 为 HTML，保留分隔符
     *
     * @param tokenStream    输入的 TokenStream
     * @param inputText      原始输入文本
     * @param termClass      词组的 CSS 类名
     * @param delimiterClass 分隔符的 CSS 类名
     * @return 渲染后的 HTML 字符串
     */
    static String renderToHtml(TokenStream tokenStream, String inputText, String termClass, String delimiterClass) throws IOException {
        // 获取 TokenStream 的属性
        CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
        OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
        TypeAttribute typeAttr = tokenStream.addAttribute(TypeAttribute.class);

        StringBuilder htmlBuilder = new StringBuilder();
        int lastEndOffset = 0; // 上一个词组的结束位置

        // 重置 TokenStream
        tokenStream.reset();

        while (tokenStream.incrementToken()) {
            String term = termAttr.toString();
            int startOffset = offsetAttr.startOffset();
            int endOffset = offsetAttr.endOffset();

            if (endOffset - startOffset > term.length()) {
                term = inputText.substring(startOffset, endOffset);
            }

            // 处理当前词之前的分隔符
            if (lastEndOffset < startOffset) {
                String delimiter = inputText.substring(lastEndOffset, startOffset);
                htmlBuilder.append("<span class=\"").append(delimiterClass).append("\">")
                    .append(escapeHtml(delimiter))
                    .append("</span>");
            }

            // 包裹当前 term
            htmlBuilder.append("<span class=\"").append(termClass)
                .append(" ").append(typeAttr.type())
                .append("\">")
                .append(escapeHtml(term))
                .append("</span>");

            // 更新上一个词组的结束位置
            lastEndOffset = endOffset;
        }

        // 处理剩余未被包裹的部分（包括分隔符）
        if (lastEndOffset < inputText.length()) {
            String remainingText = inputText.substring(lastEndOffset);
            htmlBuilder.append("<span class=\"").append(delimiterClass).append("\">")
                .append(escapeHtml(remainingText))
                .append("</span>");
        }

        // 关闭 TokenStream
        tokenStream.end();
        tokenStream.close();

        return htmlBuilder.toString();
    }

    /**
     * 转义 HTML 特殊字符
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace(" ", "&nbsp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
