package com.sageserpent.americium.java.examples.junit5;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PoorQualityGrouping {
    // Where has this implementation gone wrong? Surely we've thought of
    // everything?
    public static <Element> List<List<Element>> groupsOfAdjacentDuplicates(
            List<Element> elements) {
        final Iterator<Element> iterator = elements.iterator();

        final List<List<Element>> result = new LinkedList<>();

        final LinkedList<Element> chunk = new LinkedList<>();

        while (iterator.hasNext()) {
            final Element element = iterator.next();

            // Got to clear the chunk when the element changes...
            if (!chunk.isEmpty() && chunk.get(0) != element) {
                // Got to add the chunk to the result before it gets cleared
                // - and watch out for empty chunks...
                if (!chunk.isEmpty()) result.add(chunk);
                chunk.clear();
            }

            // Always add the latest element to the chunk...
            chunk.add(element);
        }

        // Don't forget to add the last chunk to the result - as long as it's
        // not empty...
        if (!chunk.isEmpty()) result.add(chunk);

        return result;
    }
}
