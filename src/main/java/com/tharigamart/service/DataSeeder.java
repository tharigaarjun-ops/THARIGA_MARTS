package com.tharigamart.service;

import com.tharigamart.entity.Category;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.repository.CategoryRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the database on a completely empty database:
 *  - admin + seller + demo buyer accounts (BCrypt encoded)
 *  - 10 stationery categories
 *  - sample stationery products
 * Every block only runs when its table is empty, so it is safe to restart.
 * On the shipped database (which already contains data) this does nothing.
 */
@Service
public class DataSeeder implements org.springframework.boot.CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        ProductRepository productRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 || categoryRepository.count() > 0) {
            log.info("Database already contains data - skipping seed");
            return;
        }

        User admin = userRepository.save(new User(
                "Thariga Admin", "admin@tharigamart.com",
                passwordEncoder.encode("Admin@123"), "+91 9876543210", Role.ROLE_ADMIN));

        User seller = userRepository.save(new User(
                "Min-jun Park", "kawaiistore@tharigamart.com",
                passwordEncoder.encode("Seller@123"), "+91 9876543211", Role.ROLE_SELLER));

        User buyer = userRepository.save(new User(
                "Aathira T", "buyer@tharigamart.com",
                passwordEncoder.encode("Buyer@123"), "+91 9840123456", Role.ROLE_BUYER));

        log.info("Seeded admin, seller and buyer accounts");

        Map<String, Category> categories = new HashMap<>();
        for (Object[] row : List.of(
                new Object[]{"Pencils, Erasers & Sharpeners", "pencils-erasers", 1},
                new Object[]{"Pens & Highlighters", "pens-highlighters", 2},
                new Object[]{"Notebooks & Journals", "notebooks-journals", 3},
                new Object[]{"Pencil Boxes & Pouches", "pencil-boxes-pouches", 4},
                new Object[]{"Geometry & Mathematical Tools", "geometry-math", 5},
                new Object[]{"Sticky Notes, Stickers & Washi", "sticky-notes-washi", 6},
                new Object[]{"Art, Colour Pencils & Paints", "art-colors", 7},
                new Object[]{"Craft Tools, Scissors & Staplers", "craft-tools", 8},
                new Object[]{"Desk Organizers, Files & Folders", "office-desk", 9},
                new Object[]{"Korean Kawaii Gift Sets & Combos", "kawaii-gifts", 10})) {
            Category category = new Category(
                    (String) row[0], (String) row[1],
                    "Stationery category", null, null, (Integer) row[2]);
            categoryRepository.save(category);
            categories.put((String) row[1], category);
        }
        log.info("Seeded {} categories", categories.size());

        // A couple of sample products so the store is not empty on a fresh DB.
        saveProduct(categories.get("pencils-erasers"), seller,
                "Pastel Macaron Eraser Set (Pack of 5)",
                "Cute pastel macaron-shaped erasers, soft vinyl, smudge-free.",
                "Thariga", new BigDecimal("149.00"), new BigDecimal("199.00"), 45,
                "/images/stationery/pencils.svg");
        saveProduct(categories.get("notebooks-journals"), seller,
                "A5 Lavender Hardcover Bullet Journal",
                "Thick wirebound memory journal with plastic protective corners.",
                "Thariga", new BigDecimal("499.00"), new BigDecimal("750.00"), 30,
                "/images/stationery/notebook.svg");
        saveProduct(categories.get("pens-highlighters"), seller,
                "Milkliner Soft Pastel Double-Ended Highlighters (Pack of 12)",
                "Twelve soft pastel shades, fine and brush tips, non-toxic ink.",
                "Thariga", new BigDecimal("399.00"), new BigDecimal("599.00"), 59,
                "/images/stationery/pen.svg");

        log.info("Seeded 3 sample products");
    }

    private void saveProduct(Category category, User seller, String title,
                               String description, String brand,
                               BigDecimal price, BigDecimal mrp, int stock,
                               String image) {
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setBrand(brand);
        product.setPrice(price);
        product.setMrp(mrp);
        product.setStockQuantity(stock);
        product.setImageUrl(image);
        product.setFeatured(true);
        product.setActive(true);
        product.setCategory(category);
        product.setSeller(seller);
        productRepository.save(product);
    }
}
