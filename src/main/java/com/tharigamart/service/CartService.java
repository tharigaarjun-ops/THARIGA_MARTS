package com.tharigamart.service;

import com.tharigamart.dto.CartDTO;
import com.tharigamart.dto.CartItemDTO;
import com.tharigamart.entity.Cart;
import com.tharigamart.entity.CartItem;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.CartItemRepository;
import com.tharigamart.repository.CartRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Server-side shopping cart - one cart per registered user. */
@Service
public class CartService {

    private static final int MAX_QUANTITY = 10;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** The signed-in user's cart (created on first use). */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Transactional(readOnly = true)
    public CartDTO getCart(User user) {
        Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);
        return toDto(cart);
    }

    @Transactional
    public CartDTO addItem(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        if (!product.isActive()) {
            throw new BadRequestException("\"" + product.getTitle() + "\" is no longer available");
        }
        if (product.getStockQuantity() <= 0) {
            throw new BadRequestException("\"" + product.getTitle() + "\" is out of stock");
        }

        int requested = Math.min(Math.max(1, quantity), MAX_QUANTITY);
        CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);

        if (existing != null) {
            int newQuantity = Math.min(existing.getQuantity() + requested, MAX_QUANTITY);
            if (newQuantity > product.getStockQuantity()) {
                throw new BadRequestException("Only " + product.getStockQuantity()
                        + " unit(s) of \"" + product.getTitle() + "\" available");
            }
            existing.setQuantity(newQuantity);
            cartItemRepository.save(existing);
        } else {
            if (requested > product.getStockQuantity()) {
                throw new BadRequestException("Only " + product.getStockQuantity()
                        + " unit(s) of \"" + product.getTitle() + "\" available");
            }
            cart.getItems().add(new CartItem(cart, product, requested));
            cartRepository.save(cart);
        }

        return toDto(cartRepository.findByUserId(user.getId()).orElse(cart));
    }

    @Transactional
    public CartDTO updateQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cart item", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("This item is not in your cart");
        }

        int requested = Math.min(Math.max(1, quantity), MAX_QUANTITY);
        Product product = item.getProduct();
        if (product != null && requested > product.getStockQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity()
                    + " unit(s) of \"" + product.getTitle() + "\" available");
        }

        item.setQuantity(requested);
        cartItemRepository.save(item);
        return toDto(cartRepository.findByUserId(user.getId()).orElse(cart));
    }

    @Transactional
    public CartDTO removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cart item", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("This item is not in your cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        cartRepository.save(cart);
        return toDto(cartRepository.findByUserId(user.getId()).orElse(cart));
    }

    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.save(cart);
        });
    }

    public CartDTO toDto(Cart cart) {
        CartDTO dto = new CartDTO();
        if (cart == null) {
            dto.setItems(new ArrayList<>());
            return dto;
        }

        dto.setId(cart.getId());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDTO> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product == null) {
                continue;
            }
            CartItemDTO itemDto = new CartItemDTO();
            itemDto.setId(item.getId());
            itemDto.setProductId(product.getId());
            itemDto.setName(product.getTitle());
            itemDto.setBrand(product.getBrand());
            itemDto.setPrice(product.getPrice());
            itemDto.setMrp(product.getMrp());
            itemDto.setImage(product.getImageUrl());
            itemDto.setStock(product.getStockQuantity());
            itemDto.setInStock(product.getStockQuantity() > 0 && product.isActive());
            itemDto.setCategorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null);
            itemDto.setQuantity(item.getQuantity());
            items.add(itemDto);
        }
        dto.setItems(items);
        return dto;
    }

    /** Number of units in the user's cart (navbar badge). */
    @Transactional(readOnly = true)
    public int cartCount(User user) {
        return cartRepository.findByUserId(user.getId())
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }
}
