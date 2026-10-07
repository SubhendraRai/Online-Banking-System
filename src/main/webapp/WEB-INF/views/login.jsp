<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Sign In | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper">
    <div class="auth-card">
        <div class="auth-header">
            <div class="auth-icon-badge">&#128274;</div>
            <h1 class="auth-title">Welcome Back</h1>
            <p class="auth-subtitle">Sign in with your verified credentials</p>
        </div>

        <form action="<c:url value='/login'/>" method="post" autocomplete="on">
            <div class="form-group">
                <label for="email" class="form-label">Email Address</label>
                <input type="email" id="email" name="email" class="form-control"
                       placeholder="name@example.com" required autofocus
                       value="<c:out value='${sessionScope.savedEmail}'/>">
            </div>

            <div class="form-group">
                <label for="password" class="form-label">Password</label>
                <input type="password" id="password" name="password" class="form-control"
                       placeholder="Enter your account password" required autocomplete="current-password">
                <span class="form-hint">Account locks automatically after 5 consecutive failed attempts.</span>
            </div>

            <div class="form-group mt-3">
                <button type="submit" class="btn btn-primary btn-block">Sign In Securely</button>
            </div>
        </form>

        <div class="text-center mt-3">
            <p class="form-hint">
                Don't have an online banking account yet?
                <a href="<c:url value='/register'/>" class="nav-link" style="display:inline; padding:0; color:var(--color-accent);">Register here</a>
            </p>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
