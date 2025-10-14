package com.tiximax.txm.Config;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Exception.AuthException;
import com.tiximax.txm.Service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class Filter extends OncePerRequestFilter {
    @Autowired
    TokenService tokenService;

    @Autowired
    SessionRegistry sessionRegistry;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private final List<String> AUTH_PERMISSION = List.of(
            "/accounts/login",
            "/otp/send",
            "/accounts/verify",
            "/accounts/register/staff",
            "/accounts/register/customer",
            "/accounts/update-all-passwords",
            "/accounts/login-google",
            "/accounts/callback",
            "/orders/{customerCode}/{routeId}",
            "/images/upload-image",
            "/websocket/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    private boolean isPermitted(String uri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return AUTH_PERMISSION.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        // Bỏ qua kiểm tra token cho các endpoint được phép, bao gồm /accounts/callback
//        String uri = request.getRequestURI();
//        System.out.println("URI: " + uri + " - Token from header: " + request.getHeader("Authorization")); // Log để check
//        if (isPermitted(uri) || uri.contains("/accounts/callback")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String token = getToken(request);
//        if (token == null) {
//            resolver.resolveException(request, response, null, new AuthException("Empty token!"));
//            return;
//        }
//
//        Account account;
//        try {
//            account = tokenService.extractAccount(token);
//        } catch (ExpiredJwtException expiredJwtException) {
//            resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
//            return;
//        } catch (MalformedJwtException malformedJwtException) {
//            resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
//            return;
//        }
//
//        UsernamePasswordAuthenticationToken authenToken =
//                new UsernamePasswordAuthenticationToken(account, token, account.getAuthorities());
//        authenToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(authenToken);
//        filterChain.doFilter(request, response);
//    }

//    public String getToken(HttpServletRequest request) {
//        // Kiểm tra header Authorization
//        String authHeader = request.getHeader("Authorization");
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            return authHeader.substring(7);
//        }
//
//        // Kiểm tra query parameter access_token (dành cho trường hợp OAuth2 callback)
//        String accessToken = request.getParameter("access_token");
//        if (accessToken != null) {
//            return accessToken;
//        }
//
//        return null;
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

    
    if (isPermitted(uri)) {
        filterChain.doFilter(request, response);
        return;
    }

    String token = getToken(request);
    if (token == null) {
        resolver.resolveException(request, response, null, new AuthException("Empty token!"));
        return;
    }

    try {
        
        Account account = tokenService.extractAccount(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (ExpiredJwtException e) {
        resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
        return;
    } catch (MalformedJwtException e) {
        resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
        return;
    }

    // ✅ Cho request đi tiếp
    filterChain.doFilter(request, response);
}

    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String uri = request.getRequestURI();
//        String authHeader = request.getHeader("Authorization");
//        logger.info("Request URI: {}, Method: {}, Authorization: {}" + uri +", " + request.getMethod() + "," + authHeader);
//        if (isPermitted(uri)) {
//            logger.info("Permitted URI, skipping token check: {}" + uri);
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String token = getToken(request);
//        if (token == null) {
//            logger.warn("No token found for URI: {}, redirecting to login if applicable" + uri);
//            resolver.resolveException(request, response, null, new AuthException("Empty token!"));
//            return;
//        }
//
//        Account account;
//        try {
//            account = tokenService.extractAccount(token);
//        } catch (ExpiredJwtException expiredJwtException) {
//            logger.error("Expired token for URI: {}" + uri);
//            resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
//            return;
//        } catch (MalformedJwtException malformedJwtException) {
//            logger.error("Invalid token for URI: {}" + uri);
//            resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
//            return;
//        }
//        UsernamePasswordAuthenticationToken authenToken =
//                new UsernamePasswordAuthenticationToken(account, token, account.getAuthorities());
//        authenToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(authenToken);
//        filterChain.doFilter(request, response);
//    }

}
