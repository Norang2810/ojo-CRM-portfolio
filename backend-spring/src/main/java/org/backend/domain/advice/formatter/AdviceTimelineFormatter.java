package org.backend.domain.advice.formatter;

import org.backend.domain.advice.entity.Advice;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdviceTimelineFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final NumberFormat NUMBER_FORMAT =
            NumberFormat.getNumberInstance(Locale.KOREA);


    /**
     * 예:
     * 연체 납부 확인(납부/연체/미납): base_month=202501, due=2025-01-05, billed=86500, paid_at=2025-01-13T20:00:06
     */
    private static final Pattern PAYMENT_CONFIRM_PATTERN = Pattern.compile(
            "^(.*?):\\s*base_month=(\\d{6}),\\s*due=([0-9-]+),\\s*billed=(\\d+),\\s*paid_at=([0-9T:-]+)\\s*(?:/\\s*(.*))?$"
    );

    /**
     * 예:
     * 미납 금액 안내(납부/연체/미납): base_month=202501, due=2025-01-05, billed=86500, overdue=86500
     */
    private static final Pattern OVERDUE_AMOUNT_PATTERN = Pattern.compile(
            "^(.*?):\\s*base_month=(\\d{6}),\\s*due=([0-9-]+),\\s*billed=(\\d+),\\s*overdue=(\\d+)\\s*(?:/\\s*(.*))?$"
    );

    /**
     * 예:
     * 요금 납부 확인(청구/납부): billed=86500, paid_at=2025-01-13T20:00:06 / 5G 프리미어(CALL)
     */
    private static final Pattern BILLING_PAID_PATTERN = Pattern.compile(
            "^(.*?):\\s*billed=(\\d+),\\s*paid_at=([0-9T:-]+)\\s*(?:/\\s*(.*))?$"
    );

    private static final Pattern OVERDUE_RENOTICE_PATTERN = Pattern.compile(
            "^(.*?):\\s*base_month=([0-9-]+),\\s*overdue=([0-9,]+원?),\\s*(.*)$"
    );

    public String formatDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }

        return switch (direction.toUpperCase()) {
            case "IN" -> "인바운드";
            case "OUT" -> "아웃바운드";
            default -> direction;
        };
    }

    public String formatContent(Advice advice) {
        String raw = advice.getAdviceContent();
        if (raw == null || raw.isBlank()) {
            return "상담 내용이 없습니다.";
        }

        String normalized = normalizeChannelText(raw);

        String formatted = tryFormatPaymentConfirmContent(normalized);
        if (formatted != null) {
            return removeTrailingProductInfo(formatted);
        }

        formatted = tryFormatOverdueAmountContent(normalized);
        if (formatted != null) {
            return removeTrailingProductInfo(formatted);
        }

        formatted = tryFormatBillingPaidContent(normalized);
        if (formatted != null) {
            return removeTrailingProductInfo(formatted);
        }

        formatted = tryFormatOverdueRenoticeContent(normalized);
        if (formatted != null) {
            return removeTrailingProductInfo(formatted);
        }

        formatted = tryFormatGenericKeyValueContent(normalized);
        if (formatted != null) {
            return removeTrailingProductInfo(formatted);
        }

        return removeTrailingProductInfo(normalized);
    }

    public Long formatSatisfactionScore(Long satisfactionScore) {
        if (satisfactionScore == null || satisfactionScore == 0) {
            return 0L;
        }
        if (satisfactionScore < 1) {
            return 1L;
        }
        if (satisfactionScore > 5) {
            return 5L;
        }
        return satisfactionScore;
    }

    private String tryFormatPaymentConfirmContent(String raw) {
        Matcher matcher = PAYMENT_CONFIRM_PATTERN.matcher(raw);

        if (!matcher.matches()) {
            return null;
        }

        String title = matcher.group(1).trim();
        String baseMonth = formatBaseMonth(matcher.group(2));
        String dueDate = matcher.group(3).trim();
        String billed = formatAmount(matcher.group(4));
        String paidAt = formatDateTime(matcher.group(5));
        String trailingInfo = matcher.group(6);

        String result = String.format(
                "%s: %s 청구금액 %s이 납기일(%s) 이후 %s에 납부되었습니다.",
                title, baseMonth, billed, dueDate, paidAt
        );

        return appendTrailingInfo(result, trailingInfo);
    }

    private String tryFormatOverdueAmountContent(String raw) {
        Matcher matcher = OVERDUE_AMOUNT_PATTERN.matcher(raw);

        if (!matcher.matches()) {
            return null;
        }

        String title = matcher.group(1).trim();
        String baseMonth = formatBaseMonth(matcher.group(2));
        String dueDate = matcher.group(3).trim();
        String billed = formatAmount(matcher.group(4));
        String overdue = formatAmount(matcher.group(5));
        String trailingInfo = matcher.group(6);

        String result = String.format(
                "%s: %s 기준 청구금액은 %s이며, 납기일(%s) 이후 미납금액은 %s입니다.",
                title, baseMonth, billed, dueDate, overdue
        );

        return appendTrailingInfo(result, trailingInfo);
    }

    private String tryFormatBillingPaidContent(String raw) {
        Matcher matcher = BILLING_PAID_PATTERN.matcher(raw);

        if (!matcher.matches()) {
            return null;
        }

        String title = matcher.group(1).trim();
        String billed = formatAmount(matcher.group(2));
        String paidAt = formatDateTime(matcher.group(3));
        String trailingInfo = matcher.group(4);

        String result = String.format(
                "%s: %s이 %s에 납부되었습니다.",
                title, billed, paidAt
        );

        return appendTrailingInfo(result, trailingInfo);
    }

    /**
     * 정규식에 딱 맞지 않더라도
     * base_month, billed, due, paid_at, overdue 같은 키가 들어있으면
     * 최소한 사람이 읽을 수 있게 약식 포맷팅
     */
    private String tryFormatGenericKeyValueContent(String raw) {
        if (!containsAny(raw, "base_month=", "billed=", "due=", "paid_at=", "overdue=")) {
            return null;
        }

        int colonIndex = raw.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }

        String title = raw.substring(0, colonIndex).trim();
        String body = raw.substring(colonIndex + 1).trim();

        String formattedBody = body;
        formattedBody = replaceBaseMonth(formattedBody);
        formattedBody = replaceMoneyKey(formattedBody, "billed");
        formattedBody = replaceMoneyKey(formattedBody, "overdue");
        formattedBody = replaceDateTimeKey(formattedBody, "paid_at");

        return title + ": " + formattedBody;
    }

    private String tryFormatOverdueRenoticeContent(String raw) {
        Matcher matcher = OVERDUE_RENOTICE_PATTERN.matcher(raw);

        if (!matcher.matches()) {
            return null;
        }

        String title = matcher.group(1).trim();
        String baseMonth = matcher.group(2).trim();
        String overdue = matcher.group(3).trim();
        String guideText = matcher.group(4).trim();

        String result = String.format(
                "%s: %s 기준 미납금액은 %s이며, %s했습니다.",
                title, baseMonth, overdue, guideText
        );

        return result;
    }


    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeChannelText(String raw) {
        return raw.replace("(CALL)", "(전화)")
                .replace("(SMS)", "(문자)")
                .replace("(APP)", "(앱)");
    }

    private String appendTrailingInfo(String base, String trailingInfo) {
        if (trailingInfo == null || trailingInfo.isBlank()) {
            return base;
        }
        return base + " / " + trailingInfo.trim();
    }

    private String replaceBaseMonth(String content) {
        Pattern pattern = Pattern.compile("base_month=(\\d{4})(\\d{2})");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replaced = "base_month=" + matcher.group(1) + "-" + matcher.group(2);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String replaceMoneyKey(String content, String key) {
        Pattern pattern = Pattern.compile(key + "=(\\d+)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replaced = key + "=" + formatAmount(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String replaceDateTimeKey(String content, String key) {
        Pattern pattern = Pattern.compile(key + "=([0-9T:-]+)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replaced = key + "=" + formatDateTime(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String formatAmount(String amount) {
        try {
            long value = Long.parseLong(amount);
            return NUMBER_FORMAT.format(value) + "원";
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private String formatDateTime(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value);
            return dateTime.format(DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return value;
        }
    }

    private String formatBaseMonth(String baseMonth) {
        if (baseMonth == null || baseMonth.length() != 6) {
            return baseMonth;
        }
        return baseMonth.substring(0, 4) + "-" + baseMonth.substring(4, 6);
    }

    private String removeTrailingProductInfo(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        return content.replaceFirst("\\s*/\\s*[^/]+\\((전화|문자|앱)\\)\\s*$", "").trim();
    }
}