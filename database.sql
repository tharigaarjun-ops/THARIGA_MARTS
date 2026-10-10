-- ============================================
--  THARIGA MART - Online Stationery Store
--  Database: MySQL 8
--  Run this file once (before the first start, or any time).
--  Tables use IF NOT EXISTS so it is safe to re-run.
--  Users + sample data are also created automatically by
--  the application's DataSeeder on first start.
-- ============================================

CREATE DATABASE IF NOT EXISTS thariga_mart;
USE thariga_mart;

-- ---------------- USERS ----------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,          -- BCrypt encoded
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL DEFAULT 'USER', -- USER or ADMIN
    created_at DATETIME
);

-- ---------------- CATEGORIES ----------------
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(90) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon VARCHAR(60),
    display_order INT
);

-- ---------------- PRODUCTS ----------------
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    mrp DECIMAL(10,2),
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    category_id BIGINT NOT NULL,
    created_at DATETIME,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- ---------------- PASSWORD RESET TOKENS ----------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ---------------- ORDERS ----------------
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    payment_method VARCHAR(30) NOT NULL DEFAULT 'COD',
    ship_name VARCHAR(120) NOT NULL,
    ship_phone VARCHAR(20) NOT NULL,
    ship_line1 VARCHAR(255) NOT NULL,
    ship_line2 VARCHAR(255),
    ship_city VARCHAR(80) NOT NULL,
    ship_state VARCHAR(80) NOT NULL,
    ship_pincode VARCHAR(10) NOT NULL,
    placed_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ---------------- ORDER ITEMS ----------------
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(180) NOT NULL,
    product_image VARCHAR(255),
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ================= SAMPLE CATEGORIES =================
INSERT IGNORE INTO categories (name, slug, description, icon, display_order) VALUES
('Pens & Pencils',  'pens-pencils',     'Pens, gel pens, pencils, erasers and sharpeners', 'bi-pen', 1),
('Notebooks',       'notebooks',        'Ruled, spiral and exam notebooks', 'bi-journal-bookmark', 2),
('Papers & Files',  'papers-files',     'A4 sheets, chart paper, folders and files', 'bi-file-earmark-text', 3),
('School Supplies', 'school-supplies',  'Scales, geometry boxes, staplers, scissors and glue', 'bi-bag', 4),
('Art & Craft',     'art-craft',        'Sketch pens, colour pencils, crayons and water colours', 'bi-palette', 5),
('Office Stationery','office-stationery','Highlighters, sticky notes, organizers and calculators', 'bi-briefcase', 6);

