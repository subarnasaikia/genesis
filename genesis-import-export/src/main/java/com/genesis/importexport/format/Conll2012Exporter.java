package com.genesis.importexport.format;

import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.dto.ExportOptions.Column2Mode;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import java.util.List;
import java.util.Map;

/**
 * Exporter for CoNLL-2012 format.
 *
 * <p>
 * Exports tokens and sentences to CoNLL-2012 format with configurable options:
 * <ul>
 * <li>Column 2 can be part number or sentence number</li>
 * <li>Coreference annotations in the last column</li>
 * </ul>
 */
public class Conll2012Exporter {

    private static final String PLACEHOLDER = "-";
    private static final String NER_PLACEHOLDER = "*";
    private static final String NO_COREF = "-";

    /**
     * Export a single document to CoNLL-2012 format.
     *
     * @param documentName     name of the document
     * @param sentences        list of sentences
     * @param tokensBySentence map of sentence index to tokens
     * @param corefAnnotations map of (sentenceIndex, tokenIndex) -> coref string
     * @param options          export options
     * @param sentenceOffset   offset for sentence numbering (for continuous
     *                         numbering)
     * @return CoNLL-2012 formatted string
     */
    public String export(String documentName,
            List<SentenceEntity> sentences,
            Map<Integer, List<TokenEntity>> tokensBySentence,
            Map<String, String> corefAnnotations,
            ExportOptions options,
            int sentenceOffset) {
        StringBuilder sb = new StringBuilder();

        // Document header
        sb.append("#begin document (").append(sanitizeDocName(documentName)).append("); part 000\n");

        for (SentenceEntity sentence : sentences) {
            List<TokenEntity> tokens = tokensBySentence.get(sentence.getSentenceIndex());
            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            // Calculate column 2 value based on mode
            int column2Value;
            if (options.getColumn2Mode() == Column2Mode.SENTENCE_NUMBER) {
                column2Value = sentence.getSentenceIndex() + sentenceOffset;
            } else {
                column2Value = options.getDefaultPartNumber();
            }

            // Export each token
            for (TokenEntity token : tokens) {
                String line = formatTokenLine(
                        sanitizeDocName(documentName),
                        column2Value,
                        token,
                        corefAnnotations);
                sb.append(line).append("\n");
            }

            // Blank line between sentences
            sb.append("\n");
        }

        // Document footer
        sb.append("#end document\n");

        return sb.toString();
    }

    /**
     * Export multiple documents merged into one CoNLL-2012 file.
     *
     * @param documents list of document data
     * @param options   export options
     * @return merged CoNLL-2012 formatted string
     */
    public String exportMerged(List<DocumentExportData> documents, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        int sentenceOffset = 0;

        for (DocumentExportData doc : documents) {
            String exported = export(
                    doc.documentName,
                    doc.sentences,
                    doc.tokensBySentence,
                    doc.corefAnnotations,
                    options,
                    options.isContinueSentenceNumbers() ? sentenceOffset : 0);
            sb.append(exported);

            // Update offset for next document
            if (options.isContinueSentenceNumbers() && !doc.sentences.isEmpty()) {
                sentenceOffset += doc.sentences.size();
            }
        }

        return sb.toString();
    }

    /**
     * Format a single token line.
     */
    private String formatTokenLine(String docName, int column2, TokenEntity token,
            Map<String, String> corefAnnotations) {
        // Get coreference annotation for this token
        String corefKey = token.getSentenceIndex() + "-" + token.getTokenIndex();
        String coref = corefAnnotations != null ? corefAnnotations.getOrDefault(corefKey, NO_COREF) : NO_COREF;

        // Build 12-column line
        // Columns: DocID, Part/Sent, WordNum, Word, POS, Parse, Lemma, Frame, Sense,
        // Speaker, NER, Coref
        return String.join("\t",
                docName, // 1. Document ID
                String.valueOf(column2), // 2. Part/Sentence number
                String.valueOf(token.getTokenIndex()), // 3. Word number (0-based in sentence)
                token.getForm(), // 4. Word
                emptyToPlaceholder(token.getPos()), // 5. POS tag
                NER_PLACEHOLDER, // 6. Parse bit
                emptyToPlaceholder(token.getLemma()), // 7. Predicate lemma
                PLACEHOLDER, // 8. Predicate frameset
                PLACEHOLDER, // 9. Word sense
                PLACEHOLDER, // 10. Speaker
                emptyToPlaceholder(token.getNerTag()), // 11. Named entity
                coref // 12. Coreference
        );
    }

    private String sanitizeDocName(String name) {
        if (name == null) {
            return "document";
        }
        // Remove file extension and special characters
        return name.replaceAll("\\.[^.]+$", "")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String emptyToPlaceholder(String value) {
        return (value == null || value.isEmpty()) ? PLACEHOLDER : value;
    }

    /**
     * Data container for document export.
     */
    public static class DocumentExportData {
        public String documentName;
        public List<SentenceEntity> sentences;
        public Map<Integer, List<TokenEntity>> tokensBySentence;
        public Map<String, String> corefAnnotations;

        public DocumentExportData(String documentName,
                List<SentenceEntity> sentences,
                Map<Integer, List<TokenEntity>> tokensBySentence,
                Map<String, String> corefAnnotations) {
            this.documentName = documentName;
            this.sentences = sentences;
            this.tokensBySentence = tokensBySentence;
            this.corefAnnotations = corefAnnotations;
        }
    }
}
