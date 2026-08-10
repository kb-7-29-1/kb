package com.salgosipo.global.config;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

/**
 * 모든 HTTP API 응답 데이터(JSON)를 자바 서블릿 레벨에서 GZIP으로 자동 75% 이상 압축하여 전송하는 자바 필터
 */
// @Component
// @WebFilter("/*")
public class GzipFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
        String uri = httpRequest.getRequestURI();
        boolean isPropertyApi = uri != null && uri.contains("/api/properties");

        if (isPropertyApi && acceptEncoding != null && acceptEncoding.contains("gzip")) {
            GzipResponseWrapper wrappedResponse = new GzipResponseWrapper(httpResponse);
            chain.doFilter(request, wrappedResponse);
            wrappedResponse.finish();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    private static class GzipResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private GZIPOutputStream gzipOutputStream;
        private ServletOutputStream servletOutputStream;
        private PrintWriter printWriter;

        public GzipResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (printWriter != null) {
                throw new IllegalStateException("getWriter() has already been called for this response");
            }
            if (servletOutputStream == null) {
                gzipOutputStream = new GZIPOutputStream(bos);
                servletOutputStream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                    }

                    @Override
                    public void write(int b) throws IOException {
                        gzipOutputStream.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        gzipOutputStream.write(b, off, len);
                    }
                };
            }
            return servletOutputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (servletOutputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called for this response");
            }
            if (printWriter == null) {
                gzipOutputStream = new GZIPOutputStream(bos);
                printWriter = new PrintWriter(
                        new OutputStreamWriter(gzipOutputStream, java.nio.charset.StandardCharsets.UTF_8));
            }
            return printWriter;
        }

        public void finish() throws IOException {
            if (printWriter != null) {
                printWriter.flush();
            }
            if (servletOutputStream != null) {
                servletOutputStream.flush();
            }
            if (gzipOutputStream != null) {
                gzipOutputStream.finish();
                gzipOutputStream.flush();
                byte[] bytes = bos.toByteArray();
                HttpServletResponse response = (HttpServletResponse) getResponse();

                if (bytes.length > 0) {
                    if (!response.isCommitted()) {
                        response.setHeader("Content-Encoding", "gzip");
                        response.setHeader("Content-Length", String.valueOf(bytes.length));
                    }
                    response.getOutputStream().write(bytes);
                }
                response.getOutputStream().flush();
            }
        }
    }
}
