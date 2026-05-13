package com.genesis.recommend.dto;

public class DismissRecommendationRequest {

    private String hash;
    /**
     * True = user marked the card as helpful (kept it but acknowledged).
     * False (default) = dismissed outright. Both remove the card from
     * future GETs; the flag lets us tell helpful from ignored for analytics.
     */
    private boolean accepted;

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
