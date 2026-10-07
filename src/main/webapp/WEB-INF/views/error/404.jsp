<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Page Not Found | Apex Banking System</title>
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-card text-center">
            <div class="auth-icon-badge" style="background:#fef2f2; color:#dc2626;">&#9888;</div>
            <h1 class="auth-title" style="font-size:2.5rem; color:#dc2626;">404</h1>
            <h2 style="font-size:1.25rem; font-weight:700; margin-bottom:0.75rem;">Page Not Found</h2>
            <p style="color:var(--color-text-muted); margin-bottom:2rem;">
                The resource or banking endpoint you requested does not exist or has been relocated.
            </p>
            <a href="<c:url value='/'/>" class="btn btn-primary btn-block">Return to Apex Banking</a>
        </div>
    </div>
</body>
</html>
