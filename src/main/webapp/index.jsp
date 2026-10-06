<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Online Banking System</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: #f1f5f9;
            color: #0f172a;
            margin: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
        }
        .container {
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
            padding: 2.5rem;
            max-width: 560px;
            text-align: center;
            border: 1px solid #e2e8f0;
        }
        h1 {
            color: #0f172a;
            font-size: 2rem;
            margin-bottom: 0.5rem;
        }
        p.subtitle {
            color: #64748b;
            margin-bottom: 2rem;
            font-size: 1rem;
        }
        .status-badge {
            display: inline-block;
            background-color: #dcfce7;
            color: #166534;
            padding: 0.35rem 0.75rem;
            border-radius: 9999px;
            font-size: 0.875rem;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }
        .links {
            display: flex;
            gap: 1rem;
            justify-content: center;
        }
        a.btn {
            display: inline-block;
            background-color: #0284c7;
            color: #ffffff;
            padding: 0.65rem 1.25rem;
            text-decoration: none;
            border-radius: 6px;
            font-weight: 500;
            transition: background-color 0.15s ease-in-out;
        }
        a.btn:hover {
            background-color: #0369a1;
        }
        a.btn-outline {
            background-color: transparent;
            color: #0284c7;
            border: 1px solid #0284c7;
        }
        a.btn-outline:hover {
            background-color: #f0f9ff;
        }
    </style>
</head>
<body>
    <div class="container">
        <span class="status-badge">Phase 1: Skeleton Active</span>
        <h1>Online Banking System</h1>
        <p class="subtitle">Enterprise Servlets + JSP + JDBC + MySQL Architecture on Tomcat 10.1</p>
        <div class="links">
            <a href="<c:url value='/hello'/>" class="btn">Test Hello Servlet</a>
        </div>
    </div>
</body>
</html>
