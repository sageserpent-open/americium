package com.sageserpent.americium.java.examples.junit5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class Tiers<Element extends Comparable<Element>> {
    final int worstTier;

    final List<Element> storage;

    public Tiers(int worstTier) {
        this.worstTier = worstTier;
        storage = new ArrayList<>(worstTier) {
            @Override
            public void add(int index, Element element) {
                if (size() < worstTier) {
                    super.add(index, element);
                } else if (0 < index) {
                    for (int shiftDestination = 0;
                         shiftDestination < index - 1; ++shiftDestination) {
                        super.set(shiftDestination,
                                  super.get(1 + shiftDestination));
                    }

                    super.set(index - 1, element);
                }
            }
        };
    }

    void add(Element element) {
        final int index = Collections.binarySearch(storage, element);

        if (0 > index) {
            storage.add(-(index + 1), element);
        } else {
            storage.add(index, element);
        }
    }

    Optional<Element> at(int tier) {
        return 0 < tier && tier <= storage.size()
               ? Optional.of(storage.get(storage.size() - tier))
               :
               Optional.empty();
    }
}