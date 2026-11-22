package id.kurasi.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tes perbandingan versi dan pembacaan jawaban GitHub Releases. Berjalan di JVM tanpa emulator. */
public class UpdaterTest {
    @Test
    public void newerPatchMinorMajor() {
        assertTrue(Updater.isNewer("1.1", "1.0"));
        assertTrue(Updater.isNewer("1.0.1", "1.0"));
        assertTrue(Updater.isNewer("2.0", "1.9.9"));
        assertTrue(Updater.isNewer("1.10", "1.9")); // angka, bukan urutan huruf
    }

    @Test
    public void sameOrOlderIsNotNewer() {
        assertFalse(Updater.isNewer("1.1", "1.1"));
        assertFalse(Updater.isNewer("1.1.0", "1.1"));
        assertFalse(Updater.isNewer("1.0", "1.1"));
        assertFalse(Updater.isNewer("0.9.9", "1.0"));
    }

    @Test
    public void prefixAndSuffixAreIgnored() {
        assertTrue(Updater.isNewer("v1.2", "1.1"));
        assertFalse(Updater.isNewer("V1.1", "1.1"));
        assertTrue(Updater.isNewer("1.2-beta", "1.1"));
    }

    @Test
    public void garbageNeverCountsAsNewer() {
        assertFalse(Updater.isNewer("", "1.0"));
        assertFalse(Updater.isNewer("abc", "1.0"));
        assertFalse(Updater.isNewer(null, "1.0"));
    }

    private static final String RELEASE = "{\"tag_name\":\"v1.2\",\"body\":\"Perbaikan dock\","
            + "\"assets\":[{\"name\":\"notes.txt\",\"browser_download_url\":\"https://x/notes.txt\",\"size\":5},"
            + "{\"name\":\"Kurasi-v1.2.apk\",\"browser_download_url\":\"https://x/Kurasi-v1.2.apk\",\"size\":123456}]}";

    @Test
    public void parsePicksTheApkAsset() throws Exception {
        Updater.Info i = Updater.parse(RELEASE);
        assertNotNull(i);
        assertEquals("1.2", i.version);
        assertEquals("Perbaikan dock", i.notes);
        assertEquals("https://x/Kurasi-v1.2.apk", i.apkUrl);
        assertEquals(123456L, i.size);
    }

    @Test
    public void parseReturnsNullWithoutApk() throws Exception {
        assertNull(Updater.parse("{\"tag_name\":\"v1.2\",\"assets\":[]}"));
        assertNull(Updater.parse("{\"tag_name\":\"v1.2\"}"));
    }
}
