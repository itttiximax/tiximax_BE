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
            "/accounts/register/staff",
            "/accounts/register/customer",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    private boolean isPermitted(String uri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return AUTH_PERMISSION.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
//        System.out.println(uri);
        if (isPermitted(uri)) {
            // yêu cầu truy cập 1 api => ai cũng truy cập đc
            String token = getToken(request);
            if (token != null) {
                Account account;
                try {
                    // từ token tìm ra thằng đó là ai
                    account = tokenService.extractAccount(token);
                } catch (ExpiredJwtException expiredJwtException) {
                    // token het han
                    resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
                    return;
                } catch (MalformedJwtException malformedJwtException) {
                    resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
                    return;
                }
                // token dung
                UsernamePasswordAuthenticationToken
                        authenToken =
                        new UsernamePasswordAuthenticationToken(account, token, account.getAuthorities());
                authenToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenToken);
//                HttpSession session = request.getSession(true); // Tạo session
//                sessionRegistry.registerNewSession(session.getId(), account); // Đăng ký session
            }
            filterChain.doFilter(request, response); // cho phép truy cập vô controller
        } else {
            String token = getToken(request);
            if (token == null) {
                resolver.resolveException(request, response, null, new AuthException("Empty token!"));
                return;
            }

            Account account;
            try {
                // từ token tìm ra thằng đó là ai
                account = tokenService.extractAccount(token);
            } catch (ExpiredJwtException expiredJwtException) {
                // token het han
                resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
                return;
            } catch (MalformedJwtException malformedJwtException) {
                resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
                return;
            }
            // token dung
            UsernamePasswordAuthenticationToken
                    authenToken =
                    new UsernamePasswordAuthenticationToken(account, token, account.getAuthorities());
            authenToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenToken);
            // token ok, cho vao`
//            HttpSession session = request.getSession(true); // Tạo session
//            sessionRegistry.registerNewSession(session.getId(), account); // Đăng ký session
            filterChain.doFilter(request, response);
        }
    }

    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.substring(7);
    }
}
