<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Customer Portal | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="mb-3">
    <h1 style="font-size:1.85rem; font-weight:800; letter-spacing:-0.02em;">
        Welcome back, <c:out value="${sessionScope.user.name}"/>!
    </h1>
    <p style="color:var(--color-text-muted);">
        Account ID: <c:out value="${sessionScope.user.id}"/> &bull; Primary Email: <c:out value="${sessionScope.user.email}"/>
    </p>
</div>

<!-- Portfolio Metric Cards -->
<div class="grid grid-cols-4 mb-3">
    <div class="metric-card">
        <span class="metric-label">Total Balance</span>
        <span class="metric-value">
            $<c:out value="${not empty summary ? summary.totalBalance : '0.00'}"/>
        </span>
        <span class="metric-desc">Across all depository accounts</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Money In (Month)</span>
        <span class="metric-value" style="color:var(--color-success);">
            +$<c:out value="${not empty summary ? summary.moneyInThisMonth : '0.00'}"/>
        </span>
        <span class="metric-desc">Deposits & incoming transfers</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Money Out (Month)</span>
        <span class="metric-value" style="color:var(--color-danger);">
            -$<c:out value="${not empty summary ? summary.moneyOutThisMonth : '0.00'}"/>
        </span>
        <span class="metric-desc">Withdrawals & outbound transfers</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Linked Accounts</span>
        <span class="metric-value">
            <c:out value="${not empty summary ? summary.accounts.size() : '1'}"/>
        </span>
        <span class="metric-desc">Savings & current accounts</span>
    </div>
</div>

<!-- Depository Accounts Table -->
<div class="card mb-3">
    <div class="card-header">
        <div>
            <h2 class="card-title">My Accounts</h2>
            <p class="card-subtitle">Active accounts registered under your profile</p>
        </div>
        <div style="display:flex; gap:0.5rem;">
            <a href="<c:url value='/customer/transfer'/>" class="btn btn-primary btn-sm">Transfer Funds</a>
            <a href="<c:url value='/customer/transactions'/>" class="btn btn-secondary btn-sm">View Statement</a>
        </div>
    </div>
    <div class="card-body" style="padding:0;">
        <div class="table-responsive">
            <table class="table">
                <thead>
                    <tr>
                        <th>Account Number</th>
                        <th>Type</th>
                        <th>Status</th>
                        <th>Opening Date</th>
                        <th class="text-right">Available Balance</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty summary && not empty summary.accounts}">
                            <c:forEach var="acc" items="${summary.accounts}">
                                <tr>
                                    <td style="font-weight:700; font-family:monospace; font-size:1rem;">
                                        <c:out value="${acc.accountNumber}"/>
                                    </td>
                                    <td><c:out value="${acc.type}"/></td>
                                    <td>
                                        <span class="badge badge-active"><c:out value="${acc.status}"/></span>
                                    </td>
                                    <td><c:out value="${acc.createdAt}"/></td>
                                    <td class="text-right" style="font-weight:700; font-size:1.05rem;">
                                        $<c:out value="${acc.balance}"/>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="5" class="text-center" style="padding:2rem; color:var(--color-text-muted);">
                                    Default Savings Account active. Balance and ledger synchronization ready.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Placeholder Quick Actions Banner -->
<div class="card" style="background:linear-gradient(135deg, #1e3a8a, #2563eb); color:#ffffff; border:none;">
    <div class="card-body" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem;">
        <div>
            <h3 style="font-size:1.25rem; font-weight:700; margin-bottom:0.25rem;">Explore Digital Banking Services</h3>
            <p style="opacity:0.85; font-size:0.9rem;">High-interest fixed deposits, loan applications, and instant fund transfers.</p>
        </div>
        <div style="display:flex; gap:0.75rem;">
            <a href="<c:url value='/customer/transfer'/>" class="btn btn-secondary btn-sm" style="background:#ffffff; color:#1e3a8a; border:none;">
                New Fund Transfer
            </a>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
