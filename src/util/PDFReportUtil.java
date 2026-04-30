package util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class PDFReportUtil {
    private PDFReportUtil() {
    }

    public static void writeReport(Path path, String username, Map<String, Integer> analytics, Map<String, List<String>> sections) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 760;
                y = writeLine(cs, "Asthipathra - User Full Report", 18, y);
                y = writeLine(cs, "User: " + username, 12, y - 8);
                y = writeLine(cs, "Generated: " + DateTimeUtil.now(), 12, y - 4);
                y -= 6;
                y = writeLine(cs, "Analytics Summary", 14, y);
                for (Map.Entry<String, Integer> e : analytics.entrySet()) {
                    y = writeLine(cs, e.getKey() + ": " + e.getValue(), 11, y);
                }

                for (Map.Entry<String, List<String>> section : sections.entrySet()) {
                    y -= 8;
                    if (y < 80) break;
                    y = writeLine(cs, section.getKey(), 13, y);
                    if (section.getValue().isEmpty()) {
                        y = writeLine(cs, "- No records", 11, y);
                    } else {
                        for (String row : section.getValue()) {
                            if (y < 80) break;
                            y = writeLine(cs, "- " + row, 10, y);
                        }
                    }
                }
            }
            doc.save(path.toFile());
        }
    }

    private static float writeLine(PDPageContentStream cs, String text, int size, float y) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, size);
        cs.newLineAtOffset(45, y);
        cs.showText(safe(text));
        cs.endText();
        return y - (size + 5);
    }

    private static String safe(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
