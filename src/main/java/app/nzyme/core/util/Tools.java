/*
 * This file is part of nzyme.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

package app.nzyme.core.util;

import app.nzyme.core.taps.Tap;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import jakarta.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {

    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9-_]+$");
    private static final Pattern VALID_MAC = Pattern.compile("^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$");

    public static boolean isSafeNodeName(String x) {
        if (x == null) {
            return false;
        }

        if (x.trim().isEmpty()) {
            return false;
        }

        return x.length() < 255 && SAFE_ID.matcher(x).matches();
    }

    public static boolean isValidMacAddress(String addr) {
        if (Strings.isNullOrEmpty(addr)) {
            return false;
        }

        Matcher m = VALID_MAC.matcher(addr);
        return m.find();
    }

    public static String durationToHumanReadable(Duration duration) {
        if (duration.getStandardSeconds() == 0) {
            return "n/a";
        }

        if (duration.getStandardSeconds() < 60) {
            return duration.getStandardSeconds() + " seconds";
        }

        if (duration.getStandardHours() < 2) {
            return duration.getStandardMinutes() + " minutes";
        }

        if (duration.getStandardDays() <= 3) {
            return duration.getStandardHours() + " hours";
        }

        return duration.getStandardDays() + " days";
    }

    public static String sanitizeSSID(@NotNull String ssid) {
        return ssid.replaceAll("\0", "") // NULL bytes
                .replaceAll("\\p{C}", "") // invisible control characters and unused code points.
                .replaceAll("\\p{Zl}", "") // line separator character U+2028
                .replaceAll("\\p{Zp}", "") // paragraph separator character U+2029
                .replaceAll("\t*", "") // Tabs
                .replaceAll(" +", " ") // Multiple whitespaces in succession
                .trim();
    }

    public static boolean isTapActive(DateTime lastReport) {
        return lastReport != null && lastReport.isAfter(DateTime.now().minusMinutes(2));
    }

    public static float round(float f, int decimalPlaces) {
        BigDecimal bd = new BigDecimal(Float.toString(f));
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static String buildTcpSessionKey(DateTime sessionEstablishedAt,
                                            String sourceAddress,
                                            String destinationAddress,
                                            int sourcePort,
                                            int destinationPort) {
        return Hashing.sha256()
                .hashString(sessionEstablishedAt.getMillis()
                        + sourceAddress
                        + destinationAddress
                        + sourcePort
                        + destinationPort, Charsets.UTF_8)
                .toString();
    }

    public static InetAddress stringtoInetAddress(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            // This shouldn't happen because we pass IP addresses.
            throw new RuntimeException(e);
        }
    }

}
