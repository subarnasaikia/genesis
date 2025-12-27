package com.genesis.importexport.dto;

/**
 * Export options for CoNLL-2012 format.
 */
public class ExportOptions {

    /**
     * Mode for Column 2 in CoNLL-2012 output.
     */
    private Column2Mode column2Mode = Column2Mode.PART_NUMBER;

    /**
     * Export format for multi-file workspaces.
     */
    private ExportFormat exportFormat = ExportFormat.SEPARATE_FILES_ZIP;

    /**
     * Whether to continue sentence numbers across files (only for SENTENCE_NUMBER
     * mode).
     */
    private boolean continueSentenceNumbers = true;

    /**
     * Default part number to use when column2Mode is PART_NUMBER.
     */
    private int defaultPartNumber = 0;

    // Getters and Setters

    public Column2Mode getColumn2Mode() {
        return column2Mode;
    }

    public void setColumn2Mode(Column2Mode column2Mode) {
        this.column2Mode = column2Mode;
    }

    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(ExportFormat exportFormat) {
        this.exportFormat = exportFormat;
    }

    public boolean isContinueSentenceNumbers() {
        return continueSentenceNumbers;
    }

    public void setContinueSentenceNumbers(boolean continueSentenceNumbers) {
        this.continueSentenceNumbers = continueSentenceNumbers;
    }

    public int getDefaultPartNumber() {
        return defaultPartNumber;
    }

    public void setDefaultPartNumber(int defaultPartNumber) {
        this.defaultPartNumber = defaultPartNumber;
    }

    /**
     * Mode for Column 2 in CoNLL-2012 format.
     */
    public enum Column2Mode {
        /**
         * Use part number (standard CoNLL-2012).
         */
        PART_NUMBER,

        /**
         * Use sentence number (Genesis custom).
         */
        SENTENCE_NUMBER
    }

    /**
     * Export format for multi-file workspaces.
     */
    public enum ExportFormat {
        /**
         * Merge all files into a single .conll file.
         */
        MERGED_SINGLE_FILE,

        /**
         * Export each file as separate .conll in a ZIP archive.
         */
        SEPARATE_FILES_ZIP
    }
}
