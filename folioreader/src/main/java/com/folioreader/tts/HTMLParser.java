package com.folioreader.tts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Zahra Jamshidi on 6/16/2020.
 */
public class HTMLParser {

    private static final String[] ADD_WHITE_SPACE_NODES =
            {"p", "h1", "h2", "h3", "h4", "h5", "span"};

    public static List<String> parseSentences(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        List<String> result = new ArrayList<>();
        parse(doc.body(), result);
        return result;
    }

    public static String getText(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.body().text();
    }

    private static void parse(Element node, List<String> result) {
        if (isWhiteSpaceNode(node.tagName())) {
            String text = node.text();
            text = text.replace("\n", "");
            String[] splitSpeech = text.split("\\.");
            result.addAll(Arrays.asList(splitSpeech));
        } else {
            for (Element child : node.children()) {
                parse(child, result);
            }
        }
    }

    private static boolean isWhiteSpaceNode(String nodeName) {
        for (String s : ADD_WHITE_SPACE_NODES) {
            if (s.equals(nodeName)) {
                return true;
            }
        }
        // if get here, not found
        return false;
    }


}
