<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${empty pageTitle ? 'Online Banking System' : pageTitle}"/></title>
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body>
    <header class="navbar">
        <div class="container navbar-container">
            <a href="<c:url value='/'/>" class="navbar-brand">
                <span class="brand-icon">A</span>
                <span>Apex Banking</span>
            </a>

            <nav>
                <ul class="navbar-nav">
                    <c:choose>
                        <c:when test="${empty sessionScope.user}">
                            <li><a href="<c:url value='/login'/>" class="nav-link">Sign In</a></li>
                            <li><a href="<c:url value='/register'/>" class="btn btn-primary btn-sm">Open Account</a></li>
                        </c:when>
                        <c:when test="${sessionScope.user.role == 'CUSTOMER'}">
                            <li><a href="<c:url value='/customer/dashboard'/>" class="nav-link">Dashboard</a></li>
                            <li><a href="<c:url value='/customer/transfer'/>" class="nav-link">Transfers</a></li>
                            <li><a href="<c:url value='/customer/transactions'/>" class="nav-link">History</a></li>
                            <li>
                                <span class="user-badge">
                                    <span class="user-avatar">C</span>
                                    <c:out value="${sessionScope.user.name}"/>
                                </span>
                            </li>
                            <li><a href="<c:url value='/logout'/>" class="btn btn-outline btn-sm">Sign Out</a></li>
                        </c:when>
                        <c:when test="${sessionScope.user.role == 'ADMIN'}">
                            <li><a href="<c:url value='/admin/dashboard'/>" class="nav-link">Overview</a></li>
                            <li><a href="<c:url value='/admin/users'/>" class="nav-link">Users</a></li>
                            <li><a href="<c:url value='/admin/transactions'/>" class="nav-link">Transactions</a></li>
                            <li><a href="<c:url value='/admin/settings'/>" class="nav-link">Settings</a></li>
                            <li>
                                <span class="user-badge admin-badge">
                                    <span class="user-avatar admin-avatar">A</span>
                                    <c:out value="${sessionScope.user.name}"/>
                                </span>
                            </li>
                            <li><a href="<c:url value='/logout'/>" class="btn btn-outline btn-sm">Sign Out</a></li>
                        </c:when>
                    </c:choose>
                </ul>
            </nav>
        </div>
    </header>

    <div class="container mt-2">
        <c:if test="${not empty flash}">
            <div class="alert alert-<c:out value='${flash.type}'/>" role="alert">
                <span class="alert-icon">
                    <c:choose>
                        <c:when test="${flash.type == 'success'}">&#10004;</c:when>
                        <c:when test="${flash.type == 'danger' || flash.type == 'error'}">&#9888;</c:when>
                        <c:when test="${flash.type == 'warning'}">&#9888;</c:when>
                        <c:otherwise>&#8505;</c:otherwise>
                    </c:choose>
                </span>
                <span class="alert-message"><c:out value="${flash.message}"/></span>
            </div>
        </c:if>
    </div>

    <main class="main-content">
        <div class="container">
