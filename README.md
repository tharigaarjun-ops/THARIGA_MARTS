# 🛒 THARIGA MART — Online Stationery Store

A complete, runnable full-stack e-commerce website for stationery.

**Tagline:** *Smart Shopping. Easy Buying. Everything You Need.*

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, JavaScript (ES6), Bootstrap 5.3, Bootstrap Icons |
| Backend | Java 17, Spring Boot 3.3, Maven |
| APIs | Spring Web (REST), Spring Data JPA, Bean Validation |
| Security | Spring Security 6, stateless JWT (HS256), BCrypt password hashing |
| Database | MySQL 8 — database `thariga_mart` |

---

## 📁 Folder Structure

```
thariga-mart/
├── pom.xml                              # Maven build (Spring Boot 3.3, Java 17)
├── database.sql                         # Schema + 6 categories + 27 sample products
├── README.md
└── src/main/
    ├── java/com/tharigamart/
    │   ├── TharigaMartApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java          # URL rules: public /api/auth/** + catalogue,
    │   │   │                                # protected orders/account, admin-only /api/admin/**
    │   │   ├── JwtAuthenticationFilter.java # reads "Authorization: Bearer <jwt>"
    │   │   ├── JwtUtil.java                 # HS256 token create/verify
    │   │   ├── UserDetailsServiceImpl.java  # loads users from MySQL
    │   │   ├── RestAuthenticationEntryPoint.java  # JSON 401
    │   │   └── RestAccessDeniedHandler.java       # JSON 403
    │   ├── controller/
    │   │   ├── AuthController.java          # register, login, forgot-password, reset-password
    │   │   ├── ProductController.java       # search / filter / sort / details / related
    │   │   ├── CategoryController.java      # category list with product counts
    │   │   ├── OrderController.java         # place order, history, details (JWT protected)
    │   │   ├── AccountController.java       # profile + change password (JWT protected)
    │   │   └── AdminProductController.java  # product CRUD (ROLE_ADMIN only)
    │   ├── dto/                             # ApiResponse, AuthResponse, LoginRequest,
    │   │                                    # RegisterRequest, ResetPasswordRequest,
    │   │                                    # ProductDTO, ProductPageResponse, ProductRequest,
    │   │                                    # CategoryDTO, CheckoutRequest, OrderDTO,
    │   │                                    # OrderItemDTO, UserDTO, ChangePasswordRequest,
    │   │                                    # UpdateProfileRequest, UpdateOrderStatusRequest
    │   ├── entity/                          # User, Role, Category, Product,
    │   │                                    # Order, OrderItem, OrderStatus, PasswordResetToken
    │   ├── repository/                      # JPA repositories (incl. dynamic product search query)
    │   ├── service/                         # AuthService, ProductService, CategoryService,
    │   │                                    # OrderService, AccountService, DataSeeder
    │   └── exception/                       # BadRequestException, ResourceNotFoundException,
    │                                        # OutOfStockException, GlobalExceptionHandler
    └── resources/
        ├── application.properties           # datasource, JPA, JWT settings
        └── static/                          # Frontend served by Spring Boot at /
            ├── index.html                   # Home: Navbar, Hero, Categories, Featured, Cart link, Account, Logout
            ├── home.html                    # alias that redirects to index.html
            ├── login.html                   # Login: logo, tagline, email, password, Show/Hide,
            │                                # Login, Forgot Password, Registration
            ├── register.html                # Registration
            ├── forgot-password.html         # Forgot Password
            ├── reset-password.html          # Reset Password (token link)
            ├── products.html                # Search, category filter, price filter, sort, pagination
            ├── product-details.html         # Product details, quantity, Add to Cart, Buy Now
            ├── cart.html                    # Cart: quantity management, remove, totals
            ├── checkout.html                # Checkout: address, payment, Place Order
            ├── orders.html                  # Order History
            ├── order-details.html           # Order details + status timeline
            ├── account.html                 # My Account: profile edit, change password, logout
            ├── css/style.css
            ├── js/                          # api.js (fetch + JWT wrapper), auth.js, navbar.js,
            │                                # home.js, products.js, product-details.js, cart.js,
            │                                # checkout.js, orders.js, order-details.js, account.js
            └── images/stationery/*.svg      # Product category images
```

---

## ⚙️ VS Code Setup & Run (Beginner Friendly)

### 1. Install prerequisites
- **Java JDK 17** → https://adoptium.net  (check: `java -version`)
- **Maven** → https://maven.apache.org  (check: `mvn -version`)
- **MySQL 8** → https://dev.mysql.com/downloads/mysql
- **VS Code** with the *Extension Pack for Java* and *Spring Boot Extension Pack*

