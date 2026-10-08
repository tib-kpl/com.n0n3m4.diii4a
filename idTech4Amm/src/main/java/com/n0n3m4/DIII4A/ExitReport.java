package com.n0n3m4.DIII4A;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationExitInfo;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.karin.idTech4Amm.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Says why the app's process ended when nobody asked it to (a crash of a game, the system killing it
 * for memory...): Android keeps the reason of each process exit. Checked when the launcher comes back,
 * shown once, and written in the game data folder (exit_report.txt) to be read from a computer. For a
 * native crash, Android also keeps a dump of it (a "tombstone"): its readable words name the signal,
 * the libraries and the functions on the stack, which is what a crash of an engine needs.
 */
public final class ExitReport
{
    private static final String PREF_LAST_SEEN = "exit_report_last_seen";
    private static final int MAXIMUM_LINES = 1200;

    private ExitReport() {}

    private static String ReasonName(int reason)
    {
        switch (reason)
        {
            case 1: return "EXIT_SELF";
            case 2: return "SIGNALED";
            case 3: return "LOW_MEMORY";
            case 4: return "CRASH (Java)";
            case 5: return "CRASH_NATIVE";
            case 6: return "ANR";
            case 7: return "INITIALIZATION_FAILURE";
            case 8: return "PERMISSION_CHANGE";
            case 9: return "EXCESSIVE_RESOURCE_USAGE";
            case 10: return "USER_REQUESTED";
            case 11: return "USER_STOPPED";
            case 12: return "DEPENDENCY_DIED";
            case 13: return "OTHER";
            case 14: return "FREEZER";
            case 15: return "PACKAGE_STATE_CHANGE";
            case 16: return "PACKAGE_UPDATED";
            default: return "reason " + reason;
        }
    }

    /** The readable words of the crash dump, without the lines about the system's own libraries. */
    private static String TombstoneWords(ApplicationExitInfo info)
    {
        StringBuilder out = new StringBuilder("\n--- crash dump (readable words) ---\n");
        try (InputStream in = info.getTraceInputStream())
        {
            if (in == null)
                return out.append("(Android kept no crash dump)\n").toString();
            byte[] data = new byte[1 << 20];
            int length = 0;
            int read;
            while (length < data.length && (read = in.read(data, length, data.length - length)) > 0)
                length += read;
            StringBuilder word = new StringBuilder();
            String previous = "";
            int lines = 0;
            for (int i = 0; i <= length && lines < MAXIMUM_LINES; i++)
            {
                int c = i < length ? (data[i] & 0xFF) : 0;
                if (c >= 32 && c < 127)
                {
                    word.append((char) c);
                    continue;
                }
                String text = word.toString();
                word.setLength(0);
                if (text.length() < 6 || text.equals(previous))
                    continue;
                if (text.startsWith("[anon:") || text.startsWith("/apex/") || text.startsWith("/dev/")
                        || text.startsWith("/memfd") || text.startsWith("/system/framework")
                        || text.contains("dalvik") || text.contains("libart.so") || text.contains("libc.so"))
                    continue;
                previous = text;
                out.append(text).append('\n');
                lines++;
            }
        }
        catch (Throwable t)
        {
            out.append("(could not read it: ").append(t).append(")\n");
        }
        return out.toString();
    }

    /**
     * @param dataFolder where exit_report.txt is written (the game data folder)
     */
    public static void Check(Activity activity, String dataFolder)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            return;
        try
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            long seen = prefs.getLong(PREF_LAST_SEEN, 0);
            ActivityManager manager = (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(activity.getPackageName(), 0, 8);
            ApplicationExitInfo latest = null;
            for (ApplicationExitInfo info : exits)
            {
                if (activity.getPackageName().equals(info.getProcessName())
                        && (latest == null || info.getTimestamp() > latest.getTimestamp()))
                    latest = info;
            }
            if (latest == null || latest.getTimestamp() <= seen)
                return;
            prefs.edit().putLong(PREF_LAST_SEEN, latest.getTimestamp()).commit();
            int reason = latest.getReason();
            if (seen == 0 || reason == 1 || reason == 10 || reason == 11 || reason == 15 || reason == 16)
                return; // the first run, or an end nobody needs to hear about

            String text = "reason: " + ReasonName(reason) + "\n"
                    + "status: " + latest.getStatus() + "\n"
                    + "description: " + latest.getDescription() + "\n"
                    + "rss (kB): " + latest.getRss() + "  pss (kB): " + latest.getPss() + "\n"
                    + "time: " + new Date(latest.getTimestamp()) + "\n";
            if (reason == 5)
                text += TombstoneWords(latest);
            String where = "";
            try
            {
                File dir = new File(dataFolder);
                if (dir.isDirectory() || dir.mkdirs())
                {
                    File file = new File(dir, "exit_report.txt");
                    try (FileOutputStream out = new FileOutputStream(file))
                    {
                        out.write(text.getBytes(StandardCharsets.UTF_8));
                    }
                    where = file.getAbsolutePath();
                }
            }
            catch (Exception ignored) {}
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.exit_report_title)
                    .setMessage(activity.getString(R.string.exit_report_message, ReasonName(reason),
                            String.valueOf(latest.getDescription()), where))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        catch (Throwable ignored)
        {
        }
    }
}
