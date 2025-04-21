package org.appxi.lucene.bo;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.appxi.lucene.TokenInfo;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TibetanWordFilter extends TokenFilter {
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    private final Deque<TokenInfo> buffer = new ArrayDeque<>();
    private final Trie trie;

    public TibetanWordFilter(TokenStream input, Trie trie) {
        super(input);
        this.trie = trie;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        // 填充缓冲区
        fillBuffer();

        // 尝试从缓冲区中匹配最长的词组
        List<TokenInfo> matched = trie.matchLongest(buffer);

        if (matched != null) {
            TokenInfo first = buffer.peekFirst();
            int startOffset = first.startOffset();
            int endOffset = removeMatched(matched).endOffset();

            offsetAttr.setOffset(startOffset, endOffset);
            termAttr.setEmpty().append(String.join("་", matched.stream().map(TokenInfo::term).toList()));
            typeAttr.setType("phrase");

            return true;
        } else if (!buffer.isEmpty()) {
            // 无法匹配，输出缓冲区的第一个单元词
            TokenInfo first = buffer.removeFirst();
            int startOffset = first.startOffset();
            int endOffset = first.endOffset();

            offsetAttr.setOffset(startOffset, endOffset);
            termAttr.setEmpty().append(first.term());

            return true;
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        buffer.clear();
    }

    // 填充缓冲区
    private void fillBuffer() throws IOException {
        while (buffer.size() < trie.maxDepth && input.incrementToken()) {
//            String token = trimAttr.term();
            String term = termAttr.toString();
            int startOffset = offsetAttr.startOffset();
            int endOffset = offsetAttr.endOffset();
            buffer.addLast(new TokenInfo(term, startOffset, endOffset));
        }
    }

    private TokenInfo removeMatched(List<TokenInfo> matched) {
        TokenInfo lastToken = null;
        for (int i = 0; i < matched.size(); i++) {
            lastToken = buffer.removeFirst();
        }
        return lastToken;
    }

    public static class Trie {
        private final TrieNode root = new TrieNode();
        private int maxDepth = 0;

        public void insert(String phrase) {
            this.insert(phrase.split("་"));
        }

        public void insert(String[] phraseWords) {
            maxDepth = Math.max(maxDepth, phraseWords.length);
            TrieNode current = root;
            for (String word : phraseWords) {
                current = current.children.computeIfAbsent(word, k -> new TrieNode());
            }
            current.isEndOfPhrase = true;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        private List<TokenInfo> matchLongest(Deque<TokenInfo> buffer) {
            List<TokenInfo> longestMatched = null, currentMatched = new ArrayList<>();
            TrieNode current = root;

            for (TokenInfo tokenInfo : buffer) {
                current = current.children.get(tokenInfo.term());
                if (current == null) {
                    break; // 如果当前单词不在子节点中，停止匹配
                }
                currentMatched.add(tokenInfo); // 添加到当前匹配结果

                // 如果当前节点是结束节点，更新最长匹配结果
                if (current.isEndOfPhrase) {
                    longestMatched = new ArrayList<>(currentMatched); // 更新最长匹配
                }
            }

            // 返回最长匹配结果（如果没有匹配到则返回空）
            return longestMatched;
        }

    }

    private static class TrieNode {
        final Map<String, TrieNode> children = new HashMap<>();
        boolean isEndOfPhrase = false;
    }
}