### 2. Create the database
```sql
CREATE DATABASE thariga_mart;
```

### 3. (Optional) Import the sample data
The app seeds everything automatically on first start, but if you prefer SQL:
```bash
mysql -u root -p thariga_mart < database.sql
```

### 4. Set your MySQL credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### 5. Run
Open the `thariga-mart` folder in VS Code, open the integrated terminal (**Ctrl+`**) and run:
```bash
mvn spring-boot:run
```
Wait for `Started TharigaMartApplication in X seconds`.

### 6. Open the store
→ **http://localhost:8090**

You land on the **Login page** (logo + tagline *"Smart Shopping. Easy Buying. Everything You Need."*).
Click **Registration** to create an account — or use the demo accounts:

| Email | Password | Role |
|---|---|---|
| `demo@tharigamart.com` | `Demo@1234` | Customer |
| `admin@tharigamart.com` | `Admin@123` | Administrator |

> 💡 **Forgot Password** returns the reset link directly on the page (demo mode — no mail server is configured). Click it to set a new password. In production the link would be emailed.

---

## 🛍️ Features

- **First page — Login**: THARIGA MART logo, tagline, email, password with **Show/Hide Password**, Login, **Forgot Password** flow and **Registration**.
- **Home page** (after login): responsive Navbar (search, categories, cart badge, My Account, Logout), Hero, **Categories** grid, **Featured Products**, trust badges, footer.
- **Shop**: product **search**, **category filtering**, **price range filtering**, **price/name/newest sorting**, pagination.
- **Product details**: price + struck-through MRP, discount %, stock status, quantity picker, **Add to Cart**, **Buy Now**, related products.
- **Cart** (localStorage): quantity management, remove, subtotal + delivery + total.
- **Checkout**: delivery address form (validated), COD / UPI (demo) payment, **Place Order** with server-side stock validation.
- **Order History**: status timeline (PLACED → CONFIRMED → SHIPPED → DELIVERED), itemised totals, delivery address.
- **My Account**: view/edit profile, change password, logout.
- **Security**: every authenticated API is protected by **JWT** (stateless) + **Spring Security**; passwords are **BCrypt** hashed; unknown/expired tokens get a clean JSON 401 and the UI returns to the login page.

---

## 🔌 REST API

All responses use the `{ status, message, data }` envelope (errors also include `timestamp`, `error`, `path`, and `errors` for validation).

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create account |
| POST | `/api/auth/login` | Public | Login → returns JWT |
| POST | `/api/auth/forgot-password` | Public | Generate reset token |
| POST | `/api/auth/reset-password` | Public | Set new password with token |
| GET | `/api/products` | Public | `?search=&category=&minPrice=&maxPrice=&sort=&page=&size=` |
| GET | `/api/products/featured` | Public | Featured products |
| GET | `/api/products/{id}` | Public | Product details |
| GET | `/api/products/{id}/related` | Public | Related products |
| GET | `/api/categories` | Public | Categories + product counts |
| POST | `/api/orders` | 🔒 JWT | Place order (`{ paymentMethod, shipping, items }`) |
| GET | `/api/orders` | 🔒 JWT | My order history |
| GET | `/api/orders/{id}` | 🔒 JWT | Order details (own orders only) |
| GET | `/api/account/profile` | 🔒 JWT | My profile |
| PUT | `/api/account/profile` | 🔒 JWT | Update name / phone |
| PUT | `/api/account/password` | 🔒 JWT | Change password |
| GET/POST/PUT/DELETE | `/api/admin/products[/{id}]` | 🔒 ADMIN | Catalogue management |

Authenticated requests must carry the header:
```
Authorization: Bearer <JWT>
```

The frontend uses `fetch()` everywhere (see `static/js/api.js` — the `api()` wrapper attaches the token, handles 401/403 and renders server-side validation errors).

---

## 💡 Notes

- **Delivery fee**: ₹49, free on orders of ₹499 or more (mirrored in `OrderService` and the cart/checkout JS).
- **Cart** is stored in the browser's `localStorage` (survives refresh); the server validates stock at checkout and decrements it atomically.
- **Hibernate `ddl-auto=update`** creates/updates the tables from the JPA entities; `DataSeeder` inserts the admin/demo accounts, 6 categories and 27 products only when the tables are empty — safe to restart.
- To run against a fresh database, just `CREATE DATABASE thariga_mart;` and start the app.
