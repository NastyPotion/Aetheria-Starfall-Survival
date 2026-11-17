package com.game.survival;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Owns carried resources and permanent equipment. Spending is all-or-nothing. */
public final class Inventory {
    private final EnumMap<Resource, Integer> items = new EnumMap<>(Resource.class);
    private final EnumSet<WeaponType> weapons = EnumSet.of(WeaponType.FISTS);

    public Inventory() {
        for (Resource resource : Resource.values()) items.put(resource, 0);
    }

    static Inventory restore(Map<Resource, Integer> savedItems, Set<WeaponType> savedWeapons) {
        Inventory restored = new Inventory();
        Objects.requireNonNull(savedItems, "Inventory is required");
        for (Map.Entry<Resource, Integer> entry : savedItems.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "Resource is required");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Inventory counts cannot be negative");
            }
            restored.items.put(entry.getKey(), entry.getValue());
        }
        restored.weapons.addAll(Objects.requireNonNull(savedWeapons, "Equipment is required"));
        return restored;
    }

    public int count(Resource resource) {
        return items.get(Objects.requireNonNull(resource, "Resource is required"));
    }

    public void add(Resource resource, int amount) {
        requirePositive(amount);
        long total = (long) count(resource) + amount;
        if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("Resource count is too large");
        items.put(resource, (int) total);
    }

    public boolean remove(Resource resource, int amount) {
        requirePositive(amount);
        int previous = count(resource);
        if (previous < amount) return false;
        items.put(resource, previous - amount);
        return true;
    }

    public boolean canAfford(Map<Resource, Integer> costs) {
        validateCosts(costs);
        for (Map.Entry<Resource, Integer> entry : costs.entrySet()) {
            if (count(entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    public boolean spend(Map<Resource, Integer> costs) {
        if (!canAfford(costs)) return false;
        for (Map.Entry<Resource, Integer> entry : costs.entrySet()) {
            if (entry.getValue() > 0) remove(entry.getKey(), entry.getValue());
        }
        return true;
    }

    public void grantWeapon(WeaponType weapon) { weapons.add(Objects.requireNonNull(weapon)); }
    public boolean hasWeapon(WeaponType weapon) { return weapons.contains(Objects.requireNonNull(weapon)); }
    public Set<WeaponType> getWeapons() { return Set.copyOf(weapons); }
    public Map<Resource, Integer> copyItems() { return Map.copyOf(items); }

    /** Death drops every loose resource, including diamonds, while keeping equipment. */
    public void clearCarried() { items.replaceAll((resource, amount) -> 0); }

    private static void requirePositive(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Resource amount must be positive");
    }

    private static void validateCosts(Map<Resource, Integer> costs) {
        Objects.requireNonNull(costs, "Costs are required");
        for (Map.Entry<Resource, Integer> entry : costs.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "Cost resource is required");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Costs cannot be negative");
            }
        }
    }
}
