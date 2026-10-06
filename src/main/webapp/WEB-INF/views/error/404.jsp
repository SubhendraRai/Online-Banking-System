<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Page Not Found | Online Banking</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: #f8fafc;
            color: #1e293b;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            margin: 0;
        }
        .error-card {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            padding: 2.5rem;
            max-width: 480px;
            text-align: center;
        }
        h1 {
            color: #0f172a;
            font-size: 3rem;
            margin: 0 0 0.5rem 0;
        }
        h2 {
            font-size: 1.25rem;
            margin: 0 0 1rem 0;
            color: #475569;
        }
        p {
            color: #64748b;
            line-height: 1.5;
            margin-bottom: 1.5rem;
        }
        a.btn {
            display: inline-block;
            background-color: #0284c7;
            color: #ffffff;
            padding: 0.6rem 1.2rem;
            text-decoration: none;
            border-radius: 6px;
            font-weight: 500;
        }
        a.btn:hover {
            background-color: #0369a1;
        }
    </style>
</head>
<body>
    <div class="error-card">
        <h1>404</h1>
        <h2>Resource Not Found</h2>
        <p>The page or resource you requested does not exist or has been moved.</p>
        <a href="<c:url value='/'/>" class="btn">Return Home</a>
    </div>
</body>
</html>
