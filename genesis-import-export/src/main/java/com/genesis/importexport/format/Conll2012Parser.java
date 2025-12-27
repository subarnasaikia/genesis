package com.genesis.importexport.format;

import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for CoNLL-2012 format files.
 *
 * <p>
 * Parses CoNLL-2012 formatted text into Token and Sentence entities.
 * Handles coreference annotations in the last column.
 */
public class Conll2012Parser {

    private static final Pattern BEGIN_DOCUMENT = Pattern.compile("#begin document \\(([^)]+)\\);?\\s*part\\s+(\\d+)");
    private static final Pattern END_DOCUMENT = Pattern.compile("#end document");

    /**
     * Result of parsing a CoNLL-2012 file.
     */
    public static class ParseResult {
        private final String documentId;
        private final List<SentenceEntity> sentences;
        private final List<TokenEntity> tokens;
        private final Map<Integer, List<MentionSpan>> coreferenceChains;

        public ParseResult(String documentId, List<SentenceEntity> sentences,
                List<TokenEntity> tokens, Map<Integer, List<MentionSpan>> coreferenceChains) {
            this.documentId = documentId;
            this.sentences = sentences;
            this.tokens = tokens;
            this.coreferenceChains = coreferenceChains;
        }

        public String getDocumentId() {
            return documentId;
        }

        public List<SentenceEntity> getSentences() {
            return sentences;
        }

        public List<TokenEntity> getTokens() {
            return tokens;
        }

        public Map<Integer, List<MentionSpan>> getCoreferenceChains() {
            return coreferenceChains;
        }
    }

    /**
     * Represents a mention span from coreference annotation.
     */
    public static class MentionSpan {
        private final int sentenceIndex;
        private final int startTokenIndex;
        private final int endTokenIndex;
        private final int clusterId;

        public MentionSpan(int sentenceIndex, int startTokenIndex, int endTokenIndex, int clusterId) {
            this.sentenceIndex = sentenceIndex;
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
            this.clusterId = clusterId;
        }

        public int getSentenceIndex() {
            return sentenceIndex;
        }

        public int getStartTokenIndex() {
            return startTokenIndex;
        }

        public int getEndTokenIndex() {
            return endTokenIndex;
        }

        public int getClusterId() {
            return clusterId;
        }
    }

