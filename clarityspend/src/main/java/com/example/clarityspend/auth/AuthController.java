package com.example.clarityspend.auth;

import com.example.clarityspend.User;
import com.example.clarityspend.UserRepository;
import com.example.clarityspend.dto.AuthRequest;
import com.example.clarityspend.dto.AuthResponse;
import com.example.clarityspend.security.JwtUtil;
import com.example.clarityspend.security.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow frontend to call this
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // --- REGISTER ENDPOINT ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest authRequest) {
        // 1. Check if user already exists
        if (userRepository.findByUsername(authRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        // 2. Create new user's account
        User user = new User();
        user.setUsername(authRequest.getUsername());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword())); // 3. Encrypt password

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    // --- LOGIN ENDPOINT ---
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        // 1. Authenticate the user
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            // If authentication fails, throw an error
            throw new Exception("Incorrect username or password", e);
        }

        // 2. If authentication is successful, get user details
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());

        // 3. Generate a JWT token
        final String jwt = jwtUtil.generateToken(userDetails);

        // 4. Return the token in the response
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}