-- ================= SAMPLE PRODUCTS =================
INSERT IGNORE INTO products (name, description, brand, price, mrp, stock, image, featured, category_id, created_at) VALUES
-- Pens & Pencils
('Classic Ball Pen (Pack of 5)', 'Smooth-writing blue ink ball pens with a comfortable grip, pack of 5. Perfect for everyday writing, exams and office work.', 'WriteRight', 99.00, 149.00, 250, '/images/stationery/pens-pencils.svg', TRUE, 1, NOW()),
('Gel Pen (Pack of 3)', 'Premium gel pens with vibrant, quick-drying ink in blue, black and red. 0.5 mm precision tip, pack of 3.', 'InkFlow', 149.00, 199.00, 180, '/images/stationery/pens-pencils.svg', FALSE, 1, NOW()),
('Pencil (Pack of 10)', 'HB grade pencils with dark, erasable graphite core and splinter-free wood, pack of 10.', 'Graphite', 79.00, 119.00, 300, '/images/stationery/pens-pencils.svg', FALSE, 1, NOW()),
('Eraser (Pack of 3)', 'Soft, non-dusty erasers that leave no smudges on paper, pack of 3.', 'CleanErase', 45.00, 60.00, 400, '/images/stationery/pens-pencils.svg', FALSE, 1, NOW()),
('Sharpener (Single)', 'Durable twin-hole sharpener with a long-lasting stainless steel blade.', 'SharpPro', 35.00, 50.00, 350, '/images/stationery/pens-pencils.svg', FALSE, 1, NOW()),
-- Notebooks
('Ruled Notebook (1 Subject)', '180-page ruled notebook with a soft matte cover, thick 80 GSM paper and margin lines.', 'NoteSmart', 120.00, 160.00, 200, '/images/stationery/notebooks.svg', TRUE, 2, NOW()),
('Spiral Notebook', '200-page spiral-bound A4 notebook that lies flat while writing, with a sturdy wire-o binding.', 'SpiralPro', 180.00, 240.00, 150, '/images/stationery/notebooks.svg', FALSE, 2, NOW()),
('Long Book (Exam)', '300-page long book with wide ruled lines, ideal for exams, assignments and note-taking.', 'ExamReady', 150.00, 200.00, 160, '/images/stationery/notebooks.svg', FALSE, 2, NOW()),
-- Papers & Files
('A4 Sheets (500 pc)', 'White A4 copier sheets, 75 GSM, inkjet and laser friendly, pack of 500.', 'PaperWhite', 350.00, 450.00, 120, '/images/stationery/papers-files.svg', FALSE, 3, NOW()),
('File Folder (A4)', 'A4 file folder with a flap and double stitching, available in assorted colours.', 'FileMaster', 90.00, 120.00, 220, '/images/stationery/papers-files.svg', FALSE, 3, NOW()),
('Chart Paper (A2)', 'Thick 120 GSM chart paper in A2 size, great for school projects and presentations.', 'ChartBright', 60.00, 80.00, 140, '/images/stationery/papers-files.svg', FALSE, 3, NOW()),
-- School Supplies
('Scale 30 cm', 'Transparent plastic ruler with clear, easy-to-read markings, 30 cm.', 'MeasureRight', 40.00, 55.00, 260, '/images/stationery/school-supplies.svg', FALSE, 4, NOW()),
('Geometry Box', 'Complete geometry set with compass, divider, two set squares, protractor, pencil and eraser in a sturdy box.', 'GeoMaster', 250.00, 350.00, 110, '/images/stationery/school-supplies.svg', TRUE, 4, NOW()),
('Stapler', 'Heavy-duty desktop stapler with a non-slip base, includes 1000 pins.', 'StaplerPro', 220.00, 299.00, 90, '/images/stationery/school-supplies.svg', FALSE, 4, NOW()),
('Scissors', 'Stainless steel scissors with an ergonomic grip, perfect for craft and school work.', 'CutSharp', 110.00, 150.00, 170, '/images/stationery/school-supplies.svg', FALSE, 4, NOW()),
('Glue Stick', 'Non-toxic adhesive glue stick, 20 g, dries clear and washes off easily.', 'StickIt', 65.00, 85.00, 240, '/images/stationery/school-supplies.svg', FALSE, 4, NOW()),
-- Art & Craft
('Sketch Pens (12 colours)', 'Bright sketch pens with flexible brush tips, pack of 12 vibrant colours.', 'Artistic', 299.00, 399.00, 130, '/images/stationery/art-craft.svg', TRUE, 5, NOW()),
('Colour Pencils (24)', 'Artist-grade colour pencils with soft, blendable cores, pack of 24.', 'Rainbow', 240.00, 320.00, 140, '/images/stationery/art-craft.svg', FALSE, 5, NOW()),
('Crayons (12)', 'Wax crayons for kids, bright colours, easy to hold, pack of 12.', 'ColorJoy', 130.00, 180.00, 190, '/images/stationery/art-craft.svg', FALSE, 5, NOW()),
('Water Colour Box', '12-colour water colour cake set with a brush, palette and sponge.', 'AquaArt', 340.00, 450.00, 100, '/images/stationery/art-craft.svg', FALSE, 5, NOW()),
('Marker (Pack of 6)', 'Permanent markers with a chisel tip in assorted colours, pack of 6.', 'MarkBold', 210.00, 280.00, 150, '/images/stationery/art-craft.svg', FALSE, 5, NOW()),
-- Office Stationery
('Highlighter (Pack of 4)', 'Fluorescent highlighters with a chisel tip in assorted colours, pack of 4.', 'HighlightPro', 160.00, 220.00, 180, '/images/stationery/office-stationery.svg', FALSE, 6, NOW()),
('Stapler Pins (1000)', 'Standard size 24/6 stapler pins, box of 1000, galvanized to resist rust.', 'PinPoint', 50.00, 70.00, 300, '/images/stationery/office-stationery.svg', FALSE, 6, NOW()),
('Sticky Notes (5-pack)', 'Pastel sticky notes, 3x3 inch with strong adhesive, pack of 5 pads.', 'NoteStick', 85.00, 120.00, 210, '/images/stationery/office-stationery.svg', FALSE, 6, NOW()),
('Paper Clips (100)', 'Silver paper clips, box of 100, strong spring steel.', 'ClipRight', 40.00, 55.00, 280, '/images/stationery/office-stationery.svg', FALSE, 6, NOW()),
('Desk Organizer', 'Multi-compartment mesh desk organizer with pen holders, card slot and note clip.', 'DeskTidy', 399.00, 549.00, 80, '/images/stationery/office-stationery.svg', FALSE, 6, NOW()),
('Calculator (12-digit)', '12-digit desktop calculator with large display, solar panel and battery backup.', 'CalcSmart', 450.00, 599.00, 95, '/images/stationery/office-stationery.svg', TRUE, 6, NOW());
