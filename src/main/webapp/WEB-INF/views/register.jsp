<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Open an Account | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper">
    <div class="auth-card auth-card-wide">
        <div class="auth-header">
            <div class="auth-icon-badge">&#128100;</div>
            <h1 class="auth-title">Open a New Account</h1>
            <p class="auth-subtitle">Instant onboarding with an automatically provisioned savings account</p>
        </div>

        <form action="<c:url value='/register'/>" method="post" autocomplete="on">
            <div class="form-grid-2">
                <div class="form-group">
                    <label for="fullName" class="form-label">Full Legal Name</label>
                    <input type="text" id="fullName" name="fullName" class="form-control"
                           placeholder="e.g. John Doe" required autofocus
                           value="<c:out value='${sessionScope.savedFullName}'/>">
                </div>

                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control"
                           placeholder="john.doe@example.com" required
                           value="<c:out value='${sessionScope.savedEmail}'/>">
                </div>
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label for="phone" class="form-label">Phone Number</label>
                    <input type="tel" id="phone" name="phone" class="form-control"
                           placeholder="10-digit telephone number" required
                           value="<c:out value='${sessionScope.savedPhone}'/>">
                </div>

                <div class="form-group">
                    <label for="initialDeposit" class="form-label">Initial Opening Deposit (₹)</label>
                    <input type="number" step="0.01" min="0" id="initialDeposit" name="initialDeposit"
                           class="form-control" placeholder="0.00 (optional)"
                           value="<c:out value='${sessionScope.savedDeposit}'/>">
                    <span class="form-hint">Default is ₹0.00; minimum balance rules apply per policy (₹500.00).</span>
                </div>
            </div>

            <div class="form-group">
                <label for="address" class="form-label">Residential Address</label>
                <input type="text" id="address" name="address" class="form-control"
                       placeholder="Street address, city, state, postal code" required
                       value="<c:out value='${sessionScope.savedAddress}'/>">
            </div>

            <div class="form-grid-2">
                <div class="form-group">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="At least 8 characters (letters + numbers)" required autocomplete="new-password"
                           pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$">
                    <span class="form-hint">Must contain at least 8 characters with letters and numbers.</span>
                </div>

                <div class="form-group">
                    <label for="confirmPassword" class="form-label">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                           placeholder="Re-enter password" required autocomplete="new-password">
                </div>
            </div>

            <div class="form-group mt-3">
                <button type="submit" class="btn btn-primary btn-block">Create Banking Account</button>
            </div>
        </form>

        <div class="text-center mt-3">
            <p class="form-hint">
                Already hold an Apex Banking profile?
                <a href="<c:url value='/login'/>" class="nav-link" style="display:inline; padding:0; color:var(--color-accent);">Sign in here</a>
            </p>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
