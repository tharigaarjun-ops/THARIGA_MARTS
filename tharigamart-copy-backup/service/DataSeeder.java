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
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the database on first start:
 *  - admin + demo buyer accounts (BCrypt encoded)
 *  - 6 stationery categories
 *  - 27 sample stationery products
 * Every block only runs when its table is empty, so it is safe to restart.
 */
@Service
public class DataSeeder implements CommandLineRunner {

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
        seedUsers();
        seedCategories();
        seedProducts();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("Thariga Admin", "admin@tharigamart.com",
                passwordEncoder.encode("Admin@123"), "9876543210", Role.ADMIN));
        userRepository.save(new User("Demo Customer", "demo@tharigamart.com",
                passwordEncoder.encode("Demo@1234"), "9876501234", Role.USER));
        log.info("Seeded accounts: admin@tharigamart.com / Admin@123 , demo@tharigamart.com / Demo@1234");
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        List<Category> categories = List.of(
                new Category("Pens & Pencils", "pens-pencils", "Pens, gel pens, pencils, erasers and sharpeners", "bi-pen", 1),
                new Category("Notebooks", "notebooks", "Ruled, spiral and exam notebooks", "bi-journal-bookmark", 2),
                new Category("Papers & Files", "papers-files", "A4 sheets, chart paper, folders and files", "bi-file-earmark-text", 3),
                new Category("School Supplies", "school-supplies", "Scales, geometry boxes, staplers, scissors and glue", "bi-bag", 4),
                new Category("Art & Craft", "art-craft", "Sketch pens, colour pencils, crayons and water colours", "bi-palette", 5),
                new Category("Office Stationery", "office-stationery", "Highlighters, sticky notes, organizers and calculators", "bi-briefcase", 6));
        categoryRepository.saveAll(categories);
        log.info("Seeded {} stationery categories", categories.size());
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        Map<String, Category> cat = new HashMap<>();
        categoryRepository.findAll().forEach(c -> cat.put(c.getSlug(), c));

        record Seed(String cat, String name, String brand, String price, String mrp,
                    int stock, String image, boolean featured, String description) {
        }

        List<Seed> seeds = List.of(
                // ---- Pens & Pencils ----
                new Seed("pens-pencils", "Classic Ball Pen (Pack of 5)", "WriteRight", "99.00", "149.00", 250,
                        "/images/stationery/pens-pencils.svg", true,
                        "Smooth-writing blue ink ball pens with a comfortable grip, pack of 5. Perfect for everyday writing, exams and office work."),
                new Seed("pens-pencils", "Gel Pen (Pack of 3)", "InkFlow", "149.00", "199.00", 180,
                        "/images/stationery/pens-pencils.svg", false,
                        "Premium gel pens with vibrant, quick-drying ink in blue, black and red. 0.5 mm precision tip, pack of 3."),
                new Seed("pens-pencils", "Pencil (Pack of 10)", "Graphite", "79.00", "119.00", 300,
                        "/images/stationery/pens-pencils.svg", false,
                        "HB grade pencils with dark, erasable graphite core and splinter-free wood, pack of 10."),
                new Seed("pens-pencils", "Eraser (Pack of 3)", "CleanErase", "45.00", "60.00", 400,
                        "/images/stationery/pens-pencils.svg", false,
                        "Soft, non-dusty erasers that leave no smudges on paper, pack of 3."),
                new Seed("pens-pencils", "Sharpener (Single)", "SharpPro", "35.00", "50.00", 350,
                        "/images/stationery/pens-pencils.svg", false,
                        "Durable twin-hole sharpener with a long-lasting stainless steel blade."),

                // ---- Notebooks ----
                new Seed("notebooks", "Ruled Notebook (1 Subject)", "NoteSmart", "120.00", "160.00", 200,
                        "/images/stationery/notebooks.svg", true,
                        "180-page ruled notebook with a soft matte cover, thick 80 GSM paper and margin lines."),
                new Seed("notebooks", "Spiral Notebook", "SpiralPro", "180.00", "240.00", 150,
                        "/images/stationery/notebooks.svg", false,
                        "200-page spiral-bound A4 notebook that lies flat while writing, with a sturdy wire-o binding."),
                new Seed("notebooks", "Long Book (Exam)", "ExamReady", "150.00", "200.00", 160,
                        "/images/stationery/notebooks.svg", false,
                        "300-page long book with wide ruled lines, ideal for exams, assignments and note-taking."),

                // ---- Papers & Files ----
                new Seed("papers-files", "A4 Sheets (500 pc)", "PaperWhite", "350.00", "450.00", 120,
                        "/images/stationery/papers-files.svg", false,
                        "White A4 copier sheets, 75 GSM, inkjet and laser friendly, pack of 500."),
                new Seed("papers-files", "File Folder (A4)", "FileMaster", "90.00", "120.00", 220,
                        "/images/stationery/papers-files.svg", false,
                        "A4 file folder with a flap and double stitching, available in assorted colours."),
                new Seed("papers-files", "Chart Paper (A2)", "ChartBright", "60.00", "80.00", 140,
                        "/images/stationery/papers-files.svg", false,
                        "Thick 120 GSM chart paper in A2 size, great for school projects and presentations."),

                // ---- School Supplies ----
                new Seed("school-supplies", "Scale 30 cm", "MeasureRight", "40.00", "55.00", 260,
                        "/images/stationery/school-supplies.svg", false,
                        "Transparent plastic ruler with clear, easy-to-read markings, 30 cm."),
                new Seed("school-supplies", "Geometry Box", "GeoMaster", "250.00", "350.00", 110,
                        "/images/stationery/school-supplies.svg", true,
                        "Complete geometry set with compass, divider, two set squares, protractor, pencil and eraser in a sturdy box."),
                new Seed("school-supplies", "Stapler", "StaplerPro", "220.00", "299.00", 90,
                        "/images/stationery/school-supplies.svg", false,
                        "Heavy-duty desktop stapler with a non-slip base, includes 1000 pins."),
                new Seed("school-supplies", "Scissors", "CutSharp", "110.00", "150.00", 170,
                        "/images/stationery/school-supplies.svg", false,
                        "Stainless steel scissors with an ergonomic grip, perfect for craft and school work."),
                new Seed("school-supplies", "Glue Stick", "StickIt", "65.00", "85.00", 240,
                        "/images/stationery/school-supplies.svg", false,
                        "Non-toxic adhesive glue stick, 20 g, dries clear and washes off easily."),

                // ---- Art & Craft ----
                new Seed("art-craft", "Sketch Pens (12 colours)", "Artistic", "299.00", "399.00", 130,
                        "/images/stationery/art-craft.svg", true,
                        "Bright sketch pens with flexible brush tips, pack of 12 vibrant colours."),
                new Seed("art-craft", "Colour Pencils (24)", "Rainbow", "240.00", "320.00", 140,
                        "/images/stationery/art-craft.svg", false,
                        "Artist-grade colour pencils with soft, blendable cores, pack of 24."),
                new Seed("art-craft", "Crayons (12)", "ColorJoy", "130.00", "180.00", 190,
                        "/images/stationery/art-craft.svg", false,
                        "Wax crayons for kids, bright colours, easy to hold, pack of 12."),
                new Seed("art-craft", "Water Colour Box", "AquaArt", "340.00", "450.00", 100,
                        "/images/stationery/art-craft.svg", false,
                        "12-colour water colour cake set with a brush, palette and sponge."),
                new Seed("art-craft", "Marker (Pack of 6)", "MarkBold", "210.00", "280.00", 150,
                        "/images/stationery/art-craft.svg", false,
                        "Permanent markers with a chisel tip in assorted colours, pack of 6."),

                // ---- Office Stationery ----
                new Seed("office-stationery", "Highlighter (Pack of 4)", "HighlightPro", "160.00", "220.00", 180,
                        "/images/stationery/office-stationery.svg", false,
                        "Fluorescent highlighters with a chisel tip in assorted colours, pack of 4."),
                new Seed("office-stationery", "Stapler Pins (1000)", "PinPoint", "50.00", "70.00", 300,
                        "/images/stationery/office-stationery.svg", false,
                        "Standard size 24/6 stapler pins, box of 1000, galvanized to resist rust."),
                new Seed("office-stationery", "Sticky Notes (5-pack)", "NoteStick", "85.00", "120.00", 210,
                        "/images/stationery/office-stationery.svg", false,
                        "Pastel sticky notes, 3x3 inch with strong adhesive, pack of 5 pads."),
                new Seed("office-stationery", "Paper Clips (100)", "ClipRight", "40.00", "55.00", 280,
                        "/images/stationery/office-stationery.svg", false,
                        "Silver paper clips, box of 100, strong spring steel."),
                new Seed("office-stationery", "Desk Organizer", "DeskTidy", "399.00", "549.00", 80,
                        "/images/stationery/office-stationery.svg", false,
                        "Multi-compartment mesh desk organizer with pen holders, card slot and note clip."),
                new Seed("office-stationery", "Calculator (12-digit)", "CalcSmart", "450.00", "599.00", 95,
                        "/images/stationery/office-stationery.svg", true,
                        "12-digit desktop calculator with large display, solar panel and battery backup."));

        for (Seed s : seeds) {
            Category category = cat.get(s.cat());
            if (category == null) {
                continue;
            }
            Product product = new Product();
            product.setName(s.name());
            product.setBrand(s.brand());
            product.setDescription(s.description());
            product.setPrice(new BigDecimal(s.price()));
            product.setMrp(new BigDecimal(s.mrp()));
            product.setStock(s.stock());
            product.setImage(s.image());
            product.setFeatured(s.featured());
            product.setCategory(category);
            productRepository.save(product);
        }
        log.info("Seeded {} sample stationery products", productRepository.count());
    }
}
