package com.example.clarityspend.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 1. Check if the header exists and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Get the token part
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException e) {
                System.err.println("JWT token has expired");
            } catch (Exception e) {
                System.err.println("Error parsing JWT token");
            }
        }

        // 2. If we have a username and the user is not already authenticated...
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Load the user's details from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 4. Validate the token
            if (jwtUtil.validateToken(jwt, userDetails)) {

                // 5. If valid, manually set the authentication for Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // This line tells Spring: "This user is now authenticated for this request"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 6. Pass the request along to the next filter
        chain.doFilter(request, response);
    }
}