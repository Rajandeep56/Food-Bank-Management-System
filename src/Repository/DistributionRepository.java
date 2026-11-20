package Repository;

import model.Distribution;
import common.PickupStatus;
import java.util.*;

public class DistributionRepository {
    private final Map<Integer, Distribution> distributions = new HashMap<>();

    public void save(Distribution distribution) {
        if (distribution == null) {
            throw new IllegalArgumentException("Distribution cannot be null");
        }
        if (distributions.containsKey(distribution.getDistributionId())) {
            throw new IllegalArgumentException("Distribution with this ID already exists");
        }
        distributions.put(distribution.getDistributionId(), distribution);
    }

    public Distribution findById(int distributionId) {
        Distribution distribution = distributions.get(distributionId);
        if (distribution == null) {
            throw new IllegalArgumentException("Distribution not found");
        }
        return distribution;
    }
    public List<Distribution> findByRequestId(int requestId) {
        List<Distribution> result = new ArrayList<>();
        for (Distribution distribution : distributions.values()) {
            if (distribution.getRequestId() == requestId) {
                result.add(distribution);
            }
        }
        return result;
    }
    public List<Distribution> findAll() {
        return new ArrayList<>(distributions.values());
    }
    public void delete(int distributionId) {
        findById(distributionId);
        distributions.remove(distributionId);
    }
}
