package model;

public class Category {
    private final int categoryId;
    private String name;

    public Category(int categoryId, String name) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }

        setName(name);
        this.categoryId = categoryId;
    }

    public int getCategoryId() {
        return categoryId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        this.name = name;
    }
}
