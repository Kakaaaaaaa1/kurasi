package id.kurasi.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tes rumus skor dan tier kelangkaan. Berjalan di JVM tanpa emulator. */
public class RarityTest {
    private static final long YEAR = (long) (365.25 * 24 * 3600 * 1000);

    @Test
    public void tierBoundaries() {
        assertEquals(Rarity.COMMON, Rarity.of(0));
        assertEquals(Rarity.COMMON, Rarity.of(49));
        assertEquals(Rarity.RARE, Rarity.of(50));
        assertEquals(Rarity.EPIC, Rarity.of(70));
        assertEquals(Rarity.LEGENDARY, Rarity.of(85));
        assertEquals(Rarity.LEGENDARY, Rarity.of(100));
    }

    @Test
    public void scoreStaysInRange() {
        long now = System.currentTimeMillis();
        int low = Rarity.score(0, "BEKAS", 0);
        int high = Rarity.score(1_000_000_000L, "BARU", now - 10 * YEAR);
        assertTrue(low >= 0 && low <= 100);
        assertEquals(100, high);
    }

    @Test
    public void higherValueNeverLowersScore() {
        long now = System.currentTimeMillis();
        int a = Rarity.score(100_000, "BAGUS", now);
        int b = Rarity.score(1_000_000, "BAGUS", now);
        int c = Rarity.score(2_500_000, "BAGUS", now);
        assertTrue(a <= b && b <= c);
    }

    @Test
    public void betterConditionRaisesScore() {
        long now = System.currentTimeMillis();
        assertTrue(Rarity.score(500_000, "BARU", now) > Rarity.score(500_000, "BEKAS", now));
    }

    @Test
    public void unknownConditionCountsAsWorn() {
        long now = System.currentTimeMillis();
        assertEquals(Rarity.score(500_000, "BEKAS", now), Rarity.score(500_000, "ASAL", now));
    }

    @Test
    public void maxScorePerTier() {
        assertEquals(49, Rarity.COMMON.maxScore());
        assertEquals(69, Rarity.RARE.maxScore());
        assertEquals(84, Rarity.EPIC.maxScore());
        assertEquals(100, Rarity.LEGENDARY.maxScore());
    }

    @Test
    public void overrideMinusOneKeepsAutomaticScore() {
        assertEquals(30, Rarity.withOverride(30, -1));
        assertEquals(92, Rarity.withOverride(92, -1));
    }

    @Test
    public void overrideLiftsAndLowersIntoChosenTier() {
        // dinaikkan: skor 30 dipaksa Legendary
        int up = Rarity.withOverride(30, Rarity.LEGENDARY.ordinal());
        assertEquals(Rarity.LEGENDARY, Rarity.of(up));
        // diturunkan: skor 92 dipaksa Common
        int down = Rarity.withOverride(92, Rarity.COMMON.ordinal());
        assertEquals(Rarity.COMMON, Rarity.of(down));
        // skor yang sudah di dalam tier tidak berubah
        assertEquals(75, Rarity.withOverride(75, Rarity.EPIC.ordinal()));
    }

    @Test
    public void invalidOverrideIsIgnored() {
        assertEquals(40, Rarity.withOverride(40, 99));
    }
}
