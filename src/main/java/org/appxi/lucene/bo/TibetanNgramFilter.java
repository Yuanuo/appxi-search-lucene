package org.appxi.lucene.bo;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

import java.io.IOException;
import java.util.*;

/**
 * A TokenFilter that generates N-gram phrases based on their frequency in the entire text.
 * Only phrases with a frequency above a specified threshold are emitted.
 */
public class TibetanNgramFilter extends TokenFilter {

    // Attributes for token processing
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);

    // Configuration parameters
    private final int minGram;
    private final int maxGram;
    private final int minFrequency;
    private final String sep;

    // Internal state
    private boolean isFirstPass = true; // Flag to indicate the first pass (frequency counting)
    private Map<String, Integer> phraseFrequencyMap = new HashMap<>(); // Store phrase frequencies
    private List<TokenInfo> tokenBuffer = new ArrayList<>(); // Buffer to store tokens with their offsets
    private Iterator<Map.Entry<String, Integer>> phraseIterator; // Iterator for emitting phrases
    private String currentPhrase; // Current phrase being processed
    private int currentPhraseStartIndex; // Start index of the current phrase

    public TibetanNgramFilter(TokenStream input, int minGram, int maxGram, int minFrequency, String sep) {
        super(input);
        if (minGram < 1 || maxGram < minGram || minFrequency < 1) {
            throw new IllegalArgumentException("Invalid minGram, maxGram, or minFrequency values");
        }
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.minFrequency = minFrequency;
        this.sep = sep;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (isFirstPass) {
            // First pass: Count phrase frequencies
            countPhraseFrequencies();
            isFirstPass = false;
            reset(); // Reset the input stream for the second pass
        }

        // Second pass: Emit phrases based on frequency
        while (true) {
            if (phraseIterator == null || !phraseIterator.hasNext()) {
                return false; // No more phrases to emit
            }

            if (currentPhrase == null) {
                // Get the next phrase from the iterator
                Map.Entry<String, Integer> entry = phraseIterator.next();
                currentPhrase = entry.getKey();
                if (entry.getValue() < minFrequency) {
                    currentPhrase = null;
                    continue; // Skip phrases below the frequency threshold
                }
            }

            // Emit the current phrase
            clearAttributes();
            termAtt.setEmpty().append(currentPhrase);
            TokenInfo firstToken = tokenBuffer.get(currentPhraseStartIndex);
            TokenInfo lastToken = tokenBuffer.get(currentPhraseStartIndex + currentPhrase.split(sep).length - 1);
            offsetAtt.setOffset(firstToken.startOffset, lastToken.endOffset);
            posIncAtt.setPositionIncrement(1);
            posLenAtt.setPositionLength(currentPhrase.split(sep).length);

            // Clear the current phrase and move to the next one
            currentPhrase = null;
            return true;
        }
    }

    /**
     * Counts the frequency of all possible N-gram phrases in the input stream.
     */
    private void countPhraseFrequencies() throws IOException {
        List<String> tokens = new ArrayList<>();
        List<Integer> startOffsets = new ArrayList<>();
        List<Integer> endOffsets = new ArrayList<>();

        // Read all tokens from the input stream
        while (input.incrementToken()) {
            tokens.add(termAtt.toString());
            startOffsets.add(offsetAtt.startOffset());
            endOffsets.add(offsetAtt.endOffset());
        }

        // Generate all possible N-gram phrases and count their frequencies
        for (int i = 0; i < tokens.size(); i++) {
            for (int n = minGram; n <= maxGram; n++) {
                if (i + n > tokens.size()) {
                    break; // Not enough tokens to form the phrase
                }
                StringBuilder phrase = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (j > 0) {
                        phrase.append(sep);
                    }
                    phrase.append(tokens.get(i + j));
                }
                phraseFrequencyMap.put(phrase.toString(), phraseFrequencyMap.getOrDefault(phrase.toString(), 0) + 1);
            }
        }

        // Prepare the token buffer for the second pass
        for (int i = 0; i < tokens.size(); i++) {
            tokenBuffer.add(new TokenInfo(tokens.get(i), startOffsets.get(i), endOffsets.get(i)));
        }

        // Initialize the phrase iterator
        phraseIterator = phraseFrequencyMap.entrySet().iterator();
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        if (!isFirstPass) {
            phraseIterator = phraseFrequencyMap.entrySet().iterator();
            currentPhrase = null;
        }
    }

    /**
     * Helper class to store token information with offsets.
     */
    private static class TokenInfo {
        String token;
        int startOffset;
        int endOffset;

        TokenInfo(String token, int startOffset, int endOffset) {
            this.token = token;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
