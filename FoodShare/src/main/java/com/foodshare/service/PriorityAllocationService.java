package com.foodshare.service;

import com.foodshare.entity.FoodRequest;
import com.foodshare.entity.PriorityCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * PriorityAllocationService implements the Data Structure & Algorithm requirement
 * using a Java PriorityQueue to arbitrate surplus food distribution.
 *
 * Algorithm Design:
 * - Priority Hierarchy:
 *   1. CHILDREN (Vulnerable category with critical nutrition needs) -> Weight 1
 *   2. ELDERLY (High vulnerability) -> Weight 2
 *   3. REGULAR (General recipients) -> Weight 3
 * - Tie-Breaking:
 *   FIFO based on request creation timestamp (Earliest request first)
 */
@Service
public class PriorityAllocationService {

    public static final Comparator<FoodRequest> PRIORITY_COMPARATOR = (r1, r2) -> {
        int w1 = getCategoryWeight(r1.getPriorityCategory());
        int w2 = getCategoryWeight(r2.getPriorityCategory());

        if (w1 != w2) {
            return Integer.compare(w1, w2);
        }
        // Secondary key: FIFO creation timestamp
        return r1.getCreatedAt().compareTo(r2.getCreatedAt());
    };

    private static int getCategoryWeight(PriorityCategory category) {
        if (category == null) return 3;
        return switch (category) {
            case CHILDREN -> 1;
            case ELDERLY -> 2;
            case REGULAR -> 3;
        };
    }

    /**
     * Orders a list of incoming food requests based on the priority algorithm.
     */
    public List<FoodRequest> prioritizeRequests(List<FoodRequest> rawRequests) {
        if (rawRequests == null || rawRequests.isEmpty()) {
            return new ArrayList<>();
        }

        PriorityQueue<FoodRequest> queue = new PriorityQueue<>(rawRequests.size(), PRIORITY_COMPARATOR);
        queue.addAll(rawRequests);

        List<FoodRequest> prioritized = new ArrayList<>(rawRequests.size());
        while (!queue.isEmpty()) {
            prioritized.add(queue.poll());
        }
        return prioritized;
    }
}
