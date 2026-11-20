package Repository;

import java.util.*;

import model.Category;

public class CategoryRepository {
    
    private final Map<Integer, Category> categories = new HashMap<>();

    public void save(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        //duplicate check
        if(categories.containsKey(category.getCategoryId())) {
            throw new IllegalArgumentException("Category with this ID already exists");
        }
        categories.put(category.getCategoryId(), category);
    }

    public Category findById(int categoryId) {
        Category category = categories.get(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }
        return category;
    }
    public List<Category> findAll() {
        return new ArrayList<>(categories.values());
    }
     public void updateCategory(int categoryId, String name) {

        Category category = findById(categoryId);

            for (Category existing : categories.values()) {
                if (existing.getCategoryId() != categoryId &&
                    existing.getName().equalsIgnoreCase(name)) {

                    throw new IllegalArgumentException(
                        "Category name already exists"
                    );
                }
            }

            category.setName(name);
        }
    public void deleteCategory(int categoryId) {
        findById(categoryId);
        categories.remove(categoryId);
    }
}
