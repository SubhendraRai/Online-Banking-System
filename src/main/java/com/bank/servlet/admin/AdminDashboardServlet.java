package com.bank.servlet.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * System administrative dashboard servlet rendering back-office operations console.
 * <p>
 * Displays high-level system metrics, active service health, and shortcuts to administration modules.
 * </p>
 */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
    }
}
