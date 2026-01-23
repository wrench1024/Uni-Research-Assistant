package com.uni.research.module.doc.service;

import com.uni.research.module.doc.entity.Document;
import org.springframework.stereotype.Service;

/**
 * Citation Generation Service
 * Generates academic citations in BibTeX and EndNote (RIS) formats
 */
@Service
public class CitationService {

    /**
     * Generate BibTeX citation
     */
    public String generateBibTeX(Document doc) {
        String citationKey = generateCitationKey(doc);

        StringBuilder bib = new StringBuilder();
        bib.append("@article{").append(citationKey).append(",\n");
        bib.append("  title={").append(escape(doc.getTitle())).append("},\n");

        if (doc.getAuthors() != null && !doc.getAuthors().isEmpty()) {
            bib.append("  author={").append(escape(doc.getAuthors())).append("},\n");
        }

        if (doc.getJournal() != null && !doc.getJournal().isEmpty()) {
            bib.append("  journal={").append(escape(doc.getJournal())).append("},\n");
        }

        if (doc.getPublicationYear() != null) {
            bib.append("  year={").append(doc.getPublicationYear()).append("},\n");
        }

        if (doc.getVolume() != null && !doc.getVolume().isEmpty()) {
            bib.append("  volume={").append(escape(doc.getVolume())).append("},\n");
        }

        if (doc.getPages() != null && !doc.getPages().isEmpty()) {
            bib.append("  pages={").append(escape(doc.getPages())).append("},\n");
        }

        if (doc.getDoi() != null && !doc.getDoi().isEmpty()) {
            bib.append("  doi={").append(escape(doc.getDoi())).append("},\n");
        }

        if (doc.getPublisher() != null && !doc.getPublisher().isEmpty()) {
            bib.append("  publisher={").append(escape(doc.getPublisher())).append("},\n");
        }

        // Remove trailing comma and newline, add closing brace
        String result = bib.toString();
        if (result.endsWith(",\n")) {
            result = result.substring(0, result.length() - 2) + "\n";
        }
        result += "}";

        return result;
    }

    /**
     * Generate EndNote (RIS) citation
     */
    public String generateEndNote(Document doc) {
        StringBuilder ris = new StringBuilder();
        ris.append("TY  - JOUR\n"); // Journal Article

        if (doc.getAuthors() != null && !doc.getAuthors().isEmpty()) {
            String[] authors = doc.getAuthors().split(",");
            for (String author : authors) {
                ris.append("AU  - ").append(author.trim()).append("\n");
            }
        }

        ris.append("TI  - ").append(doc.getTitle()).append("\n");

        if (doc.getJournal() != null && !doc.getJournal().isEmpty()) {
            ris.append("JO  - ").append(doc.getJournal()).append("\n");
        }

        if (doc.getPublicationYear() != null) {
            ris.append("PY  - ").append(doc.getPublicationYear()).append("\n");
        }

        if (doc.getVolume() != null && !doc.getVolume().isEmpty()) {
            ris.append("VL  - ").append(doc.getVolume()).append("\n");
        }

        if (doc.getPages() != null && !doc.getPages().isEmpty()) {
            String[] pageRange = doc.getPages().split("-");
            if (pageRange.length == 2) {
                ris.append("SP  - ").append(pageRange[0].trim()).append("\n");
                ris.append("EP  - ").append(pageRange[1].trim()).append("\n");
            } else {
                ris.append("SP  - ").append(doc.getPages()).append("\n");
            }
        }

        if (doc.getDoi() != null && !doc.getDoi().isEmpty()) {
            ris.append("DO  - ").append(doc.getDoi()).append("\n");
        }

        if (doc.getPublisher() != null && !doc.getPublisher().isEmpty()) {
            ris.append("PB  - ").append(doc.getPublisher()).append("\n");
        }

        ris.append("ER  - \n");

        return ris.toString();
    }

    /**
     * Generate citation key (e.g., "zhang2024llm")
     */
    private String generateCitationKey(Document doc) {
        StringBuilder key = new StringBuilder();

        // Extract first author's last name
        if (doc.getAuthors() != null && !doc.getAuthors().isEmpty()) {
            String firstAuthor = doc.getAuthors().split(",")[0].trim();
            String[] nameParts = firstAuthor.split("\\s+");
            if (nameParts.length > 0) {
                key.append(nameParts[nameParts.length - 1].toLowerCase());
            }
        } else {
            key.append("unknown");
        }

        // Add year
        if (doc.getPublicationYear() != null) {
            key.append(doc.getPublicationYear());
        } else {
            key.append("n.d.");
        }

        // Add first word of title
        if (doc.getTitle() != null && !doc.getTitle().isEmpty()) {
            String[] titleWords = doc.getTitle().split("\\s+");
            if (titleWords.length > 0) {
                String firstWord = titleWords[0].replaceAll("[^a-zA-Z]", "").toLowerCase();
                if (!firstWord.isEmpty()) {
                    key.append(firstWord);
                }
            }
        }

        return key.toString();
    }

    /**
     * Escape special characters for BibTeX
     */
    private String escape(String text) {
        if (text == null)
            return "";
        return text.replace("{", "\\{")
                .replace("}", "\\}")
                .replace("$", "\\$")
                .replace("&", "\\&")
                .replace("%", "\\%");
    }
}
