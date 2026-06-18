package com.smartreport.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Configuration
@ServletComponentScan
public class WebConfig {

    @WebFilter(urlPatterns = "/*", asyncSupported = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class ForceUtf8Filter implements Filter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setCharacterEncoding("UTF-8");
            chain.doFilter(req, new ForceUtf8Wrapper(response));
        }
    }

    public static class ForceUtf8Wrapper extends HttpServletResponseWrapper {
        public ForceUtf8Wrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setContentType(String type) {
            if (type != null && type.toLowerCase().contains("text/event-stream")) {
                super.setContentType(type);
                return;
            }
            if (type != null && !type.toLowerCase().contains("charset")) {
                super.setContentType(type + ";charset=UTF-8");
            } else {
                super.setContentType(type);
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return super.getOutputStream();
        }

        @Override
        public java.io.PrintWriter getWriter() throws IOException {
            return super.getWriter();
        }
    }
}
