package com.genesis.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Deployment-level storage configuration.
 *
 * <p>
 * Two orthogonal knobs:
 * <ul>
 *   <li>{@link #provider} — <b>where</b> raw uploaded files live
 *       ({@code cloudinary} or {@code local}).</li>
 *   <li>{@link #retainSource} — <b>whether</b> the raw source file is kept after
 *       a document is tokenized. Everything except re-tokenization reads from the
 *       database token rows, so the source can be reclaimed once tokenization
 *       succeeds.</li>
 * </ul>
 *
 * <p>Prefixed with {@code genesis.storage} and bound from
 * {@code application.properties} / environment variables.
 */
@ConfigurationProperties(prefix = "genesis.storage")
public class StorageProperties {

    /** Cloudinary-backed object storage (default). */
    public static final String PROVIDER_CLOUDINARY = "cloudinary";

    /** Local-filesystem storage. */
    public static final String PROVIDER_LOCAL = "local";

    /**
     * Active storage provider: {@code cloudinary} (default) or {@code local}.
     */
    private String provider = PROVIDER_CLOUDINARY;

    /**
     * Keep the raw source file after successful tokenization. When {@code false},
     * the source is deleted once {@code DocumentTokenizedEvent} fires (re-tokenize
     * from the original is then unavailable).
     */
    private boolean retainSource = true;

    /**
     * Settings used only when {@link #provider} is {@code local}.
     */
    private final Local local = new Local();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isRetainSource() {
        return retainSource;
    }

    public void setRetainSource(boolean retainSource) {
        this.retainSource = retainSource;
    }

    public Local getLocal() {
        return local;
    }

    /**
     * Local-filesystem backend settings.
     */
    public static class Local {

        /**
         * Base directory under which uploaded files are written. Relative paths
         * resolve against the application's working directory.
         */
        private String basePath = "./data/uploads";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
