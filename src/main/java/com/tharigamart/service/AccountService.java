package com.tharigamart.service;

import com.tharigamart.dto.AddressDTO;
import com.tharigamart.dto.AddressRequest;
import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.ChangePasswordRequest;
import com.tharigamart.dto.UpdateProfileRequest;
import com.tharigamart.dto.UserDTO;
import com.tharigamart.entity.Address;
import com.tharigamart.entity.AddressType;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.repository.AddressRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** My Account: profile view / edit, password change, address book. */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AccountService(UserRepository userRepository,
                           AddressRepository addressRepository,
                           PasswordEncoder passwordEncoder,
                           AuthService authService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UserDTO getProfile(String email) {
        return authService.toDto(requireUser(email));
    }

    @Transactional
    public UserDTO updateProfile(String email, UpdateProfileRequest request) {
        User user = requireUser(email);
        user.setName(request.getFullName().trim());
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim());
        return authService.toDto(userRepository.save(user));
    }

    @Transactional
    public ApiResponse<Void> changePassword(String email, ChangePasswordRequest request) {
        User user = requireUser(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Your current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.ok("Password changed successfully", null);
    }

    /* ------------------------- address book ------------------------- */

    @Transactional(readOnly = true)
    public List<AddressDTO> listAddresses(String email) {
        User user = requireUser(email);
        return addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AddressDTO addAddress(String email, AddressRequest request) {
        User user = requireUser(email);

        Address address = new Address();
        address.setUser(user);
        apply(address, request);

        if (request.isDefault() || addressRepository.countByUserId(user.getId()) == 0) {
            markOnlyDefault(user, address);
        }

        return toDto(addressRepository.save(address));
    }

    @Transactional
    public AddressDTO updateAddress(String email, Long addressId, AddressRequest request) {
        User user = requireUser(email);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BadRequestException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only edit your own addresses");
        }

        apply(address, request);

        if (request.isDefault()) {
            markOnlyDefault(user, address);
        }

        return toDto(addressRepository.save(address));
    }

    @Transactional
    public ApiResponse<Void> deleteAddress(String email, Long addressId) {
        User user = requireUser(email);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BadRequestException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own addresses");
        }

        addressRepository.delete(address);
        return ApiResponse.ok("Address deleted", null);
    }

    private void apply(Address address, AddressRequest request) {
        address.setFullName(request.getFullName().trim());
        address.setPhone(request.getPhone().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setLandmark(request.getLandmark() == null || request.getLandmark().isBlank()
                ? null : request.getLandmark().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPostalCode(request.getPostalCode().trim());
        address.setCountry(request.getCountry() == null || request.getCountry().isBlank()
                ? "India" : request.getCountry().trim());
        try {
            address.setAddressType(request.getAddressType() == null || request.getAddressType().isBlank()
                    ? AddressType.HOME : AddressType.valueOf(request.getAddressType().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            address.setAddressType(AddressType.HOME);
        }
        address.setDefault(request.isDefault());
    }

    private void markOnlyDefault(User user, Address target) {
        addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(user.getId()).forEach(address -> {
            address.setDefault(address.getId().equals(target.getId()));
            addressRepository.save(address);
        });
    }

    public AddressDTO toDto(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setFullName(address.getFullName());
        dto.setPhone(address.getPhone());
        dto.setStreetAddress(address.getStreetAddress());
        dto.setLandmark(address.getLandmark());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        dto.setAddressType(address.getAddressType() == null ? null : address.getAddressType().name());
        dto.setDefault(address.isDefault());
        return dto;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Account not found"));
    }
}
