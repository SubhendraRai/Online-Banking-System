<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Customer Dashboard | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="mb-3" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem;">
    <div>
        <h1 style="font-size:1.85rem; font-weight:800; letter-spacing:-0.02em;">
            Welcome back, <c:out value="${sessionScope.user.name}"/>!
        </h1>
        <p style="color:var(--color-text-muted);">
            Customer ID: #<c:out value="${sessionScope.user.id}"/> &bull; Email: <c:out value="${sessionScope.user.email}"/>
        </p>
    </div>
    <div style="display:flex; gap:0.5rem; flex-wrap:wrap;">
        <a href="<c:url value='/customer/deposit'/>" class="btn btn-secondary btn-sm">+ Deposit</a>
        <a href="<c:url value='/customer/withdraw'/>" class="btn btn-secondary btn-sm">- Withdraw</a>
        <a href="<c:url value='/customer/transfer'/>" class="btn btn-primary btn-sm">&#10148; Transfer Funds</a>
    </div>
</div>

<!-- Portfolio Metric Cards -->
<div class="grid grid-cols-4 mb-3">
    <div class="metric-card">
        <span class="metric-label">Total Portfolio Balance</span>
        <span class="metric-value">
            <c:out value="${not empty summary ? summary.formattedTotalBalance : '₹0.00'}"/>
        </span>
        <span class="metric-desc">Net aggregate across active accounts</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Money In (This Month)</span>
        <span class="metric-value" style="color:var(--color-success);">
            +<c:out value="${not empty summary ? summary.formattedMoneyInThisMonth : '₹0.00'}"/>
        </span>
        <span class="metric-desc">Deposits & incoming transfers</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Money Out (This Month)</span>
        <span class="metric-value" style="color:var(--color-danger);">
            -<c:out value="${not empty summary ? summary.formattedMoneyOutThisMonth : '₹0.00'}"/>
        </span>
        <span class="metric-desc">Withdrawals & outbound transfers</span>
    </div>

    <div class="metric-card">
        <span class="metric-label">Linked Accounts</span>
        <span class="metric-value">
            <c:out value="${not empty summary ? summary.accounts.size() : '0'}"/>
        </span>
        <span class="metric-desc"><a href="<c:url value='/customer/accounts'/>" style="color:var(--color-accent); font-weight:600; text-decoration:none;">Manage Accounts &rarr;</a></span>
    </div>
</div>

<!-- Depository Accounts Table -->
<div class="card mb-3">
    <div class="card-header">
        <div>
            <h2 class="card-title">My Accounts</h2>
            <p class="card-subtitle">Active depository accounts linked to your profile</p>
        </div>
        <div style="display:flex; gap:0.5rem;">
            <a href="<c:url value='/customer/accounts'/>" class="btn btn-outline btn-sm">+ Open New Account</a>
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
                        <th class="text-right">Available Balance</th>
                        <th class="text-right">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty summary && not empty summary.accounts}">
                            <c:forEach var="acc" items="${summary.accounts}">
                                <tr>
                                    <td style="font-weight:700; font-family:monospace; font-size:1rem;">
                                        <c:out value="${acc.accountNo}"/>
                                    </td>
                                    <td>
                                        <span class="badge badge-info"><c:out value="${acc.accountType}"/></span>
                                    </td>
                                    <td>
                                        <span class="badge badge-active"><c:out value="${acc.status}"/></span>
                                    </td>
                                    <td class="text-right" style="font-weight:700; font-size:1.05rem; color:var(--color-primary-dark);">
                                        <c:out value="${acc.formattedAvailableBalance}"/>
                                    </td>
                                    <td class="text-right">
                                        <a href="<c:url value='/customer/transactions?accountNo=${acc.accountNo}'/>" class="btn btn-secondary btn-sm" style="padding:0.25rem 0.6rem; font-size:0.8rem;">Statement</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="5" class="text-center" style="padding:2rem; color:var(--color-text-muted);">
                                    No active accounts found. <a href="<c:url value='/customer/accounts'/>">Open an account</a> to get started.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Last 5 Recent Transactions -->
<div class="card mb-3">
    <div class="card-header">
        <div>
            <h2 class="card-title">Recent Activity (Last 5 Transactions)</h2>
            <p class="card-subtitle">Latest postings across your accounts sorted newest first</p>
        </div>
        <div>
            <a href="<c:url value='/customer/transactions'/>" class="btn btn-secondary btn-sm">Full Transaction History &rarr;</a>
        </div>
    </div>
    <div class="card-body" style="padding:0;">
        <div class="table-responsive">
            <table class="table">
                <thead>
                    <tr>
                        <th>Txn ID</th>
                        <th>Date & Time</th>
                        <th>Type</th>
                        <th>Details / Remarks</th>
                        <th class="text-right">Amount</th>
                        <th class="text-center">Status</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty summary && not empty summary.recentTransactions}">
                            <c:forEach var="txn" items="${summary.recentTransactions}">
                                <tr>
                                    <td style="font-family:monospace; font-weight:700; color:var(--color-text-muted);">
                                        #<c:out value="${txn.txnId}"/>
                                    </td>
                                    <td><c:out value="${txn.createdAt}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${txn.txnType == 'DEPOSIT'}">
                                                <span class="badge badge-active">DEPOSIT</span>
                                            </c:when>
                                            <c:when test="${txn.txnType == 'WITHDRAWAL'}">
                                                <span class="badge badge-pending">WITHDRAW</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-info">TRANSFER</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div style="font-weight:600;"><c:out value="${txn.remarks}"/></div>
                                        <div style="font-size:0.8rem; color:var(--color-text-muted);">
                                            <c:if test="${not empty txn.fromAccount}">From: <c:out value="${txn.fromAccount}"/> </c:if>
                                            <c:if test="${not empty txn.toAccount}">To: <c:out value="${txn.toAccount}"/></c:if>
                                        </div>
                                    </td>
                                    <td class="text-right" style="font-weight:700; font-size:1rem;">
                                        <c:out value="${txn.formattedAmount}"/>
                                    </td>
                                    <td class="text-center">
                                        <span class="badge badge-active"><c:out value="${txn.status}"/></span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="6" class="text-center" style="padding:2rem; color:var(--color-text-muted);">
                                    No recent transactions recorded yet.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
