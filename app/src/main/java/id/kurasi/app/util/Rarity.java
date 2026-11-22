package id.kurasi.app.util;

/**
 * Tier kelangkaan kartu. Skor 0..100 dihitung dari tiga hal:
 * nilai (bobot 60%), kondisi (30%), dan lama dimiliki (10%).
 */
public enum Rarity {
    COMMON("COMMON", Neo.COMMON, 0),
    RARE("RARE", Neo.CYAN, 50),
    EPIC("EPIC", Neo.LAVENDER, 70),
    LEGENDARY("LEGENDARY", Neo.LIME, 85);

    public final String label;
    public final int color;
    public final int minScore;

    Rarity(String label, int color, int minScore) {
        this.label = label;
        this.color = color;
        this.minScore = minScore;
    }

    /** Skor tertinggi yang masih masuk tier ini. */
    public int maxScore() {
        Rarity[] all = values();
        return ordinal() == all.length - 1 ? 100 : all[ordinal() + 1].minScore - 1;
    }

    /**
     * Terapkan tier buatan pengguna dengan menjepit skor ke rentang tier itu, sehingga warna kartu,
     * statistik, dan urutan semuanya ikut tanpa perubahan lain. override -1 berarti otomatis.
     */
    public static int withOverride(int score, int override) {
        if (override < 0 || override >= values().length) return score;
        Rarity t = values()[override];
        return Math.max(t.minScore, Math.min(t.maxScore(), score));
    }

    public static Rarity of(int score) {
        Rarity r = COMMON;
        for (Rarity t : values()) if (score >= t.minScore) r = t;
        return r;
    }

    public static int score(long value, String condition, long acquiredAt) {
        // Nilai: Rp 50 rb -> 0, Rp 2,5 jt -> 100 (skala logaritmik)
        double priceScore = 0;
        if (value > 50_000) {
            priceScore = Math.log10(value / 50_000.0) / Math.log10(50) * 100;
            priceScore = Math.min(100, priceScore);
        }
        double condScore;
        switch (condition == null ? "" : condition) {
            case "BARU": condScore = 100; break;
            case "MULUS": condScore = 85; break;
            case "BAGUS": condScore = 65; break;
            default: condScore = 40;
        }
        double years = acquiredAt <= 0 ? 0
                : (System.currentTimeMillis() - acquiredAt) / (365.25 * 24 * 3600 * 1000);
        double ageScore = Math.max(0, Math.min(100, years * 25));
        return (int) Math.round(0.6 * priceScore + 0.3 * condScore + 0.1 * ageScore);
    }
}
