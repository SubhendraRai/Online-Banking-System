<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Admin Operations Console | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="mb-3">
    <h1 style="font-size:1.85rem; font-weight:800; letter-spacing:-0.02em;">
        System Administration Console
    </h1>
    <p style="color:var(--color-text-muted);">
        Authenticated Operator: <c:out value="${sessionScope.user.name}"/> &bull;
        Role: <span class="badge badge-admin"><c:out value="${sessionScope.user.role}"/></span> &bull;
        Session ID: Protected & Rotated
    </p>
</div>

<!-- Operational Metric Cards -->
<div class="grid grid-cols-4 mb-3">
    <div class="metric-card">
        <span class="metric-label">System Status</span>
        <span class="metric-value" style="color:var(--color-success); font-size:1.5rem;">
            &#9679; HEALTHY
        </span>
        <span class="metric-desc">Tomcat 10.1 / Jakarta EE 10</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Persistence</span>
        <span class="metric-value" style="font-size:1.5rem; color:var(--color-info);">
            MySQL 8
        </span>
        <span class="metric-desc">InnoDB ACID Transactions Active</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Concurrency Control</span>
        <span class="metric-value" style="font-size:1.5rem; color:var(--color-primary);">
            LockManager
        </span>
        <span class="metric-desc">Per-Account ReentrantLock FIFO</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Credential Security</span>
        <span class="metric-value" style="font-size:1.5rem; color:var(--color-warning);">
            BCrypt Cost 10
        </span>
        <span class="metric-desc">5-Attempt Lockout Defense</span>
    </div>
</div>

<!-- Administration Operational Modules -->
<div class="grid grid-cols-3 mb-3">
    <div class="card">
        <div class="card-body">
            <h3 class="card-title" style="margin-bottom:0.5rem;">User Management</h3>
            <p style="color:var(--color-text-muted); font-size:0.9rem; margin-bottom:1.25rem;">
                Provision new accounts, edit profiles, toggle user status (Active/Locked), and manage credentials.
            </p>
            <a href="<c:url value='/admin/users'/>" class="btn btn-secondary btn-sm btn-block">
                Manage Customers & Admins
            </a>
        </div>
    </div>

    <div class="card">
        <div class="card-body">
            <h3 class="card-title" style="margin-bottom:0.5rem;">Transaction Monitoring</h3>
            <p style="color:var(--color-text-muted); font-size:0.9rem; margin-bottom:1.25rem;">
                Audit ledger entries across accounts, search transaction references, and inspect transfer states.
            </p>
            <a href="<c:url value='/admin/transactions'/>" class="btn btn-secondary btn-sm btn-block">
                View Ledger Activity
            </a>
        </div>
    </div>

    <div class="card">
        <div class="card-body">
            <h3 class="card-title" style="margin-bottom:0.5rem;">System Settings</h3>
            <p style="color:var(--color-text-muted); font-size:0.9rem; margin-bottom:1.25rem;">
                Maintain daily transfer limits, minimum balance requirements, and operational parameters.
            </p>
            <a href="<c:url value='/admin/settings'/>" class="btn btn-secondary btn-sm btn-block">
                Configure Parameters
            </a>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