    /**
     * Parse CoNLL-2012 formatted content.
     *
     * @param content          the CoNLL-2012 file content
     * @param targetDocumentId the UUID to assign to tokens
     * @return parsed result with tokens, sentences, and coreference info
     */
    public ParseResult parse(String content, UUID targetDocumentId) throws IOException {
        List<SentenceEntity> sentences = new ArrayList<>();
        List<TokenEntity> tokens = new ArrayList<>();
        Map<Integer, List<MentionSpan>> coreferenceChains = new HashMap<>();

        // Track open mentions (clusterID -> startTokenIndex in current sentence)
        Map<Integer, Integer> openMentions = new HashMap<>();

        String documentId = "unknown";
        int currentSentenceIndex = -1;
        int globalTokenIndex = 0;
        int currentCharOffset = 0;

        List<TokenEntity> currentSentenceTokens = new ArrayList<>();
        StringBuilder currentSentenceText = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines (sentence boundary) but process sentence
                if (line.trim().isEmpty()) {
                    if (!currentSentenceTokens.isEmpty()) {
                        // Save current sentence
                        SentenceEntity sentence = createSentence(
                                targetDocumentId,
                                currentSentenceIndex,
                                currentSentenceText.toString().trim(),
                                currentSentenceTokens);
                        sentences.add(sentence);
                        tokens.addAll(currentSentenceTokens);

                        currentSentenceTokens = new ArrayList<>();
                        currentSentenceText = new StringBuilder();
                        openMentions.clear();
                    }
                    continue;
                }

                // Check for document markers
                Matcher beginMatcher = BEGIN_DOCUMENT.matcher(line);
                if (beginMatcher.find()) {
                    documentId = beginMatcher.group(1);
                    continue;
                }
                if (END_DOCUMENT.matcher(line).find()) {
                    continue;
                }

                // Skip comments
                if (line.startsWith("#")) {
                    continue;
                }

                // Parse token line
                String[] columns = line.split("\\s+");
                if (columns.length < 4) {
                    continue; // Invalid line
                }

                // Extract columns (partOrSentence in column[1] is intentionally not used as we
                // detect
                // sentence boundaries from tokenIndex == 0)
                @SuppressWarnings("unused")
                int partOrSentence = Integer.parseInt(columns[1]);
                int tokenIndex = Integer.parseInt(columns[2]);
                String word = columns[3];
                String pos = columns.length > 4 ? columns[4] : "-";
                String coref = columns[columns.length - 1]; // Last column

                // Check if new sentence
                if (tokenIndex == 0) {
                    if (!currentSentenceTokens.isEmpty()) {
                        // Save previous sentence
                        SentenceEntity sentence = createSentence(
                                targetDocumentId,
                                currentSentenceIndex,
                                currentSentenceText.toString().trim(),
                                currentSentenceTokens);
                        sentences.add(sentence);
                        tokens.addAll(currentSentenceTokens);

                        currentSentenceTokens = new ArrayList<>();
                        currentSentenceText = new StringBuilder();
                        openMentions.clear();
                    }
                    currentSentenceIndex++;
                }

                // Create token
                TokenEntity token = new TokenEntity();
                token.setDocumentId(targetDocumentId);
                token.setSentenceIndex(currentSentenceIndex);
                token.setTokenIndex(tokenIndex);
                token.setGlobalIndex(globalTokenIndex);
                token.setForm(word);
                token.setPos("-".equals(pos) ? null : pos);

                // Calculate offsets
                int startOffset = currentCharOffset;
                int endOffset = currentCharOffset + word.length();
                token.setStartOffset(startOffset);
                token.setEndOffset(endOffset);

                currentSentenceTokens.add(token);

                // Build sentence text
                if (currentSentenceText.length() > 0) {
                    currentSentenceText.append(" ");
                    currentCharOffset++; // Space
                }
                currentSentenceText.append(word);
                currentCharOffset = endOffset;

                // Parse coreference
                if (!"-".equals(coref) && !"*".equals(coref)) {
                    parseCoreference(coref, currentSentenceIndex, tokenIndex,
                            openMentions, coreferenceChains);
                }

                globalTokenIndex++;
            }
        }

        // Save last sentence
        if (!currentSentenceTokens.isEmpty()) {
            SentenceEntity sentence = createSentence(
                    targetDocumentId,
                    currentSentenceIndex,
                    currentSentenceText.toString().trim(),
                    currentSentenceTokens);
            sentences.add(sentence);
            tokens.addAll(currentSentenceTokens);
        }

        return new ParseResult(documentId, sentences, tokens, coreferenceChains);
    }

    private SentenceEntity createSentence(UUID documentId, int sentenceIndex,
            String text, List<TokenEntity> tokens) {
        SentenceEntity sentence = new SentenceEntity();
        sentence.setDocumentId(documentId);
        sentence.setSentenceIndex(sentenceIndex);
        sentence.setText(text);
        sentence.setTokenCount(tokens.size());

        if (!tokens.isEmpty()) {
            sentence.setStartOffset(tokens.get(0).getStartOffset());
            sentence.setEndOffset(tokens.get(tokens.size() - 1).getEndOffset());
        } else {
            sentence.setStartOffset(0);
            sentence.setEndOffset(0);
        }

        return sentence;
    }

    /**
     * Parse coreference annotation from the last column.
     * Formats: (N), (N, N), (N|M), etc.
     */
    private void parseCoreference(String coref, int sentenceIndex, int tokenIndex,
            Map<Integer, Integer> openMentions,
            Map<Integer, List<MentionSpan>> chains) {
        // Split by pipe for multiple annotations
        String[] annotations = coref.split("\\|");

        for (String annotation : annotations) {
            annotation = annotation.trim();
            if (annotation.isEmpty() || "-".equals(annotation)) {
                continue;
            }

            // Check for opening bracket (start of mention)
            if (annotation.startsWith("(")) {
                String numPart = annotation.substring(1);
                boolean closes = numPart.endsWith(")");
                if (closes) {
                    numPart = numPart.substring(0, numPart.length() - 1);
                }

                int clusterId = Integer.parseInt(numPart);

                if (closes) {
                    // Single-token mention (N)
                    MentionSpan span = new MentionSpan(sentenceIndex, tokenIndex, tokenIndex, clusterId);
                    chains.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(span);
                } else {
                    // Opens multi-token mention (N
                    openMentions.put(clusterId, tokenIndex);
                }
            }
            // Check for closing bracket (end of mention)
            else if (annotation.endsWith(")")) {
                String numPart = annotation.substring(0, annotation.length() - 1);
                int clusterId = Integer.parseInt(numPart);

                Integer startIndex = openMentions.remove(clusterId);
                if (startIndex != null) {
                    MentionSpan span = new MentionSpan(sentenceIndex, startIndex, tokenIndex, clusterId);
                    chains.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(span);
                }
            }
        }
    }
}
